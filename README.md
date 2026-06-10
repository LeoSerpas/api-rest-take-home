# API REST — Procesamiento de Transacciones

API REST desarrollada con Spring Boot para el registro, consulta y procesamiento asíncrono de transacciones posiblemente financieras provenientes de sistemas externos.

## Descripción de la solución

El sistema expone una API que permite a sistemas externos registrar transacciones, consultarlas con filtros y paginación, ver su historial de eventos y reprocesarlas manualmente en caso de fallo.

Internamente, un scheduler se ejecuta cada 10 segundos y procesa de forma automática todas las transacciones pendientes (`RECEIVED` y `RETRY_PENDING`). El procesamiento simula un resultado aleatorio (70 % éxito, 30 % fallo) y registra cada cambio de estado como un evento en la tabla `transaction_events`, construyendo así un historial de auditoría completo.

### Arquitectura

───────────────────────────────────────────────────────
|                   TransactionController              
|           POST /api/transactions                     
|           GET  /api/transactions                     
|           GET  /api/transactions/{id}                
|           POST /api/transactions/{id}/reprocess      
───────────────────────────────────────────────────────
                         │
───────────────────────────────────────────────────────
|                   TransactionService                 
|  register() · findAll() · findById()                 
|  process()  · reprocess()                            
───────────────────────────────────────────────────────
           │                          │
───────────────────────  ──────────────────────────────
| TransactionRepository   TransactionEventRepository
───────────────────────  ─────────────────────────────
           │                          │
──────────────────────────────────────────────────────
|                     MySQL Database                  
|         transactions · transaction_events           
───────────────────────────────────────────────────────

───────────────────────────────────────────────────────
|            TransactionProcessor (@Scheduled)         
|    Cada 10 s: procesa RECEIVED y RETRY_PENDING       
───────────────────────────────────────────────────────

### Flujo de estados

RECEIVED → PROCESSING → PROCESSED
                      ↘ FAILED
                      ↘ RETRY_PENDING → PROCESSING → ... 
                      (aquí el flujo puede volver a repetirse hasta terminar en PROCESSED o FAILED)

## Stack tecnológico

| Tecnología | Versión |
|------------|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Data JPA | (incluido en Boot) |
| MySQL Connector/J | (incluido en Boot) |
| Lombok | (incluido en Boot) |
| SpringDoc OpenAPI (Swagger UI) | 2.8.9 |
| JaCoCo (cobertura de tests) | 0.8.12 |
| Maven | 3.x |

## Cómo ejecutar el proyecto

### Con Docker (recomendado)
Requiere tener Docker Desktop instalado y corriendo.

```bash
docker-compose up --build
```

La aplicación estará disponible en:
- **API:** http://localhost:8080/api/transactions
- **Swagger:** http://localhost:8080/swagger-ui/index.html

Para detener:
```bash
docker-compose down
```

---

### Sin Docker (local)

### Prerrequisitos

- Java 21 instalado
- MySQL 8.x corriendo en `localhost:3306`
- Maven 3.x instalado (o usar el wrapper `mvnw` incluido)

### 1. Crear la base de datos

```sql
CREATE DATABASE trx_api_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

> Las tablas se crean automáticamente al iniciar la aplicación (`spring.jpa.hibernate.ddl-auto=update`).

### 2. Configurar credenciales para acceder a la base de datos local

Editar `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3306/trx_api_db
spring.datasource.username=root
spring.datasource.password=tu_password   (En mi caso tengo el password vacío)
```

### 3. Compilar y ejecutar

```bash
# Con Maven instalado
mvn spring-boot:run
```

La aplicación arranca en `http://localhost:8080`.

### Swagger UI

Una vez levantada la aplicación, la documentación interactiva está disponible en:

http://localhost:8080/swagger-ui/index.html

---

## Cómo ejecutar pruebas

### Ejecutar todos los tests

```bash
mvn test
```

### Ejecutar tests de un paquete específico 
Solo se crearon Test para 3 paquetes que conforman el 92% de los test de la aplicación según el reporte Jacoco.

```bash
# Solo tests del servicio
mvn test -Dtest="TransactionServiceTest"

# Solo tests del exception handler
mvn test -Dtest="GlobalExceptionHandlerTest"

# Solo tests del scheduler
mvn test -Dtest="TransactionProcessorTest"
```

