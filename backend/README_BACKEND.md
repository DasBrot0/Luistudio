### Backend — Spring Boot + Java + Spring Data JPA

```
src/main/
├── java/com/luistudio/reservas/
│   │
│   ├── config/           # SecurityConfig (JWT), CorsConfig, MailConfig
│   │
│   ├── controller/       # REST controllers por dominio
│   │   # AuthController, ReservaController, SalaController,
│   │   # UsuarioController, MantenimientoController,
│   │   # NotificacionController, AuditoriaController
│   │
│   ├── service/          # Lógica de negocio, uno por dominio
│   │   # + interfaces separadas de implementaciones
│   │
│   ├── repository/       # Interfaces que extienden JpaRepository
│   │
│   ├── model/            # Entidades JPA (@Entity)
│   │   # Reserva, Sala, Usuario, Mantenimiento,
│   │   # Notificacion, RegistroSeguridad, AuditoriaLog
│   │
│   ├── dto/              # Objetos de entrada/salida de la API
│   │   ├── request/      # LoginRequest, CrearReservaRequest, etc.
│   │   └── response/     # ReservaResponse, UsuarioResponse, etc.
│   │
│   ├── security/         # JwtUtil, JwtFilter, UserDetailsServiceImpl
│   │
│   ├── exception/        # GlobalExceptionHandler, excepciones custom
│   │
│   ├── scheduler/        # Jobs programados (recordatorios, bloqueos)
│   │
│   └── util/             # Helpers: generadores ICS, formateadores
│
└── resources/
    ├── application.yml        # Config general (puerto, datasource, JWT)
    ├── application-dev.yml    # Config local
    ├── application-prod.yml   # Config Render/Railway
    └── db/migration/          # Scripts SQL versionados (si usan Flyway)
```
