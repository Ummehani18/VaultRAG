package com.vaultRag.backend.controller;

import com.vaultRag.backend.dto.AdminUserResponse;
import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100"
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.ASC,
                        "createdAt"
                )
        );

        Page<User> userPage =
                userRepository.findAll(pageable);

        List<AdminUserResponse> content =
                userPage.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return Map.of(
                "content", content,
                "page", userPage.getNumber(),
                "size", userPage.getSize(),
                "totalElements",
                userPage.getTotalElements(),
                "totalPages",
                userPage.getTotalPages(),
                "first",
                userPage.isFirst(),
                "last",
                userPage.isLast()
        );
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}