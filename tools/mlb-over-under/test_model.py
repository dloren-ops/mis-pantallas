"""Tests del modelo over/under. Corren con: python3 test_model.py"""

import math

import params
from model import (
    Game,
    Pitcher,
    Team,
    american_to_prob,
    devig,
    evaluate,
    expected_value,
    prob_to_american,
    regressed_ra9,
    simulate,
    staff_ra9,
)

failures = []


def check(label, condition):
    if condition:
        print(f"  ok   {label}")
    else:
        print(f"  FAIL {label}")
        failures.append(label)


def close(a, b, tol=1e-6):
    return abs(a - b) <= tol


print("regresion de ERA a RA/9")

# Sin ERA cargado se cae al promedio de liga.
check("sin ERA usa la media de liga", close(regressed_ra9(Pitcher("TBD")), params.LEAGUE_RA9))

# Muchos innings => casi nada de regresion.
veteran = Pitcher("veterano", era=2.61, innings=150)
raw = 2.61 * params.ERA_TO_RA9
check("150 IP quedan cerca del ERA observado", regressed_ra9(veteran) - raw < 0.55)

# Pocos innings => fuerte regresion hacia la liga.
spot = Pitcher("emergencia", era=10.13, innings=10)
check("10 IP se regresan fuerte", regressed_ra9(spot) < 5.5)
check("pero siguen por encima de la liga", regressed_ra9(spot) > params.LEAGUE_RA9)

# Monotonia: mas innings con ERA alto => proyeccion mas alta.
check(
    "mas IP con ERA malo empeora la proyeccion",
    regressed_ra9(Pitcher("a", era=10.13, innings=60))
    > regressed_ra9(Pitcher("b", era=10.13, innings=10)),
)

print("mezcla abridor + bullpen")

team = Team("test", starter=Pitcher("as", era=2.00, innings=200), bullpen_ra9=6.00)
blended = staff_ra9(team)
check("la mezcla queda entre abridor y bullpen", 2.0 < blended < 6.0)

# Un abridor que cubre los 9 innings elimina al bullpen de la cuenta.
complete = Team(
    "completo",
    starter=Pitcher("as", era=2.00, innings=200, expected_ip=9.0),
    bullpen_ra9=6.00,
)
check("con 9 IP del abridor el bullpen no pesa", close(staff_ra9(complete), regressed_ra9(complete.starter)))

print("conversion de cuotas")

check("-110 implica 52.38%", close(american_to_prob(-110), 110 / 210))
check("+100 implica 50%", close(american_to_prob(100), 0.5))
check("+150 implica 40%", close(american_to_prob(150), 0.4))

over_p, under_p = devig(-110, -110)
check("devig de -110/-110 da 50/50", close(over_p, 0.5) and close(under_p, 0.5))
check("las probabilidades devigadas suman 1", close(over_p + under_p, 1.0))

over_p, under_p = devig(-124, 102)
check("devig asimetrico sigue sumando 1", close(over_p + under_p, 1.0))
check("el favorito conserva la mayor probabilidad", over_p > under_p)

check("prob_to_american invierte a american_to_prob", abs(american_to_prob(prob_to_american(0.62)) - 0.62) < 0.01)

print("valor esperado")

check("EV nulo en una moneda justa a +100", close(expected_value(0.5, 100), 0.0))
check("EV negativo al 50% pagando -110", expected_value(0.5, -110) < 0)
check("EV positivo con ventaja real", expected_value(0.60, -110) > 0)

print("simulacion")

p_over, p_under, avg = simulate(4.4, 4.4, 8.5, trials=20_000)
check("P(over) y P(under) suman 1", close(p_over + p_under, 1.0, tol=1e-9))
check("el total simulado sigue a las medias", abs(avg - 8.8) < 0.25)
check("con medias de 4.4 el over de 8.5 ronda 50%", 0.40 < p_over < 0.60)

# Una linea muy baja casi siempre se pasa; una muy alta casi nunca.
high, _, _ = simulate(4.4, 4.4, 2.5, trials=10_000)
low, _, _ = simulate(4.4, 4.4, 16.5, trials=10_000)
tail, _, _ = simulate(4.4, 4.4, 20.5, trials=10_000)
check("linea baja => over casi seguro", high > 0.95)
# La cola derecha del beisbol es gorda: los partidos de 17+ carreras
# ocurren alrededor del 5% del tiempo, asi que 16.5 no debe dar casi cero.
check("linea alta => over poco probable pero no nulo", 0.02 < low < 0.09)
check("la cola lejana si se apaga", tail < 0.03)

# La simulacion es determinista con la misma semilla.
a = simulate(4.4, 4.4, 8.5, trials=5_000)
b = simulate(4.4, 4.4, 8.5, trials=5_000)
check("misma semilla => mismo resultado", a == b)

# La sobredispersion tiene que superar a una Poisson pura.
check("la dispersion configurada es mayor que Poisson", params.RUNS_DISPERSION > 1.0)

print("evaluacion de un partido")

neutral = Game(
    away=Team("Away"),
    home=Team("Home"),
    venue="Estadio Inexistente",
    total_line=8.5,
)
res = evaluate(neutral, trials=20_000)
check("una sede desconocida cae en park factor neutral", close(params.park_factor("Estadio Inexistente"), 1.0))
check("el local anota mas con ofensivas iguales", res.home_runs > res.away_runs)
check("el total del modelo es la suma de ambos", close(res.model_total, res.away_runs + res.home_runs))
check("sin datos la confianza es baja", res.confidence == "baja")
check("el lado elegido es OVER o UNDER", res.side in ("OVER", "UNDER"))

# Un parque ofensivo tiene que subir el total proyectado.
hitter = Game(away=Team("A"), home=Team("H"), venue="Coors Field", total_line=8.5)
pitcher = Game(away=Team("A"), home=Team("H"), venue="Oracle Park", total_line=8.5)
check(
    "Coors proyecta mas carreras que Oracle",
    evaluate(hitter, trials=5_000).model_total > evaluate(pitcher, trials=5_000).model_total,
)

# Un abridor peor tiene que subir las carreras del rival.
good = Game(
    away=Team("A"),
    home=Team("H", starter=Pitcher("as", era=2.50, innings=180)),
    venue="Estadio Inexistente",
    total_line=8.5,
)
bad = Game(
    away=Team("A"),
    home=Team("H", starter=Pitcher("malo", era=6.50, innings=180)),
    venue="Estadio Inexistente",
    total_line=8.5,
)
check(
    "un abridor peor sube las carreras del rival",
    evaluate(bad, trials=5_000).away_runs > evaluate(good, trials=5_000).away_runs,
)

print()
if failures:
    print(f"{len(failures)} test(s) fallaron:")
    for name in failures:
        print(f"  - {name}")
    raise SystemExit(1)

print("todos los tests pasaron")
