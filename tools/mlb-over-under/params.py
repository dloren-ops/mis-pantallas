"""Constantes de liga y park factors para el modelo over/under.

Todos los valores son aproximaciones publicas y regresadas a la media.
Estan centralizados aca para que se puedan calibrar sin tocar el modelo.
"""

# --- Constantes de liga (temporada 2026, aproximadas) ---

# Carreras anotadas por equipo por juego.
LEAGUE_RPG = 4.40

# Carreras permitidas por 9 innings a nivel liga.
LEAGUE_RA9 = 4.40

# ERA mide carreras limpias; RA/9 incluye las sucias. El multiplicador
# convierte de una a la otra.
ERA_TO_RA9 = 1.075

# RA/9 tipico de un bullpen promedio. Los bullpens rinden algo mejor que
# el promedio global porque trabajan en rafagas cortas.
LEAGUE_BULLPEN_RA9 = 4.25

# Innings esperados de un abridor promedio en 2026.
DEFAULT_STARTER_IP = 5.2

# Ventaja de local, expresada como multiplicador sobre las carreras del
# equipo de casa. Historicamente vale poco menos de un 2%.
HOME_FIELD_BOOST = 1.017

# Peso de la regresion a la media para el ERA de un abridor. Con IP innings
# lanzados, el peso de su ERA observado es IP / (IP + REGRESSION_IP).
# Un abridor con ~70 IP queda a mitad de camino entre su ERA y la liga.
REGRESSION_IP = 70.0

# Dispersion de las carreras por equipo por juego. La varianza real ronda
# 2.1 veces la media (las carreras estan sobredispersas respecto a Poisson).
RUNS_DISPERSION = 2.10


# --- Park factors de carreras (100 = neutral) ---
#
# Valores de tres temporadas, regresados. Un park factor de 105 significa
# que el estadio infla las carreras totales un 5%.

PARK_FACTORS = {
    "Dodger Stadium": 99,
    "Daikin Park": 101,
    "PNC Park": 97,
    "Comerica Park": 96,
    "Tropicana Field": 96,
    "Truist Park": 102,
    "Rogers Centre": 102,
    "Progressive Field": 98,
    "Oracle Park": 93,
    "Angel Stadium": 100,
    "Wrigley Field": 101,
    "Great American Ball Park": 105,
    "Citi Field": 96,
    "Sutter Health Park": 104,
    "Coors Field": 112,
    "Fenway Park": 104,
    "Yankee Stadium": 102,
    "Globe Life Field": 99,
    "Target Field": 100,
    "Petco Park": 95,
    "Busch Stadium": 97,
    "loanDepot park": 96,
    "American Family Field": 101,
    "T-Mobile Park": 92,
    "Kauffman Stadium": 101,
    "Guaranteed Rate Field": 101,
    "Camden Yards": 100,
    "Nationals Park": 100,
    "Chase Field": 102,
}

NEUTRAL_PARK_FACTOR = 100


def park_factor(venue: str) -> float:
    """Devuelve el park factor normalizado (1.00 = neutral) de una sede."""
    return PARK_FACTORS.get(venue, NEUTRAL_PARK_FACTOR) / 100.0
