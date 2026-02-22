# Contributing to VibeCode AI

## Project Philosophy

VibeCode AI is built as a **learning-first** project. Every architectural decision prioritizes understanding over convenience. This means:

- We intentionally include complexity that forces deep learning (Docker-in-Docker, JWT inter-service auth, RAG pipeline)
- We don't skip "hard parts" — blockers are learning milestones, not obstacles to avoid
- Code quality matters, but learning depth matters more

## Development Workflow

### Branch Strategy

**Main branch:** `main` — stable, always deployable  
**Feature branches:** `feature/service-name-feature` (e.g., `feature/auth-oauth2`)  
**Bugfix branches:** `fix/service-name-issue` (e.g., `fix/billing-duplicate-events`)  
**Blocker branches:** `blocker/name` (e.g., `blocker/docker-in-docker-phase1`)

**Blocker branches** are special: These are where we intentionally break things to learn. Never merge blocker branches to main. They exist to document the learning process.

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat(auth): add Google OAuth2 flow
fix(billing): prevent duplicate Kafka event processing
docs(architecture): update RAG pipeline diagram
refactor(ai-gen): extract prompt template builder
test(user): add integration tests for quota enforcement
chore(deps): bump Spring Boot to 3.2.2
```

**Service-level commits** include service name in scope:
```
feat(auth): implement JWT refresh token rotation
feat(project): add MongoDB index for user queries
fix(notification): WebSocket reconnection logic
```

### Pull Request Process

1. **Self-review first** — Read your own diff. Check for debug code, commented code, TODOs.
2. **Write PR description** — What, why, how. Include:
   - Problem statement
   - Solution approach
   - Tradeoffs considered
   - Testing done
3. **Add tests** — Unit tests for business logic, integration tests for API contracts.
4. **Update docs** — If architecture changed, update ARCHITECTURE.md. If new endpoint, update API_CONTRACTS.md.
5. **Screenshot/video for UI** — Include visual proof for frontend changes.

**PR template:**
```markdown
## Problem
[Describe the issue or feature request]

## Solution
[Explain your approach]

## Tradeoffs
[What alternatives were considered? Why this approach?]

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style
- [ ] Documentation updated
- [ ] No breaking changes (or documented if unavoidable)
- [ ] Logs include structured fields (traceId, userId, action)
```

### Code Review Guidelines

**For reviewer:**
- **Architecture fit** — Does this align with service boundaries? Should this be in a different service?
- **Learning value** — Does this solution teach something? Or is it a shortcut that skips learning?
- **Production readiness** — Could this run in prod? (Even if we're in dev phase, think ahead)
- **Observability** — Are there logs? Metrics? Tracing spans?

**For author:**
- Respond to all comments, even if just "Fixed" or "Good point, will change"
- Don't take criticism personally — we're all learning
- Ask questions if review feedback is unclear

### Merge Requirements

- ✅ CI pipeline passes (build, test, Docker image)
- ✅ At least 1 approval (if working in team)
- ✅ No merge conflicts
- ✅ Branch up-to-date with main

Merge strategy: **Squash and merge** — keeps main history clean.

## Code Style

### Java (Spring Boot Services)

**Formatting:** Google Java Style with minor tweaks (see `.editorconfig`)

**Structure:**
```
src/main/java/ai/vibecode/[service]/
├── controller/       # REST endpoints (@RestController)
├── service/          # Business logic (@Service)
├── repository/       # Data access (@Repository, JPA interfaces)
├── model/
│   ├── entity/       # JPA entities
│   └── dto/          # Request/response DTOs
├── config/           # Spring @Configuration
├── security/         # Auth filters, JWT utils
├── exception/        # Custom exceptions, @ControllerAdvice
└── client/           # OpenFeign interfaces (@FeignClient)
```

**Naming conventions:**
- Entities: `User`, `Project`, `UsageLog` (singular, not `Users`)
- DTOs: `UserRegistrationRequest`, `ProjectResponse` (suffix: Request/Response)
- Services: `UserService`, `BillingService` (suffix: Service)
- Controllers: `AuthController`, `ProjectController` (suffix: Controller)

**Dependencies:** Inject via constructor (not @Autowired fields):
```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final KafkaTemplate kafkaTemplate;

    public UserService(UserRepository userRepository, KafkaTemplate kafkaTemplate) {
        this.userRepository = userRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
}
```

**Logging:**
```java
@Slf4j  // Lombok
@Service
public class BillingService {
    public void processUsageEvent(UsageEvent event) {
        log.info("Processing usage event", 
            Map.of(
                "eventId", event.getId(),
                "userId", event.getUserId(),
                "tokensUsed", event.getTokensUsed()
            )
        );
    }
}
```

Structured logs (Map of fields) over string concatenation.

### TypeScript (React Frontend)

**Formatting:** Prettier with default config

**Structure:**
```
src/
├── components/
│   ├── auth/         # Login, Register
│   ├── editor/       # Monaco wrapper
│   ├── projects/     # Project list, detail
│   └── common/       # Reusable (Button, Modal)
├── hooks/            # Custom React hooks
├── services/         # API clients (axios)
├── store/            # State management (Zustand)
├── types/            # TypeScript interfaces
└── utils/            # Helpers
```

**Naming:**
- Components: `PascalCase.tsx` (e.g., `ProjectList.tsx`)
- Hooks: `use` prefix (e.g., `useAuth.ts`, `useWebSocket.ts`)
- Services: `camelCase.service.ts` (e.g., `auth.service.ts`)

**API calls:**
```typescript
// services/auth.service.ts
export const authService = {
  async login(email: string, password: string): Promise<AuthResponse> {
    const { data } = await axios.post('/api/auth/login', { email, password });
    return data;
  },
};
```

### Database Migrations

**Flyway (PostgreSQL):**
```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__add_oauth_fields.sql
└── V3__create_refresh_tokens_table.sql
```

**Naming:** `V{version}__{description}.sql` (double underscore)

**MongoDB migrations (if needed):**
```javascript
// scripts/migrations/001_create_indexes.js
db.projects.createIndex({ userId: 1, createdAt: -1 });
```

Run manually via `mongosh`.

## Testing Guidelines

### Unit Tests

**Coverage target:** 80% for service layer, 60% overall

**Structure:**
```java
@SpringBootTest
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Should create user with hashed password")
    void shouldCreateUserWithHashedPassword() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest("test@example.com", "Pass1234!");
        
        // When
        User user = userService.register(request);
        
        // Then
        assertThat(user.getPasswordHash()).isNotEqualTo("Pass1234!");
        assertThat(BCrypt.checkpw("Pass1234!", user.getPasswordHash())).isTrue();
    }
}
```

**Use AssertJ** over JUnit assertions: `assertThat(x).isEqualTo(y)` more readable than `assertEquals(y, x)`.

### Integration Tests

**Testcontainers** for database tests:
```java
@SpringBootTest
@Testcontainers
class UserRepositoryIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void shouldPersistUser() {
        // Test with real database
    }
}
```

### API Tests

**RestAssured** for endpoint tests:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthControllerIntegrationTest {
    @LocalServerPort
    private int port;

    @Test
    void shouldReturnJwtOnValidLogin() {
        given()
            .contentType(ContentType.JSON)
            .body(new LoginRequest("test@example.com", "Pass1234!"))
        .when()
            .post("http://localhost:" + port + "/auth/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .body("expiresIn", equalTo(900));  // 15 minutes
    }
}
```

