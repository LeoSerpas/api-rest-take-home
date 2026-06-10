# AI_USAGE

En este archivo se documenta la manera en que se usó IA para solventar algunas tareas.

### Herramienta utilizada

La herramienta que se ha usado es claude.ai / Claude Code integrado al editor VS Code.

### Para qué la utilizaste
1) Para crear validaciones más rápido. 
2) Para arreglar errores marcados por SonarQube.
3) Para generar los test Unitarios.
4) Para agregar el TraceID
5) Para revisar la documentación, este archivo AI_USAGE y el archivo README, ya que como escribí mucho en ellos, tenía errores ortográficos, le pedí que analizara la ortografía y que arreglara los errores. También para arreglar errores de formato, alguna vez usaba guiones y otras veces almohadillas para comenzar un enunciado, así que le pedí a la IA que modificara los detalles de formato para que la documentación fuera más consistente.

### Qué partes aceptaste, modificaste o descartaste?

1) Se creó el archivo GlobalExceptionHandler.java, que se encarga de las validaciones generales de las apis, a medida que se fueron creando las apis se fueron creando métodos para validación de los campos y mensajes de error. Varias respuestas de claude no se ajustaban a la petición o usaba imports que no servían para nada, como cuando me sugirió usar org.springframework.validation.BindException para validar la URL con tipo Inválido para el api de ConsultarTrxsPaginadas. En este caso se modificó el prompt para que se ajustara a lo solicitado además se le compartió los logs de Springboot para mayor comprensión de la petición.
No todo el código generado era correcto aun cuando parecía una secuencia lógica y que resolvería lo solicitado, muchas veces no daba el resultado esperado, así que se usaron logs para mostrar lo que contenía una variable por ejemplo ya que no daba el valor correcto, y se siguió en ocasiones el flujo de datos para encontrar la solución más óptima. 

2) También se usó para mejorar las prácticas de programación ya que tengo instalado SonarQube en el editor, me marcaba warnings como la siguiente:
 "NullPointerException" could be thrown; "getRequiredType()" can return null. [+2 locations]sonarqube(java:S2259)
Lo cual indicaba que había null checks pero estaban mal estructurados.
Por ejemplo la línea ex.getValue().toString() debía crearse un objeto rawValue y luego asignarlo a values, así:

// Antes
String value = ex.getValue() != null ? ex.getValue().toString() : "null";
// Después
Object rawValue = ex.getValue();
String value = rawValue != null ? rawValue.toString() : "null";

Se usó para múltiples warnings que mostraba SonarQube lo cual mejoró la mantenibilidad, legibilidad y evita fallos en producción como un posible NullPointerException, como en el ejemplo anterior.
SonarQube también ayuda encontrando problemas de seguridad como SQL Injection y elimina los llamados Code Smells lo cual hace el código difícil de mantener.

3) Se usó para crear los test unitarios, ahora no es tan necesario poner un prompt tan complejo, una vez terminadas las APIs, solo basta con poner algo parecido a esto: "Créame los test unitarios para el paquete com.ols.exception, agrega a cada test un comentario al inicio del mismo para saber qué hace cada uno."
Luego deben revisarse para aceptarse, modificarse o descartarse. Pero ayuda mucho la implementación de JacocoReport para ver el porcentaje de cobertura.

4) Para generar el reproceso automático de los estados RETRY_PENDING se le solicitó a la IA generar un plan de acción y luego ir generando cada parte a la vez para poder revisar que el código tuviera lógica y que cumpliera con la petición.

5) Para agregar el Trace ID / Correlation ID: Se solicitó implementar trazabilidad por request usando MDC de SLF4J. La IA generó el `MdcFilter`, la configuración del patrón de logs en `application.properties` y la lógica en el scheduler para asignar un Trace ID propio por ciclo de ejecución. La sugerencia inicial también incluía persistir el `traceId` como columna en la tabla `transactions`, lo cual se revisó y se descartó (ver sección de partes descartadas).

### Cómo validaste que el resultado era correcto. 

