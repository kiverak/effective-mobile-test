package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
    }

    @Test
    @DisplayName("requireAdmin should not throw exception when user is an admin")
    void requireAdmin_shouldNotThrowException_whenUserIsAdmin() {
        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        testUser.setRoles(Set.of(adminRole));

        when(userService.getCurrentUser()).thenReturn(testUser);

        assertDoesNotThrow(() -> adminService.requireAdmin(), "Should not throw AccessDeniedException for an admin user.");
    }

    @Test
    @DisplayName("requireAdmin should throw AccessDeniedException when user is not an admin")
    void requireAdmin_shouldThrowAccessDeniedException_whenUserIsNotAdmin() {
        Role userRole = new Role();
        userRole.setName("ROLE_USER");
        testUser.setRoles(Set.of(userRole));

        when(userService.getCurrentUser()).thenReturn(testUser);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            adminService.requireAdmin();
        }, "Should throw AccessDeniedException for a non-admin user.");

        assertEquals("Admin rights required", exception.getMessage());
    }

    @Test
    @DisplayName("requireAdmin should throw AccessDeniedException when user has no roles")
    void requireAdmin_shouldThrowAccessDeniedException_whenUserHasNoRoles() {
        testUser.setRoles(Collections.emptySet());

        when(userService.getCurrentUser()).thenReturn(testUser);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            adminService.requireAdmin();
        }, "Should throw AccessDeniedException for a user with no roles.");

        assertEquals("Admin rights required", exception.getMessage());
    }
}