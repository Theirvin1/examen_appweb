# Cuestionario — Parte A del examen de la Unidad IV

> **Cómo se llena este archivo.** Responda **dentro de este mismo archivo**, debajo de cada pregunta, en el bloque marcado como `**Respuesta:**`. No borre ni reescriba los enunciados: el evaluador compara pregunta por pregunta. No añada ni quite secciones.
>
> **Este archivo se versiona en el repositorio.** Debe existir en la raíz, llamarse exactamente `Cuestionario.md`, y sus respuestas deben llegar por *commits* sucesivos hechos cuando el docente lo indique. Un archivo que aparece completo en un único *commit* al final de la sesión no cumple el protocolo y se trata según el criterio de piso 4 del examen.
>
> Se valora la precisión técnica y la justificación, **no la extensión**. Una respuesta correcta de seis líneas vale más que una página imprecisa. Cuando la pregunta pida referirse al proyecto base, hágalo con nombres concretos de clases o de *endpoints*.

---

## Datos del estudiante

| Campo                | Valor                      |
|----------------------|----------------------------|
| Apellidos y nombres  | CAJAS IBARRA IRVIN MARCELO |
|  Número de carnet    | 1251039606                 |
| Correo institucional | icajasi@uteq.edu.ec        |
| Fecha                | 28/8/2026                  |
| URL del repositorio  | https://github.com/Theirvin1/examen_appweb                           |

---

## A1. Restricciones de REST aplicadas a un caso concreto — 8 puntos

**a) Enuncie las seis restricciones del estilo arquitectónico REST según Fielding. (3 puntos)**

**Respuesta:**

1. **Client-Server**: separación de preocupaciones entre interfaz de usuario y almacenamiento de datos.
2. **Stateless**: cada petición contiene toda la información necesaria; el servidor no guarda estado de sesión entre peticiones.
3. **Cache**: las respuestas deben declararse cacheables o no para reducir latencia.
4. **Uniform Interface**: interfaz uniforme basada en recursos identificados por URI, representaciones, mensajes autodescriptivos y HATEOAS.
5. **Layered System**: el cliente no necesita saber si se comunica directamente con el servidor final o con intermediarios (proxies, balanceadores).
6. **Code on Demand** *(opcional)*: el servidor puede enviar código ejecutable al cliente (ej. JavaScript).


**b) El proyecto base expone `GET /api/v1/autores` y guarda el estado de la sesión del usuario solo en el JWT que el cliente envía en cada petición. Explique qué restricción concreta se está cumpliendo con esa decisión y qué consecuencia práctica tiene para escalar el sistema a varios servidores detrás de un balanceador. (3 puntos)**

**Respuesta:**

Se cumple la restricción **Stateless**: el servidor no almacena contexto de sesión; toda la información de autenticación (usuario, rol, expiración) viaja en el JWT que el cliente adjunta en cada petición mediante la cabecera `Authorization: Bearer <token>`.

Consecuencia práctica: cualquier nodo del clúster puede atender cualquier petición sin necesidad de sesiones pegajosas (*sticky sessions*) ni de compartir estado entre servidores. El balanceador puede distribuir libremente las peticiones entre instancias porque cada una valida el JWT de forma independiente con la misma clave secreta (`JwtService`).


**c) De las seis restricciones, indique cuál es opcional y dé un ejemplo real de una API que la use. (2 puntos)**

**Respuesta:**

La restricción opcional es **Code on Demand**. Un ejemplo real es la API de Google Maps JavaScript: el servidor envía el código JS del SDK al cliente, que lo ejecuta localmente para renderizar mapas y calcular rutas.


---

## A2. Anatomía y ciclo de vida de un JWT — 8 puntos

**a) Un JWT tiene tres partes separadas por puntos. Nómbrelas en orden e indique qué contiene cada una. (3 puntos)**

**Respuesta:**

1. **Header** (cabecera): JSON en Base64Url con el tipo de token (`"typ": "JWT"`) y el algoritmo de firma (`"alg": "HS256"`).
2. **Payload** (cuerpo): JSON en Base64Url con los *claims* (afirmaciones): emisor, sujeto, expiración, roles, y cualquier dato personalizado.
3. **Signature** (firma): resultado de aplicar el algoritmo del header sobre `Base64Url(header) + "." + Base64Url(payload)` usando la clave secreta. Garantiza integridad.


