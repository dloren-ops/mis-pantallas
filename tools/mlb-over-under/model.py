"""Modelo over/under de carreras totales para partidos de MLB.

El flujo es:

  1. Se estima el RA/9 "real" de cada abridor regresando su ERA a la media
     de la liga segun cuantos innings lleva lanzados.
  2. Se mezcla ese abridor con su bullpen segun los innings que se espera
     que cubra cada uno, para obtener el RA/9 del staff completo.
  3. Se proyectan las carreras esperadas de cada equipo combinando su
     ofensiva, el staff rival, el park factor y la ventaja de local.
  4. Se simulan las carreras con una binomial negativa (Monte Carlo) para
     obtener la probabilidad de que el total supere la linea.
  5. Se compara esa probabilidad contra la linea de la casa sin vigorish
     para calcular el edge.
"""

from __future__ import annotations

import math
import random
from dataclasses import dataclass, field

import params


# --------------------------------------------------------------------------
# Entrada
# --------------------------------------------------------------------------


@dataclass
class Pitcher:
    """Abridor con sus estadisticas de la temporada en curso."""

    name: str
    era: float | None = None
    innings: float = 0.0
    expected_ip: float = params.DEFAULT_STARTER_IP

    @property
    def is_estimated(self) -> bool:
        """True cuando no hay ERA cargado y se usa el promedio de liga."""
        return self.era is None


@dataclass
class Team:
    """Equipo con su ofensiva y su bullpen."""

    name: str
    # Carreras por juego relativas a la liga. 1.00 = ofensiva promedio.
    offense_index: float = 1.00
    bullpen_ra9: float = params.LEAGUE_BULLPEN_RA9
    starter: Pitcher = field(default_factory=lambda: Pitcher("TBD"))


@dataclass
class Game:
    """Un partido con su linea de casa."""

    away: Team
    home: Team
    venue: str
    total_line: float
    over_odds: int = -110
    under_odds: int = -110
    notes: str = ""


# --------------------------------------------------------------------------
# Componentes del modelo
# --------------------------------------------------------------------------


def regressed_ra9(pitcher: Pitcher) -> float:
    """RA/9 del abridor, regresado a la media de la liga.

    Un ERA de 10.13 en 8 innings dice mucho menos que uno de 2.61 en 150,
    asi que el peso de lo observado crece con los innings lanzados.
    """
    if pitcher.era is None:
        return params.LEAGUE_RA9

    observed_ra9 = pitcher.era * params.ERA_TO_RA9
    weight = pitcher.innings / (pitcher.innings + params.REGRESSION_IP)
    return weight * observed_ra9 + (1 - weight) * params.LEAGUE_RA9


def staff_ra9(team: Team) -> float:
    """RA/9 combinado del abridor y el bullpen, ponderado por innings."""
    starter_ip = min(max(team.starter.expected_ip, 0.0), 9.0)
    relief_ip = 9.0 - starter_ip

    starter_runs = regressed_ra9(team.starter) * starter_ip
    relief_runs = team.bullpen_ra9 * relief_ip

    return (starter_runs + relief_runs) / 9.0


def expected_runs(offense: Team, defense: Team, park: float, is_home: bool) -> float:
    """Carreras esperadas de `offense` contra el staff de `defense`."""
    suppression = staff_ra9(defense) / params.LEAGUE_RA9
    runs = params.LEAGUE_RPG * offense.offense_index * suppression * park

    if is_home:
        runs *= params.HOME_FIELD_BOOST

    return runs


# --------------------------------------------------------------------------
# Simulacion
# --------------------------------------------------------------------------


def _sample_poisson(lam: float, rng: random.Random) -> int:
    """Muestra una Poisson por el metodo de Knuth (valido con lam chico)."""
    target = math.exp(-lam)
    product = 1.0
    count = 0
    while True:
        product *= rng.random()
        if product <= target:
            return count
        count += 1


def sample_runs(mean: float, rng: random.Random) -> int:
    """Muestra las carreras de un equipo con una binomial negativa.

    Se arma como mezcla gamma-Poisson: la varianza queda en
    `RUNS_DISPERSION * mean`, que refleja la sobredispersion real de las
    carreras de beisbol mucho mejor que una Poisson pura.
    """
    dispersion = params.RUNS_DISPERSION
    if dispersion <= 1.0 or mean <= 0:
        return _sample_poisson(max(mean, 0.0), rng)

    shape = mean / (dispersion - 1.0)
    scale = dispersion - 1.0
    lam = rng.gammavariate(shape, scale)
    return _sample_poisson(lam, rng)