### Generar reporte de cobertura (JaCoCo)

```bash
mvn test
```

El reporte HTML se genera automáticamente en la raíz del proyecto y entrando a:

target/site/jacoco/index.html

El archivo Index se debe abrir en cualquier navegador y se podrá ver la cobertura de los diferentes paquetes.
Si se agregan más test unitarios, se debe volver a correr 'mvn test' para actualizar la cobertura.

### Estructura de tests

```
src/test/java/com/ols/
├── ApiRestTakeHomeApplicationTests.java   — carga del contexto Spring
├── service/
│   └── TransactionServiceTest.java        — lógica de negocio (register, findAll, findById, process, reprocess)
├── exception/
│   └── GlobalExceptionHandlerTest.java    — manejo de errores HTTP
└── scheduler/
    └── TransactionProcessorTest.java      — procesamiento programado
```

---

## Endpoints disponibles

| Método | Ruta | Descripción | Código éxito |
|---|---|---|---|
| `POST` | `/api/transactions` | Registrar nueva transacción | `201 Created` |
| `GET` | `/api/transactions` | Listar transacciones con filtros y paginación | `200 OK` |
| `GET` | `/api/transactions/{id}` | Detalle de una transacción con historial de eventos | `200 OK` |
| `POST` | `/api/transactions/{id}/reprocess` | Reprocesar manualmente una transacción fallida | `200 OK` |

> **Nota:** Todas las respuestas incluyen el header `X-Trace-Id` con un identificador de 8 caracteres para correlacionar logs del request en consola.

### Parámetros de filtro — `GET /api/transactions`

| Parámetro | Tipo | Descripción |
|---|---|---|
| `status` | `TransactionStatus` | `RECEIVED`, `PROCESSING`, `PROCESSED`, `FAILED`, `RETRY_PENDING` |
| `transactionType` | `TransactionType` | `PAYMENT`, `TRANSFER`, `REFUND`, `WITHDRAWAL`, `DEPOSIT` |
| `sourceSystem` | `String` | Sistema origen (ej. `SYSTEM_A`) |
| `dateFrom` | `LocalDateTime` | Fecha desde, en formato ISO 8601: `2026-06-01T00:00:00` |
| `dateTo` | `LocalDateTime` | Fecha hasta, en formato ISO 8601: `2026-06-30T23:59:59` |
| `page` | `int` | Número de página (default: `0`) |
| `size` | `int` | Registros por página (default: `10`) |

---

## Manejo de errores

Todos los errores siguen la misma estructura de respuesta:

```json
{
  "timestamp": "2026-06-09T10:30:00.000",
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del error"
}
```

---

### POST /api/transactions — Errores esperados

#### Campos vacíos o nulos (`400 Bad Request`)

| Campo | Condición | Mensaje |
|---|---|---|
| `externalId` | Ausente, nulo o vacío (`""`) | `externalId: El identificador externo es obligatorio` |
| `transactionType` | Ausente o nulo | `transactionType: El tipo de transacción es obligatorio` |
| `sourceSystem` | Ausente, nulo o vacío (`""`) | `sourceSystem: El sistema origen es obligatorio` |
| `receivedAt` | Ausente o nulo | `receivedAt: La fecha/hora de recepción es obligatoria` |
| `payload` | Ausente, nulo o vacío (`""`) | `payload: El payload no puede estar vacío` |

> Si se envían varios campos inválidos a la vez, el mensaje concatena todos los errores separados por `, `.

#### Valor inválido en enum (`400 Bad Request`)

| Campo | Ejemplo de valor inválido | Mensaje |
|---|---|---|
| `transactionType` | `"COMPRA"` | `El campo 'transactionType' tiene un valor inválido. Los valores permitidos son: REFUND, TRANSFER, WITHDRAWAL, PAYMENT, DEPOSIT` |

#### Formato de fecha inválido (`400 Bad Request`)

| Campo | Ejemplo de valor inválido | Mensaje |
|---|---|---|
| `receivedAt` | `"09-06-2026"` / `"2026/06/09"` / `"hoy"` | `El campo 'receivedAt' tiene un formato de fecha inválido. El formato esperado es: yyyy-MM-ddTHH:mm:ss (Ejemplo: 2026-06-07T17:00:00)` |

