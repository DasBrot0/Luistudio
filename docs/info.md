**Backend Spring Boot**
Lo más importante:

* **Arquitectura por capas**
  * **controller**: recibe requests HTTP.
  * **service**: lógica de negocio.
  * **repository**: acceso a base de datos.
  * **model/entity**: tablas representadas como clases.
  * **dto**: objetos que entran/salen por API.
* **Anotaciones Spring**
  * **@RestController**: clase que expone endpoints.
  * **@RequestMapping**: ruta base del controller.
  * **@GetMapping**, **@PostMapping**, **@PutMapping**, **@DeleteMapping**: endpoints HTTP.
  * **@RequestBody**: cuerpo JSON del request.
  * **@PathVariable**: variable en la URL, tipo **/bookings/{id}**.
  * **@RequestParam**: query param, tipo **?page=0**.
  * **@Service**: clase de lógica de negocio.
  * **@Repository**: clase/interfaz de persistencia.
  * **@Component**: bean genérico manejado por Spring.
  * **@Autowired** o inyección por constructor: cómo Spring conecta clases.
  * **@Transactional**: abre una transacción de BD.
* **Spring Data JPA**
  * **JpaRepository**: CRUD automático.
  * Métodos por nombre: **findByEmail**, **findByUsuarioOrderByFechaDesc**.
  * Relaciones entre entidades: **@ManyToOne**, **@OneToMany**.
  * **@Entity**, **@Table**, **@Column**, **@Id**.
* **DTOs y validación**
  * Por qué no se devuelve directamente una entidad.
  * **record** en Java para requests/responses.
  * Validaciones tipo **@NotNull**, **@Email**, **@Size**.
  * Mapeo entidad → DTO, como hace **DtoMapper**.
* **Seguridad**
  * JWT o cookie de sesión.
  * **HttpOnly**, **Secure**, **SameSite**.
  * Filtros de Spring Security.
  * Roles: **ADMIN**, **ESTUDIANTE**.
  * Guards/checks de permisos, por ejemplo **AccessGuard**.
* **Excepciones**
  * Excepciones de negocio tipo **BusinessException**.
  * **@ControllerAdvice** / handler global.
  * Códigos HTTP: **400**, **401**, **403**, **404**, **500**.
* **Persistencia y SQL**
  * Migraciones/seed: **001_init.sql**, **002_seed_release01.sql**.
  * Llaves foráneas.
  * Índices.
  * Constraints.
  * Diferencia entre datos reales y datos seed.
* **Patrones usados**
  * Strategy: login normal vs 2FA.
  * Facade: AuthService como entrada estable del modulo de autenticacion.
  * Strategy: politicas de recordatorios programados.
  * Rules/Strategy: reglas de validación de reservas.

**Frontend React**
Lo más importante:

* **Componentes**
  * Componentes funcionales.
  * Props.
  * Estado local con **useState**.
  * Efectos con **useEffect**.
  * Render condicional.
* **Routing**
  * React Router.
  * Rutas tipo **/login**, **/reservas**, **/salas**.
  * Navegación con **useNavigate**.
  * Protección de rutas según sesión/rol.
* **Servicios API**
  * **fetch** o cliente centralizado en **services/api.ts**.
  * Métodos **GET**, **POST**, **PUT**, **DELETE**.
  * Enviar JSON.
  * Manejar errores del backend.
  * **credentials: "include"** si usan cookies.
* **Autenticación en frontend**
  * Estado de usuario actual.
  * Cargar **/me**.
  * Login/logout.
  * Qué se guarda en memoria/localStorage y qué no.
  * Diferencia entre token visible en JS y cookie **HttpOnly**.
* **Estado y flujo de datos**
  * Estado de formularios.
  * Estado de carga: **loading**.
  * Estado de error.
  * Evitar requests duplicados.
  * Sincronizar UI después de crear/editar/cancelar reserva.
* **TypeScript**
  * Interfaces/types para usuarios, salas, reservas.
  * Tipos de respuesta API.
  * Props tipadas.
  * Manejo de **null**/**undefined**.
* **Tailwind CSS**
  * Clases utilitarias.
  * Responsive: **sm:**, **md:**, **lg:**.
  * Estados visuales: **hover:**, **disabled:**.
  * Layout con flex/grid.

**Conceptos Cliente-Servidor**
Estos conectan ambos mundos:

* Qué es una API REST.
* Qué significa request/response.
* Códigos HTTP.
* CORS.
* Preflight **OPTIONS**.
* Cookies cross-site.
* JSON.
* Latencia: frontend vs backend vs DB.
* Variables de entorno: **.env.backend**, **.env.frontend**.

**Para este proyecto en particular, estudiaría en este orden**

1. Controllers de backend: cómo entra una request.
2. Services: dónde está la lógica real.
3. Repositories/entities: cómo se guarda en BD.
4. **api.ts** del frontend: cómo llama al backend.
5. Pages de React: cómo se usan esos datos.
6. Auth completa: login → cookie/token → **/me** → rutas protegidas.
7. Reservas completa: formulario → POST → validaciones → DB → respuesta.