**b) Un compañero afirma: «como el JWT va firmado, puedo guardar en el *payload* la contraseña del usuario sin riesgo». Explique por qué está equivocado, precisando la diferencia entre firmar y cifrar. (2 puntos)**

**Respuesta:**

Está equivocado. **Firmar** garantiza integridad y autenticidad (detecta si el token fue alterado), pero **no oculta el contenido**: el payload es solo Base64Url, decodificable por cualquiera sin necesidad de clave. **Cifrar** (ej. JWE) sí hace el contenido ilegible sin la clave de descifrado. Como el proyecto usa JWT firmado (no cifrado), cualquier intermediario puede leer el payload y ver la contraseña en texto claro.


**c) El JWT es *stateless* por diseño, lo que genera un problema conocido: no se puede invalidar un token antes de que expire. Describa dos estrategias distintas para revocarlo y señale la desventaja de cada una. (3 puntos)**

**Respuesta:**

1. **Lista negra de tokens (Token Blacklist)**: se almacena en Redis el `jti` (JWT ID) de los tokens revocados. En cada petición se verifica que el token no esté en la lista.
   - *Desventaja*: introduce estado en el servidor y una consulta extra a Redis por cada petición, eliminando parcialmente la ventaja stateless.

2. **Tokens de vida corta + refresh token**: el access token expira en minutos; al cerrar sesión se invalida el refresh token en base de datos.
   - *Desventaja*: no revoca inmediatamente el access token ya emitido; el usuario puede seguir operando hasta que expire (ventana de riesgo).


---

## A3. SOAP frente a REST — 8 puntos

**a) Complete la tabla comparativa con seis criterios entre SOAP y REST. (5 puntos)**

**Respuesta:**

| Criterio | SOAP | REST |
|---|---|---|
| Formato del mensaje | XML obligatorio | Libre (JSON, XML, etc.); JSON es el estándar de facto |
| Contrato de descripción | WSDL (Web Services Description Language) | OpenAPI / Swagger (no obligatorio) |
| Sobrecarga de serialización | Alta (envelope XML verboso) | Baja (JSON compacto) |
| Tipado | Estrictamente tipado mediante XSD | Tipado débil; validación opcional (Bean Validation, JSON Schema) |
| Facilidad de consumo desde un cliente móvil | Difícil; requiere librerías SOAP específicas | Sencillo; cualquier cliente HTTP puede consumirlo |
| Manejo de errores | Elemento `<Fault>` estandarizado dentro del envelope | Códigos de estado HTTP + cuerpo de error (ej. RFC 9457 Problem Details) |

**b) El Servicio de Rentas Internas del Ecuador expone la autorización de comprobantes electrónicos mediante servicios SOAP. Explique dos razones técnicas por las que una institución de ese tipo mantiene SOAP en lugar de migrar a REST. (3 puntos)**

**Respuesta:**

1. **Contratos formales y trazabilidad legal**: WSDL define de forma vinculante la estructura exacta de cada mensaje (tipos XSD). En contextos tributarios, el contrato debe ser inmutable y verificable; cualquier cambio en el esquema genera una nueva versión del WSDL, garantizando compatibilidad con sistemas auditables.

2. **WS-Security y transacciones distribuidas**: SOAP soporta estándares de seguridad a nivel de mensaje (WS-Security, WS-ReliableMessaging) que permiten firmar digitalmente cada mensaje XML (requerimiento legal para comprobantes electrónicos) y garantizar entrega exactamente-una-vez. REST no tiene equivalentes nativos estandarizados para estos escenarios.


---

## A4. Cache-aside sobre un servicio externo — 8 puntos

> El proyecto base define en `CacheConfig` dos espacios de caché: `libros` con TTL de 2 minutos y `openlibrary` con TTL de 24 horas.

**a) Describa el patrón *cache-aside* en sus cuatro pasos, desde que llega la petición hasta que se responde. (3 puntos)**

**Respuesta:**

1. **Petición → buscar en caché**: la aplicación consulta primero el caché (Redis) con la clave correspondiente.
2. **Cache hit**: si el valor existe y no ha expirado, se devuelve directamente desde Redis sin tocar la fuente de datos. Fin.
3. **Cache miss**: si no existe (o expiró), la aplicación consulta la fuente real (base de datos u Open Library).
4. **Poblar caché y responder**: el resultado obtenido se almacena en Redis con el TTL configurado y se devuelve al cliente. Las siguientes peticiones encontrarán el valor en caché.


