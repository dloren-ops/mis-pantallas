#!/usr/bin/env python3
"""Corre el modelo over/under sobre un slate en JSON.

Uso:
    python3 run.py slate_2026-08-14.json
    python3 run.py slate_2026-08-14.json --min-edge 0.04
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

import params
from model import Game, Pitcher, Team, evaluate


def load_pitcher(raw: dict | None) -> Pitcher:
    if not raw:
        return Pitcher("TBD")
    return Pitcher(
        name=raw.get("name", "TBD"),
        era=raw.get("era"),
        innings=raw.get("ip", 0.0),
        expected_ip=raw.get("expected_ip", params.DEFAULT_STARTER_IP),
    )


def load_slate(path: Path) -> tuple[dict, list[Game]]:
    data = json.loads(path.read_text(encoding="utf-8"))
    games = []

    for raw in data["games"]:
        away = Team(
            name=raw["away"],
            offense_index=raw.get("away_offense_index", 1.00),
            bullpen_ra9=raw.get("away_bullpen_ra9", params.LEAGUE_BULLPEN_RA9),
            starter=load_pitcher(raw.get("away_starter")),
        )
        home = Team(
            name=raw["home"],
            offense_index=raw.get("home_offense_index", 1.00),
            bullpen_ra9=raw.get("home_bullpen_ra9", params.LEAGUE_BULLPEN_RA9),
            starter=load_pitcher(raw.get("home_starter")),
        )
        games.append(
            Game(
                away=away,
                home=home,
                venue=raw["venue"],
                total_line=raw["total_line"],
                over_odds=raw.get("over_odds", -110),
                under_odds=raw.get("under_odds", -110),
                notes=raw.get("notes", ""),
            )
        )

    return data, games


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("slate", type=Path, help="Archivo JSON del slate")
    parser.add_argument(
        "--min-edge",
        type=float,
        default=0.04,
        help="Edge minimo para marcar una jugada (default 0.04 = 4 puntos)",
    )
    parser.add_argument("--trials", type=int, default=40_000)
    args = parser.parse_args(argv)

    if not args.slate.exists():
        print(f"No existe el slate: {args.slate}", file=sys.stderr)
        return 1

    data, games = load_slate(args.slate)
    results = [evaluate(game, trials=args.trials) for game in games]

    print(f"\nMLB over/under — {data['date']}  ({len(games)} partidos)\n")
    # Se muestra siempre P(over), del modelo y del mercado, para que la
    # comparacion sea directa. Ojo: la media del modelo puede quedar por
    # encima de la linea y aun asi P(over) ser menor al 50%, porque la
    # distribucion de carreras esta sesgada a la derecha (la media supera
    # a la mediana). El lado sale de comparar probabilidades, no medias.
    header = (
        f"{'Partido':<26} {'Sede':<26} {'Linea':>6} {'Media':>6} "
        f"{'P(over)':>8} {'Mercado':>8} {'Lado':>6} {'Edge':>7} {'EV':>7}  Conf"
    )
    print(header)
    print("-" * len(header))

    for res in results:
        game = res.game
        matchup = f"{game.away.name} @ {game.home.name}"
        print(
            f"{matchup:<26} {game.venue:<26} {game.total_line:>6.1f} "
            f"{res.model_total:>6.2f} {res.p_over:>7.1%} {res.market_p_over:>7.1%} "
            f"{res.side:>6} {res.edge:>+6.1%} {res.ev:>+6.1%}  {res.confidence}"
        )

    plays = [
        r for r in results if r.edge >= args.min_edge and r.confidence != "baja"
    ]

    print(f"\nJugadas con edge >= {args.min_edge:.0%} y confianza no baja:")
    if not plays:
        print("  ninguna — el modelo no le gana al mercado con estos datos.")
    else:
        for res in sorted(plays, key=lambda r: -r.edge):
            game = res.game
            print(
                f"  {res.side} {game.total_line:.1f}  "
                f"{game.away.name} @ {game.home.name}  "
                f"(edge {res.edge:+.1%}, EV {res.ev:+.1%}, conf {res.confidence})"
            )

    low = [r for r in results if r.confidence == "baja"]
    print(
        f"\n{len(low)} de {len(results)} partidos tienen confianza baja: "
        "les falta el ERA de al menos un abridor."
    )
    print(
        "Ningun partido llega a confianza alta: no se pudo relevar la ofensiva "
        "(carreras por juego) de casi ningun equipo, asi que todos corren con "
        "una ofensiva promedio."
    )

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
