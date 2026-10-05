# Gestion de citas medicas

Ejercicio de Arquitectura de Sistemas. Monolito modular **Spring Boot 3.3 + PostgreSQL 16** para
administrar la agenda de una clinica: consultar disponibilidad, reservar, cancelar y enviar
recordatorios, **sin permitir reservas duplicadas** en el mismo horario.

- No se guarda informacion clinica: solo agenda, datos de contacto y el motivo textual de la solicitud.
- La identidad y los roles se **simulan** con dos encabezados, no hay login ni tokens.

## Como levantar el proyecto

Requisitos: Docker Desktop (o Docker Engine + Compose v2).

```bash
docker compose up -d --build
docker compose ps          # esperar a que app y postgres estén "healthy"
```

| Servicio | Puerto en el host | Puerto interno |Nota|
|---|---|---|---|
| API (Spring Boot) | **8083** | 8080 | `http://localhost:8083` |
| PostgreSQL | **5443** | 5432 | `admin / secretpassword123`, base `citas_db` |

- Zona horaria por defecto: **America/Guayaquil** (`CITA_ZONA_HORARIA`, ver `.env.example`).
- Healthcheck: `curl http://localhost:8083/actuator/health`.
- Pagina web de demostracion: **http://localhost:8083/pagina/index.html** (`/` redirige alla).
- Los valores por defecto estan en `.env.example`; para personalizarlos copie ese archivo a `.env`
  (`.env` esta en `.gitignore` y no se versiona).

Datos de demostracion que se crean al iniciar (solo si la base esta vacia):

| Entidad | Documento | Detalle |
|---|---|---|
| Medico | MED-0001 | Ana Rivas, Medicina general, lunes a viernes 08:00-12:00 (slots de 30 min) |
| Medico | MED-0002 | Luis Ferrer, Cardiologia, lunes a viernes 13:00-17:00 |
| Paciente | CED-1001 | Maria Lopez, `+18095550201`, `maria.lopez@correo.local` |
| Paciente | CED-1002 | Jose Perez, `+18095550202`, `jose.perez@correo.local` |

## Como se evitan las reservas duplicadas

1. **Restriccion UNIQUE** en `cita (medico_id, fecha_hora, reserva_activa)`. Las citas canceladas
   ponen `reserva_activa = NULL`, por lo que el horario vuelve a liberarse sin romper la restriccion.
2. **Bloqueo pesimista** (`SELECT ... FOR UPDATE`) sobre el medico al reservar y sobre la cita al
   cancelar, de modo que dos transactions concurrentes se serializan.
3. La segunda transaccion que llega al mismo horario recibe **409 Conflict** con el codigo
   `HORARIO_OCUPADO`, ya sea por el bloqueo o por la restriccion de integridad.
4. Cada cambio de estado se escribe en `auditoria_cita` (reserva, cancelacion, notificaciones).

