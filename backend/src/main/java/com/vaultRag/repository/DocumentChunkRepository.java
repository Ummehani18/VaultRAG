package com.vaultRag.backend.repository;

import com.vaultRag.backend.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository
        extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocumentIdOrderByChunkIndex(
            UUID documentId
    );

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE document_chunks
            SET embedding = CAST(:embedding AS vector)
            WHERE id = :chunkId
            """, nativeQuery = true)
    int updateEmbedding(
            @Param("chunkId") UUID chunkId,
            @Param("embedding") String embedding
    );

    @Query(
            value = """
                    SELECT embedding IS NOT NULL
                    FROM document_chunks
                    WHERE id = :chunkId
                    """,
            nativeQuery = true
    )
    boolean hasEmbedding(
            @Param("chunkId") UUID chunkId
    );

    @Query(value = """
        SELECT
            dc.id AS chunk_id,
            dc.document_id AS document_id,
            d.name AS document_name,
            dc.chunk_index AS chunk_index,
            dc.page_number AS page_number,
            dc.content AS content,
            (dc.embedding <=> CAST(:embedding AS vector)) AS distance
        FROM document_chunks dc
        JOIN documents d
            ON d.id = dc.document_id
        WHERE dc.embedding IS NOT NULL
          AND d.uploaded_by = :userId
          AND (dc.embedding <=> CAST(:embedding AS vector)) <= :maxDistance
        ORDER BY dc.embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<SimilarChunkProjection> findSimilarChunks(
            @Param("embedding") String embedding,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit,
            @Param("userId") UUID userId
    );
}