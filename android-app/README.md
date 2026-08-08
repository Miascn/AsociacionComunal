# Aplicación Android — Asociación Comunal

Cliente para residentes construido con Kotlin, Jetpack Compose y Material 3.

## Configuración

La URL de la API nunca se fija en el código fuente. Para desarrollo, agregue en `~/.gradle/gradle.properties`:

```properties
API_BASE_URL=https://servidor-ejemplo.ngrok.app/
```

La URL debe terminar en `/`. En el emulador, el valor predeterminado `http://10.0.2.2:8080/` apunta al equipo anfitrión.

## Alcance 0.1.0

- autenticación contra `/api/auth/login`;
- recuperación de identidad con `/api/me`;
- renovación rotativa de sesión;
- persistencia local cifrada con AES-GCM y Android Keystore sobre DataStore;
- tema claro y oscuro según el sistema;
- navegación Inicio, Pagos, Comunidad y Mi cuenta;
- cierre de sesión con revocación en el servidor.

Pagos, proyectos, reuniones y votaciones todavía no están implementados.

## Actualizaciones

La aplicación consulta el canal público de actualizaciones de la API. GitHub Actions compila y firma cada APK, y lo publica en ese canal usando credenciales que nunca llegan al teléfono. Cuando existe una versión superior muestra el cambio, descarga el APK con progreso y abre el instalador oficial de Android.
