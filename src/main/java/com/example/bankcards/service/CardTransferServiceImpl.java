package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotActiveException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.repository.CardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class CardTransferServiceImpl implements CardTransferService {

    private final CardRepository cardRepository;
    private final UserService userService;

    public CardTransferServiceImpl(CardRepository cardRepository, UserService userService) {
        this.cardRepository = cardRepository;
        this.userService = userService;
    }

    @Override
    @Transactional
    public void transferBetweenOwnCards(UUID fromCardId, UUID toCardId, String transferAmount) {
        BigDecimal amount = new BigDecimal(transferAmount);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        User currentUser = userService.getCurrentUser();

        Card from = getCardById(fromCardId, currentUser);
        Card to = getCardById(toCardId, currentUser);

        validateCards(from, to, amount);

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        cardRepository.saveAll(List.of(from, to));
    }

    private Card getCardById(UUID cardId, User user) {
        return cardRepository.findByIdAndOwner(cardId, user)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));
    }

    private void validateCards(Card from, Card to,  BigDecimal amount) {
        if (!from.getStatus().equals(Card.CardStatus.ACTIVE)) {
            throw new CardNotActiveException("Card is not active: " + from.getId());
        }

        if (!to.getStatus().equals(Card.CardStatus.ACTIVE)) {
            throw new CardNotActiveException("Card is not active: " + to.getId());
        }

        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
    }
}
