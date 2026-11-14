# 🐕 Patitas al Rescate — Seguridad con JWT

Taller práctico **Parte 1: Criptografía y Seguridad Básica con JWT** para el refugio de mascotas *Patitas al Rescate*.

Implementación en **Spring Boot 4.1.2 / Java 25 / PostgreSQL** de los cuatro hitos:

| Hito | Tema | Estado |
|------|------|--------|
| 1 | Registro y login vulnerable (contraseña en texto plano) | Documentado como evidencia; reemplazado por BCrypt |
| 2 | Cifrado unidireccional con BCrypt (`jbcrypt 0.4`) | ✅ Implementado |
| 3 | Generación de JWT de 30 minutos (`java-jwt 4.4.0`) | ✅ Implementado |
| 4 | Endpoint `/perfil` protegido manualmente con `Bearer` | ✅ Implementado |

> ⚠️ El código de producción **nunca** guarda contraseñas en texto plano. El Hito 1 se conserva documentado en
> [`docs/ANALISIS_HITO1.md`](docs/ANALISIS_HITO1.md) con la versión vulnerable y el análisis técnico exigido.

---

## 🧱 Arquitectura de identidad

| Rol | Perfil | Permisos funcionales |
|-----|--------|----------------------|
| `ADMIN` | Voluntario del refugio | Registrar rescates, actualizar fichas de salud, autorizar adopciones |
| `USER` | Adoptante registrado | Ver mascotas disponibles, postularse para adoptar, actualizar contacto |

El rol viaja dentro del JWT en el claim `rol`, pero la autorización real del endpoint `/perfil` se decide en el
servidor a partir del token **firmado**, nunca de un valor enviado por el cliente.

---

## 🗂️ Estructura del proyecto

```
src/main/java/com/krakedev/jwt
├── JwtApplication.java
├── controllers/
│   └── AuthController.java          # /registrar, /login, /perfil
├── dtos/
│   ├── RegistroRequest.java
│   ├── LoginRequest.java
│   ├── RegistroResponse.java
│   ├── LoginResponse.java
│   ├── PerfilResponse.java
│   └── ErrorResponse.java
├── entidades/
│   └── Usuario.java                 # tabla usuarios
├── exceptions/
│   ├── CredencialesInvalidasException.java
│   ├── UsuarioDuplicadoException.java
│   ├── RolInvalidoException.java
│   ├── TokenInvalidoException.java
│   └── GlobalExceptionHandler.java
├── repositories/
│   └── UsuarioRepository.java
├── services/
│   └── UsuarioService.java          # BCrypt + reglas de negocio
└── utils/
    └── JwtUtil.java                 # firma/verificación HS256
```

Documentación adicional:

- [`docs/ANALISIS_HITO1.md`](docs/ANALISIS_HITO1.md): análisis técnico de la vulnerabilidad y código original.
- [`docs/EVIDENCIAS.md`](docs/EVIDENCIAS.md): checklist de capturas para WhatsApp y pasos exactos.
- [`docs/postman/PatitasAlRescate.postman_collection.json`](docs/postman/PatitasAlRescate.postman_collection.json):
  colección lista para importar (guarda el token automáticamente).
- [`database/init.sql`](database/init.sql): creación de la base y de la tabla `usuarios`.

---

## ✅ Requisitos

- JDK 25 (el `pom.xml` declara `<java.version>25</java.version>`).
- PostgreSQL 14 o superior.
- No hace falta instalar Maven: el proyecto incluye **Maven Wrapper** (`mvnw.cmd` / `./mvnw`).
- Postman (o `curl`) para las pruebas manuales.

---

## 🛠️ Configuración de la base de datos

1. Cree la base de datos y la tabla con [`database/init.sql`](database/init.sql) desde pgAdmin o `psql`.
2. Ajuste las credenciales. Por defecto la aplicación espera:

| Variable | Valor por defecto |
|----------|-------------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/refugio_patitas` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `JWT_SECRET` | Secreto de laboratorio incluido en `application.properties` |

Puede cambiar los valores en `src/main/resources/application.properties` o definirlos como variables de entorno:

```powershell
$env:DB_PASSWORD = "mi_password_real"
$env:JWT_SECRET = "un-secreto-aleatorio-de-al-menos-32-bytes"
```

> La aplicación usa `spring.jpa.hibernate.ddl-auto=update`, por lo que también puede crear la tabla
> automáticamente si la base `refugio_patitas` existe.

---

## ▶️ Ejecutar la aplicación

```powershell
.\mvnw.cmd spring-boot:run
```

Servidor disponible en `http://localhost:8080`.

### Ejecutar las pruebas automatizadas

```powershell
.\mvnw.cmd test
```

Las pruebas usan **H2 en memoria** (perfil de test) y no requieren PostgreSQL. Cubren los cuatro hitos:
hash BCrypt, login, generación/validación del JWT, alteración de un carácter, expiración, roles y validaciones.

Resultado esperado: `Tests run: 19, Failures: 0, Errors: 0`.

---

## 🔌 Endpoints

| Método | Ruta | Autenticación | Descripción |
|--------|------|---------------|-------------|
| `POST` | `/api/auth/registrar` | Pública | Crea un `ADMIN` o `USER` con contraseña hasheada |
| `POST` | `/api/auth/login` | Pública | Valida credenciales y devuelve un JWT de 30 minutos |
| `GET` | `/api/auth/perfil` | `Authorization: Bearer <token>` | Devuelve el saludo según el rol |

