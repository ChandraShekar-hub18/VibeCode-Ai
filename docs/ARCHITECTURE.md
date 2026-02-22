# Architecture Documentation

## System Design Overview

VibeCode AI is a microservices-based SaaS platform implementing event-driven architecture with CQRS-lite patterns. The system prioritizes learning depth over operational simplicity — intentionally including production patterns that force understanding of distributed systems tradeoffs.

## Core Principles

1. **Database-per-Service** — No shared database tables. Services own their data schema.
2. **API Gateway as Single Entry** — All external traffic enters through port 8080. Internal calls use service mesh (Eureka).
3. **Event-Driven Async** — State changes publish Kafka events. No request-response for cross-service side effects.
4. **Defense-in-Depth Security** — JWT validated at Gateway AND each service. Zero-trust internal network.
5. **Observability-First** — Structured logging, distributed tracing, and metrics from day one.

## Service Topology

```
┌─────────────────────────────────────────────────────────────┐
│                     API Gateway :8080                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ - JWT Validation (Spring Security FilterChain)      │   │
│  │ - Rate Limiting (Resilience4J RateLimiter)         │   │
│  │ - Request Routing (Spring Cloud Gateway)           │   │
│  │ - SSL Termination                                   │   │
│  └─────────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Eureka Registry     │
              │  :8761               │
              │  Service Discovery   │
              └──────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    [Auth Svc]     [User Svc]     [Project Svc]
      :8081          :8082            :8083
         │               │               │
         └───────┬───────┴───────┬───────┘
                 │               │
                 ▼               ▼
         [AI Generation]   [Code Executor]
              :8084            :8085
                 │               │
                 └───────┬───────┘
                         ▼
                   Apache Kafka
                         │
         ┌───────────────┼───────────────┐
         ▼               ▼               ▼
    [Billing]    [Notification]   [User Svc]
      :8086          :8087           (update)
```

## Data Architecture

### Database Selection Rationale

| Service | Database | Why |
|---------|----------|-----|
| Auth | PostgreSQL | ACID transactions for auth state. Mature ecosystem. |
| User | PostgreSQL | Relational user-profile-role model. JPA mappings straightforward. |
| Billing | PostgreSQL | Financial data requires ACID. Complex JOINs for analytics queries. |
| Project | MongoDB | Nested file trees are document-shaped. Schema flexibility for code structure. |
| AI Generation | pgvector | Vector similarity search. PostgreSQL extension = familiar tooling. |
| Cache | Redis | In-memory KV for session + cache. Pub/Sub for WebSocket fanout. |

### Schema Ownership

Each service has full autonomy over its schema. No foreign keys across databases. Cross-service references use UUIDs, validated via API calls or event consistency.

**Example: User deletion cascade**
```
1. User Service: DELETE user → publish user.deleted event
2. Auth Service: consume user.deleted → DELETE auth record
3. Billing Service: consume user.deleted → anonymize usage_logs (GDPR)
4. Project Service: consume user.deleted → transfer projects to [deleted-user]
```

Event-driven eventual consistency, not synchronous cascades.

## Inter-Service Communication

### Synchronous (REST via OpenFeign)

Used for:
- Read operations (GET /users/{id})
- Immediate validation (POST /auth/validate-token)
- Transactional flows requiring rollback

**JWT Propagation Pattern:**
```java
@Component
public class FeignJwtInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // Extract JWT from SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String) {
            template.header("Authorization", "Bearer " + auth.getCredentials());
        }
    }
}
```

Every service validates JWT independently. No "trusted internal network" assumption.

### Asynchronous (Kafka)

Used for:
- Side effects (billing, notifications)
- Audit logging
- Analytics pipelines

**Topic Design:**
- `generation.complete` — AI service publishes, Billing/Notification consume
- `user.quota.exceeded` — Billing publishes, Notification consumes
- `payment.success` — Billing publishes, User/Notification consume

**Consumer Groups:**
- Each logical service = separate consumer group
- Billing and Notification both consume `generation.complete` independently
- Guarantees: At-least-once delivery, idempotent consumers via event_id deduplication

## Security Model

### External Security (Client → Gateway)

1. **HTTPS only** — TLS 1.3, SSL termination at Gateway
2. **JWT validation** — Spring Security `JwtAuthenticationFilter` at Gateway
3. **Rate limiting** — Resilience4J `RateLimiter` per user IP (100 req/min default)
4. **CSRF protection** — SameSite cookies, CSRF tokens for state-changing ops
5. **CORS** — Whitelist origins via Spring Cloud Gateway CORS config

### Internal Security (Service → Service)

**Pattern: JWT Forwarding with Independent Validation**

