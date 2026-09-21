package com.vaultRag.backend.entity;

import jakarta.persistence.*;


@Entity
@Table(name = "document_chunks")
public class DocumentChunk extends BaseEntity{


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    /*
     * The embedding column uses PostgreSQL pgvector.
     *
     * We are intentionally not mapping it as a normal JPA
     * Java field yet. We'll handle vector operations through
     * a dedicated repository/query layer when we implement
     * semantic search.
     */

    public DocumentChunk() {
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }
}