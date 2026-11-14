# Evidencias del taller — checklist para WhatsApp

Este documento indica **qué capturar**, **cómo producirlo** y **qué debe verse** en cada captura. Guarde las
imágenes en `docs/evidencias/` (carpeta ignorada por Git) con los nombres sugeridos.

> ⚠️ Use datos de laboratorio. Nunca publique contraseñas reales, cadenas de conexión ni secretos JWT.

---

## Hito 1 — Contraseña en texto plano

**Preparación:** si ya migró a BCrypt, reproduzca el Hito 1 temporalmente con el código vulnerable documentado
en [`ANALISIS_HITO1.md`](ANALISIS_HITO1.md), o utilice la evidencia tomada antes de aplicar el hash.

1. Con el servidor levantado, registre el adoptante:

```powershell
curl.exe -i -X POST http://localhost:8080/api/auth/registrar `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"adoptante_pedro\",\"password\":\"mi_clave_secreta_123\",\"rol\":\"USER\"}'
```

2. Abra pgAdmin, conéctese a `refugio_patitas` y ejecute:

```sql
SELECT * FROM usuarios;
```

3. Captura `01_hito1_texto_plano.png`.

**Debe verse claramente:** el `username = adoptante_pedro` y `password = mi_clave_secreta_123` en texto legible.

4. Envíe la captura + el párrafo de 5 líneas de [`ANALISIS_HITO1.md`](ANALISIS_HITO1.md) al grupo de WhatsApp.

---

## Hito 2 — Transición a BCrypt

1. Registre el voluntario administrativo:

```powershell
curl.exe -i -X POST http://localhost:8080/api/auth/registrar `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"voluntario_carlos\",\"password\":\"mi_clave_secreta_123\",\"rol\":\"ADMIN\"}'
```

2. En pgAdmin ejecute nuevamente:

```sql
SELECT id, username, password, rol FROM usuarios ORDER BY id;
```

3. Captura `02_hito2_bcrypt.png`.

**Debe verse claramente:**

```
adoptante_pedro   | mi_clave_secreta_123                      | USER    <- Hito 1 (si se conserva)
voluntario_carlos | $2a$12$.................................. | ADMIN   <- Hito 2
```

El valor de `voluntario_carlos` debe empezar con `$2a$` y tener 60 caracteres.

4. Envíe de inmediato la captura al grupo de WhatsApp como evidencia de la transición criptográfica.

---

## Hito 3 — Token JWT en el login

1. En Postman (o `curl`) ejecute:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "voluntario_carlos",
  "password": "mi_clave_secreta_123"
}
```

2. Captura `03_hito3_token.png` mostrando el `200 OK` y el campo `token` completo.

**Debe verse claramente:** el token comienza con `eyJ` y tiene tres segmentos separados por puntos.
También se observa `"validezSegundos": 1800` (30 minutos).

3. Envíe el token devuelto al grupo de WhatsApp.

---

## Hito 4 — Perfil protegido y token alterado

### 4.1 Acceso autorizado

1. En Postman cree `GET http://localhost:8080/api/auth/perfil`.
2. En **Headers** agregue:

```
Authorization: Bearer <token del login>
```

3. Captura `04_hito4_perfil_admin.png` mostrando `200 OK` y el mensaje:

> "Hola, voluntario_carlos. Bienvenido al panel de administración de Patitas al Rescate..."

### 4.2 Token alterado

1. Copie el token y cambie **un solo carácter** de la firma (por ejemplo, la última letra).
2. Vuelva a enviar `GET /api/auth/perfil` con el token modificado.
3. Captura `05_hito4_token_alterado.png` mostrando `401 Unauthorized` y el mensaje de firma inválida.

**Debe verse claramente:** el código `401` y el mensaje que indica que la firma digital no coincide o el token
fue alterado.

---

## Resumen de capturas

| # | Archivo sugerido | Hito | Qué demuestra |
|---|------------------|------|----------------|
| 1 | `01_hito1_texto_plano.png` | 1 | Contraseña legible en PostgreSQL |
| 2 | `02_hito2_bcrypt.png` | 2 | Hash `$2a$` de BCrypt |
| 3 | `03_hito3_token.png` | 3 | JWT emitido por el login |
| 4 | `04_hito4_perfil_admin.png` | 4 | Perfil autorizado con saludo ADMIN |
| 5 | `05_hito4_token_alterado.png` | 4 | Rechazo 401 por firma rota |

---

## Pruebas automatizadas (opcional como evidencia adicional)

```powershell
.\mvnw.cmd test
```

Captura `06_pruebas_automatizadas.png` con `Tests run: 19, Failures: 0, Errors: 0`.

---

## Colección de Postman

Importe `docs/postman/PatitasAlRescate.postman_collection.json`. La petición **Login voluntario** guarda
automáticamente:

- `{{token}}`: token válido.
- `{{tokenAlterado}}`: copia con un carácter modificado.

Luego ejecute **Perfil ADMIN** y **Perfil con token alterado** sin copiar nada a mano.