#### Idempotencia — `externalId` duplicado (`409 Conflict`)

| Condición | Mensaje |
|---|---|
| El `externalId` ya existe en la base de datos | `La transacción con externalId 'EXT-001' ya fue registrada.` |

---

### GET /api/transactions — Errores esperados

Los filtros son opcionales. Si se envía un valor inválido en alguno de ellos se retorna `400 Bad Request`.

#### Valor inválido en enum como parámetro de URL (`400 Bad Request`)

| Parámetro | Ejemplo de valor inválido | Mensaje |
|---|---|---|
| `status` | `?status=ACTIVO` | `El parámetro 'status' tiene un valor inválido: 'ACTIVO'.` |
| `transactionType` | `?transactionType=COMPRA` | `El parámetro 'transactionType' tiene un valor inválido: 'COMPRA'.` |

#### Formato de fecha inválido como parámetro de URL (`400 Bad Request`)

| Parámetro | Ejemplo de valor inválido | Mensaje |
|---|---|---|
| `dateFrom` | `?dateFrom=09-06-2026` | `El campo 'dateFrom' tiene un valor inválido.` |
| `dateTo` | `?dateTo=mañana` | `El campo 'dateTo' tiene un valor inválido.` |

> El formato correcto para fechas en parámetros de URL es ISO 8601: `2026-06-01T00:00:00`

---

### GET /api/transactions/{id} — Errores esperados

| Condición | Código | Mensaje |
|---|---|---|
| El `id` no existe en la base de datos | `404 Not Found` | `No se encontró la transacción con id: 99` |

---

### POST /api/transactions/{id}/reprocess — Errores esperados

| Condición | Código | Mensaje |
|---|---|---|
| El `id` no existe en la base de datos | `404 Not Found` | `No se encontró la transacción con id: 99` |
| La transacción existe pero su estado no es `FAILED` | `409 Conflict` | `La transacción con id: 5 no puede reprocesarse. Estado actual: PROCESSED. Solo se pueden reprocesar transacciones en estado FAILED.` |

> Solo las transacciones en estado `FAILED` admiten reproceso manual. Las transacciones en `RETRY_PENDING` las maneja automáticamente el scheduler.

---

### Error genérico (`500 Internal Server Error`)

Ante cualquier error no controlado, la respuesta siempre oculta el detalle interno:

```json
{
  "timestamp": "2026-06-09T10:30:00.000",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Ocurrió un error inesperado"
}
```

---

## Colección de Postman

La colección de Postman con todos los endpoints preconfigurados se encuentra en la carpeta `docs/`:

```
docs/
└── api-rest-take-home.postman_collection.json
```

Importarla en Postman: **File → Import** y seleccionar el archivo. La colección incluye ejemplos de todos los endpoints con requests de éxito y casos de error.

---

## Ejemplos de requests/responses

### POST /api/transactions — Registrar transacción

**Se accede a través del Request:**
```http
POST /api/transactions
Content-Type: application/json

{
  "externalId": "EXT-20260609-001",
  "transactionType": "PAYMENT",
  "sourceSystem": "SYSTEM_A",
  "receivedAt": "2026-06-09T10:30:00",
  "payload": "{\"amount\": 1500.00, \"currency\": \"USD\"}"
}
```
- La respuesta esperada es la siguiente:
**Response `201 Created`:**
```json
{
  "id": 1,
  "externalId": "EXT-20260609-001",
  "transactionType": "PAYMENT",
  "sourceSystem": "SYSTEM_A",
  "receivedAt": "2026-06-09T10:30:00",
  "createdAt": "2026-06-09T10:30:01.123",
  "status": "RECEIVED"
}
```

**Response `409 Conflict` — externalId duplicado:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "La transacción con externalId 'EXT-20260609-001' ya fue registrada.",
  "timestamp": "2026-06-09T18:30:11.1174875"
}
```

**Response `400 Bad Request` — campos faltantes o inválidos:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "externalId: El identificador externo es obligatorio",
  "timestamp": "2026-06-09T18:31:10.890996"
}
```

---

### GET /api/transactions — Listar con filtros

**Request (filtro por tipo y estado):**
```http
GET /api/transactions?status=RECEIVED&transactionType=PAYMENT&page=0&size=5
```

