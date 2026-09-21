package com.vaultRag.backend.repository;

import com.vaultRag.backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;



import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUser() {


        User user = new User();

        user.setUsername("test-user-" + System.nanoTime());
        user.setPasswordHash("test-password-hash");
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        assertNotNull(savedUser);
        assertNotNull(savedUser.getId());
        assertNotNull(savedUser.getCreatedAt());

        User foundUser = userRepository
                .findByUsername(savedUser.getUsername())
                .orElseThrow();

        assertEquals(savedUser.getId(), foundUser.getId());
        assertEquals(savedUser.getUsername(), foundUser.getUsername());
        assertEquals("USER", foundUser.getRole());
    }
}