**b) Justifique técnicamente por qué el TTL de `openlibrary` es doce veces mayor que el de `libros`, y qué criterio general debe guiar la elección de un TTL. (3 puntos)**

**Respuesta:**

Los metadatos bibliográficos de un ISBN en Open Library (título, autor, año) son prácticamente inmutables: no cambian de un día para otro. Por eso el TTL de 24 h es seguro; refrescar cada 2 min generaría llamadas externas innecesarias con latencia y riesgo de rate-limiting.

El catálogo local (`libros`) cambia con frecuencia (nuevas adquisiciones, ediciones), por lo que el TTL corto de 2 min garantiza consistencia sin penalizar demasiado.

**Criterio general**: el TTL debe ser inversamente proporcional a la frecuencia de cambio del dato y proporcional al costo de obtenerlo. Datos estables y costosos de recuperar → TTL largo; datos volátiles y baratos de leer → TTL corto.


**c) Explique por qué nunca debe almacenarse en caché la respuesta de un fallo del servicio externo, y describa qué le ocurriría al sistema si se hiciera. (2 puntos)**

**Respuesta:**

Un fallo (timeout, error 5xx de Open Library) es un estado transitorio, no un dato válido. Si se cachease, el error se propagaría durante todo el TTL (hasta 24 h): **todas** las peticiones subsiguientes recibirían la respuesta de error aunque el servicio externo ya se haya recuperado, bloqueando la funcionalidad sin posibilidad de auto-recuperación hasta que expire la entrada. Por eso `CacheConfig` usa `disableCachingNullValues()` y la lógica debe lanzar `ServicioExternoException` en vez de retornar null.


---

## A5. Diagnóstico de códigos de estado y contrato de errores — 8 puntos

> Todos los errores del proyecto base salen en formato *Problem Details* conforme a la RFC 9457, que obsoleta a la RFC 7807.

Para cada escenario indique el código HTTP correcto y explique en una línea por qué. **Cada fila vale 1 punto** (0,5 por el código y 0,5 por la justificación); el literal g) vale 2 puntos.

| # | Escenario | Código | Justificación (una línea) |
|---|---|---|---|
| a | `GET /api/v1/libros/999999` y ese identificador no existe | 404 Not Found | `RecursoNoEncontradoException` → el recurso solicitado no existe en el sistema. |
| b | `POST /api/v1/libros` sin cabecera `Authorization` | 401 Unauthorized | No se aportaron credenciales; el servidor no puede identificar al cliente. |
| c | Usuario autenticado con rol `LECTOR` envía `POST /api/v1/libros` | 403 Forbidden | `AccessDeniedException` → el usuario está autenticado pero carece del rol `ADMIN` requerido. |
| d | `POST /api/v1/libros` con el campo `titulo` vacío | 400 Bad Request | `MethodArgumentNotValidException` → la petición no supera las validaciones de Bean Validation. |
| e | Prestar un libro a un socio que ya tiene tres préstamos activos | 409 Conflict | `ReglaNegocioException` → se incumple la regla de negocio del límite de préstamos activos. |
| f | La API de Open Library no responde dentro del *timeout* configurado | 502 Bad Gateway | `ServicioExternoException` → el servidor actuó como proxy y el servicio de destino no respondió. |

**g) Explique por qué devolver `200 OK` con un cuerpo `{"success": false}` es un error de diseño, y qué restricción de REST se incumple al hacerlo. (2 puntos)**

**Respuesta:**

El código de estado HTTP **es** el indicador semántico del resultado de la operación. Devolver `200 OK` comunica al cliente (y a proxies, caches, monitores) que la petición fue exitosa cuando en realidad falló, obligando al cliente a parsear el cuerpo para detectar el error.

Se incumple la restricción de **Uniform Interface**, concretamente el principio de *mensajes autodescriptivos*: el código de estado debe describir el resultado sin necesidad de inspeccionar el cuerpo. Además, un caché podría almacenar esa respuesta fallida y servirla como éxito a clientes posteriores.


---

## Declaración de honestidad académica

Marque con una `x` y complete:

- [ X ] Declaro que estas respuestas son de mi autoría, redactadas durante la sesión de examen, sin asistencia de inteligencia artificial ni comunicación con terceros.

Firma (nombre completo): Irvin Marcelo Cajas Ibarra