1. Gateway validates JWT, adds `X-Authenticated-User` header, forwards request
2. Downstream service re-validates JWT signature (defense-in-depth)
3. OpenFeign `RequestInterceptor` propagates `Authorization` header automatically

**Why not service-to-service tokens (mTLS)?**  
Added complexity. For learning project, JWT forwarding teaches real-world auth patterns. Production evolution would add Istio service mesh with mTLS.

### Code Execution Security

Code Executor Service runs untrusted user code. Security layers:

1. **Docker isolation** — Separate container per execution
2. **Resource limits** — `--memory 128m --cpu-quota 50000`
3. **No network** — `--network none`
4. **Capability dropping** — `--cap-drop ALL`
5. **Read-only filesystem** — `--read-only`
6. **Hard timeout** — Java `ExecutorService` with 10s `Future.get()` timeout

**Known limitation:** Docker-in-Docker still shares kernel with host. Production upgrade path: gVisor (user-space kernel) or Firecracker (microVM isolation).

## AI Architecture

### Spring AI Abstraction

```java
@Service
public class CodeGenerationService {
    private final ChatModel chatModel;  // Abstraction — OpenAI or Ollama
    private final VectorStore vectorStore;  // pgvector via Spring AI
    
    public Flux<String> generate(String prompt) {
        // 1. RAG: Retrieve context
        List<Document> context = vectorStore.similaritySearch(
            SearchRequest.query(prompt).withTopK(3)
        );
        
        // 2. Enrich prompt
        String enrichedPrompt = buildPrompt(prompt, context);
        
        // 3. Stream from LLM
        return chatModel.stream(new Prompt(enrichedPrompt));
    }
}
```

**Swap Ollama → OpenAI:**
```yaml
# Dev: Ollama (local, free)
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: codellama:7b-instruct-q4_K_M

# Prod: OpenAI (swap config only, zero code changes)
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        model: gpt-4o
```

### RAG Pipeline

```
User Prompt
    ↓
Embed via nomic-embed-text (Ollama)
    ↓
pgvector: SELECT ... ORDER BY embedding <=> ? LIMIT 3
    ↓
Retrieved: [snippet1, snippet2, snippet3]
    ↓
Inject into prompt template:
  "You are a code generator. Here are similar examples:
   [snippet1]
   [snippet2]
   [snippet3]
   Now generate: {user_prompt}"
    ↓
Send to CodeLlama 7B
    ↓
Stream response token-by-token (SSE)
```

**Vector index:** HNSW (Hierarchical Navigable Small World) for approximate nearest neighbor search. O(log n) query time, 95%+ recall at 10ms latency for <100k vectors.

### MCP (Model Context Protocol)

Spring AI's tool-use abstraction. Exposes Java methods as LLM-callable tools:

```java
@Component
@Description("Creates a new file in the project")
public class CreateFileTool implements Function<CreateFileRequest, String> {
    @Override
    public String apply(CreateFileRequest req) {
        // Validate, write file, return status
        return "File created: " + req.path();
    }
}

// LLM can now call: create_file(path="src/App.jsx", content="...")
```

CodeLlama orchestrates multi-step flows: create package.json → install deps → create components → write tests.

## Observability

### Logging (ELK Stack)

**Structured JSON logs** via Logstash pattern:
```json
{
  "timestamp": "2025-02-17T10:30:45.123Z",
  "level": "INFO",
  "service": "ai-generation-service",
  "traceId": "abc123",
  "spanId": "def456",
  "userId": "uuid-here",
  "action": "generation.complete",
  "tokensUsed": 1234,
  "durationMs": 8234
}
```

Indexed in Elasticsearch. Kibana dashboards for error rates, latency p95/p99, user activity.

### Tracing (Zipkin + Spring Cloud Sleuth)

Distributed trace across services:
```
[Gateway] POST /generate → [AI Gen Svc] → [pgvector query] → [Ollama HTTP] → [Kafka publish]
```

Each service adds span. Zipkin visualizes full request flow with timing breakdown.

**Setup:** Just add `spring-cloud-starter-sleuth` + `spring-cloud-sleuth-zipkin` dependencies. Zero code changes. Trace ID propagated via HTTP headers automatically.

### Metrics (Spring Boot Actuator + Prometheus)

Key metrics exposed at `/actuator/prometheus`:
- JVM: heap, GC pauses, thread count
- HTTP: request rate, latency histogram, error rate
- Custom: tokens consumed, quota exceeded events, cache hit rate

Grafana dashboards pre-configured in `k8s/monitoring/grafana-dashboards.yaml`.

## Deployment

### Local Development