### 1) Registro

```http
POST /api/auth/registrar
Content-Type: application/json

{
  "username": "voluntario_carlos",
  "password": "mi_clave_secreta_123",
  "rol": "ADMIN"
}
```

Respuesta `201 Created`:

```json
{
  "id": 2,
  "username": "voluntario_carlos",
  "rol": "ADMIN",
  "algoritmoPassword": "BCrypt (costo 12)",
  "mensaje": "Usuario registrado correctamente. La contraseña se almacenó como hash BCrypt."
}
```

En PostgreSQL se observa el hash (nunca la clave):

```
id | username           | password                                                     | rol
---+--------------------+--------------------------------------------------------------+-------
 1 | adoptante_pedro    | $2a$12$Q9...60 caracteres...                                 | USER
 2 | voluntario_carlos  | $2a$12$k3...60 caracteres...                                 | ADMIN
```

### 2) Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "voluntario_carlos",
  "password": "mi_clave_secreta_123"
}
```

Respuesta `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "Bearer",
  "username": "voluntario_carlos",
  "rol": "ADMIN",
  "validezSegundos": 1800,
  "emitidoEn": "2026-11-14T08:30:00Z",
  "expiraEn": "2026-11-14T09:00:00Z"
}
```

### 3) Perfil protegido

```http
GET /api/auth/perfil
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

Respuesta `200 OK` para un voluntario:

```json
{
  "id": 2,
  "username": "voluntario_carlos",
  "rol": "ADMIN",
  "mensaje": "Hola, voluntario_carlos. Bienvenido al panel de administración de Patitas al Rescate: podés registrar rescates, actualizar fichas de salud y autorizar adopciones.",
  "expiracionToken": "2026-11-14T09:00:00Z",
  "autenticado": true
}
```

Si se altera **un solo carácter** del token, la firma HMAC deja de coincidir y el servidor responde:

```json
{
  "timestamp": "2026-11-14T08:35:12Z",
  "status": 401,
  "error": "No autorizado",
  "mensaje": "Token JWT inválido: la firma digital no coincide o el token fue alterado.",
  "ruta": "/api/auth/perfil",
  "detalles": {}
}
```

---

## 🧪 Prueba manual del Hito 4 (Postman)

1. Ejecute `POST /api/auth/login` con `voluntario_carlos` y copie `token`.
2. Cree una pestaña `GET http://localhost:8080/api/auth/perfil`.
3. En **Headers** agregue `Authorization` con el valor `Bearer <token>`.
4. Envíe y verifique el saludo de administrador.
5. Modifique un carácter del token y vuelva a enviar: debe recibir **401 Unauthorized**.

La colección de Postman incluida automatiza los pasos 1, 4 y 5: al ejecutar el login guarda `token` y
`tokenAlterado` como variables de colección.

También puede probarlo con `curl.exe`:

```powershell
curl.exe -i -X POST http://localhost:8080/api/auth/registrar `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"voluntario_carlos\",\"password\":\"mi_clave_secreta_123\",\"rol\":\"ADMIN\"}'

curl.exe -s -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"voluntario_carlos\",\"password\":\"mi_clave_secreta_123\"}'

curl.exe -i http://localhost:8080/api/auth/perfil `
  -H "Authorization: Bearer PEGUE_AQUI_EL_TOKEN"
```

---

## 🔐 Decisiones de seguridad aplicadas

- **BCrypt con costo 12 y salt aleatorio**: cada hash es distinto incluso para la misma contraseña.
- **`BCrypt.checkpw` en lugar de `.equals()`**: comparación criptográfica, resistente a fuerza bruta.
- **Compatibilidad segura con datos del Hito 1**: si una fila antigua conserva texto plano, el login la rechaza
  (no se acepta como válida) y se registra una advertencia.
- **Mensaje genérico `Credenciales inválidas`**: evita revelar si el username existe (enumeración de usuarios).
- **JWT firmado con HS256 y emisor verificado**: alterar un carácter invalida la firma → `401`.
- **Vigencia estricta de 30 minutos** (`jwt.expiration-seconds=1800`).
- **El hash y la contraseña nunca se devuelven** en las respuestas HTTP.
- **Validación de entrada** con Jakarta Validation (username, longitud de contraseña, roles `ADMIN`/`USER`).
- **Secreto JWT externo** vía `JWT_SECRET` para no versionar credenciales reales.
- **SQL Injection mitigado** por el uso de Spring Data JPA (consultas parametrizadas); el hash BCrypt además
  hace inútil el volcado de la tabla para reutilizar contraseñas.

> Para producción faltaría: HTTPS obligatorio, rotación de secretos, rate limiting en `/login`, refresh tokens,
> migración forzada de contraseñas heredadas y Spring Security para autorización declarativa.

---

## 📸 Entregables

1. **Capturas al WhatsApp del taller** — ver checklist detallado en [`docs/EVIDENCIAS.md`](docs/EVIDENCIAS.md):
   - Hito 1: `SELECT * FROM usuarios;` con la contraseña en texto plano.
   - Hito 2: comparación texto plano vs hash `$2a$`.
   - Hito 3: token devuelto por Postman.
   - Hito 4: perfil `ADMIN` y respuesta `401` con el token alterado.
2. **Repositorio GitHub** con este proyecto, la colección de Postman y la documentación.
