package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CardMapper;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.SortingUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final AdminService adminService;
    private final UserService userService;

    public CardServiceImpl(CardRepository cardRepository, AdminService adminService, UserService userService) {
        this.cardRepository = cardRepository;
        this.adminService = adminService;
        this.userService = userService;
    }

    // ===== ADMIN =====

    @Transactional
    @Override
    public CardDto createCard(CreateCardRequest request) {
        adminService.requireAdmin();

        User owner = userService.getById(request.getOwnerUid())
                .orElseThrow(() -> new UserNotFoundException("User with id " + request.getOwnerUid() + " not found"));

        Card card = Card.builder()
                .owner(owner)
                .cardNumberEnc(request.getEncryptedNumber())
                .last4(request.getLast4())
                .holderName(request.getHolderName())
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .status(Card.CardStatus.ACTIVE)
                .currency(request.getCurrency())
                .balance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .build();

        card = cardRepository.save(card);

        return CardMapper.toDto(card);
    }

    @Transactional
    @Override
    public CardDto changeCardStatus(UUID cardId, Card.CardStatus status) {
        adminService.requireAdmin();

        Card card = getCardById(cardId);
        card.setStatus(status);
        card = cardRepository.save(card);

        return CardMapper.toDto(card);
    }

    @Transactional
    @Override
    public void deleteCard(UUID cardId) {
        adminService.requireAdmin();

        if (!cardRepository.existsById(cardId)) {
            throw new CardNotFoundException("Card not found: " + cardId);
        }
        cardRepository.deleteById(cardId);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<CardDto> getAllCards(int page, int size, String sort) {
        adminService.requireAdmin();
        Pageable pageable = PageRequest.of(page, size, SortingUtils.getSort(sort));
        Page<Card> cards = cardRepository.findAll(pageable);

        return CardMapper.toCardDtoPage(cards);
    }

    // ===== USER =====

    @Transactional(readOnly = true)
    @Override
    public Page<CardDto> getUserCards(int page, int size, String sort) {
        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, SortingUtils.getSort(sort));
        Page<Card> cards = cardRepository.findByOwner(currentUser, pageable);

        return CardMapper.toCardDtoPage(cards);
    }

    @Transactional
    @Override
    public CardDto requestBlockCard(UUID cardId) {
        Card card = getCardById(cardId);
        User currentUser = userService.getCurrentUser();
        requireOwner(card, currentUser);
        card.setStatus(Card.CardStatus.BLOCKED);

        card = cardRepository.save(card);

        return CardMapper.toDto(card);
    }

    @Transactional(readOnly = true)
    @Override
    public BigDecimal getBalance(UUID cardId) {
        Card card = getCardById(cardId);
        User currentUser = userService.getCurrentUser();
        requireOwner(card, currentUser);
        return card.getBalance();
    }

    @Override
    public void requireOwner(Card card, User user) {
        if (!card.getOwner().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }

    // ===== PRIVATE HELPERS =====

    private Card getCardById(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardId));
    }
}
