# Análisis técnico del Hito 1 — Contraseñas en texto plano

## 📌 Párrafo de análisis (5 líneas)

> Almacenar la contraseña en texto plano dentro de `usuarios.password` convierte a la base de datos en un botín de credenciales: cualquier atacante con acceso interno al servidor (un DBA, un backup filtrado o una cuenta comprometida) puede ejecutar `SELECT * FROM usuarios` y leer inmediatamente las claves de todos los voluntarios y adoptantes. La misma exposición se produce desde fuera mediante una inyección SQL en cualquier consulta vulnerable, ya que el atacante no necesita descifrar nada: la contraseña está disponible en claro y puede reutilizarse en el login del refugio y en otros servicios donde los usuarios repiten la misma clave. Como las personas suelen reutilizar contraseñas, el compromiso se propaga a correos, redes sociales y cuentas bancarias, produciendo robo de identidad y fraude. Además, el uso de `.equals()` no protege contra ataques de fuerza bruta ni permite detectar manipulación, y el administrador legítimo tampoco puede saber si las credenciales ya fueron copiadas. La mitigación correcta es almacenar únicamente un hash lento y con salt (BCrypt), comparar con `BCrypt.checkpw` y nunca devolver ni registrar la contraseña original.

---

## 🔎 Ampliación del análisis

### Vectores de ataque

| Vector | Cómo se explota | Consecuencia |
|--------|-----------------|--------------|
| Acceso interno a la BD | `SELECT username, password FROM usuarios;` | Todas las identidades quedan comprometidas |
| Backup o dump filtrado | El archivo `.sql` contiene las claves legibles | Reutilización de credenciales en otros sistemas |
| SQL Injection | `' OR '1'='1` en un formulario mal construido | Volcado de la tabla completa |
| Logs o soporte | La contraseña aparece en trazas o correos | Exposición silenciosa y difícil de detectar |

### Por qué falla el esquema del Hito 1

1. **Confidencialidad nula en reposo**: la base de datos no aporta ninguna protección; leer la tabla equivale a
   leer todas las contraseñas.
2. **Comparación manual con `.equals()`**: es una comparación de cadenas, no un mecanismo criptográfico. No
   incorpora salt, no es lenta por diseño y no protege ante filtraciones.
3. **Reutilización de contraseñas**: un único volcado compromete la identidad de los voluntarios y adoptantes en
   múltiples servicios.
4. **Sin trazabilidad**: no es posible saber si las credenciales ya fueron exfiltradas; solo un cambio masivo de
   contraseñas mitiga el daño.
5. **Cumplimiento normativo**: almacenar credenciales legibles viola principios básicos de protección de datos
   (por ejemplo, minimización y seguridad en el tratamiento).

### Código vulnerable original (conservado solo como evidencia)

```java
// HITO 1 — NO USAR EN PRODUCCIÓN
@Transactional
public Usuario registrar(String username, String password, String rol) {
    Usuario usuario = new Usuario();
    usuario.setUsername(username);
    usuario.setPassword(password); // ❌ texto plano
    usuario.setRol(rol);
    return usuarioRepository.save(usuario);
}

@Transactional(readOnly = true)
public Usuario login(String username, String password) {
    Usuario usuario = usuarioRepository.findByUsername(username).orElseThrow();
    if (!usuario.getPassword().equals(password)) { // ❌ comparación directa
        throw new CredencialesInvalidasException();
    }
    return usuario;
}
```

La evidencia del Hito 1 se obtiene registrando el usuario y ejecutando en pgAdmin:

```sql
SELECT * FROM usuarios;
```

Captura esperada: `adoptante_pedro | mi_clave_secreta_123 | USER`.

### Cómo se corrigió (Hito 2)

```java
// HITO 2 — implementación actual en UsuarioService
String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
usuario.setPassword(hash);

// Login
if (!BCrypt.checkpw(passwordRecibida, hashAlmacenado)) {
    throw new CredencialesInvalidasException();
}
```

El hash resultante comienza con `$2a$12$` y tiene 60 caracteres. La columna `VARCHAR(100)` del taller es
suficiente. Si existen filas heredadas del Hito 1, el login actual las **rechaza** porque no son hashes BCrypt
válidos; deben volver a registrarse o forzar un restablecimiento de contraseña.

### Conclusión

El Hito 1 demuestra que la seguridad de una aplicación no depende del formulario de login sino de **cómo se
almacenan y verifican las credenciales**. BCrypt elimina la legibilidad y encarece la fuerza bruta; el JWT
elimina el estado en memoria del servidor y permite verificar la integridad del rol mediante una firma digital.
