# VaultRAG

> **Privacy-first local RAG platform for secure, citation-backed document retrieval.**

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

VaultRAG is a **local Retrieval-Augmented Generation (RAG) platform** that helps organizations securely search and ask questions about sensitive documents using local AI.

Instead of sending private documents to external AI services, VaultRAG is designed to keep document processing, retrieval, embeddings, and AI inference within the organization's own environment, with a long-term goal of supporting **offline / air-gapped deployments**.

---

[What is VaultRAG?](#what-is-vaultrag) | [Primary Use Case](#primary-use-case) | [Key Features](#key-features) | [How to Use](#how-to-use) | [Architecture](#architecture) | [Tech Stack](#tech-stack) | [Local Setup](#local-setup) | [Privacy & Offline Direction](#privacy--offline-direction) | [Project Structure](#project-structure) | [License](#license)


## What is VaultRAG?

VaultRAG turns private organizational documents into a searchable knowledge base.

Users can upload documents, ask questions in natural language, and receive answers grounded in those documents with page-level sources.

```text
Upload Documents
       ↓
Extract & Process
       ↓
Create Searchable Knowledge
       ↓
Ask a Question
       ↓
Retrieve Relevant Information
       ↓
Local AI Generates Answer
       ↓
Answer + Sources
```

### Example

A user can ask:

> **What documents are required for a reimbursement claim?**

VaultRAG searches the authorized document collection and returns an answer with sources such as:

```text
Reimbursement Policy.pdf — Page 8
Claims Guidelines.pdf — Page 12
```

The user can then verify the information directly from the source document.

**[⬆ back to top](#table-of-contents)**

---

## Primary Use Case

### Insurance / TPA Operations

VaultRAG is initially designed around **insurance and TPA operations**, where organizations manage large collections of sensitive documents such as:

- Claims guidelines
- Pre-authorization policies
- Reimbursement rules
- Denial guidelines
- Billing SOPs
- Payer agreements
- Cashless hospitalization procedures

An authorized employee can ask:

> What documents are required for this type of reimbursement claim?

or:

> What is the pre-authorization process?

VaultRAG retrieves the relevant information from the organization's private knowledge base and provides a grounded answer with source references.

> **Scope:** VaultRAG is a knowledge and retrieval system. It does not approve or reject insurance claims or replace the organization's core claims system.

**[⬆ back to top](#table-of-contents)**

---

## Key Features

- 🔐 **Authentication & Authorization** — JWT-based authentication and role-based access control
- 📄 **Document Processing** — Upload and process PDF documents
- 🔎 **Semantic Search** — Search document content using vector similarity
- 🤖 **Local AI** — Generate answers using a locally running LLM
- 📑 **Source Citations** — Return document and page information with answers
- 👤 **Access Isolation** — Restrict documents and retrieval to authorized users
- 📋 **Audit Logging** — Record security-relevant application activity
- 📴 **Offline / Air-Gapped Goal** — Designed toward fully offline deployments

**[⬆ back to top](#table-of-contents)**

---

## How to Use

### 1. Upload Documents

Upload a PDF through the **Documents** section.

VaultRAG processes the document:

```text
PDF
 ↓
Text Extraction
 ↓
Page-aware Chunking
 ↓
Embeddings
 ↓
Vector Index
 ↓
Ready
```

### 2. Ask a Question

Open **Ask VaultRAG** and enter a question about the available documents.

Example:

```text
What documents are required for pre-authorization?
```

### 3. Get an Answer

VaultRAG retrieves relevant document sections and provides them to the local AI model.

Example response:

```text
Answer

The required documents include ...

Sources

Pre-Authorization Policy.pdf — Page 4
Claims Guidelines.pdf — Page 7
```

### 4. Verify

Use the cited document and page to verify the answer against the original source.

**[⬆ back to top](#table-of-contents)**

---

## Architecture

```text
                        Next.js
                           │
                           │ REST API
                           ▼
                  Java / Spring Boot
                           │
          ┌────────────────┼────────────────┐
          │                │                │
          ▼                ▼                ▼
      Documents        Retrieval         Audit
          │                │                │
          └────────────────┼────────────────┘
                           ▼
                  PostgreSQL + pgvector
                           │
                           ▼
                    Local AI Runtime
                       │          │
                       ▼          ▼
                  Embeddings   Local LLM
```

### RAG Flow

```text
User Question
      ↓
Query Embedding
      ↓
pgvector Similarity Search
      ↓
Relevant Authorized Chunks
      ↓
Local LLM
      ↓
Grounded Answer + Sources
```

**[⬆ back to top](#table-of-contents)**

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Next.js, React, TypeScript, Tailwind CSS |
| Backend | Java 17, Spring Boot 4.1.1 |
| Security | Spring Security, JWT, BCrypt |
| Database | PostgreSQL, pgvector |
| ORM | Spring Data JPA |
| Migrations | Flyway |
| Document Processing | Apache Tika, Apache PDFBox |
| AI Runtime | Ollama |
| Embeddings | Local embedding model |
| Generation | Local LLM / SLM |
| Infrastructure | Docker, Docker Compose |
| Testing | JUnit, Mockito, Spring Boot integration tests |

**[⬆ back to top](#table-of-contents)**

---

## Local Setup

### Prerequisites

Install:

- Java 17
- Node.js
- pnpm
- Docker Desktop
- Ollama
- Git

### 1. Clone the Repository

```bash
git clone <repository-url>
cd VaultRAG
```

### 2. Start PostgreSQL + pgvector

From the project root:

```bash
docker compose up -d
```

VaultRAG uses:

```text
Host: localhost
Port: 5434
Database: vaultrag
Username: vaultrag
```

### 3. Start the Backend

On Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Backend:

```text
http://localhost:8080
```

### 4. Start the Frontend

Open another terminal:

```bash
cd frontend
pnpm install
pnpm dev
```

Frontend:

```text
http://localhost:3000
```

### 5. Start Ollama

Make sure Ollama is running locally and the required models are available.

Example:

```bash
ollama pull all-minilm
ollama pull llama3
```

**[⬆ back to top](#table-of-contents)**

---

## Privacy & Offline Direction

VaultRAG is designed around the principle that sensitive organizational knowledge should remain under the organization's control.

The intended deployment model is:

```text
┌──────────────────────────────────────────────┐
│           Organization Environment           │
│                                              │
│  Next.js → Spring Boot → PostgreSQL/pgvector │
│                         ↓                    │
│                      Local AI                │
│                                              │
└──────────────────────────────────────────────┘
```

The long-term goal is to support **offline / air-gapped deployments**, where the core application, document retrieval, embeddings, and AI inference can operate without an internet connection after the required software and models have been provisioned.

This is especially relevant for organizations handling confidential:

- Business documents
- Financial information
- Insurance policies
- Claims information
- Internal procedures
- Operational knowledge

**[⬆ back to top](#table-of-contents)**

---

## Project Structure

```text
VaultRAG/
│
├── backend/          # Java / Spring Boot backend
├── frontend/         # Next.js frontend
├── infra/            # Infrastructure configuration
├── docs/             # Project documentation
└── storage/          # Local document storage
```

**[⬆ back to top](#table-of-contents)**

---

## License

This project is currently under development.

A formal open-source license will be added before public distribution.

**[⬆ back to top](#table-of-contents)**
