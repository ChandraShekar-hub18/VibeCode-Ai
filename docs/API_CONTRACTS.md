# API Contracts

Service-level API documentation. All endpoints require JWT authentication unless marked `[Public]`.

**Base URLs:**
- Development: `http://localhost:8080/api`
- Production: `https://api.vibecode.ai`

All requests through API Gateway (:8080). Gateway routes to services via Eureka discovery.

---

## Auth Service (:8081)

### POST /auth/register `[Public]`

Register new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe"  // optional
}
```

**Response (201):**
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "message": "Verification email sent"
}
```

**Errors:**
- `400` — Invalid email format, weak password (< 8 chars, no uppercase/number/symbol)
- `409` — Email already registered

---

### POST /auth/login `[Public]`

Authenticate and receive JWT tokens.

**Request:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "550e8400-e29b-41d4-a716...",
  "expiresIn": 900,  // 15 minutes
  "tokenType": "Bearer",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "fullName": "John Doe"
  }
}
```

**Errors:**
- `401` — Invalid credentials
- `403` — Account not verified or disabled

---

### POST /auth/refresh `[Public]`

Refresh access token using refresh token.

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716..."
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "expiresIn": 900
}
```

**Errors:**
- `401` — Invalid or expired refresh token

---

### POST /auth/logout

Revoke refresh token (access token expires naturally).

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716..."
}
```

**Response (204):** No content

---

### GET /auth/oauth/google `[Public]`

Initiate Google OAuth2 flow. Redirects to Google consent screen.

**Query params:**
- `redirect_uri` — Where to return after auth (default: `http://localhost:3000/auth/callback`)

**Response:** HTTP 302 redirect to Google

---

### GET /auth/oauth/callback `[Public]`

Google OAuth2 callback handler. Called by Google after user consent.

**Query params:**
- `code` — Authorization code from Google
- `state` — CSRF token (validated)

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiIs...",
  "refreshToken": "550e8400-e29b-41d4-a716...",
  "user": { /* same as /login */ }
}
```

---

## User Service (:8082)

### GET /users/me

Get current user profile.

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "fullName": "John Doe",
  "avatarUrl": "https://cdn.vibecode.ai/avatars/abc123.jpg",
  "bio": "Full-stack developer learning AI",
  "planType": "FREE",  // FREE | PRO | ENTERPRISE
  "tokenQuota": 10000,
  "tokensUsed": 3450,
  "quotaResetAt": "2025-03-01T00:00:00Z",
  "roles": ["USER"],
  "createdAt": "2025-02-01T10:30:00Z"
}
```

---

### PATCH /users/me

Update current user profile.

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "fullName": "Jane Doe",  // optional
  "bio": "Updated bio",    // optional
  "avatarUrl": "https://..."  // optional
}
```

**Response (200):** Updated user object

---

### GET /users/me/usage

Get detailed usage statistics.

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "currentPeriod": {
    "start": "2025-02-01T00:00:00Z",
    "end": "2025-03-01T00:00:00Z",
    "tokensUsed": 3450,
    "quota": 10000,
    "percentUsed": 34.5
  },
  "lastGeneration": {
    "timestamp": "2025-02-17T14:30:00Z",
    "tokensUsed": 1234,
    "projectName": "Todo App"
  },
  "topProjects": [
    {
      "projectId": "...",
      "name": "Todo App",
      "tokensUsed": 1800
    }
  ]
}
```

---

### POST /users/me/upgrade

Initiate plan upgrade (redirects to Stripe Checkout).

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "planType": "PRO",  // PRO | ENTERPRISE
  "billingCycle": "MONTHLY"  // MONTHLY | ANNUAL
}
```

**Response (200):**
```json
{
  "checkoutUrl": "https://checkout.stripe.com/session/abc123...",
  "sessionId": "cs_test_abc123"
}
```

---

## Project Service (:8083)

### POST /projects

Create new project.

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "name": "Todo App",
  "description": "Full-stack todo with React + Spring Boot",  // optional
  "prompt": "Build me a todo app with user authentication...",
  "visibility": "PRIVATE",  // PRIVATE | PUBLIC | UNLISTED
  "tags": ["react", "spring-boot", "crud"]  // optional
}
```