## Documentation

### When to Update Docs

- **ARCHITECTURE.md** — Any design pattern change, new service, database schema change
- **API_CONTRACTS.md** — New endpoint, changed request/response format
- **SETUP.md** — New dependency, changed startup order, environment variable
- **README.md** — Major feature added, tech stack change

### Architecture Decision Records (ADRs)

For significant decisions, create an ADR:

```
docs/adr/
├── 001-use-separate-repos-per-service.md
├── 002-ollama-over-openai.md
└── 003-jwt-forwarding-pattern.md
```

**Template:**
```markdown
# ADR-003: JWT Forwarding Pattern for Inter-Service Auth

## Status
Accepted

## Context
Services need to know which user initiated a request. Options:
1. Gateway-only validation (internal network trusted)
2. JWT forwarding (zero-trust)
3. Service-to-service tokens (mTLS)

## Decision
Use JWT forwarding with OpenFeign RequestInterceptor. Each service validates JWT independently.

## Consequences
**Positive:**
- Defense-in-depth security
- Works in cloud environments where internal network isn't trusted
- Simple to implement with Spring Security

**Negative:**
- Extra CPU cycles per hop (JWT signature verification)
- More complex than gateway-only validation

## Alternatives Considered
- **Gateway-only:** Too risky, single point of failure
- **mTLS:** Over-engineered for learning project, adds Istio complexity
```

## Common Pitfalls

### Don't Do This

❌ **Hardcoded URLs in service calls**
```java
restTemplate.getForObject("http://localhost:8082/users/1", User.class);  // BAD
```

✅ **Use Eureka service names**
```java
@FeignClient(name = "user-service")
interface UserClient {
    @GetMapping("/users/{id}")
    UserResponse getUser(@PathVariable String id);
}
```

---

❌ **Catch-all exception handlers**
```java
try {
    // ...
} catch (Exception e) {
    log.error("Something went wrong");  // BAD
}
```

✅ **Specific exceptions with context**
```java
try {
    // ...
} catch (UserNotFoundException e) {
    log.error("User not found", Map.of("userId", userId, "action", "getProfile"));
    throw e;
}
```

---

❌ **Synchronous Kafka publish with `.get()`**
```java
kafkaTemplate.send("topic", message).get();  // BAD - blocks thread
```

✅ **Async publish with callback**
```java
kafkaTemplate.send("topic", message)
    .addCallback(
        success -> log.info("Event published", Map.of("topic", "topic")),
        failure -> log.error("Event failed", Map.of("error", failure.getMessage()))
    );
```

## Getting Help

- **Slack:** #vibecode-dev (if team exists)
- **Issues:** Use GitHub Issues for bugs, GitHub Discussions for questions
- **Code review:** Tag @yourname for review requests
- **Stuck on blocker:** This is expected! Document what you tried, what failed, what you learned. Then ask.

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
