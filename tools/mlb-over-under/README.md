# Modelo over/under de MLB

Modelo de carreras totales para partidos de MLB. Proyecta las carreras
esperadas de cada equipo, simula el partido y compara la probabilidad
resultante contra la linea de la casa para calcular el edge.

Solo usa la libreria estandar de Python (3.11+). No hay dependencias.

## Uso

```bash
cd tools/mlb-over-under
python3 run.py slate_2026-08-14.json
python3 run.py slate_2026-08-14.json --min-edge 0.05 --trials 100000
python3 test_model.py
```

## Como funciona

1. **Regresion del abridor.** El ERA se convierte a RA/9 y se regresa a la
   media de la liga con peso `IP / (IP + 70)`. Un abridor con 10 innings y
   10.13 de ERA termina proyectado cerca del promedio, no como un desastre.
2. **Mezcla con el bullpen.** Se pondera el RA/9 del abridor por los innings
   que se espera que cubra (5.2 por defecto) y el del bullpen por el resto.
3. **Carreras esperadas.** Se combinan ofensiva del equipo, capacidad de
   contencion del staff rival, park factor y ventaja de local.
4. **Simulacion.** 40.000 partidos con una binomial negativa (mezcla
   gamma-Poisson) con varianza `2.1 x media`. Es clave: las carreras de
   beisbol estan sobredispersas respecto a una Poisson, y la cola derecha
   gorda es lo que hace que los partidos de 17+ carreras pasen ~5% del
   tiempo.
5. **Edge.** Se le saca el vigorish a las cuotas de la casa y se compara la
   probabilidad justa del mercado contra la del modelo.

### Media contra mediana

La distribucion de carreras esta sesgada a la derecha, asi que la media
supera a la mediana. Un partido puede proyectar 8.88 carreras de media
contra una linea de 8.5 y aun asi tener P(over) por debajo del 50%. **El
lado se elige comparando probabilidades, nunca la media contra la linea.**

## Formato del slate

```json
{
  "date": "2026-08-14",
  "games": [
    {
      "away": "Rockies",
      "home": "Giants",
      "venue": "Oracle Park",
      "total_line": 7.5,
      "over_odds": -115,
      "under_odds": -105,
      "away_starter": { "name": "Kyle Freeland", "era": 6.63, "ip": 110 },
      "home_starter": { "name": "Landen Roupp", "era": 4.22, "ip": 140 },
      "home_offense_index": 1.05
    }
  ]
}
```

Campos opcionales por equipo: `away_offense_index` / `home_offense_index`
(carreras por juego relativas a la liga, 1.00 = promedio) y
`away_bullpen_ra9` / `home_bullpen_ra9`. Un abridor sin `era` se proyecta
como lanzador promedio.

## Niveles de confianza

| Nivel | Significado |
|-------|-------------|
| alta  | ERA de ambos abridores + ofensiva real de ambos equipos |
| media | ERA de ambos abridores, ofensiva en promedio de liga |
| baja  | falta el ERA de al menos un abridor |

## Limitaciones

Conviene tenerlas presentes antes de creerle a un numero:

- **La ofensiva es el agujero grande.** Sin `offense_index` cargado, todos
  los equipos batean como el promedio de la liga. En el slate del 14/08/2026
  eso significa que el modelo ignora, por ejemplo, que los Yankees venian
  bateando .208 y los Blue Jays .239. El modelo marca OVER en ese partido;
  el consenso publico marcaba UNDER justamente por esas rachas.
- **No hay clima.** Temperatura y viento mueven bastante los totales, sobre
  todo en Wrigley Field.
- **No hay lineups.** No contempla dias de descanso, lesiones ni bateador
  designado.
- **Los park factors son estaticos.** Son valores de tres temporadas, sin
  ajuste por condiciones del dia.
- **El bullpen es generico.** Todos arrancan en el promedio de liga salvo
  que se cargue `bullpen_ra9`, y no se modela el desgaste de los ultimos dias.
- **Los abridores no tienen todos IP reales.** En el slate incluido, los
  marcados con `ip_estimated` se estimaron a partir de record y ponches.

Un edge calculado sobre entradas incompletas no es un edge real. Con la
ofensiva sin cargar, tratar las salidas como exploratorias.