**Response (201):**
```json
{
  "id": "65a1b2c3d4e5f6789abcdef0",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Todo App",
  "description": "Full-stack todo with React + Spring Boot",
  "prompt": "Build me a todo app with...",
  "visibility": "PRIVATE",
  "tags": ["react", "spring-boot", "crud"],
  "files": [],  // Empty initially, populated after generation
  "metadata": {
    "totalFiles": 0,
    "totalSize": 0,
    "framework": null
  },
  "createdAt": "2025-02-17T10:30:00Z",
  "updatedAt": "2025-02-17T10:30:00Z"
}
```

---

### GET /projects

List user's projects.

**Headers:** `Authorization: Bearer {accessToken}`

**Query params:**
- `page` (default: 0)
- `size` (default: 20)
- `sort` (default: createdAt,desc)
- `tag` (filter by tag, optional)

**Response (200):**
```json
{
  "content": [ /* array of project objects */ ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```

---

### GET /projects/{id}

Get project details.

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):** Full project object with files

**Errors:**
- `403` — User doesn't own project (unless public)
- `404` — Project not found

---

### GET /projects/{id}/files

Get project file tree.

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "files": [
    {
      "path": "frontend/src/App.jsx",
      "content": "import React from 'react';\n...",
      "language": "javascript",
      "size": 1234
    },
    {
      "path": "backend/pom.xml",
      "content": "<project>...</project>",
      "language": "xml",
      "size": 567
    }
  ]
}
```

---

### POST /projects/{id}/fork

Fork (duplicate) a project.

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "name": "My Fork of Todo App"  // optional, defaults to "Fork of {original}"
}
```

**Response (201):** New project object

---

### DELETE /projects/{id}

Delete project (soft delete).

**Headers:** `Authorization: Bearer {accessToken}`

**Response (204):** No content

---

## AI Generation Service (:8084)

### POST /generate/stream

Generate code with streaming response (SSE).

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "prompt": "Create a React button component with hover effect",
  "projectId": "65a1b2c3d4e5f6789abcdef0",  // optional, if null creates new project
  "context": {  // optional
    "existingFiles": ["src/App.jsx"],
    "framework": "react"
  }
}
```

**Response:** Server-Sent Events stream

```
event: token
data: {"token": "import", "index": 0}

event: token
data: {"token": " React", "index": 1}

event: complete
data: {"projectId": "...", "tokensUsed": 1234, "durationMs": 8234}
```

**Errors:**
- `402` — Quota exceeded (upgrade required)
- `429` — Rate limited (too many concurrent generations)

---

### POST /generate/chat

Non-streaming generation (wait for full response).

**Headers:** `Authorization: Bearer {accessToken}`

**Request:** Same as `/generate/stream`

**Response (200):**
```json
{
  "projectId": "65a1b2c3d4e5f6789abcdef0",
  "generatedCode": {
    "files": [ /* array of file objects */ ]
  },
  "tokensUsed": 1234,
  "model": "codellama:7b-instruct-q4_K_M",
  "durationMs": 8234
}
```

---

### POST /generate/embed

Generate embedding for text (RAG pipeline use).

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "text": "React component with TypeScript",
  "model": "nomic-embed-text"  // optional
}
```

**Response (200):**
```json
{
  "embedding": [0.123, -0.456, 0.789, /* ... 768 dimensions */],
  "model": "nomic-embed-text",
  "dimensions": 768
}
```

---

## Code Executor Service (:8085)

### POST /execute

Execute code in sandbox.

**Headers:** `Authorization: Bearer {accessToken}`

**Request:**
```json
{
  "projectId": "65a1b2c3d4e5f6789abcdef0",
  "language": "javascript",  // javascript | python | java
  "entrypoint": "src/index.js",  // optional, defaults to main file
  "timeout": 10  // seconds, max 30
}
```