**Docker Compose** for infrastructure:
```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: auth_db
  
  postgres-vector:
    image: ankane/pgvector:latest  # pgvector pre-installed
    environment:
      POSTGRES_DB: rag_db
  
  mongodb:
    image: mongo:7
  
  redis:
    image: redis:7-alpine
  
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_HEAP_OPTS: "-Xmx256m -Xms128m"  # Limit memory
```

**JVM tuning per service:**
```bash
export JAVA_OPTS="-Xms128m -Xmx256m"  # Small services
export JAVA_OPTS="-Xms256m -Xmx512m"  # AI Generation (needs more heap)
```

### Production (Kubernetes on AWS EKS)

**Manifest structure:**
```
k8s/
├── base/
│   ├── deployment.yaml       # Common deployment template
│   ├── service.yaml          # ClusterIP service
│   └── configmap.yaml        # Spring Cloud Config
├── overlays/
│   ├── dev/
│   ├── staging/
│   └── prod/
│       ├── kustomization.yaml
│       ├── hpa.yaml          # Horizontal Pod Autoscaler
│       ├── ingress.yaml      # ALB ingress controller
│       └── secrets.yaml      # Sealed secrets
└── monitoring/
    ├── prometheus.yaml
    ├── grafana.yaml
    └── elasticsearch.yaml
```

**Autoscaling:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-generation-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-generation
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Pods
    pods:
      metric:
        name: kafka_consumer_lag
      target:
        type: AverageValue
        averageValue: "100"
```

**CI/CD Pipeline (AWS CodePipeline):**
1. Source: GitHub webhook trigger
2. Build: `mvn clean package` + Docker build
3. Test: Unit + integration tests (Testcontainers)
4. Push: ECR (Elastic Container Registry)
5. Deploy: `kubectl apply -k overlays/prod`
6. Smoke tests: Health check + E2E test suite
7. Rollback: Automatic on failed health checks

## Performance Considerations

### Expected Latencies (p95)

| Operation | Target | Notes |
|-----------|--------|-------|
| Auth login | <200ms | BCrypt verification + JWT generation |
| Token validation | <10ms | JWT signature verify (CPU-bound) |
| Vector search (RAG) | <50ms | HNSW index, 10k vectors, top-3 retrieval |
| LLM first token | <2s | CodeLlama 7B cold start on 4GB VRAM |
| LLM streaming | 15-25 tok/s | GPU inference, Q4 quantization |
| Code execution | <5s | ProcessBuilder + timeout, no Docker overhead |
| Kafka publish | <5ms | Async, no ack wait |

### Bottlenecks & Mitigations

**1. Ollama throughput**  
Problem: Single Ollama instance = 1 concurrent generation max  
Mitigation: Queue requests in AI Generation Service. Scale Ollama horizontally (multiple instances behind load balancer). Future: Batch inference.

**2. pgvector query latency**  
Problem: Linear scan on >100k vectors  
Mitigation: HNSW index (already implemented). Pre-filter by language/framework before vector search. Shard by category if >1M vectors.

**3. JWT validation overhead**  
Problem: Every service re-validates = N × RSA signature verification per request  
Mitigation: Cache validated tokens in Redis (5min TTL). Check cache before verify.

**4. Kafka consumer lag**  
Problem: Billing service slow → Kafka topic backpressure  
Mitigation: Scale consumer instances. Monitor lag via Prometheus. Alert on >1000 lag.

## Known Tradeoffs & Technical Debt

### Chosen Tradeoffs

1. **Ollama vs OpenAI** — Zero cost, learning depth > production performance. Swap in prod.
2. **ProcessBuilder vs Docker SDK** — Easier to implement, acceptable for MVP. Upgrade in Phase 5.
3. **Separate repos** — More setup overhead, but teaches real multi-repo CI/CD patterns.
4. **JWT forwarding vs mTLS** — Simpler, sufficient for learning. Istio service mesh for prod.

### Intentional Technical Debt (Learning Blockers)

1. **No circuit breaker on Ollama calls** — Will hit it, add Resilience4J in Phase 3.
2. **No saga pattern for distributed transactions** — Keep it simple, rely on event eventual consistency.
3. **Redis not persistent** — Cache-only acceptable for dev. Add RDB snapshots in staging.
4. **No blue-green deployment** — K8s rolling updates sufficient. Add Argo Rollouts for canary deploys later.

## References

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)
- [Kafka: The Definitive Guide](https://www.confluent.io/resources/kafka-the-definitive-guide/)
- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [Martin Fowler: Microservices](https://martinfowler.com/articles/microservices.html)
- [CNCF Cloud Native Landscape](https://landscape.cncf.io/)

---

**Last Updated:** 2025-02-17  
**Author:** Chandra Shekar Mekala 
**Review Cycle:** Update after each major architectural decision