Tambien hay una prueba automatica que envia dos reservas simultaneas del mismo medico y horario y
comprueba que solo una gana ([seccion de pruebas](#pruebas)).

## Permisos por rol (simulacion)

No hay autenticacion real. Cada peticion a `/api/**` debe enviar:

| Encabezado | Valor |
|---|---|
| `X-Usuario-Id` | UUID del usuario. Para `PACIENTE` y `MEDICO` debe ser el id de su registro, porque el alcance se valida contra la cita. |
| `X-Rol` | `PACIENTE`, `RECEPCION`, `MEDICO` o `ADMIN` |

- Sin encabezados, o con valores invalidos, la respuesta es **401 NO_AUTENTICADO**.
- Con un rol que no corresponde al recurso, la respuesta es **403 ACCESO_DENEGADO**.
- **Todo 403 queda registrado** en `auditoria_acceso_denegado` (usuario, rol, metodo, ruta, motivo, IP y fecha)
  y se puede consultar con `GET /api/v1/auditoria/accesos-denegados` (solo `ADMIN`).
- El campo `created_by` / `updated_by` de las entidades y el campo `usuario` de la auditoria toman el
  valor `ROL:uuid` de quien hizo la peticion.

Alcance por rol:

| Rol | Puede hacer |
|---|---|
| `PACIENTE` | Ver **solo sus citas** y **cancelar solo sus citas**. No puede reservar ni listar pacientes. |
| `RECEPCION` | Ver, crear, modificar y cancelar **todas** las citas, gestionar medicos y pacientes, disparar el barrido de recordatorios y reintentar envios. |
| `MEDICO` | Ver **solo su agenda** (y la disponibilidad de su propia agenda). No puede cancelar citas ni auditar. |
| `ADMIN` | Ver todo, incluida la **auditoria** de citas (`GET /api/v1/citas/{id}/auditoria`) y de accesos denegados. |

Matriz por endpoint:

| Endpoint | PACIENTE | RECEPCION | MEDICO | ADMIN |
|---|---|---|---|---|
| `GET /api/v1/medicos`, `GET /api/v1/medicos/{id}` | si | si | si | si |
| `POST /api/v1/medicos`, `PUT /api/v1/medicos/{id}/desactivar` | no | si | no | si |
| `GET /api/v1/pacientes` | no | si | no | si |
| `GET /api/v1/pacientes/{id}` | solo la propia | si | no | si |
| `POST /api/v1/pacientes`, `PUT /api/v1/pacientes/{id}/desactivar` | no | si | no | si |
| `GET /api/v1/citas/disponibilidad` | si | si | solo su medico | si |
| `POST /api/v1/citas` (reservar) | no | si | no | si |
| `GET /api/v1/citas/{id}` | solo su cita | si | solo su agenda | si |
| `GET /api/v1/citas?pacienteId=` | solo su id | si | no | si |
| `GET /api/v1/citas?medicoId=` | no | si | solo su id | si |
| `PUT /api/v1/citas/{id}/cancelar` | solo su cita | si | no | si |
| `GET /api/v1/citas/{id}/auditoria`, `POST /api/v1/citas/{id}/auditoria` | no | no | no | si |
| `GET /api/v1/notificaciones?citaId=` | solo su cita | si | solo su agenda | si |
| `POST /api/v1/notificaciones/barrido`, `POST /api/v1/notificaciones/{id}/reintentar` | no | si | no | si |
| `GET /api/v1/auditoria/accesos-denegados` | no | no | no | si |

## Pruebas con curl

Preparacion (los ids de medicos y pacientes se leen del catalogo):

```bash
BASE=http://localhost:8083
RECEPCION_ID=11111111-1111-1111-1111-111111111111
ADMIN_ID=22222222-2222-2222-2222-222222222222

MEDICO_ID=$(curl -s "$BASE/api/v1/medicos" -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
PACIENTE_ID=$(curl -s "$BASE/api/v1/pacientes" -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" \
  | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
OTRO_PACIENTE_ID=$(curl -s "$BASE/api/v1/pacientes" -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" \
  | grep -o '"id":"[^"]*"' | sed -n '2p' | cut -d'"' -f4)
FECHA=$(date -d tomorrow +%F)
```

### 1. Buscar disponibilidad

La API trabaja y devuelve **instantes en UTC** (sufijo `Z`). `America/Guayaquil` es UTC-5, asi que las
08:00 locales son `13:00Z`: las 08:00 se escriben `2026-10-06T13:00:00Z` o, con offset explicito,
`2026-10-06T08:00:00-05:00` (es el mismo instante). Lo mas seguro es copiar el valor `inicio` que
devuelve la disponibilidad. El medico MED-0001 atiende de 08:00 a 12:00, de lunes a viernes.

```bash
curl -i -G "$BASE/api/v1/citas/disponibilidad" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" \
  --data-urlencode "medicoId=$MEDICO_ID" --data-urlencode "fecha=$FECHA"
```

Respuesta `200 OK`: cada slot trae `inicio`, `fin`, `duracionMinutos` y `disponible`.
Si la fecha es fin de semana la respuesta es `422 REGLA_NEGOCIO` (el medico no atiende ese dia).

### 2. Reservar

```bash
curl -i -X POST "$BASE/api/v1/citas" \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" \
  -d "{\"pacienteId\":\"$PACIENTE_ID\",\"medicoId\":\"$MEDICO_ID\",\"fechaHora\":\"${FECHA}T13:00:00Z\",\"motivo\":\"Consulta de control\"}"
```

Respuesta `201 Created` con el `id` de la cita. Guarde ese id:

```bash
CITA_ID=<id devuelto en el paso anterior>
```

### 3. Reserva duplicada: 409

Mismo medico, mismo horario, otro paciente, otra peticion:

```bash
curl -i -X POST "$BASE/api/v1/citas" \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" \
  -d "{\"pacienteId\":\"$OTRO_PACIENTE_ID\",\"medicoId\":\"$MEDICO_ID\",\"fechaHora\":\"${FECHA}T13:00:00Z\",\"motivo\":\"Intento duplicado\"}"
```

```json
{"timestamp":"...","status":409,"codigo":"HORARIO_OCUPADO",
 "mensaje":"El medico ya tiene una cita reservada para el 2026-10-05T13:00Z","ruta":"/api/v1/citas"}
```

### 4. Cancelar

Como el paciente dueño de la cita (solo puede cancelar las suyas):

```bash
curl -i -X PUT "$BASE/api/v1/citas/$CITA_ID/cancelar" \
  -H "Content-Type: application/json" \
  -H "X-Usuario-Id: $PACIENTE_ID" -H "X-Rol: PACIENTE" \
  -d '{"motivo":"No puedo asistir"}'
```

Respuesta `200 OK` con `"estado":"CANCELADA"`. Al cancelar, `reserva_activa` pasa a `NULL` y el
horario vuelve a quedar libre en la disponibilidad. Repetir la reserva del paso 2 ahora devuelve `201`.

### 5. Acceso denegado: 403

Un paciente intenta ver la agenda de otro paciente:

```bash
curl -i "$BASE/api/v1/citas?pacienteId=$OTRO_PACIENTE_ID" \
  -H "X-Usuario-Id: $PACIENTE_ID" -H "X-Rol: PACIENTE"
```

```json
{"timestamp":"...","status":403,"codigo":"ACCESO_DENEGADO",
 "mensaje":"El paciente solo puede consultar sus propias citas","ruta":"/api/v1/citas"}
```

Y el intento quedo auditado:

```bash
curl -s "$BASE/api/v1/auditoria/accesos-denegados" \
  -H "X-Usuario-Id: $ADMIN_ID" -H "X-Rol: ADMIN"
```

```json
[{"id":"...","usuarioId":"<id del paciente>","rol":"PACIENTE","metodo":"GET",
  "ruta":"/api/v1/citas","motivo":"El paciente solo puede consultar sus propias citas",
  "direccionIp":"172.18.0.1","fechaRegistro":"..."}]
```

Un `PACIENTE` que intenta reservar tambien recibe `403`, porque solo `RECEPCION` y `ADMIN` reservan:

```bash
curl -i -X POST "$BASE/api/v1/citas" -H "Content-Type: application/json" \
  -H "X-Usuario-Id: $PACIENTE_ID" -H "X-Rol: PACIENTE" -d "{\"pacienteId\":\"$PACIENTE_ID\",\"medicoId\":\"$MEDICO_ID\",\"fechaHora\":\"${FECHA}T15:00:00Z\"}"
```

Sin encabezados la respuesta es `401 NO_AUTENTICADO`:

```bash
curl -i "$BASE/api/v1/citas/$CITA_ID"
```

### 6. Dos reservas simultaneas: solo una gana

Dos peticiones en paralelo al mismo horario, con `curl --parallel` (una por paciente):

```bash
printf '{"pacienteId":"%s","medicoId":"%s","fechaHora":"%sT14:00:00Z"}' "$PACIENTE_ID" "$MEDICO_ID" "$FECHA" > a.json
printf '{"pacienteId":"%s","medicoId":"%s","fechaHora":"%sT14:00:00Z"}' "$OTRO_PACIENTE_ID" "$MEDICO_ID" "$FECHA" > b.json

curl --parallel -o r1.json -w "peticion 1 -> %{http_code}\n" -X POST "$BASE/api/v1/citas" \
  -H "Content-Type: application/json" -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" -d @a.json \
  --next --parallel -o r2.json -w "peticion 2 -> %{http_code}\n" -X POST "$BASE/api/v1/citas" \
  -H "Content-Type: application/json" -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" -d @b.json
```

```
peticion 1 -> 201
peticion 2 -> 409
```

Solo queda **una** cita en ese horario. La misma comprobacion automatizada esta en
[la seccion de pruebas](#pruebas).

### 7. Recordatorios con reintentos (`NOTIFICACIONES_SIMULAR_FALLO=true`)

Los canales (email y SMS) estan simulados. Al reservar se encola la confirmacion (`CONFIRMACION_RESERVA`)
y el barrido programa el recordatorio (`RECORDATORIO`) de las citas dentro de la ventana de aviso
(24 h por defecto). Si el envio falla queda `FALLIDO` con `proximoIntento` calculado por backoff
exponencial (`espera-reintento-inicial * 2^(intentos-1)`, 30 s por defecto) y el barrido siguiente lo
reintenta; al agotar `max-intentos` (4) pasa a `DESCARTADO` (carta muerta). Cada intento fallido
queda auditado como `NOTIFICACION_FALLIDA`.
Para que la demonstracion no tarde minutos se Bajan los tiempos:

```bash
NOTIFICACIONES_SIMULAR_FALLO=true \
NOTIFICACIONES_ESPERA_REINTENTO=5s \
NOTIFICACIONES_INTERVALO_BARRIDO_MS=10000 \
docker compose up -d --force-recreate app
```

```bash
# 1. Reservar una cita dentro de la ventana de aviso (24 h por defecto)
curl -s -X POST "$BASE/api/v1/citas" -H "Content-Type: application/json" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION" \
  -d "{\"pacienteId\":\"$PACIENTE_ID\",\"medicoId\":\"$MEDICO_ID\",\"fechaHora\":\"${FECHA}T13:30:00Z\",\"motivo\":\"Revision #fallar\"}" | tee /dev/stderr | grep -o '"id":"[^"]*"' | cut -d'"' -f4
CITA_ID=<id devuelto>

# 2. Programar y procesar recordatorios ahora
curl -i -X POST "$BASE/api/v1/notificaciones/barrido" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION"
# X-Notificaciones-Programadas: 1 ... X-Notificaciones-Enviadas: 0

# 3. Ver el fallo: estado FALLIDO, intentos 1, proximoIntento en el futuro
curl -s "$BASE/api/v1/notificaciones?citaId=$CITA_ID" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION"

# 4. Esperar el backoff y reintentar: ahora intentos 2, 3, 4 ... hasta DESCARTADO
sleep 12
curl -i -X POST "$BASE/api/v1/notificaciones/barrido" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION"
curl -s "$BASE/api/v1/notificaciones?citaId=$CITA_ID" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION"

# 5. La auditoria muestra un NOTIFICACION_FALLIDA por cada intento fallido
curl -s "$BASE/api/v1/citas/$CITA_ID/auditoria" \
  -H "X-Usuario-Id: $ADMIN_ID" -H "X-Rol: ADMIN"
```

Tambien se puede forzar un reintento manual de un envio (incluido uno que quedo `DESCARTADO`, que
reabre el intento y reinicia el contador):

```bash
curl -i -X POST "$BASE/api/v1/notificaciones/<id del envio>/reintentar" \
  -H "X-Usuario-Id: $RECEPCION_ID" -H "X-Rol: RECEPCION"
```

Para volver a la configuracion normal:

```bash
docker compose up -d --force-recreate app
```

El motivo de la solicitud tambien puede forzar el fallo sin la variable global: si el `motivo`
contiene `#fallar`, ese mensaje siempre se rechaza (`NOTIFICACIONES_DISPARADOR_FALLO`). Es la forma
mas rapida de ver el ciclo completo de reintentos sin reiniciar nada, porque la confirmacion incluye
el `motivo` en su cuerpo.

## Pagina web de demostracion

`static/index.html` se sirve en `http://localhost:8083/pagina/index.html`. Permite elegir el medico y la
fecha, ver los horarios libres, reservar uno y ver el `409` si el horario ya fue tomado. Los identificadores
y el rol se escriben en el formulario porque la pagina envia los mismos encabezados que los curl.
Tambien se puede abrir el archivo con doble clic: en ese caso hay que cambiar `const API` por
`http://localhost:8083` (y el navegador bloqueara el acceso si el origen no coincide).

## Pruebas

La prueba de concurrencia (`ReservaConcurrenteTest`) levanta la aplicacion completa contra un PostgreSQL
real, envia **dos reservas simultaneas** del mismo medico y horario y verifica que una responde `201`,
la otra `409 HORARIO_OCUPADO` y que al final existe una sola cita en ese horario.

Necesita una base de datos para pruebas (no toca `citas_db`):

```bash
docker compose exec postgres psql -U admin -d postgres -c "CREATE DATABASE citas_test"
```

Con Maven en el host:

```bash
mvn -B -ntp test        # usa jdbc:postgresql://localhost:5443/citas_test
```

O con Maven dentro de un contenedor, sin instalar nada:

```bash
docker run --rm -v "$PWD:/workspace" -w /workspace --network gestion-citas-medicas_default \
  -e TEST_DB_URL=jdbc:postgresql://postgres:5432/citas_test \
  maven:3.9-eclipse-temurin-21 mvn -B -ntp test
```

Resultado esperado: `Tests run: 1, Failures: 0, Errors: 0` y `BUILD SUCCESS`.

## Estructura

```
src/main/java/com/citasmedicas
  auditoria/      auditoria de citas y de accesos denegados (tabla propia, sin datos clinicos)
  cita/           disponibilidad, reserva, cancelacion; bloqueo pesimista y evento de dominio
  config/         propiedades, datos de demostracion, auditoria JPA, configuracion web
  medico/         medicos y su agenda semanal (coleccion de horarios)
  paciente/       pacientes
  notificacion/   programacion de recordatorios, canales simulados y reintentos con backoff
  seguridad/      roles simulados, filtro de identidad, interceptor de permisos y alcances
  shared/         entidad base, errores y manejador global de errores
static/index.html pagina de demostracion
```

## Comandos utiles

```bash
docker compose logs -f app          # ver recordatorios, reintentos y errores
docker compose exec postgres psql -U admin -d citas_db -c "select * from cita"
docker compose exec postgres psql -U admin -d citas_db -c "select accion, detalle from auditoria_cita order by fecha_registro desc limit 10"
docker compose down -v              # detener y borrar el volumen de datos
```
