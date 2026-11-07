# VaultRAG

> **Privacy-first local RAG platform for secure, citation-backed retrieval of sensitive organizational documents.**

[![Status](https://img.shields.io/badge/status-in%20development-yellow)](#license)
[![License](https://img.shields.io/badge/license-TBD-lightgrey)](#license)
[![Java](https://img.shields.io/badge/Java-17-b07219?logo=openjdk&logoColor=white)](#tech-stack)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](#tech-stack)
[![Next.js](https://img.shields.io/badge/Next.js-black?logo=next.js&logoColor=white)](#tech-stack)
[![TypeScript](https://img.shields.io/badge/TypeScript-3178C6?logo=typescript&logoColor=white)](#tech-stack)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-pgvector-4169E1?logo=postgresql&logoColor=white)](#tech-stack)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](#local-setup)
[![Ollama](https://img.shields.io/badge/AI%20Runtime-Ollama-000000)](#tech-stack)
[![Offline Goal](https://img.shields.io/badge/deployment-offline%20%2F%20air--gapped%20goal-informational)](#privacy--offline-direction)


VaultRAG is a locally hosted Retrieval-Augmented Generation (RAG) platform designed to help organizations securely search and query sensitive documents using local AI models.

Instead of sending private documents to external AI services, VaultRAG is designed to keep document processing, embedding generation, retrieval, and AI inference within the organization's controlled environment.

The long-term objective is to support offline and air-gapped deployments for organizations that handle confidential business and operational information.

> **Project status:** Working full-stack prototype under active development. Core PDF ingestion, embedding generation, vector retrieval, local LLM integration, and streamed answers are operational. Retrieval accuracy, chunking quality, answer grounding, automated evaluation, and production security validation remain in progress.

---

## Table of Contents

- [What is VaultRAG?](#what-is-vaultrag)
- [Primary Use Case](#primary-use-case)
- [Key Features](#key-features)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Current Implementation Status](#current-implementation-status)
- [Local Development Setup](#local-development-setup)
- [Privacy and Offline Direction](#privacy-and-offline-direction)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)
- [License](#license)

## What is VaultRAG?

VaultRAG transforms private organizational documents into a searchable knowledge base.

Users can upload documents, ask questions in natural language, and receive answers generated from retrieved document passages.

The system follows a document-grounded RAG workflow:

```text
Upload Documents
       |
       v
Extract and Process
       |
       v
Create Document Chunks
       |
       v
Generate Local Embeddings
       |
       v
Store Searchable Knowledge
       |
       v
Ask a Question
       |
       v
Retrieve Relevant Information
       |
       v
Local AI Generates Answer
       |
       v
Answer + Source References
```

The objective is to make organizational knowledge accessible without unnecessarily exposing sensitive documents to external AI providers.

## Primary Use Case

### Insurance / Third-Party Administrator Operations

VaultRAG is initially designed around insurance and TPA operations, where teams work with sensitive and policy-heavy documents, including:

- Claims guidelines
- Pre-authorization policies
- Reimbursement rules
- Denial guidelines
- Billing standard operating procedures
- Payer agreements
- Cashless hospitalization procedures

Example questions:

- What documents are required for a reimbursement claim?
- What is the pre-authorization process?
- Which policy section describes the required supporting documents?

VaultRAG is intended to retrieve relevant information from documents the user is authorized to access and return answers with source references so the user can verify the original material.

**Scope:** VaultRAG is a document knowledge and retrieval tool. It does not approve or reject insurance claims and does not replace an organization's core claims-processing system.

## Key Features

- **Document Processing:** Upload and extract text from PDF documents.
- **Semantic Search:** Retrieve relevant document passages using vector similarity.
- **Local Embeddings:** Generate embeddings using locally hosted Ollama models.
- **Local AI Inference:** Generate answers using a locally running LLM.
- **Document-Scoped Retrieval:** Search within a selected document.
- **User-Scoped Retrieval Logic:** Apply authenticated-user filtering in the retrieval flow.
- **Source References:** Return document names and page metadata for retrieved passages.
- **Streaming Responses:** Stream generated answers to the frontend.
- **Privacy-First Architecture:** Designed to keep document processing and AI inference within the organization's environment.
- **Offline / Air-Gapped Direction:** Long-term goal for environments requiring restricted network connectivity.

Some capabilities, particularly complete authorization coverage, citation accuracy, audit coverage, and air-gapped operation, still require further validation.

## How It Works

### 1. Document Ingestion

```text
PDF document
    |
    v
Text extraction
    |
    v
Chunk creation
    |
    v
Embedding generation
    |
    v
PostgreSQL + pgvector
```

The backend extracts PDF text, creates chunks, generates embeddings using the configured local embedding model, and stores the content and vector data.

### 2. Ask a Question

A user selects a document or searches the documents available to them and submits a natural-language question.

### 3. Retrieve Relevant Passages

The backend embeds the question and queries pgvector for similar chunks. Retrieval can be scoped to a selected document and the authenticated user.

### 4. Generate an Answer Locally

The retrieved passages are assembled into the document context and sent to the locally hosted generation model through Ollama.

### 5. Return the Answer and Sources

The backend streams the generated answer to the frontend and returns source metadata for the retrieved chunks. Users should verify important information against the original document.

## Architecture

```text
                    Next.js Frontend
                  React + TypeScript
                          |
                          | HTTP / streaming API
                          v
                 Java 17 / Spring Boot
                          |
          +---------------+----------------+
          |               |                |
          v               v                v
    Document flow     Retrieval flow    RAG / LLM flow
    PDF extraction    SearchService     RagService
    Chunking          pgvector query    LlmService
    Embeddings
          |               |                |
          +---------------+----------------+
                          |
                          v
                 PostgreSQL + pgvector
                  Documents / Chunks
                     / Embeddings
                          |
                          v
                       Ollama
                  /             \
           Embedding model    Generation model
             all-minilm        llama3.2:1b
```

### Current RAG Flow

```text
User question
     |
     v
Query embedding
     |
     v
pgvector similarity search
     |
     v
User/document-scoped retrieved chunks
     |
     v
Context construction
     |
     v
Local LLM generation
     |
     v
Streamed answer + retrieved source metadata
```

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js, React, TypeScript |
| Backend | Java 17, Spring Boot |
| API | Spring REST APIs and streamed responses |
| Authentication | Spring Security and JWT-based authentication |
| Password hashing | BCrypt, where configured in the security implementation |
| Database | PostgreSQL |
| Vector search | pgvector |
| Persistence | Spring Data JPA / Hibernate |
| Database migrations | Flyway |
| PDF processing | Apache Tika, Apache PDFBox |
| Embedding runtime | Ollama |
| Embedding model | `all-minilm` |
| Generation runtime | Ollama |
| Generation model | `llama3.2:1b` |
| Build | Maven Wrapper |
| Infrastructure | Docker / Docker Compose configuration, where present in the repository |
| Testing | JUnit / Mockito test sources; full test-suite validation is pending |

Check the repository's `pom.xml`, frontend package manifest, Docker Compose file, and actual configuration for exact versions and enabled components.

## Current Implementation Status

This table distinguishes implemented code from behavior that has been fully validated.

| Capability | Current status |
|---|---|
| Spring Boot backend starts locally | Working |
| Next.js question-and-answer UI | Working |
| PDF extraction | Implemented; broader regression testing needed |
| Document chunking | Implemented; chunk boundaries and overlap under review |
| Ollama embedding integration | Implemented |
| 384-dimensional embeddings | Confirmed in current runtime diagnostics |
| PostgreSQL + pgvector integration | Working |
| Similarity retrieval | Working; relevance and threshold calibration in progress |
| Selected-document retrieval | Present in the search flow |
| Authenticated-user filtering | Present in the search flow; cross-user isolation needs security tests |
| JWT authentication | Part of the backend security design; verify actual route and role coverage |
| Role-based access control | Do not assume complete until role enforcement is tested across endpoints |
| Local LLM generation | Working |
| Streaming answer responses | Working; normal stream completion observed |
| Source metadata | Returned for retrieved chunks; citation-to-claim accuracy needs validation |
| Audit logging | Audit-related code exists in the project; coverage and completeness need validation |
| Docker-based setup | Verify against the current Compose and container configuration |
| Fully offline / air-gapped operation | Goal; not yet demonstrated end to end |
| Automated RAG quality evaluation | Pending |
| Production readiness | Not yet achieved |

## Local Development Setup

### Prerequisites

Install the tools required by the current repository configuration:

- Java 17
- Node.js and the package manager used by the frontend
- PostgreSQL with pgvector, or the repository's Docker Compose database setup
- Ollama
- Git
- Docker Desktop, if using the Compose setup

### 1. Clone the Repository

```powershell
git clone <YOUR_REPOSITORY_URL>
cd VaultRAG
```

### 2. Start PostgreSQL

If the repository includes a working Compose file for PostgreSQL and pgvector, start the configured services from the project root:

```powershell
docker compose up -d
```

The current local backend configuration has used:

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5434` |
| Database | `vaultrag` |
| Username | `vaultrag` |

Use the actual values in your local configuration and Compose file. Do not assume these defaults apply to every environment.

Ensure pgvector is enabled in the database:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. Start Ollama and Obtain the Models

Ensure Ollama is running locally, then pull the configured models:

```powershell
ollama pull all-minilm
ollama pull llama3.2:1b
ollama list
```

Default Ollama endpoint:

```text
http://localhost:11434
```

### 4. Configure and Start the Backend

```powershell
cd backend
```

Configure database credentials, JWT secrets, storage paths, and Ollama settings using local environment variables or profile-specific configuration.

The current application defaults include:

```properties
vaultrag.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
vaultrag.ollama.embedding-model=${OLLAMA_EMBEDDING_MODEL:all-minilm}
vaultrag.ollama.generation-model=${OLLAMA_GENERATION_MODEL:llama3.2:1b}
vaultrag.rag.max-distance=${VAULTRAG_RAG_MAX_DISTANCE:0.60}
```

Build the backend:

```powershell
.\mvnw.cmd clean package -Dmaven.test.skip=true
```

**Note:** This command skips test compilation and execution. It is a temporary workaround for the current test compilation issue, not a successful full test-suite run.

Start the backend using the local profile if required by your configuration:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=local" "-Dmaven.test.skip=true" spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

### 5. Start the Frontend

Open another terminal and navigate to the directory containing the frontend `package.json`:

```powershell
cd frontend
```

Install dependencies and start the development server using the package manager configured by the repository. For pnpm:

```powershell
pnpm install
pnpm dev
```

The frontend has previously been run at `http://localhost:3001`. Use the actual URL printed by the development server and ensure it matches the backend CORS configuration.

## Security and Privacy Model

VaultRAG follows a privacy-first design objective: sensitive documents should remain within the organization's controlled environment during document processing, retrieval, and AI inference.

The intended deployment boundary is:

```text
+--------------------------------------------------+
|            Organization-controlled network      |
|                                                  |
|  Next.js -> Spring Boot -> PostgreSQL/pgvector   |
|                         |                        |
|                         v                        |
|                    Local Ollama                 |
|                                                  |
+--------------------------------------------------+
```

Security controls that must be implemented and validated include:

- Authentication for protected application endpoints.
- Authorization checks for document operations.
- User/document ownership enforcement in every retrieval path.
- Role checks where role-based permissions are required.
- Secure password hashing and JWT handling.
- Audit records for defined security-relevant actions.
- Safe file upload validation and size limits.
- Avoiding sensitive document content and credentials in logs.
- Secure database credentials and least-privilege database access.

**Important:** Local inference alone does not guarantee privacy. Network egress, telemetry, model provisioning, logs, backups, access controls, and deployment configuration must also be reviewed.

### Offline / Air-Gapped Direction

The long-term goal is to support offline or air-gapped deployments after the application, dependencies, and model artifacts have been provisioned.

This requires validating that:

- The frontend and backend do not depend on external runtime services.
- PostgreSQL and pgvector run inside the deployment environment.
- Ollama and required models are available locally.
- No external API calls or unexpected network dependencies are required.
- Installation, updates, backups, and model provisioning can be performed under the organization's security procedures.

Offline / air-gapped operation should not be claimed as complete until tested in a network-restricted environment.

## Project Structure

The intended high-level layout is:

```text
VaultRAG/
|
+-- backend/       # Java / Spring Boot application
+-- frontend/      # Next.js application
+-- infra/         # Infrastructure configuration, if present
+-- docs/          # Project documentation, if present
+-- storage/       # Local document storage; runtime data should not be committed
+-- README.md
+-- .gitignore
```

The actual repository tree is authoritative; optional directories should only be documented as present if they exist.

## Roadmap

### Phase 1 — Retrieval and Answer Quality

- Review chunk size, overlap, page boundaries, and table extraction.
- Review pgvector SQL, distance ordering, threshold filtering, and ownership filters.
- Calibrate retrieval thresholds with relevant and irrelevant questions.
- Improve context construction and answer grounding.
- Verify source metadata and citation accuracy.

### Phase 2 — Automated Validation

- Resolve Maven test compilation errors.
- Add unit tests for extraction, chunking, embedding orchestration, search, and RAG services.
- Add PostgreSQL/pgvector integration tests.
- Add authorization and cross-user isolation tests.
- Create a fixed evaluation dataset.
- Measure retrieval Recall@K and answer faithfulness.

### Phase 3 — Advanced Retrieval

- Evaluate hybrid keyword and vector search.
- Evaluate reranking of retrieved chunks.
- Improve metadata-aware and multi-document retrieval.
- Improve long-document and structured-table handling.
- Compare local generation models using the same evaluation dataset.

### Phase 4 — Production and Offline Readiness

- Complete authentication and authorization validation.
- Verify audit coverage and sensitive-data handling.
- Improve timeouts, retries, error handling, and observability.
- Validate document deletion and re-indexing consistency.
- Make local setup reproducible with Docker Compose.
- Add CI/CD and dependency/security checks.
- Test in a network-restricted environment before claiming air-gapped support.
- Benchmark realistic document sizes and concurrent users.

## License

VaultRAG is currently under development. A formal open-source license will be added before public distribution.
"""

path = Path("/mnt/data/README.md")
path.write_text(readme, encoding="utf-8")
print(f"Created {path} ({path.stat().st_size} bytes)")
Encountered exception: <class 'caas.internal.errors.ClientError'>.
from pathlib import Path

content = """# VaultRAG

**Privacy-first local RAG platform for secure, citation-backed retrieval of sensitive organizational documents.**

VaultRAG is a locally hosted Retrieval-Augmented Generation (RAG) platform designed to help organizations securely search and query sensitive documents using local AI models.

Instead of sending private documents to external AI services, VaultRAG is designed to keep document processing, embedding generation, retrieval, and AI inference within the organization's controlled environment.

The long-term objective is to support offline and air-gapped deployments for organizations that handle confidential business and operational information.

> **Project status:** Working full-stack prototype under active development. Core PDF ingestion, embedding generation, vector retrieval, local LLM integration, and streamed answers are operational. Retrieval accuracy, chunking quality, answer grounding, automated evaluation, and production security validation remain in progress.

---

## Table of Contents

- [What is VaultRAG?](#what-is-vaultrag)
- [Primary Use Case](#primary-use-case)
- [Key Features](#key-features)
- [How It Works](#how-it-works)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Current Implementation Status](#current-implementation-status)
- [Local Development Setup](#local-development-setup)
- [Privacy and Offline Direction](#privacy-and-offline-direction)
- [Project Structure](#project-structure)
- [Roadmap](#roadmap)
- [License](#license)

## What is VaultRAG?

VaultRAG transforms private organizational documents into a searchable knowledge base.

Users can upload documents, ask questions in natural language, and receive answers generated from retrieved document passages.

```text
Upload Documents
       |
       v
Extract and Process
       |
       v
Create Document Chunks
       |
       v
Generate Local Embeddings
       |
       v
Store Searchable Knowledge
       |
       v
Ask a Question
       |
       v
Retrieve Relevant Information
       |
       v
Local AI Generates Answer
       |
       v
Answer + Source References
```

The objective is to make organizational knowledge accessible without unnecessarily exposing sensitive documents to external AI providers.

## Primary Use Case

### Insurance / Third-Party Administrator Operations

VaultRAG is initially designed around insurance and TPA operations, where teams work with sensitive and policy-heavy documents, including:

- Claims guidelines
- Pre-authorization policies
- Reimbursement rules
- Denial guidelines
- Billing standard operating procedures
- Payer agreements
- Cashless hospitalization procedures

Example questions:

- What documents are required for a reimbursement claim?
- What is the pre-authorization process?
- Which policy section describes the required supporting documents?

VaultRAG is intended to retrieve relevant information from documents the user is authorized to access and return answers with source references so the user can verify the original material.

**Scope:** VaultRAG is a document knowledge and retrieval tool. It does not approve or reject insurance claims and does not replace an organization's core claims-processing system.

## Key Features

- **Document Processing:** Upload and extract text from PDF documents.
- **Semantic Search:** Retrieve relevant document passages using vector similarity.
- **Local Embeddings:** Generate embeddings using locally hosted Ollama models.
- **Local AI Inference:** Generate answers using a locally running LLM.
- **Document-Scoped Retrieval:** Search within a selected document.
- **User-Scoped Retrieval Logic:** Apply authenticated-user filtering in the retrieval flow.
- **Source References:** Return document names and page metadata for retrieved passages.
- **Streaming Responses:** Stream generated answers to the frontend.
- **Privacy-First Architecture:** Designed to keep document processing and AI inference within the organization's environment.
- **Offline / Air-Gapped Direction:** Long-term goal for environments requiring restricted network connectivity.

Some capabilities, particularly complete authorization coverage, citation accuracy, audit coverage, and air-gapped operation, still require further validation.

## How It Works

### 1. Document Ingestion

```text
PDF document
    |
    v
Text extraction
    |
    v
Chunk creation
    |
    v
Embedding generation
    |
    v
PostgreSQL + pgvector
```

The backend extracts PDF text, creates chunks, generates embeddings using the configured local embedding model, and stores the content and vector data.

### 2. Ask a Question

A user selects a document or searches the documents available to them and submits a natural-language question.

### 3. Retrieve Relevant Passages

The backend embeds the question and queries pgvector for similar chunks. Retrieval can be scoped to a selected document and the authenticated user.

### 4. Generate an Answer Locally

The retrieved passages are assembled into the document context and sent to the locally hosted generation model through Ollama.

### 5. Return the Answer and Sources

The backend streams the generated answer to the frontend and returns source metadata for the retrieved chunks. Users should verify important information against the original document.

## Architecture

```text
                    Next.js Frontend
                  React + TypeScript
                          |
                          | HTTP / streaming API
                          v
                 Java 17 / Spring Boot
                          |
          +---------------+----------------+
          |               |                |
          v               v                v
    Document flow     Retrieval flow    RAG / LLM flow
    PDF extraction    SearchService     RagService
    Chunking          pgvector query    LlmService
    Embeddings
          |               |                |
          +---------------+----------------+
                          |
                          v
                 PostgreSQL + pgvector
                  Documents / Chunks
                     / Embeddings
                          |
                          v
                       Ollama
                  /             \\\\
           Embedding model    Generation model
             all-minilm        llama3.2:1b
```

### Current RAG Flow

```text
User question
     |
     v
Query embedding
     |
     v
pgvector similarity search
     |
     v
User/document-scoped retrieved chunks
     |
     v
Context construction
     |
     v
Local LLM generation
     |
     v
Streamed answer + retrieved source metadata
```

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js, React, TypeScript |
| Backend | Java 17, Spring Boot |
| API | Spring REST APIs and streamed responses |
| Authentication | Spring Security and JWT-based authentication |
| Password hashing | BCrypt, where configured in the security implementation |
| Database | PostgreSQL |
| Vector search | pgvector |
| Persistence | Spring Data JPA / Hibernate |
| Database migrations | Flyway |
| PDF processing | Apache Tika, Apache PDFBox |
| Embedding runtime | Ollama |
| Embedding model | `all-minilm` |
| Generation runtime | Ollama |
| Generation model | `llama3.2:1b` |
| Build | Maven Wrapper |
| Infrastructure | Docker / Docker Compose configuration, where present in the repository |
| Testing | JUnit / Mockito test sources; full test-suite validation is pending |

Check the repository's `pom.xml`, frontend package manifest, Docker Compose file, and actual configuration for exact versions and enabled components.

## Current Implementation Status

This table distinguishes implemented code from behavior that has been fully validated.

| Capability | Current status |
|---|---|
| Spring Boot backend starts locally | Working |
| Next.js question-and-answer UI | Working |
| PDF extraction | Implemented; broader regression testing needed |
| Document chunking | Implemented; chunk boundaries and overlap under review |
| Ollama embedding integration | Implemented |
| 384-dimensional embeddings | Confirmed in current runtime diagnostics |
| PostgreSQL + pgvector integration | Working |
| Similarity retrieval | Working; relevance and threshold calibration in progress |
| Selected-document retrieval | Present in the search flow |
| Authenticated-user filtering | Present in the search flow; cross-user isolation needs security tests |
| JWT authentication | Part of the backend security design; verify actual route and role coverage |
| Role-based access control | Do not assume complete until role enforcement is tested across endpoints |
| Local LLM generation | Working |
| Streaming answer responses | Working; normal stream completion observed |
| Source metadata | Returned for retrieved chunks; citation-to-claim accuracy needs validation |
| Audit logging | Audit-related code exists in the project; coverage and completeness need validation |
| Docker-based setup | Verify against the current Compose and container configuration |
| Fully offline / air-gapped operation | Goal; not yet demonstrated end to end |
| Automated RAG quality evaluation | Pending |
| Production readiness | Not yet achieved |

## Local Development Setup

### Prerequisites

Install the tools required by the current repository configuration:

- Java 17
- Node.js and the package manager used by the frontend
- PostgreSQL with pgvector, or the repository's Docker Compose database setup
- Ollama
- Git
- Docker Desktop, if using the Compose setup

### 1. Clone the Repository

```powershell
git clone <YOUR_REPOSITORY_URL>
cd VaultRAG
```

### 2. Start PostgreSQL

If the repository includes a working Compose file for PostgreSQL and pgvector, start the configured services from the project root:

```powershell
docker compose up -d
```

The current local backend configuration has used:

| Setting | Value |
|---|---|
| Host | `localhost` |
| Port | `5434` |
| Database | `vaultrag` |
| Username | `vaultrag` |

Use the actual values in your local configuration and Compose file. Do not assume these defaults apply to every environment.

Ensure pgvector is enabled in the database:

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. Start Ollama and Obtain the Models

Ensure Ollama is running locally, then pull the configured models:

```powershell
ollama pull all-minilm
ollama pull llama3.2:1b
ollama list
```

Default Ollama endpoint:

```text
http://localhost:11434
```

### 4. Configure and Start the Backend

```powershell
cd backend
```

Configure database credentials, JWT secrets, storage paths, and Ollama settings using local environment variables or profile-specific configuration.

The current application defaults include:

```properties
vaultrag.ollama.base-url=${OLLAMA_BASE_URL:http://localhost:11434}
vaultrag.ollama.embedding-model=${OLLAMA_EMBEDDING_MODEL:all-minilm}
vaultrag.ollama.generation-model=${OLLAMA_GENERATION_MODEL:llama3.2:1b}
vaultrag.rag.max-distance=${VAULTRAG_RAG_MAX_DISTANCE:0.60}
```

Build the backend:

```powershell
.\\mvnw.cmd clean package -Dmaven.test.skip=true
```

**Note:** This command skips test compilation and execution. It is a temporary workaround for the current test compilation issue, not a successful full test-suite run.

Start the backend using the local profile if required by your configuration:

```powershell
.\\mvnw.cmd "-Dspring-boot.run.profiles=local" "-Dmaven.test.skip=true" spring-boot:run
```

Backend URL:

```text
http://localhost:8080
```

### 5. Start the Frontend

Open another terminal and navigate to the directory containing the frontend `package.json`:

```powershell
cd frontend
```

Install dependencies and start the development server using the package manager configured by the repository. For pnpm:

```powershell
pnpm install
pnpm dev
```

The frontend has previously been run at `http://localhost:3001`. Use the actual URL printed by Next.js and ensure it matches the backend CORS configuration.

## Security and Privacy Model

VaultRAG follows a privacy-first design objective: sensitive documents should remain within the organization's controlled environment during document processing, retrieval, and AI inference.

The intended deployment boundary is:

```text
+--------------------------------------------------+
|            Organization-controlled network      |
|                                                  |
|  Next.js -> Spring Boot -> PostgreSQL/pgvector   |
|                         |                        |
|                         v                        |
|                    Local Ollama                 |
|                                                  |
+--------------------------------------------------+
```

Security controls that must be implemented and validated include:

- Authentication for protected application endpoints.
- Authorization checks for document operations.
- User/document ownership enforcement in every retrieval path.
- Role checks where role-based permissions are required.
- Secure password hashing and JWT handling.
- Audit records for defined security-relevant actions.
- Safe file upload validation and size limits.
- Avoiding sensitive document content and credentials in logs.
- Secure database credentials and least-privilege database access.

**Important:** Local inference alone does not guarantee privacy. Network egress, telemetry, model provisioning, logs, backups, access controls, and deployment configuration must also be reviewed.

### Offline / Air-Gapped Direction

The long-term goal is to support offline or air-gapped deployments after the application, dependencies, and model artifacts have been provisioned.

This requires validating that:

- The frontend and backend do not depend on external runtime services.
- PostgreSQL and pgvector run inside the deployment environment.
- Ollama and required models are available locally.
- No external API calls or unexpected network dependencies are required.
- Installation, updates, backups, and model provisioning can be performed under the organization's security procedures.

Offline / air-gapped operation should not be claimed as complete until tested in a network-restricted environment.

## Project Structure

The intended high-level layout is:

```text
VaultRAG/
|
+-- backend/       # Java / Spring Boot application
+-- frontend/      # Next.js application
+-- infra/         # Infrastructure configuration, if present
+-- docs/          # Project documentation, if present
+-- storage/       # Local document storage; runtime data should not be committed
+-- README.md
+-- .gitignore
```

The actual repository tree is authoritative; optional directories should only be documented as present if they exist.

## Roadmap

### Phase 1 — Retrieval and Answer Quality

- Review chunk size, overlap, page boundaries, and table extraction.
- Review pgvector SQL, distance ordering, threshold filtering, and ownership filters.
- Calibrate retrieval thresholds with relevant and irrelevant questions.
- Improve context construction and answer grounding.
- Verify source metadata and citation accuracy.

### Phase 2 — Automated Validation

- Resolve Maven test compilation errors.
- Add unit tests for extraction, chunking, embedding orchestration, search, and RAG services.
- Add PostgreSQL/pgvector integration tests.
- Add authorization and cross-user isolation tests.
- Create a fixed evaluation dataset.
- Measure retrieval Recall@K and answer faithfulness.

### Phase 3 — Advanced Retrieval

- Evaluate hybrid keyword and vector search.
- Evaluate reranking of retrieved chunks.
- Improve metadata-aware and multi-document retrieval.
- Improve long-document and structured-table handling.
- Compare local generation models using the same evaluation dataset.

### Phase 4 — Production and Offline Readiness

- Complete authentication and authorization validation.
- Verify audit coverage and sensitive-data handling.
- Improve timeouts, retries, error handling, and observability.
- Validate document deletion and re-indexing consistency.
- Make local setup reproducible with Docker Compose.
- Add CI/CD and dependency/security checks.
- Test in a network-restricted environment before claiming air-gapped support.
- Benchmark realistic document sizes and concurrent users.

## License

VaultRAG is currently under development. A formal open-source license will be added before public distribution.


**[⬆ back to top](#table-of-contents)**
