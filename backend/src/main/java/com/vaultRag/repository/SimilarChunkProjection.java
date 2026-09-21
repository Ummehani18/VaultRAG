package com.vaultRag.backend.repository;

import java.util.UUID;

public interface SimilarChunkProjection {

    UUID getChunkId();

    UUID getDocumentId();

    String getDocumentName();

    Integer getChunkIndex();

    Integer getPageNumber();

    String getContent();

    Double getDistance();
}