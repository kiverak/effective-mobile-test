package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardTransferServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CardTransferServiceImpl cardTransferService;

    @Captor
    private ArgumentCaptor<List<Card>> cardListCaptor;

    private User currentUser;
    private Card fromCard;
    private Card toCard;
    private UUID fromCardId;
    private UUID toCardId;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setUsername("testuser");

        fromCardId = UUID.randomUUID();
        fromCard = new Card();
        fromCard.setId(fromCardId);
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setStatus(Card.CardStatus.ACTIVE);
        fromCard.setOwner(currentUser);

        toCardId = UUID.randomUUID();
        toCard = new Card();
        toCard.setId(toCardId);
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(Card.CardStatus.ACTIVE);
        toCard.setOwner(currentUser);
    }

    @Test
    @DisplayName("transferBetweenOwnCards should succeed with valid data")
    void transferBetweenOwnCards_shouldSucceed_withValidData() {
        // Arrange
        String amount = "100.00";
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(cardRepository.findByIdAndOwner(fromCardId, currentUser)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toCardId, currentUser)).thenReturn(Optional.of(toCard));

        // Act
        cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, amount);

        // Assert
        verify(cardRepository).saveAll(cardListCaptor.capture());
        List<Card> savedCards = cardListCaptor.getValue();

        Card savedFromCard = savedCards.stream().filter(c -> c.getId().equals(fromCardId)).findFirst().orElseThrow();
        Card savedToCard = savedCards.stream().filter(c -> c.getId().equals(toCardId)).findFirst().orElseThrow();

        assertEquals(0, new BigDecimal("900.00").compareTo(savedFromCard.getBalance()), "From-card balance is incorrect");
        assertEquals(0, new BigDecimal("600.00").compareTo(savedToCard.getBalance()), "To-card balance is incorrect");
    }

    @Test
    @DisplayName("transferBetweenOwnCards should throw IllegalArgumentException for non-positive amount")
    void transferBetweenOwnCards_shouldThrowException_forNonPositiveAmount() {
        // Arrange
        String zeroAmount = "0";
        String negativeAmount = "-100.00";

        // Act & Assert
        IllegalArgumentException zeroException = assertThrows(IllegalArgumentException.class, () ->
                cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, zeroAmount));
        assertEquals("Amount must be positive", zeroException.getMessage());

        IllegalArgumentException negativeException = assertThrows(IllegalArgumentException.class, () ->
                cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, negativeAmount));
        assertEquals("Amount must be positive", negativeException.getMessage());

        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("transferBetweenOwnCards should throw IllegalStateException for insufficient funds")
    void transferBetweenOwnCards_shouldThrowException_forInsufficientFunds() {
        // Arrange
        String amount = "2000.00"; // More than balance
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(cardRepository.findByIdAndOwner(fromCardId, currentUser)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toCardId, currentUser)).thenReturn(Optional.of(toCard));

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
                cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, amount));

        assertEquals("Insufficient funds", exception.getMessage());
        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("transferBetweenOwnCards should throw CardNotFoundException if 'from' card not found")
    void transferBetweenOwnCards_shouldThrowException_whenFromCardNotFound() {
        // Arrange
        String amount = "100.00";
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(cardRepository.findByIdAndOwner(fromCardId, currentUser)).thenReturn(Optional.empty());

        // Act & Assert
        CardNotFoundException exception = assertThrows(CardNotFoundException.class, () ->
                cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, amount));

        assertEquals("Card not found: " + fromCardId, exception.getMessage());
        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("transferBetweenOwnCards should throw CardNotFoundException if 'to' card not found")
    void transferBetweenOwnCards_shouldThrowException_whenToCardNotFound() {
        // Arrange
        String amount = "100.00";
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(cardRepository.findByIdAndOwner(fromCardId, currentUser)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toCardId, currentUser)).thenReturn(Optional.empty());

        // Act & Assert
        CardNotFoundException exception = assertThrows(CardNotFoundException.class, () ->
                cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, amount));

        assertEquals("Card not found: " + toCardId, exception.getMessage());
        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("transferBetweenOwnCards should throw exception if 'from' card not owned by user")
    void transferBetweenOwnCards_shouldThrowException_whenFromCardNotOwned() {
        // Arrange
        String amount = "100.00";
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(cardRepository.findByIdAndOwner(fromCardId, currentUser)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () ->
                cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, amount));

        verify(cardRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("transferBetweenOwnCards should throw exception if 'to' card not owned by user")
    void transferBetweenOwnCards_shouldThrowException_whenToCardNotOwned() {
        // Arrange
        String amount = "100.00";
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(cardRepository.findByIdAndOwner(fromCardId, currentUser)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findByIdAndOwner(toCardId, currentUser)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CardNotFoundException.class, () ->
                cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, amount));

        // Verify both ownership checks were attempted
        verify(cardRepository, never()).saveAll(any());
    }
}
