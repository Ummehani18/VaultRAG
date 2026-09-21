CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE documents (
                           id UUID PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           file_type VARCHAR(50),
                           file_size BIGINT,
                           storage_path VARCHAR(500),
                           uploaded_by UUID NOT NULL,
                           status VARCHAR(50) NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT fk_documents_user
                               FOREIGN KEY (uploaded_by)
                                   REFERENCES users(id)
);

CREATE TABLE document_chunks (
                                 id UUID PRIMARY KEY,
                                 document_id UUID NOT NULL,
                                 chunk_index INT NOT NULL,
                                 content TEXT NOT NULL,
                                 embedding vector(384),

                                 CONSTRAINT fk_chunks_document
                                     FOREIGN KEY (document_id)
                                         REFERENCES documents(id)
                                         ON DELETE CASCADE
);

CREATE TABLE audit_logs (
                            id UUID PRIMARY KEY,
                            user_id UUID,
                            action VARCHAR(100) NOT NULL,
                            resource_type VARCHAR(100),
                            resource_id UUID,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_audit_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
);

