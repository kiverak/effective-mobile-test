package com.example.bankcards.service;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

public interface CardService {

    @Transactional
    CardDto createCard(CreateCardRequest request);

    @Transactional
    CardDto changeCardStatus(UUID cardId, Card.CardStatus status);

    @Transactional
    void deleteCard(UUID cardId);

    @Transactional
    CardDto requestBlockCard(UUID cardId);

    @Transactional(readOnly = true)
    BigDecimal getBalance(UUID cardId);

    void requireOwner(Card card, User user);

    Page<CardDto> getAllCards(int page, int size, String sort);

    Page<CardDto> getUserCards(int page, int size, String sort);
}