**Request (filtro por estado FAILED con paginación — útil para encontrar candidatos a reprocesar):**
```http
GET /api/transactions?status=FAILED&page=0&size=2
```
- Recordando que:

| Parámetro | Tipo | Descripción |
|---|---|---|
| `status` | `TransactionStatus` | `RECEIVED`, `PROCESSING`, `PROCESSED`, `FAILED`, `RETRY_PENDING` |
| `page` | `int` | Número de página (default: `0`) |
| `size` | `int` | Registros por página (default: `10`) |

- La respuesta esperada con un solo filtro es la siguiente:
**Response `200 OK`:**
```json
{
    "content": [
        {
            "id": 17,
            "externalId": "TRX-017",
            "transactionType": "PAYMENT",
            "status": "FAILED",
            "sourceSystem": "SISTEMA-A",
            "receivedAt": "2026-06-07T17:00:00",
            "createdAt": "2026-06-08T16:47:15.116579"
        },
        {
            "id": 10,
            "externalId": "TRX-009",
            "transactionType": "PAYMENT",
            "status": "FAILED",
            "sourceSystem": "SISTEMA-A",
            "receivedAt": "2026-06-07T17:00:00",
            "createdAt": "2026-06-08T16:31:56.634596"
        }
    ],
    "empty": false,
    "first": true,
    "last": false,
    "number": 0,
    "numberOfElements": 2,
    "pageable": {
        "offset": 0,
        "pageNumber": 0,
        "pageSize": 2,
        "paged": true,
        "sort": {
            "empty": false,
            "sorted": true,
            "unsorted": false
        },
        "unpaged": false
    },
    "size": 2,
    "sort": {
        "empty": false,
        "sorted": true,
        "unsorted": false
    },
    "totalElements": 4,
    "totalPages": 2
}

```

### Cabe aclarar que se pueden combinar las peticiones como se muestra a continuación:

```http
# Un solo filtro
GET /api/transactions?status=FAILED

# Dos filtros combinados
GET /api/transactions?status=FAILED&transactionType=PAYMENT

# Tres filtros combinados
GET /api/transactions?status=FAILED&transactionType=PAYMENT&sourceSystem=SISTEMA-A

# Todos combinados
GET /api/transactions?status=FAILED&transactionType=PAYMENT&sourceSystem=SISTEMA-A&dateFrom=2026-06-01T00:00:00&dateTo=2026-06-09T23:59:59&page=0&size=10
```

Todas estas peticiones se pueden hacer correctamente como se pide en el punto 4.2 Consultar transacciones.

---

### GET /api/transactions/{id} — Detalle con historial de eventos

**Se accede a través del Request:**
```http
GET /api/transactions/19
```
La respuesta esperada es la siguiente:
**Response `200 OK`:**
```json
{
    "id": 19,
    "externalId": "TRX-019",
    "transactionType": "PAYMENT",
    "sourceSystem": "SISTEMA-A",
    "receivedAt": "2026-06-07T17:00:00",
    "createdAt": "2026-06-08T16:58:46.60967",
    "payload": "{ \"amount\": 100.00, \"currency\": \"USD\" }",
    "status": "PROCESSED",
    "errorMessage": "Error simulado durante el procesamiento",
    "events": [
        {
            "id": 39,
            "status": "PROCESSING",
            "description": "Iniciando procesamiento",
            "createdAt": "2026-06-08T16:58:53.948937"
        },
        {
            "id": 40,
            "status": "FAILED",
            "description": "Transacción fallida",
            "createdAt": "2026-06-08T16:58:53.954438"
        },
        {
            "id": 46,
            "status": "FAILED",
            "description": "Reproceso solicitado manualmente",
            "createdAt": "2026-06-08T17:59:47.809885"
        },
        {
            "id": 47,
            "status": "PROCESSING",
            "description": "Iniciando procesamiento",
            "createdAt": "2026-06-08T17:59:47.841892"
        },
        {
            "id": 48,
            "status": "PROCESSED",
            "description": "Transacción procesada exitosamente",
            "createdAt": "2026-06-08T17:59:47.847025"
        }
    ]
}
```

**Response `404 Not Found`:**
```json
{
  "timestamp": "2026-06-09T10:31:00.000",
  "status": 404,
  "error": "Not Found",
  "message": "No se encontró la transacción con id: 99"
}
```

---

