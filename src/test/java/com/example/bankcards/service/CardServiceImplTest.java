package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.SortingUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private AdminService adminService;
    @Mock
    private UserService userService;

    @InjectMocks
    private CardServiceImpl cardService;

    @Captor
    private ArgumentCaptor<Card> cardCaptor;

    private MockedStatic<SortingUtils> mockedSortingUtils;

    private User cardOwner;
    private User otherUser;
    private Card testCard;
    private UUID cardId;
    private UUID ownerId;
    private CreateCardRequest createCardRequest;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        cardOwner = new User();
        cardOwner.setId(ownerId);
        cardOwner.setUsername("cardowner");

        otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setUsername("otheruser");

        cardId = UUID.randomUUID();
        testCard = new Card();
        testCard.setId(cardId);
        testCard.setCardNumberEnc("1234123412341234");
        testCard.setLast4("1234");
        testCard.setOwner(cardOwner);
        testCard.setBalance(new BigDecimal("1000.00"));
        testCard.setStatus(Card.CardStatus.ACTIVE);

        createCardRequest = new CreateCardRequest(ownerId, "enc123", "1234", "Test Holder", 12, 2025, "USD", BigDecimal.TEN);

        mockedSortingUtils = Mockito.mockStatic(SortingUtils.class);
    }

    @AfterEach
    void tearDown() {
        mockedSortingUtils.close();
    }

    // ===== ADMIN TESTS =====

    @Test
    @DisplayName("ADMIN: createCard should succeed for a valid user")
    void createCard_shouldSucceed_forValidUser() {
        // Arrange
        when(userService.getById(ownerId)).thenReturn(Optional.of(cardOwner));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CardDto result = cardService.createCard(createCardRequest);

        // Assert
        verify(adminService).requireAdmin();
        verify(cardRepository).save(cardCaptor.capture());
        Card savedCard = cardCaptor.getValue();

        assertNotNull(result);
        assertEquals(cardOwner, savedCard.getOwner());
        assertEquals("1234", savedCard.getLast4());
        assertEquals(0, BigDecimal.TEN.compareTo(savedCard.getBalance()));
        assertEquals(Card.CardStatus.ACTIVE, savedCard.getStatus());
    }

    @Test
    @DisplayName("ADMIN: createCard should throw UserNotFoundException for invalid user ID")
    void createCard_shouldThrowException_forInvalidUserId() {
        // Arrange
        when(userService.getById(ownerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                cardService.createCard(createCardRequest)
        );
        verify(adminService).requireAdmin();
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("ADMIN: changeCardStatus should succeed for existing card")
    void changeCardStatus_shouldSucceed_forExistingCard() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CardDto result = cardService.changeCardStatus(cardId, Card.CardStatus.BLOCKED);

        // Assert
        verify(adminService).requireAdmin();
        verify(cardRepository).save(cardCaptor.capture());
        Card savedCard = cardCaptor.getValue();

        assertEquals(Card.CardStatus.BLOCKED, savedCard.getStatus());
        assertEquals(Card.CardStatus.BLOCKED.name(), result.status());
    }

    @Test
    @DisplayName("ADMIN: changeCardStatus should throw EntityNotFoundException for non-existent card")
    void changeCardStatus_shouldThrowException_forNonExistentCard() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () ->
                cardService.changeCardStatus(cardId, Card.CardStatus.BLOCKED)
        );
        verify(adminService).requireAdmin();
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("ADMIN: deleteCard should succeed for existing card")
    void deleteCard_shouldSucceed_forExistingCard() {
        // Arrange
        when(cardRepository.existsById(cardId)).thenReturn(true);

        // Act
        cardService.deleteCard(cardId);

        // Assert
        verify(adminService).requireAdmin();
        verify(cardRepository).deleteById(cardId);
    }

    @Test
    @DisplayName("ADMIN: deleteCard should throw CardNotFoundException for non-existent card")
    void deleteCard_shouldThrowException_forNonExistentCard() {
        // Arrange
        when(cardRepository.existsById(cardId)).thenReturn(false);

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> cardService.deleteCard(cardId));
        verify(adminService).requireAdmin();
        verify(cardRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("ADMIN: getAllCards should return a page of cards")
    void getAllCards_shouldReturnPageOfCards() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        List<Card> cardList = List.of(testCard);
        Page<Card> cardPage = new PageImpl<>(cardList, pageable, cardList.size());

        mockedSortingUtils.when(() -> SortingUtils.getSort("id:asc")).thenReturn(Sort.by("id"));
        when(cardRepository.findAll(any(Pageable.class))).thenReturn(cardPage);

        // Act
        Page<CardDto> result = cardService.getAllCards(0, 10, "id:asc");

        // Assert
        verify(adminService).requireAdmin();
        assertEquals(1, result.getTotalElements());
        assertEquals(testCard.getBalance(), result.getContent().get(0).balance());
    }

    // ===== USER TESTS =====

    @Test
    @DisplayName("USER: getUserCards should return cards for the current user")
    void getUserCards_shouldReturnCardsForCurrentUser() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        List<Card> cardList = List.of(testCard);
        Page<Card> cardPage = new PageImpl<>(cardList, pageable, cardList.size());

        when(userService.getCurrentUser()).thenReturn(cardOwner);
        mockedSortingUtils.when(() -> SortingUtils.getSort("id:asc")).thenReturn(Sort.by("id"));
        when(cardRepository.findByOwner(eq(cardOwner), any(Pageable.class))).thenReturn(cardPage);

        // Act
        Page<CardDto> result = cardService.getUserCards(0, 10, "id:asc");

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(testCard.getBalance(), result.getContent().get(0).balance());
        verify(cardRepository).findByOwner(eq(cardOwner), any(Pageable.class));
    }

    @Test
    @DisplayName("USER: requestBlockCard should succeed for card owner")
    void requestBlockCard_shouldSucceed_forCardOwner() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(userService.getCurrentUser()).thenReturn(cardOwner);
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CardDto result = cardService.requestBlockCard(cardId);

        // Assert
        verify(cardRepository).save(cardCaptor.capture());
        Card savedCard = cardCaptor.getValue();

        assertEquals(Card.CardStatus.BLOCKED, savedCard.getStatus());
        assertEquals(Card.CardStatus.BLOCKED.name(), result.status());
    }

    @Test
    @DisplayName("USER: requestBlockCard should throw AccessDeniedException for non-owner")
    void requestBlockCard_shouldThrowException_forNonOwner() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(userService.getCurrentUser()).thenReturn(otherUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> cardService.requestBlockCard(cardId));
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("USER: requestBlockCard should throw CardNotFoundException for non-existent card")
    void requestBlockCard_shouldThrowException_forNonExistentCard() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> cardService.requestBlockCard(cardId));
        verify(userService, never()).getCurrentUser();
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("USER: getBalance should succeed for card owner")
    void getBalance_shouldSucceed_forCardOwner() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(userService.getCurrentUser()).thenReturn(cardOwner);

        // Act
        BigDecimal balance = cardService.getBalance(cardId);

        // Assert
        assertEquals(0, new BigDecimal("1000.00").compareTo(balance));
    }

    @Test
    @DisplayName("USER: getBalance should throw AccessDeniedException for non-owner")
    void getBalance_shouldThrowException_forNonOwner() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(testCard));
        when(userService.getCurrentUser()).thenReturn(otherUser);

        // Act & Assert
        assertThrows(AccessDeniedException.class, () -> cardService.getBalance(cardId));
    }

    @Test
    @DisplayName("USER: getBalance should throw CardNotFoundException for non-existent card")
    void getBalance_shouldThrowException_forNonExistentCard() {
        // Arrange
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () -> cardService.getBalance(cardId));
        verify(userService, never()).getCurrentUser();
    }

    // ===== HELPER METHOD TESTS =====

    @Test
    @DisplayName("requireOwner should not throw exception for the owner")
    void requireOwner_shouldNotThrowException_forOwner() {
        // Act & Assert
        assertDoesNotThrow(() -> cardService.requireOwner(testCard, cardOwner));
    }

    @Test
    @DisplayName("requireOwner should throw AccessDeniedException for a non-owner")
    void requireOwner_shouldThrowException_forNonOwner() {
        // Act & Assert
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () ->
                cardService.requireOwner(testCard, otherUser)
        );
        assertEquals("Access denied", exception.getMessage());
    }

    @Test
    @DisplayName("requireOwner should handle users with different IDs")
    void requireOwner_shouldHandleDifferentUserIds() {
        // Arrange
        User ownerWithDifferentInstance = new User();
        ownerWithDifferentInstance.setId(ownerId);

        // Act & Assert
        assertDoesNotThrow(() -> cardService.requireOwner(testCard, ownerWithDifferentInstance));
    }

    @Test
    @DisplayName("createCard should set balance to ZERO if initialBalance is null")
    void createCard_shouldSetZeroBalance_whenInitialBalanceIsNull() {
        // Arrange
        when(userService.getById(ownerId)).thenReturn(Optional.of(cardOwner));
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        createCardRequest.setInitialBalance(null);
        cardService.createCard(createCardRequest);

        // Assert
        verify(cardRepository).save(cardCaptor.capture());
        Card savedCard = cardCaptor.getValue();

        assertEquals(0, BigDecimal.ZERO.compareTo(savedCard.getBalance()));
    }
}