1) En el caso de las validaciones probando en Postman con valores erróneos y viendo el resultado, algunas veces no generaba el resultado esperado, aun cuando la lógica parecía correcta, Se usaron logs para ver el flujo de datos y realizar modificaciones. La ruta más óptima de solución es igual solicitando a la IA mostrando el error y mostrando los logs de algunas variables para modificar todo el método de validación.
2) En el caso de las incidencias de SonarQube, se probó que no se rompiera la funcionalidad luego del cambio y que la advertencia que mostraba en amarillo SonarQube ya no apareciera marcado el código.
3) En el caso de los test unitarios solo se hizo inspección general de los resultados de Claude, y utilizando JacocoReport se puede ver la cobertura de los paquetes y archivos, así que mientras la cobertura suba y se encuentre sobre el 80% global se dio como bueno.
4) En el caso del Trace ID se validó de dos formas: primero revisando en Postman que el header `X-Trace-Id` apareciera en la respuesta de cada request con un identificador de 8 caracteres; segundo verificando en la consola de la aplicación que cada línea de log incluyera el prefijo `[traceId]` con el mismo valor del header devuelto y que ese identificador persistiera en todo el ciclo de vida de la transacción.

### Qué decisiones técnicas fueron tuyas.

1) Arquitectura
    - Separar el código en capas controller, service, repository, dto, entity, enums, scheduler, exception.
    - Usar DTOs para no exponer la entidad directamente, segmentando Request y response.
2) Diseño
    - Uso de enums para tener valores válidos para tipo de transacción y status.
3) Procesamiento
    - En la parte 4.4 Procesar o simular procesamiento se recomienda usar "Procesamiento automático al registrar." Esto se hizo separando los estados FAILED de RETRY_PENDING; las FAILED requieren intervención manual, las RETRY_PENDING se reintentan automáticamente hasta que termine en un estado final PROCESSED o FAILED. Esto se hizo de esta manera ya que es así como trabajan la mayoría de sistemas que he visto, RETRY_PENDING queda como estado intermedio el cual debe resolver el sistema y los estados PROCESSED o FAILED se consideran estados finales. Para el estado FAILED se debe reprocesar manualmente o en caso de ser una transacción inválida se pide crear otra transacción que sí sea válida (Aunque este caso no debería pasar si se ha validado todo correctamente aunque siempre hay campos no validados como en este caso el payload).
4) Manejo de Seguridad y Calidad
    - Se valida idempotencia con existsByExternalId así aseguro que no haya duplicados
    - Manejo centralizado de excepciones o errores con el archivo GlobalExceptionHandler.java
    - Los mensajes de error muestran el campo en el cual ocurre y los valores permitidos en dicho campo. (No se usaron mensajes genéricos o ambiguos).
5) Diseño del Trace ID
    - La decisión de NO persistir el `traceId` en la base de datos fue propia. La IA sugirió guardarlo como columna en la tabla `transactions`, pero se consideró que el Trace ID no debía persistir en la base de datos sino que solo pertenece a la correlación de logs, por lo tanto no es un dato del negocio. Guardarlo en BD agrega una columna sin valor funcional y no era algo que se pedía en los requerimientos. Se optó por mantenerlo únicamente en el MDC del thread activo durante el ciclo de vida de cada request o ciclo del scheduler.

## Uso de Coding Assistants con IA

### Qué prompts o instrucciones generales usaste

Pondré ejemplo de algunos prompts generales se usaron:

"Quiero manejar todos los errores de mi API REST en Spring Boot desde un solo lugar. Cómo creo un GlobalExceptionHandler que retorne mensajes de error claros y tipificados en lugar del error genérico 500?"

"Quiero crear un manejador global de excepciones en Spring Boot que cubra estos casos:
Errores de validación de campos
Enum con valor inválido enviado en el body JSON
Enum con valor inválido enviado como parámetro en la URL
Fecha con formato inválido
Recurso no encontrado 
Transacción duplicada
Cualquier error inesperado
Cada error debe retornar un JSON con timestamp, status, error y message con un mensaje claro en español que indique el campo afectado y los valores permitidos cuando aplique."

"Tengo este código en Java que SonarQube me marca con la advertencia 'NullPointerException could be thrown' en la regla java:S2259:
javaString value = ex.getValue().toString();
Cómo corrijo este código para que SonarQube deje de marcarlo sin cambiar la lógica del negocio? Explícame por qué SonarQube lo detecta como problema."


"Crea los test unitarios para el paquete com.ols.scheduler, agrega a cada test un comentario al inicio del mismo para saber que hace cada uno."

### Qué revisaste antes de aceptar código sugerido
Antes de aceptar cualquier código verifiqué que no me marcara errores en el IDE. Por ejemplo cuando se sugirió usar import com.fasterxml.jackson.databind.exc.InvalidFormatException VS Code lo marcó en rojo porque en la versión de Jackson 3 el paquete cambió a tools.jackson. No acepté el código hasta encontrar una solución que se adaptara a la petición. En este caso al final no se usó la librería y se cambió el método que se usaba.

