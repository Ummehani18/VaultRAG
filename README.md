# VaultRAG

Privacy-first local RAG platform for sensitive organizational documents.

## Goal

VaultRAG allows users to upload sensitive documents and ask questions
about them without sending the documents to external AI services.

## Initial Domain

Healthcare Finance / Insurance Operations

## Architecture

Next.js
    ↓
Spring Boot
    ↓
PostgreSQL + pgvector
    ↓
Local AI Runtime
    ↓
Local SLM

## Technology

- Next.js
- TypeScript
- Java 21
- Spring Boot
- PostgreSQL
- pgvector
- Ollama
- Docker