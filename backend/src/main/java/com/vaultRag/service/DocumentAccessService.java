package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.Document;
import com.vaultRag.backend.entity.User;
import org.springframework.stereotype.Service;

@Service
public class DocumentAccessService {

    private final UserService userService;

    public DocumentAccessService(UserService userService) {
        this.userService = userService;
    }

    public boolean canAccess(Document document, String username) {

        if (document == null || username == null || username.isBlank()) {
            return false;
        }

        User user = userService.findByUsername(username);

        if (document.getUploadedBy() == null) {
            return false;
        }

        return document.getUploadedBy()
                .getId()
                .equals(user.getId());
    }
}