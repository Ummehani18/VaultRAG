package com.vaultRag.backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "document_id",
            nullable = false
    )
    private Document document;

    @Column(
            name = "chunk_index",
            nullable = false
    )
    private Integer chunkIndex;

    @Column(
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String content;

    /**
     * Physical PDF page number.
     *
     * This is the page index inside the PDF,
     * starting from 1.
     *
     * It does not depend on a printed page number
     * being present in the document.
     */
    @Column(name = "page_number")
    private Integer pageNumber;

    /*
     * The embedding column uses PostgreSQL pgvector.
     *
     * Vector operations are handled through the
     * repository/query layer.
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

    public void setPageNumber(
            Integer pageNumber
    ) {
        this.pageNumber = pageNumber;
    }
}