### POST /api/transactions/{id}/reprocess — Reprocesar transacción fallida

**Se accede a través del Request:**
```http
POST /api/transactions/9/reprocess
```
- La respuesta esperada es la siguiente:

**Response `200 OK`:** mismo formato que el detalle (`GET /api/transactions/{id}`), con el historial actualizado incluyendo el nuevo intento.

Debería verse así:
```json
{
    "id": 9,
    "externalId": "TRX-008",
    "transactionType": "PAYMENT",
    "sourceSystem": "SISTEMA-A",
    "receivedAt": "2026-06-07T17:00:00",
    "createdAt": "2026-06-08T16:30:46.722271",
    "payload": "{ \"amount\": 100.00, \"currency\": \"USD\" }",
    "status": "FAILED",
    "errorMessage": "Error simulado durante el procesamiento",
    "events": [
        {
            "id": 17,
            "status": "PROCESSING",
            "description": "Iniciando procesamiento",
            "createdAt": "2026-06-08T16:30:51.68488"
        },
        {
            "id": 18,
            "status": "RETRY_PENDING",
            "description": "Fallo temporal, reintento pendiente",
            "createdAt": "2026-06-08T16:30:51.694379"
        },
        {
            "id": 19,
            "status": "PROCESSING",
            "description": "Iniciando procesamiento",
            "createdAt": "2026-06-08T16:31:01.713874"
        },
        {
            "id": 20,
            "status": "FAILED",
            "description": "Transacción fallida",
            "createdAt": "2026-06-08T16:31:01.721772"
        },
        {
            "id": 49,
            "status": "FAILED",
            "description": "Reproceso solicitado manualmente",
            "createdAt": "2026-06-08T18:00:58.238367"
        },
        {
            "id": 50,
            "status": "PROCESSING",
            "description": "Iniciando procesamiento",
            "createdAt": "2026-06-08T18:00:58.245867"
        },
        {
            "id": 51,
            "status": "FAILED",
            "description": "Transacción fallida",
            "createdAt": "2026-06-08T18:00:58.252576"
        },
        {
            "id": 58,
            "status": "FAILED",
            "description": "Reproceso solicitado manualmente",
            "createdAt": "2026-06-09T19:38:24.9308462"
        },
        {
            "id": 59,
            "status": "PROCESSING",
            "description": "Iniciando procesamiento",
            "createdAt": "2026-06-09T19:38:24.9393445"
        },
        {
            "id": 60,
            "status": "PROCESSED",
            "description": "Transacción procesada exitosamente",
            "createdAt": "2026-06-09T19:40:15.490265"
        }
    ]
}
```

**Response `409 Conflict` — la transacción no está en estado FAILED:**
```json
{
    "status": 409,
    "error": "Conflict",
    "message": "La transacción con id: 9 no puede reprocesarse. Estado actual: PROCESSED. Solo se pueden reprocesar transacciones en estado FAILED.",
    "timestamp": "2026-06-09T19:40:51.2818009"
}
```

---

## Decisiones técnicas relevantes

- # Idempotencia por `externalId`
Cada transacción llega con un identificador externo único (`externalId`). Antes de persistir se verifica con `existsByExternalId()` para prevenir duplicados, respondiendo `409 Conflict` si ya existe. Esto protege de re-envíos accidentales desde el sistema origen.

- # Historial de eventos como auditoría
Cada cambio de estado genera un registro en `transaction_events` con timestamp propio. Esto permite tener trazabilidad completa del ciclo de vida de una transacción sin necesidad de herramientas externas de auditoría, y facilita depurar reprocesos fallidos.

- # Scheduler con verificación previa (`existsByStatus`)
Antes de cargar listas de transacciones, el scheduler hace dos consultas tipo `COUNT` (`existsByStatus`). Si no hay trabajo, sale inmediatamente sin traer datos. Esto evita consultas costosas con `SELECT *` cuando la base de datos ya está al día.

- # Separación de DTOs por casos de uso
Se usan DTOs distintos según el contexto:
- `TransactionResponseDTO` — respuesta ligera al registrar (sin historial ni payload completo).
- `TransactionSummaryDTO` — listado paginado sin `payload` (puede ser un texto pesado).
- `TransactionEventDetailDTO` — detalle completo con historial de eventos, solo cuando se pide explícitamente.