Revisé que el código no generara advertencias de SonarQube. Por ejemplo cuando se sugirió:
javaString value = ex.getValue().toString();
SonarQube marcó un posible NullPointerException. No acepté el código hasta corregirlo guardando la referencia primero en una variable antes de llamar métodos sobre ella.

Las respuestas de la IA algunas veces no se acoplaban con la petición, por ejemplo quería hacer el reproceso automático pero como sugiere en el punto 4.4 y 4.5 el reproceso solo debía hacerse para los estados RETRY_PENDING y no para los estados FAILED. La respuesta de la IA para esta parte hacía un reproceso para ambos estados a la vez, así que separé esas funcionalidades. 

Se revisaron además que las validaciones fueran consistentes, o sea lógicas que describieran el error correctamente y que no fueran ambiguas, que no solo mostrara un mensaje de error genérico "Inconsistencia en los datos" si no algo más consistente parecido a "El parámetro 'size' tiene un valor inválido: 'a'." u otro ejemplo podría ser "El campo 'transactionType' tiene un valor inválido. Los valores permitidos son: REFUND, TRANSFER, WITHDRAWAL, PAYMENT, DEPOSIT". Y así con todas las validaciones hechas.

### Qué errores encontraste y corregiste
1) Errores de compatibilidad de versiones
Al intentar agregar jackson-databind con la versión 2.21.2.redhat-00002 que SonarQube recomendaba, Maven falló con:
Missing artifact com.fasterxml.jackson.core:jackson-databind:jar:2.21.2.redhat-00002
La corrección fue quitar esa versión y buscar otra solución en este caso que Spring Boot la gestionara automáticamente.
2) Error de paquete incorrecto en Jackson 3
La IA sugirió importar:
javaimport com.fasterxml.jackson.databind.exc.InvalidFormatException;
Pero en la versión de Jackson 3 el paquete cambió a tools.jackson. No se podía usar directamente, la solución fue parsear el mensaje de la excepción con expresiones regulares para extraer el campo y los valores válidos sin depender de dicha librería.
3) Errores marcados por SonarQube
Aunque los errores marcados por SonarQube no los encontré yo sino que los marcaba la herramienta, sí me di a la tarea de corregirlos para que el código fuera limpio y aumentara la mantenibilidad y claridad.
4) Evento RECEIVED en lugar equivocado
El evento RECEIVED se estaba guardando dentro del método process() en lugar del método register(). Esto causaba que el historial mostrara RECEIVED como si fuera parte del procesamiento y no de la recepción. La corrección fue mover el saveEvent() al método register().
5) Scheduler procesando transacciones FAILED
El scheduler reprocesaba automáticamente tanto RETRY_PENDING como FAILED. Esto estaba mal porque las transacciones FAILED pueden tener errores que requieren revisión manual posterior. Se corrigió para que se procesara solo a RECEIVED y reprocesara el RETRY_PENDING, además de crear un endpoint manual POST /api/transactions/{id}/reprocess para las FAILED. Como se solicitaba en los puntos 4.3 y 4.4.
6) Trace ID persistido innecesariamente en BD
La implementación inicial del Trace ID incluía un campo `traceId` en la entidad `Transaction` con su columna `trace_id` en base de datos. Al revisar el diseño, se identificó que esto era incorrecto: el Trace ID sirve para correlacionar logs por request, no para almacenar datos de negocio. Se eliminó el campo de la entidad, el import de MDC del servicio, y la lógica que lo leía desde la entidad al procesar. La funcionalidad de trazabilidad se mantuvo intacta ya que el MDC lo gestiona el filtro HTTP y el scheduler de forma independiente.

### Cómo evitaste introducir código inseguro, innecesario o no entendido
1) No agregar código que no compilaba y se marcaba en Rojo en el editor, algunos imports sugeridos estaban desfasados y marcaban errores de seguridad y se tuvo que buscar alternativas o librerías más recientes.  Por ejemplo el import de InvalidFormatException se quedó en rojo hasta entender que el paquete había cambiado en Jackson 3 y buscar una solución alternativa.
2) Al agregar versiones manuales de dependencias, siempre revisar que estas estén actualizadas y que no falle su implementación o que el editor marque en rojo el import. Meter dependencias manualmente algunas veces puede romper la funcionalidad o generar más fallos que beneficios.
3) Cuando quería agregar alguna funcionalidad que no recordaba cómo implementar o salían conceptos nuevos que desconocía, siempre preguntaba a Claude que me explicara cuál era el mejor camino a seguir o si era seguro y funcional utilizar determinada librería.