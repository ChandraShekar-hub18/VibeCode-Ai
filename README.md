# VibeCode AI

A production-grade AI-powered code generation platform built as a microservices architecture learning project. Think Lovable.dev meets enterprise Spring Boot.

## Overview

VibeCode AI accepts natural language prompts and generates full-stack application code using LLMs with RAG-enhanced context retrieval. Built entirely with Spring Boot 3, Spring AI, and modern cloud-native patterns.

**Status:** Active Development | **Target:** Learning + Portfolio + Production-Ready Architecture

## Architecture

- **8 microservices** — Auth, User, Project, AI Generation, Code Executor, Billing, Notification, Gateway
- **Event-driven** — Apache Kafka for async communication
- **AI Layer** — Spring AI + Ollama (CodeLlama 7B) + RAG pipeline with pgvector
- **Polyglot persistence** — PostgreSQL, MongoDB, Redis, pgvector
- **Observability** — ELK Stack, Zipkin, Spring Boot Actuator
- **Orchestration** — Docker Compose (dev), Kubernetes on AWS EKS (prod)

See [ARCHITECTURE.md](./ARCHITECTURE.md) for technical deep-dive.

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend** | Spring Boot 3, Spring Cloud (Gateway, Config, Eureka), Spring Security 6, Spring AI |
| **AI/ML** | Ollama, CodeLlama 7B Q4, Spring AI, pgvector (RAG), MCP |
| **Messaging** | Apache Kafka, Redis Pub/Sub |
| **Data** | PostgreSQL, MongoDB, pgvector, Redis |
| **Frontend** | React 18, TypeScript, Vite, TailwindCSS, Monaco Editor |
| **Infra** | Docker, Kubernetes (EKS), AWS CodePipeline, Terraform |
| **Observability** | ELK, Zipkin, Prometheus, Grafana |

## Quick Start

**Prerequisites:** Java 21, Maven 3.9+, Docker, Node.js 18+, Ollama

```bash
# Clone all service repos
./scripts/clone-all.sh

# Start infrastructure
docker-compose up -d postgres mongodb redis kafka zookeeper

# Install & start Ollama
ollama serve &
ollama pull codellama:7b-instruct-q4_K_M
ollama pull nomic-embed-text

# Start services (order matters)
cd eureka-registry && mvn spring-boot:run &
cd api-gateway && mvn spring-boot:run &
cd auth-service && mvn spring-boot:run &
# ... (see SETUP.md for full sequence)

# Start frontend
cd vibecode-ui && npm install && npm run dev
```

Full setup guide: [SETUP.md](./SETUP.md)

## Repository Structure

VibeCode AI uses a **multi-repo** approach. Each service is independently versioned:

```
vibecode-ai/                    # This repo (docs + shared configs)
├── docs/                       # Architecture docs
├── docker-compose.yml          # Local dev infrastructure
├── k8s/                        # Kubernetes manifests
└── terraform/                  # IaC for AWS deployment

Services (separate repos):
├── eureka-registry             # Service discovery
├── api-gateway                 # Entry point + JWT validation
├── auth-service                # Authentication + OAuth2
├── user-service                # Profiles + quotas
├── project-service             # Project CRUD (MongoDB)
├── ai-generation-service       # LLM + RAG + MCP
├── code-executor-service       # Sandboxed execution
├── billing-service             # Usage tracking + Stripe
├── notification-service        # Real-time push + email
└── vibecode-ui                 # React frontend
```

## Service Communication

```
Client → API Gateway (JWT validation, rate limiting)
  → Eureka (service discovery)
    → Microservices (REST + OpenFeign)
      → Kafka (async events: generation.complete, quota.exceeded, payment.success)
        → Databases (PostgreSQL, MongoDB, pgvector, Redis)
```

Inter-service auth: JWT forwarded via OpenFeign `RequestInterceptor`. Each service validates independently (defense-in-depth).

## Key Design Decisions

**Why separate repos per service?**  
Mirrors real-world microservice teams. Each service has independent CI/CD, versioning, and ownership. Tradeoff: slightly more setup overhead.

**Why Ollama instead of OpenAI?**  
Zero cost during development. Spring AI makes swapping to OpenAI in production a 3-line config change.

**Why pgvector over Pinecone?**  
Self-hosted. Learning PostgreSQL extensions is valuable. pgvector performs well for <1M vectors (our scale).

**Why Kafka over RabbitMQ?**  
Kafka's log-based persistence and consumer groups fit our event-sourcing + multiple-consumer pattern better. Cohort also teaches Kafka specifically.

**Why Docker-in-Docker for code execution?**  
Intentional learning blocker. First implementation will fail. Understanding *why* (namespaces, capabilities, privilege escalation) is the goal. See [docs/blockers/docker-in-docker.md](./docs/blockers/docker-in-docker.md).

## Development Phases

| Phase | Duration | Focus | Status |
|-------|----------|-------|--------|
| **Phase 1** | Weeks 1-3 | Gateway, Auth, User | 🔄 In Progress |
| **Phase 2** | Weeks 4-5 | Project, Config Server | ⏳ Planned |
| **Phase 3** | Weeks 6-7 | AI Gen + RAG + Ollama | ⏳ Planned |
| **Phase 4** | Weeks 8-9 | Kafka, Billing, Notification | ⏳ Planned |
| **Phase 5** | Weeks 10-11 | Code Executor | ⏳ Planned |
| **Phase 6** | Weeks 12-14 | Testing, K8s, CI/CD | ⏳ Planned |

See [docs/phases/](./docs/phases/) for detailed phase breakdowns.

## API Documentation

OpenAPI specs for each service:
- Gateway: `http://localhost:8080/swagger-ui.html`
- Auth: `http://localhost:8081/swagger-ui.html`
- User: `http://localhost:8082/swagger-ui.html`
- [Full API contracts](./API_CONTRACTS.md)

## Testing

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify -P integration-tests

# Load testing (K6)
k6 run tests/load/generation-flow.js
```

Coverage target: 80% for business logic, 60% overall.

## Deployment

**Local:** Docker Compose  
**Staging:** Kubernetes on Minikube  
**Production:** AWS EKS + RDS + ElastiCache

See [DEPLOYMENT.md](./DEPLOYMENT.md) for CI/CD pipeline and infrastructure-as-code.

## Monitoring

- **Logs:** ELK Stack at `http://localhost:5601`
- **Traces:** Zipkin at `http://localhost:9411`
- **Metrics:** Prometheus + Grafana at `http://localhost:3000`
- **Health:** Actuator endpoints at `http://localhost:808x/actuator/health`

## Contributing

This is a learning project, but contributions welcome. See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.

**Team:** Solo developer + potential collaborator  
**License:** MIT (see [LICENSE](./LICENSE))


## Resources

- [Architecture Diagrams](./docs/diagrams/)
- [Database Schemas](./docs/schemas/)
- [Postman Collection](./docs/api/vibecode-postman.json)
- [Service Dependency Graph](./docs/diagrams/service-dependencies.svg)
- [Blocker Learning Guides](./docs/blockers/)

## Contact

**Project Lead:** [Chandra Shekar Mekala]  
**Email:** [chandrashekar.mekala2001@gmail.com]  
**LinkedIn:** [https://www.linkedin.com/in/chandra-shekar-m/]

Built as part of Spring Boot AI journey.
