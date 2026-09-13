# Asociación Comunal — Cliente Móvil Flutter (Completo y Terminado)

Cliente móvil multiplataforma para residentes y miembros de la comunidad, desarrollado con **Flutter y Dart**, con interfaz moderna basada en **Glassmorphism**, componentes modulares y arquitectura por capas **MVVM + Repository**.

---

## Módulos y Funcionalidades Completas

1. **Estética Glassmorphism & UI Premium:**
   - Contenedores traslúcidos con desenfoque Gaussiano real (`BackdropFilter` e `ImageFilter.blur`).
   - Fondos ambientales con orbes radiales dinámicos.
   - Tarjeta *Hero Dark* de balance en alto contraste, píldora de solvencia y acción rápida central.
   - Botones de acción rápida con tonos pastel (*Aportar*, *Asambleas*, *Votaciones*).
   - Barra de navegación inferior flotante tipo cápsula (`GlassNavBar`).
   - Soporte para Tema Claro y Tema Oscuro.

2. **Autenticación y Seguridad:**
   - Inicio de sesión contra `POST /api/auth/login`.
   - Renovación rotativa automática de sesión contra `POST /api/auth/refresh`.
   - Cierre de sesión con revocación en servidor contra `POST /api/auth/logout`.
   - Pantalla obligatoria de cambio de contraseña para cuentas generadas por DUI contra `POST /api/auth/change-password`.
   - Persistencia local de tokens mediante `SessionStorageService`.

3. **Módulo de Pagos y Aportaciones:**
   - Balance en tiempo real (Total aportado vs. Total pendiente).
   - Filtros por estado (*Todos*, *Pagados*, *Pendientes*).
   - Modal con desglose detallado de comprobante / recibo de aportación.

4. **Vida Comunitaria (Asambleas y Proyectos):**
   - Convocatorias a reuniones con fecha, lugar, tipo y porcentaje de asistencia.
   - Listado de proyectos comunitarios con presupuesto asignado y descripción.

5. **Sistema Electoral y Votaciones Comunitarias:**
   - Consulta de procesos de votación abiertos y cerrados.
   - Emisión de votos por opción (`POST /api/votos`) con diálogo de confirmación.
   - Verificación de participación previa (`GET /api/votaciones/{id}/mi-participacion`).
   - Barras de progreso con porcentajes de resultados en tiempo real para votaciones cerradas o ya votadas.

6. **Vivienda y Censo Habitacional:**
   - Consulta de lote/vivienda asignada al miembro.
   - Dirección, referencias y censo de habitantes (adultos y menores).

7. **Centro de Notificaciones y Avisos:**
   - Modal flotante con alertas sobre asambleas, nuevas votaciones y recordatorios de aportación.

---

## Arquitectura del Proyecto

```text
flutter-app/
├── pubspec.yaml                           # Dependencias y configuración
├── analysis_options.yaml                  # Reglas del linter Dart
├── README.md                              # Documentación completa
├── android/                               # Runner nativo Android preconfigurado
│   ├── app/
│   │   ├── build.gradle.kts
│   │   └── src/main/AndroidManifest.xml
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── web/                                   # Runner web preconfigurado (index.html, manifest.json)
├── test/
│   └── models_test.dart                   # Suite completa de pruebas unitarias
└── lib/
    ├── main.dart                          # Inyección de dependencias y runApp
    ├── core/
    │   ├── constants/api_constants.dart   # Detección de emulador y endpoints
    │   ├── theme/
    │   │   ├── app_colors.dart            # Paleta de color y gradientes
    │   │   └── app_theme.dart             # Material 3 claro y oscuro
    │   ├── utils/
    │   │   ├── formatters.dart            # Formato de moneda ($), fecha y texto
    │   │   └── responsive.dart            # Layout adaptativo
    │   └── widgets/
    │       ├── glass_container.dart       # Core Glassmorphic y AmbientBackground
    │       ├── glass_nav_bar.dart         # Barra flotante tipo cápsula
    │       ├── custom_button.dart         # Botón con gradiente y feedback
    │       ├── custom_text_field.dart     # Input estilizado con alternancia de clave
    │       ├── status_badge.dart          # Píldora de estado (Pagado, Solvente, etc.)
    │       └── empty_state.dart           # Mensaje amigable sin datos
    ├── data/
    │   ├── models/                        # Auth, Pagos, Reuniones, Proyectos, Votaciones, Viviendas, Avisos
    │   ├── services/                      # ApiClient HTTP y SessionStorageService
    │   └── repositories/                  # Auth, Pagos, Comunidad, Votaciones, Viviendas
    ├── viewmodels/                        # Auth, Home, Pagos, Comunidad, Votaciones, Viviendas
    └── views/
        ├── auth/                          # LoginView y ChangePasswordView
        ├── home/                          # HomeView y NotificationsSheet
        ├── payments/                      # PaymentsView
        ├── community/                     # CommunityView y VotingView
        ├── profile/                       # ProfileView y HousingDetailView
        └── main_navigation_shell.dart     # Shell de navegación con 4 destinos
```

---

## Ejecución

```bash
cd flutter-app

# 1. Obtener dependencias
flutter pub get

# 2. Correr pruebas
flutter test

# 3. Iniciar aplicación
flutter run               # En emulador Android o dispositivo móvil
flutter run -d chrome     # En navegador web
```