- # Manejo centralizado de errores
`GlobalExceptionHandler` captura excepciones conocidas y las convierte en respuestas HTTP semánticas con estructura uniforme (`timestamp`, `status`, `error`, `message`). Los detalles internos de errores inesperados nunca se exponen al cliente (el mensaje genérico es siempre `"Ocurrió un error inesperado"`).

- # Validación en la capa de entrada
Se usa Bean Validation (`@NotBlank`, `@NotNull`) en el DTO de request para rechazar peticiones malformadas antes de que lleguen al servicio. Los errores de validación se transforman en mensajes legibles por el cliente.

- # Trazabilidad con Trace ID (MDC)
Cada request HTTP es interceptado por `MdcFilter`, que genera un Trace ID de 8 caracteres (prefijo de UUID), lo inserta en el MDC de SLF4J y lo devuelve como header `X-Trace-Id` en la respuesta. El patrón de logs está configurado para incluir `[traceId]` automáticamente en cada línea de log, sin necesidad de pasarlo manualmente.

El Trace ID vive únicamente en el MDC del thread activo: en requests HTTP lo gestiona `MdcFilter`, y en el scheduler `TransactionProcessor` genera uno nuevo por ciclo de ejecución. No se persiste en la base de datos porque su propósito es correlacionar logs, no almacenar datos de negocio.

Ejemplo real de salida en consola — ciclo de vida completo de la transacción ID 34:

**1. Registro via HTTP `POST /api/transactions` — Trace ID `[3d403ce8]`:**
```
2026-06-09 23:39:19 [3d403ce8] INFO  com.ols.service.TransactionService - ID: 34 | Transaccion registrada con estado RECEIVED
```

**2. Scheduler detecta estado RECEIVED y procesa — Trace ID `[0165259a]`:**
```
2026-06-09 23:39:24 [0165259a] INFO  c.ols.scheduler.TransactionProcessor - Procesando 1 transacciones en estado RECEIVED.
2026-06-09 23:39:24 [0165259a] INFO  com.ols.service.TransactionService - >>> Procesando transacción ID: 34 | Estado actual: RECEIVED
2026-06-09 23:39:24 [0165259a] INFO  com.ols.service.TransactionService - >>> ID: 34 | Cambiando a PROCESSING
2026-06-09 23:39:24 [0165259a] INFO  com.ols.service.TransactionService - >>> ID: 34 | Cambiando a RETRY_PENDING
```

**3. Scheduler reintenta RETRY_PENDING — Trace ID `[d10f9cfe]`:**
```
2026-06-09 23:39:34 [d10f9cfe] INFO  c.ols.scheduler.TransactionProcessor - Reintentando 1 transacciones en estado RETRY_PENDING.
2026-06-09 23:39:34 [d10f9cfe] INFO  com.ols.service.TransactionService - >>> Procesando transacción ID: 34 | Estado actual: RETRY_PENDING
2026-06-09 23:39:34 [d10f9cfe] INFO  com.ols.service.TransactionService - >>> ID: 34 | Cambiando a PROCESSING
2026-06-09 23:39:34 [d10f9cfe] INFO  com.ols.service.TransactionService - >>> ID: 34 | Cambiando a FAILED
```

**4. Reproceso manual `POST /api/transactions/34/reprocess` — Trace ID `[2c97c06d]`:**
```
2026-06-09 23:42:33 [2c97c06d] INFO  com.ols.service.TransactionService - >>> ID: 34 | Reproceso solicitado manualmente
2026-06-09 23:42:33 [2c97c06d] INFO  com.ols.service.TransactionService - >>> Procesando transacción ID: 34 | Estado actual: FAILED
2026-06-09 23:42:33 [2c97c06d] INFO  com.ols.service.TransactionService - >>> ID: 34 | Cambiando a PROCESSING
2026-06-09 23:42:33 [2c97c06d] INFO  com.ols.service.TransactionService - >>> ID: 34 | Cambiando a PROCESSED
```

### Cobertura con JaCoCo integrada al build
JaCoCo está configurado en el `pom.xml` para ejecutarse automáticamente con `mvn test`, sin pasos adicionales. El reporte se genera en `target/site/jacoco/`.

---

## Supuestos realizados