**Response (200):**
```json
{
  "executionId": "exec_abc123",
  "status": "RUNNING",
  "logsUrl": "ws://localhost:8085/execute/exec_abc123/logs"
}
```

---

### GET /execute/{executionId}/status

Poll execution status.

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "executionId": "exec_abc123",
  "status": "COMPLETED",  // RUNNING | COMPLETED | FAILED | TIMEOUT
  "exitCode": 0,
  "output": "Hello, World!\n",
  "error": null,
  "durationMs": 1234,
  "startedAt": "2025-02-17T10:30:00Z",
  "completedAt": "2025-02-17T10:30:01Z"
}
```

---

### WebSocket /execute/{executionId}/logs

Real-time execution logs.

**Headers:** `Authorization: Bearer {accessToken}`

**Messages (server → client):**
```json
{"type": "stdout", "data": "Starting server...\n"}
{"type": "stderr", "data": "Warning: deprecated API\n"}
{"type": "exit", "code": 0}
```

---

## Billing Service (:8086)

### GET /billing/me/usage

Get current billing period usage (proxied from User Service, but with more detail).

**Headers:** `Authorization: Bearer {accessToken}`

**Response (200):**
```json
{
  "currentPeriod": { /* same as User Service */ },
  "usageLogs": [
    {
      "timestamp": "2025-02-17T10:30:00Z",
      "projectName": "Todo App",
      "tokensUsed": 1234,
      "costUsd": 0.0123,  // PRO/ENTERPRISE only
      "model": "codellama:7b-instruct-q4_K_M"
    }
  ]
}
```

---

### POST /billing/webhooks/stripe `[Public, Stripe-only]`

Stripe webhook endpoint (called by Stripe, not frontend).

**Headers:** `Stripe-Signature: {signature}`

**Request:** Stripe event payload (subscription created, updated, payment succeeded, etc.)

**Response (200):** `{"received": true}`

---

## Notification Service (:8087)

### WebSocket /notifications/stream

Real-time notification stream.

**Headers:** `Authorization: Bearer {accessToken}` (in initial handshake)

**Messages (server → client):**
```json
{
  "type": "generation.complete",
  "data": {
    "projectId": "65a1b2c3d4e5f6789abcdef0",
    "projectName": "Todo App",
    "tokensUsed": 1234
  },
  "timestamp": "2025-02-17T10:30:01Z"
}

{
  "type": "quota.exceeded",
  "data": {
    "used": 10500,
    "limit": 10000,
    "planType": "FREE"
  },
  "timestamp": "2025-02-17T10:30:01Z"
}
```

---

## Error Response Format

All services return errors in consistent format:

```json
{
  "timestamp": "2025-02-17T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid email format",
  "path": "/api/auth/register",
  "traceId": "abc123def456"  // For log correlation
}
```

**HTTP status codes:**
- `400` — Validation error (client's fault)
- `401` — Unauthorized (missing/invalid JWT)
- `403` — Forbidden (valid JWT, insufficient permissions)
- `404` — Resource not found
- `409` — Conflict (e.g., email already exists)
- `422` — Unprocessable entity (business logic rejection)
- `429` — Rate limited
- `500` — Internal server error
- `502` — Bad gateway (downstream service unavailable)
- `503` — Service unavailable (maintenance, overload)

---

## Postman Collection

Import `docs/api/vibecode-postman.json` for pre-configured requests with example payloads.

**Environment variables:**
- `base_url` — `http://localhost:8080/api`
- `access_token` — Auto-updated by login request
- `user_id` — Auto-updated by login request

---

## OpenAPI/Swagger Docs

Each service exposes OpenAPI spec at:
- Auth: http://localhost:8081/v3/api-docs
- User: http://localhost:8082/v3/api-docs
- Project: http://localhost:8083/v3/api-docs
- AI Gen: http://localhost:8084/v3/api-docs
- Code Exec: http://localhost:8085/v3/api-docs
- Billing: http://localhost:8086/v3/api-docs

Swagger UI at `http://localhost:808x/swagger-ui.html` for interactive testing.
