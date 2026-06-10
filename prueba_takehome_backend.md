# Prueba técnica take-home — Desarrollador Backend

## 1. Objetivo

Construir una API backend, que demuestre capacidad para desarrollar servicios empresariales con **Java + Spring Boot**, manejo de persistencia, validaciones, errores, trazabilidad, pruebas y documentación técnica.

La intención no es evaluar cantidad de funcionalidades, sino la calidad del diseño, claridad del código, mantenibilidad y criterio técnico.

## 2. Contexto del ejercicio

Una plataforma de integración recibe transacciones desde sistemas externos. Cada transacción debe registrarse, validarse, procesarse y quedar disponible para consulta posterior. Algunas transacciones pueden fallar y deben permitir reproceso controlado.

Debes construir un servicio backend para administrar estas transacciones.

## 3. Stack esperado

### Obligatorio

- Java 17 o superior.
- Spring Boot.
- API REST.
- Base de datos relacional, puede ser H2, PostgreSQL o MySQL.
- Maven o Gradle.
- Git.

### Deseable

- Docker / Docker Compose.
- OpenAPI / Swagger.
- JUnit y Mockito.
- Logs estructurados.
- Manejo de correlation ID o trace ID.
- CI básico, si decides incluirlo.

## 4. Funcionalidades mínimas

### 4.1 Registrar transacción

Crear un endpoint para registrar una transacción enviada por un sistema externo.

Debe recibir como mínimo:

- Identificador externo de la transacción.
- Tipo de transacción.
- Sistema origen.
- Fecha/hora de recepción.
- Payload o detalle de la transacción.

Debe validar:

- Campos obligatorios.
- Identificador externo único o comportamiento idempotente.
- Tipo de transacción válido.
- Payload no vacío.

### 4.2 Consultar transacciones

Crear un endpoint para listar transacciones con filtros básicos:

- Estado.
- Tipo.
- Sistema origen.
- Rango de fechas.

Debe incluir paginación o una estrategia simple para evitar respuestas sin límite.

### 4.3 Consultar detalle de transacción

Crear un endpoint para consultar una transacción específica por ID.

Debe mostrar:

- Datos generales.
- Estado actual.
- Historial mínimo de procesamiento o eventos.
- Mensaje de error, si aplica.

### 4.4 Procesar o simular procesamiento

Crear una lógica que simule el procesamiento de una transacción.

Puedes hacerlo de una de estas formas:

- Endpoint manual: `POST /transactions/{id}/process`.
- Procesamiento automático al registrar.
- Job simple interno.

La lógica debe permitir al menos estos estados:

- `RECEIVED`
- `PROCESSING`
- `PROCESSED`
- `FAILED`
- `RETRY_PENDING`

### 4.5 Reprocesar transacción fallida

Crear un endpoint para reprocesar una transacción fallida.

Debe validar que:

- Solo se puedan reprocesar transacciones en estado válido.
- No se generen duplicados innecesarios.
- Se registre el intento de reproceso.

## 5. Requisitos técnicos esperados

El proyecto debe demostrar:

- Separación clara de responsabilidades.
- Uso adecuado de controladores, servicios, repositorios, DTOs y entidades.
- Manejo centralizado de errores.
- Validaciones de entrada.
- Persistencia relacional.
- Código legible y mantenible.
- Pruebas unitarias o de integración para los casos principales.
- Documentación suficiente para ejecutar y revisar el proyecto.

## 6. Entregables

Debes entregar un repositorio o archivo comprimido con:

```text
/backend-solution
  /src
  README.md
  AI_USAGE.md
  pom.xml o build.gradle
  docker-compose.yml opcional
```

### README.md

Debe incluir:

- Descripción de la solución.
- Cómo ejecutar el proyecto.
- Cómo ejecutar pruebas.
- Endpoints disponibles.
- Ejemplos de requests/responses.
- Decisiones técnicas relevantes.
- Supuestos realizados.
- Pendientes o mejoras futuras.

### AI_USAGE.md

Debes indicar si usaste o no herramientas de asistencia con IA.

Si usaste IA, documenta:

- Herramienta utilizada.
- Para qué la utilizaste.
- Qué partes aceptaste, modificaste o descartaste.
- Cómo validaste que el resultado era correcto.
- Qué decisiones técnicas fueron tuyas.

No incluyas credenciales, información confidencial ni datos sensibles en herramientas externas.

## 7. Uso de Coding Assistants con IA

El uso de IA está permitido. Será evaluado positivamente si demuestra criterio técnico y responsabilidad.

Se espera que puedas explicar:

- Qué prompts o instrucciones generales usaste.
- Qué revisaste antes de aceptar código sugerido.
- Qué errores encontraste y corregiste.
- Cómo evitaste introducir código inseguro, innecesario o no entendido.

El uso de IA no reemplaza la responsabilidad del candidato sobre el diseño, calidad y funcionamiento de la solución.

## 8. Criterios de evaluación

| Criterio | Peso |
|---|---:|
| Cumplimiento funcional | 20 |
| Diseño de API y contrato REST | 15 |
| Arquitectura y separación de responsabilidades | 15 |
| Persistencia, validaciones y manejo de errores | 15 |
| Pruebas y calidad de código | 15 |
| Observabilidad, trazabilidad y operación | 10 |
| Documentación y claridad de entrega | 5 |
| Uso responsable de IA | 5 |
| **Total** | **100** |

## 9. Criterios eliminatorios

La prueba puede ser descartada si:

- No se puede ejecutar.
- No incluye instrucciones claras.
- No usa Java/Spring Boot.
- No implementa API REST.
- No persiste información.
- Presenta código copiado sin comprensión aparente.
- Incluye credenciales reales o información sensible.

## 10. Revisión posterior

Durante la entrevista posterior se podrá pedir que expliques:

- Diseño general de la solución.
- Decisiones de arquitectura.
- Manejo de errores e idempotencia.
- Estrategia de pruebas.
- Uso de IA, si aplica.
- Qué mejorarías con más tiempo.
