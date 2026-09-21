package com.vaultRag.backend.service;

import com.vaultRag.backend.entity.User;
import com.vaultRag.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldCreateUserWithHashedPassword() {

        String username = "service-test-" + System.nanoTime();
        String rawPassword = "MySecret123!";

        User user = userService.createUser(
                username,
                rawPassword,
                "USER"
        );

        assertNotNull(user.getId());

        assertNotEquals(
                rawPassword,
                user.getPasswordHash()
        );

        assertTrue(
                userService.verifyPassword(rawPassword, user)
        );

        assertFalse(
                userService.verifyPassword("WrongPassword", user)
        );

        assertEquals(
                user.getId(),
                userService.findByUsername(username).getId()
        );
    }
}