def simulate(
    away_mean: float,
    home_mean: float,
    line: float,
    trials: int = 40_000,
    seed: int = 20260814,
) -> tuple[float, float, float]:
    """Simula el partido y devuelve (P(over), P(under), total promedio)."""
    rng = random.Random(seed)
    overs = 0
    pushes = 0
    total_runs = 0

    for _ in range(trials):
        combined = sample_runs(away_mean, rng) + sample_runs(home_mean, rng)
        total_runs += combined
        if combined > line:
            overs += 1
        elif combined == line:
            pushes += 1

    decided = trials - pushes
    p_over = overs / decided if decided else 0.0
    return p_over, 1.0 - p_over, total_runs / trials


# --------------------------------------------------------------------------
# Odds
# --------------------------------------------------------------------------


def american_to_prob(odds: int) -> float:
    """Convierte cuota americana a probabilidad implicita (con vig)."""
    if odds < 0:
        return -odds / (-odds + 100.0)
    return 100.0 / (odds + 100.0)


def devig(over_odds: int, under_odds: int) -> tuple[float, float]:
    """Saca el vigorish y devuelve las probabilidades justas del mercado."""
    raw_over = american_to_prob(over_odds)
    raw_under = american_to_prob(under_odds)
    booked = raw_over + raw_under
    return raw_over / booked, raw_under / booked


def prob_to_american(prob: float) -> int:
    """Convierte una probabilidad en su cuota americana justa."""
    if prob <= 0 or prob >= 1:
        return 0
    if prob >= 0.5:
        return round(-100 * prob / (1 - prob))
    return round(100 * (1 - prob) / prob)


def expected_value(prob: float, odds: int) -> float:
    """EV por unidad apostada dada una probabilidad y una cuota."""
    payout = (odds / 100.0) if odds > 0 else (100.0 / -odds)
    return prob * payout - (1 - prob)


# --------------------------------------------------------------------------
# Evaluacion
# --------------------------------------------------------------------------


@dataclass
class Result:
    game: Game
    away_runs: float
    home_runs: float
    model_total: float
    sim_total: float
    p_over: float
    p_under: float
    market_p_over: float
    edge: float
    side: str
    ev: float
    confidence: str


def _confidence(game: Game) -> str:
    """Etiqueta cuanta confianza merece la proyeccion segun los datos.

    El discriminante principal son los abridores: sin su ERA el partido se
    proyecta con un lanzador promedio y la estimacion vale poco. La ofensiva
    suma un escalon extra cuando se conoce la de ambos equipos.
    """
    known_starters = sum(
        1 for team in (game.away, game.home) if not team.starter.is_estimated
    )
    known_offense = sum(
        1 for team in (game.away, game.home) if team.offense_index != 1.00
    )

    if known_starters < 2:
        return "baja"
    if known_offense == 2:
        return "alta"
    return "media"


def evaluate(game: Game, trials: int = 40_000) -> Result:
    """Corre el modelo completo sobre un partido."""
    park = params.park_factor(game.venue)

    away_runs = expected_runs(game.away, game.home, park, is_home=False)
    home_runs = expected_runs(game.home, game.away, park, is_home=True)
    model_total = away_runs + home_runs

    p_over, p_under, sim_total = simulate(
        away_runs, home_runs, game.total_line, trials=trials
    )

    market_p_over, market_p_under = devig(game.over_odds, game.under_odds)

    if p_over >= market_p_over:
        side = "OVER"
        edge = p_over - market_p_over
        ev = expected_value(p_over, game.over_odds)
    else:
        side = "UNDER"
        edge = p_under - market_p_under
        ev = expected_value(p_under, game.under_odds)

    return Result(
        game=game,
        away_runs=away_runs,
        home_runs=home_runs,
        model_total=model_total,
        sim_total=sim_total,
        p_over=p_over,
        p_under=p_under,
        market_p_over=market_p_over,
        edge=edge,
        side=side,
        ev=ev,
        confidence=_confidence(game),
    )