- **El procesamiento es simulado:** El resultado (éxito/fallo) es aleatorio con probabilidades 70/30 al no existir un sistema real al que conectarse. Es suficiente para demostrar el flujo completo de estados.
- **Cuando la transacción está en el estado `RETRY_PENDING` se procesa igual que `RECEIVED`:** El scheduler reutiliza el mismo método `process()` para ambos estados ya que la lógica es idéntica.
- **Solo cuando la transacción se encuentra en el estado `FAILED` permite reproceso manual:** Solicitar el reproceso de una transacción en cualquier otro estado lanza `409 Conflict`. Las transacciones en `RETRY_PENDING` se manejan exclusivamente por el scheduler automático.
- **El `payload` es texto libre:** No se valida el formato interno del payload (puede ser JSON, XML u otro). Solo se verifica que no esté vacío.
- **Una sola base de datos MySQL local:** No se incluyó configuración de múltiples entornos (dev/prod) ya que el alcance del ejercicio es ejecutar el proyecto de forma local.
- **No se requiere autenticación:** La API no implementa seguridad (JWT, API keys, etc.) porque no formaba parte del alcance definido en el ejercicio.

---

## Pendientes o mejoras futuras

- **Autenticación y autorización:** Agregar Spring Security con JWT o API keys para proteger los endpoints.
- **Configuración por entornos:** Separar `application-dev.properties` y `application-prod.properties` con credenciales y configuraciones por ambiente.
- **Procesamiento real:** Reemplazar la simulación aleatoria por integración con un sistema externo real (cola de mensajes, servicio de pagos, etc.).
- **Reintentos con backoff exponencial:** Implementar espera incremental entre reintentos de `RETRY_PENDING` en lugar de reprocesar en cada ciclo del scheduler.
- **Límite de reintentos:** Agregar un contador de intentos en `Transaction` y marcar como `FAILED` definitivo tras N intentos fallidos consecutivos.
- **Tests de integración:** Agregar tests con `@SpringBootTest` y base de datos H2 en memoria para validar el flujo completo de extremo a extremo.
- **Métricas con Actuator:** Exponer métricas de procesamiento de transacciones en tiempo real (transacciones por estado, tiempos de respuesta) vía Spring Boot Actuator + Micrometer.
- **Mejora en la documentación:** La documentación se pidió en un archivo README, no creo que sea óptimo entregar documentación en este formato ya que podría ser más gráfica la evidencia de ejecución. A mi parecer sería mejor crear documentos separando análisis, diseño de solución y pruebas, los cuales pueden contener imágenes de las pruebas exitosas y de las pruebas con errores controlados, así como los mensajes mostrados en las validaciones.

## Documentos recomendados

A continuación se describen los documentos adicionales que complementarían esta entrega si el formato lo permitiera:

### 1 — Documento de Análisis
Describe el problema y la solución propuesta antes de codificar:
- Descripción del sistema
- Requerimientos funcionales (4.1 Registrar, 4.2 Consultar listado, 4.3 Consultar detalle, 4.4 Procesar, 4.5 Reprocesar)
- Requerimientos no funcionales (paginación, idempotencia, trazabilidad)
- Decisiones técnicas tomadas y por qué
- Diagrama de flujo de estados de la transacción

### 2 — Documento de Diseño de Solución
Describe la arquitectura y estructura del código:
- Diagrama de arquitectura en capas
- Diagrama de la base de datos (tablas y relaciones)
- Descripción de cada endpoint con request/response esperado
- Estructura de carpetas del proyecto

### 3 — Documento de Pruebas
Evidencia gráfica de que todo funciona:
- Capturas de Postman con cada endpoint exitoso
- Capturas de errores controlados (`400`, `404`, `409`)
- Capturas de validaciones (enum inválido, campo vacío, fecha incorrecta)
- Capturas del historial de eventos completo
- Capturas de los logs con `traceId`
- Capturas del reporte de JaCoCo

> **Nota:** No es necesario agregarlo todo pero tener una referencia visual ayuda mucho a hacer las pruebas posteriores al desarrollo.

- **Estructura sugerida:**
README.md        → guía rápida para correr el proyecto (instalación, comandos)
Documentos Word  → documentación formal y completa

proyecto/
├── src/
├── docs/
│   ├── 1-Analisis.docx
│   ├── 2-Diseno-Solucion.docx
│   └── 3-Pruebas.docx
├── README.md
└── pom.xml