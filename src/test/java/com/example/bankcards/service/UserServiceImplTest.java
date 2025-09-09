package com.example.bankcards.service;

import com.example.bankcards.entity.User;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private MockedStatic<SecurityContextHolder> mockedContext;

    private User testUser;
    private String testUsername;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testUsername = "testuser";
        testUserId = UUID.randomUUID();
        testUser = new User();
        testUser.setUsername(testUsername);
        mockedContext = Mockito.mockStatic(SecurityContextHolder.class);
    }

    @AfterEach
    void tearDown() {
        mockedContext.close();
    }

    private void mockSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(testUsername);
        mockedContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
    }

    @Test
    @DisplayName("getCurrentUsername should return username from SecurityContext")
    void getCurrentUsername_shouldReturnUsernameFromSecurityContext() {
        // Arrange
        mockSecurityContext();

        // Act
        String currentUsername = userService.getCurrentUsername();

        // Assert
        assertEquals(testUsername, currentUsername);
    }

    @Test
    @DisplayName("getByUsername should return user when found")
    void getByUsername_shouldReturnUser_whenFound() {
        // Arrange
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // Act
        User foundUser = userService.getByUsername(testUsername);

        // Assert
        assertNotNull(foundUser);
        assertEquals(testUsername, foundUser.getUsername());
    }

    @Test
    @DisplayName("getByUsername should throw UserNotFoundException when not found")
    void getByUsername_shouldThrowException_whenNotFound() {
        // Arrange
        String nonExistentUsername = "nonexistent";
        when(userRepository.findByUsername(nonExistentUsername)).thenReturn(Optional.empty());

        // Act & Assert
        UserNotFoundException exception = assertThrows(UserNotFoundException.class, () -> {
            userService.getByUsername(nonExistentUsername);
        });
        assertEquals("User not found: " + nonExistentUsername, exception.getMessage());
    }

    @Test
    @DisplayName("getById should return Optional of user when found")
    void getById_shouldReturnOptionalOfUser_whenFound() {
        // Arrange
        when(userRepository.findById(testUserId)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> foundUserOpt = userService.getById(testUserId);

        // Assert
        assertTrue(foundUserOpt.isPresent());
        assertEquals(testUser, foundUserOpt.get());
    }

    @Test
    @DisplayName("getById should return empty Optional when not found")
    void getById_shouldReturnEmptyOptional_whenNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<User> foundUserOpt = userService.getById(nonExistentId);

        // Assert
        assertTrue(foundUserOpt.isEmpty());
    }

    @Test
    @DisplayName("getCurrentUser should return the currently authenticated user")
    void getCurrentUser_shouldReturnAuthenticatedUser() {
        // Arrange
        mockSecurityContext();
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.of(testUser));

        // Act
        User currentUser = userService.getCurrentUser();

        // Assert
        assertNotNull(currentUser);
        assertEquals(testUsername, currentUser.getUsername());
    }

    @Test
    @DisplayName("getCurrentUser should throw UserNotFoundException if authenticated user not in DB")
    void getCurrentUser_shouldThrowException_whenUserNotInDb() {
        // Arrange
        mockSecurityContext();
        when(userRepository.findByUsername(testUsername)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.getCurrentUser());
    }
}