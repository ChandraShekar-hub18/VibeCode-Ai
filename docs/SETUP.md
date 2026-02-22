# Development Environment Setup

## Prerequisites

- **Java:** 21 (LTS) — `java -version`
- **Maven:** 3.9+ — `mvn -version`
- **Docker:** 24+ with Docker Compose V2 — `docker compose version`
- **Node.js:** 18+ — `node -v`
- **Git:** 2.40+ — `git --version`
- **Ollama:** Latest — [ollama.ai](https://ollama.ai)

**Recommended:**
- IntelliJ IDEA Ultimate (Spring Boot plugin included)
- Postman or HTTPie for API testing
- K9s for Kubernetes cluster management
- jq for JSON parsing in scripts

## Repository Structure

VibeCode AI uses **separate repositories** per service. Clone all repos into a common parent directory:

```bash
mkdir vibecode-ai && cd vibecode-ai

# Core services
git clone git@github.com:yourorg/eureka-registry.git
git clone git@github.com:yourorg/api-gateway.git
git clone git@github.com:yourorg/auth-service.git
git clone git@github.com:yourorg/user-service.git
git clone git@github.com:yourorg/project-service.git
git clone git@github.com:yourorg/ai-generation-service.git
git clone git@github.com:yourorg/code-executor-service.git
git clone git@github.com:yourorg/billing-service.git
git clone git@github.com:yourorg/notification-service.git

# Frontend
git clone git@github.com:yourorg/vibecode-ui.git

# Shared infrastructure
git clone git@github.com:yourorg/vibecode-infra.git  # docker-compose, k8s manifests
```

**Or use the clone script:**
```bash
curl -O https://raw.githubusercontent.com/yourorg/vibecode-infra/main/scripts/clone-all.sh
chmod +x clone-all.sh && ./clone-all.sh
```

## Infrastructure Setup

### 1. Docker Compose for Local Services

Start backing services (databases, Kafka, Redis):

```bash
cd vibecode-infra
docker compose up -d

# Verify all containers running
docker compose ps
```

**Expected services:**
- `postgres` (5432) — Auth, User, Billing DBs
- `postgres-vector` (5433) — pgvector for RAG
- `mongodb` (27017) — Project service
- `redis` (6379) — Cache + Pub/Sub
- `zookeeper` (2181) — Kafka dependency
- `kafka` (9092) — Event bus
- `schema-registry` (8081) — Avro schemas
- `elasticsearch` (9200) — Log aggregation
- `logstash` (5044) — Log shipper
- `kibana` (5601) — Log UI
- `zipkin` (9411) — Distributed tracing

**Memory limits configured:** Each service capped at 256-512MB to fit 16GB RAM machines. Adjust in `docker-compose.yml` if needed.

### 2. Ollama Setup

Install Ollama:
```bash
# macOS
brew install ollama

# Linux
curl -fsSL https://ollama.ai/install.sh | sh

# Windows
# Download from https://ollama.ai/download
```

Pull required models:
```bash
ollama serve &  # Start Ollama server

# Primary: Code generation (4GB VRAM)
ollama pull codellama:7b-instruct-q4_K_M

# Embeddings: RAG pipeline (300MB)
ollama pull nomic-embed-text

# Optional: Fallback general model
ollama pull mistral:7b-instruct-q4_K_M
```

Verify:
```bash
curl http://localhost:11434/api/tags  # Should list models
```

### 3. Database Initialization

Run schema migrations:
```bash
# Auth service
cd auth-service
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/auth_db

# User service
cd ../user-service
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/user_db

# Billing service
cd ../billing-service
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/billing_db

# pgvector setup (for RAG)
cd ../ai-generation-service
psql -h localhost -p 5433 -U postgres -d rag_db -f src/main/resources/db/init.sql
```

**MongoDB indexes** (run once):
```bash
cd ../project-service
npm run setup-indexes  # Creates indexes defined in schema
```

### 4. Kafka Topics

Create topics with correct partition counts:
```bash
docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic generation.complete \
  --partitions 3 \
  --replication-factor 1

docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic user.quota.exceeded \
  --partitions 1 \
  --replication-factor 1

docker exec -it kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic payment.success \
  --partitions 1 \
  --replication-factor 1
```

Verify:
```bash
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092
```

## Service Startup Order

**Critical:** Services must start in dependency order.

### Step 1: Discovery & Config
```bash
# Terminal 1: Eureka Registry
cd eureka-registry
mvn spring-boot:run

# Wait for: "Registered instance EUREKA-REGISTRY with status UP"
```

Verify: http://localhost:8761 — should show empty dashboard.

### Step 2: API Gateway
```bash
# Terminal 2: API Gateway
cd api-gateway
mvn spring-boot:run

# Wait for: "Netty started on port 8080"
```

Verify: http://localhost:8080/actuator/health — should return `{"status":"UP"}`

### Step 3: Core Services (parallel)
```bash
# Terminal 3: Auth Service
cd auth-service && mvn spring-boot:run &

# Terminal 4: User Service
cd user-service && mvn spring-boot:run &

# Terminal 5: Project Service
cd project-service && mvn spring-boot:run &
```

Check Eureka: http://localhost:8761 — should show 3 services registered.

### Step 4: AI Generation Service
```bash
# Terminal 6: AI Generation Service
cd ai-generation-service
export OLLAMA_BASE_URL=http://localhost:11434  # Ensure Ollama is reachable
mvn spring-boot:run
```

**First run:** RAG vector seeding takes ~2 minutes. Watch logs for "Indexed 5000 code patterns".

### Step 5: Remaining Services
```bash
# Terminal 7: Code Executor
cd code-executor-service && mvn spring-boot:run &

# Terminal 8: Billing Service
cd billing-service && mvn spring-boot:run &

# Terminal 9: Notification Service
cd notification-service && mvn spring-boot:run &
```

### Step 6: Frontend
```bash
# Terminal 10: React UI
cd vibecode-ui
npm install
npm run dev

# Opens browser at http://localhost:3000
```

## Verification Checklist

### Health Checks
```bash
# All services should return {"status":"UP"}
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # User
curl http://localhost:8083/actuator/health  # Project
curl http://localhost:8084/actuator/health  # AI Gen
curl http://localhost:8085/actuator/health  # Code Executor
curl http://localhost:8086/actuator/health  # Billing
curl http://localhost:8087/actuator/health  # Notification
```

### Service Discovery
```bash
# Check Eureka dashboard
open http://localhost:8761

# Should show 8 registered services
```

### Database Connections
```bash
# PostgreSQL
psql -h localhost -p 5432 -U postgres -c "\l"  # List databases

# MongoDB
docker exec -it mongodb mongosh --eval "show dbs"

# Redis
docker exec -it redis redis-cli ping  # Should return PONG
```

### Kafka
```bash
# Check topics
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Test produce/consume
docker exec kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic generation.complete

# Type a test message, Ctrl+C to exit

docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic generation.complete \
  --from-beginning
```

### Ollama
```bash
# Test code generation
curl http://localhost:11434/api/generate -d '{
  "model": "codellama:7b-instruct-q4_K_M",
  "prompt": "Write a hello world in Python",
  "stream": false
}'
```

### End-to-End Test
```bash
# 1. Register user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}'

# 2. Login (get JWT)
JWT=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}' \
  | jq -r '.token')

# 3. Create project
curl -X POST http://localhost:8080/api/projects \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"name":"Test Project","prompt":"Build a todo app"}'

# 4. Generate code (streaming)
curl -X POST http://localhost:8080/api/generate/stream \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{"prompt":"Create a React button component"}'
```

## Development Tools

### IntelliJ IDEA Configuration

**Import services:**
1. File → New → Project from Existing Sources
2. Select `vibecode-ai` parent directory
3. Import all Maven projects
4. Enable Spring Boot run configuration auto-detection

**Recommended plugins:**
- Spring Boot Assistant
- Lombok
- Docker
- Kubernetes

**Run configurations:**
Create compound run config:
```
Name: All Services
Run: eureka-registry, api-gateway, auth-service, user-service, project-service, ai-generation-service, code-executor-service, billing-service, notification-service
```

### VS Code Configuration

**Extensions:**
- Spring Boot Extension Pack
- Java Extension Pack
- Docker
- Kubernetes
- MongoDB for VS Code

**launch.json:**
```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Eureka Registry",
      "request": "launch",
      "mainClass": "ai.vibecode.eureka.EurekaRegistryApplication",
      "projectName": "eureka-registry"
    }
    // ... repeat for each service
  ]
}
```

### Testing Tools

**Postman collection:**
Import `docs/api/vibecode-postman.json` — includes all endpoints with example requests.

**K6 load testing:**
```bash
brew install k6  # or snap install k6
k6 run tests/load/generation-flow.js --vus 10 --duration 30s
```

**Database clients:**
- DBeaver (PostgreSQL/MongoDB)
- MongoDB Compass
- RedisInsight

## Troubleshooting

### Service Won't Start

**Port already in use:**
```bash
# Find process on port 8081
lsof -i :8081
kill -9 <PID>
```

**Database connection refused:**
```bash
# Check if container is running
docker compose ps postgres

# Check logs
docker compose logs postgres

# Restart container
docker compose restart postgres
```

**Eureka registration failed:**
Check `application.yml`:
```yaml
eureka:
  client:
    serviceUrl:
      defaultZone: http://localhost:8761/eureka/  # Must match Eureka port
```

### Ollama Issues

**Model not found:**
```bash
ollama list  # Check installed models
ollama pull codellama:7b-instruct-q4_K_M  # Re-pull if missing
```

**Slow inference (CPU fallback):**
```bash
# Check GPU usage (NVIDIA)
nvidia-smi

# Ollama should show GPU utilization
# If 0%, check CUDA drivers
```

**Out of VRAM:**
```bash
# Use smaller model
ollama pull codellama:7b-instruct-q4_0  # 3.8GB instead of 4GB

# Or switch to CPU-only
export OLLAMA_NUM_GPU=0
ollama serve
```

### Kafka Issues

**Consumer lag growing:**
```bash
# Check consumer group lag
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group billing-service-group
```

Scale consumers if LAG > 1000.

**Topic not found:**
```bash
# Recreate topic
docker exec kafka kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic generation.complete \
  --partitions 3 \
  --replication-factor 1
```

### Memory Issues (8 Services + Infra)

**Reduce JVM heap:**
```bash
export JAVA_OPTS="-Xms128m -Xmx256m"  # Per service
```

**Use Docker Compose profiles:**
```bash
# Only start services for current feature work
docker compose --profile core up -d  # postgres, mongodb, redis only
docker compose --profile kafka up -d  # add kafka + zookeeper
```

**Monitor memory:**
```bash
docker stats  # Live container memory usage
```

## Next Steps

1. ✅ Verify all health checks pass
2. ✅ Check Eureka shows 8 services
3. ✅ Run end-to-end test script
4. ✅ Open Kibana (http://localhost:5601) and see logs flowing
5. ✅ Open Zipkin (http://localhost:9411) and trace a request
6. → Start Phase 1 development: [docs/phases/phase-1.md](./phases/phase-1.md)

## Additional Resources

- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Ollama Documentation](https://github.com/ollama/ollama/blob/main/docs/api.md)
- [Spring Boot DevTools](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools)
- [IntelliJ Spring Boot Guide](https://www.jetbrains.com/help/idea/spring-boot.html)
