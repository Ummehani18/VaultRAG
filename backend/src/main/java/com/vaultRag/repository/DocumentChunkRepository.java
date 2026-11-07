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

    /*
     * Search across all documents owned by the authenticated user.
     */
    @Query(value = """
            SELECT
                dc.id AS chunkId,
                dc.document_id AS documentId,
                d.name AS documentName,
                dc.chunk_index AS chunkIndex,
                dc.page_number AS pageNumber,
                dc.content AS content,
                (dc.embedding <=> CAST(:embedding AS vector)) AS distance
            FROM document_chunks dc
            JOIN documents d
                ON d.id = dc.document_id
            WHERE dc.embedding IS NOT NULL
              AND d.uploaded_by = :userId
              AND d.status = 'READY'
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

    /*
     * Search only within one selected document.
     *
     * IMPORTANT:
     * The userId restriction remains here.
     * This prevents a user from retrieving another
     * user's document simply by supplying its UUID.
     */
    @Query(value = """
            SELECT
                dc.id AS chunkId,
                dc.document_id AS documentId,
                d.name AS documentName,
                dc.chunk_index AS chunkIndex,
                dc.page_number AS pageNumber,
                dc.content AS content,
                (dc.embedding <=> CAST(:embedding AS vector)) AS distance
            FROM document_chunks dc
            JOIN documents d
                ON d.id = dc.document_id
            WHERE dc.embedding IS NOT NULL
              AND d.uploaded_by = :userId
              AND d.id = :documentId
              AND d.status = 'READY'
              AND (dc.embedding <=> CAST(:embedding AS vector)) <= :maxDistance
            ORDER BY dc.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<SimilarChunkProjection> findSimilarChunksByDocument(
            @Param("embedding") String embedding,
            @Param("maxDistance") double maxDistance,
            @Param("limit") int limit,
            @Param("userId") UUID userId,
            @Param("documentId") UUID documentId
    );


    /*
     * TEMPORARY DIAGNOSTIC SEARCH.
     * Returns the nearest chunks without applying maxDistance.
     * Keeps user ownership and READY-document restrictions.
     */
    @Query(value = """
        SELECT
            dc.id AS chunkId,
            dc.document_id AS documentId,
            d.name AS documentName,
            dc.chunk_index AS chunkIndex,
            dc.page_number AS pageNumber,
            dc.content AS content,
            (dc.embedding <=> CAST(:embedding AS vector)) AS distance
        FROM document_chunks dc
        JOIN documents d
            ON d.id = dc.document_id
        WHERE dc.embedding IS NOT NULL
          AND d.uploaded_by = :userId
          AND d.status = 'READY'
        ORDER BY dc.embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<SimilarChunkProjection> findNearestChunksForDiagnostics(
            @Param("embedding") String embedding,
            @Param("limit") int limit,
            @Param("userId") UUID userId
    );

    /*
     * TEMPORARY DIAGNOSTIC SEARCH within one selected document.
     * Still enforces ownership and READY-document restrictions.
     */
    @Query(value = """
        SELECT
            dc.id AS chunkId,
            dc.document_id AS documentId,
            d.name AS documentName,
            dc.chunk_index AS chunkIndex,
            dc.page_number AS pageNumber,
            dc.content AS content,
            (dc.embedding <=> CAST(:embedding AS vector)) AS distance
        FROM document_chunks dc
        JOIN documents d
            ON d.id = dc.document_id
        WHERE dc.embedding IS NOT NULL
          AND d.uploaded_by = :userId
          AND d.id = :documentId
          AND d.status = 'READY'
        ORDER BY dc.embedding <=> CAST(:embedding AS vector)
        LIMIT :limit
        """, nativeQuery = true)
    List<SimilarChunkProjection> findNearestChunksForDiagnosticsByDocument(
            @Param("embedding") String embedding,
            @Param("limit") int limit,
            @Param("userId") UUID userId,
            @Param("documentId") UUID documentId
    );

}