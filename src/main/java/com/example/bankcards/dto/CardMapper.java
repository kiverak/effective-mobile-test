package com.example.bankcards.dto;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

public class CardMapper {

    private CardMapper() {
    }

    public static CardDto toDto(Card card) {
        if (card == null) {
            return null;
        }

        return new CardDto(
                maskCardNumber(card.getLast4()),
                card.getOwner().getUsername(),
                card.getExpiryMonth(),
                card.getExpiryYear(),
                card.getStatus().name(),
                card.getBalance()
        );
    }

    public static Page<CardDto> toCardDtoPage(Page<Card> cards) {
        List<CardDto> content = cards.stream()
                .map(CardMapper::toDto)
                .toList();

        return new PageImpl<>(content, cards.getPageable(), cards.getTotalElements());
    }

    private static String maskCardNumber(String last4) {
        if (last4 == null || last4.length() < 4) {
            throw new IllegalArgumentException("Invalid card number");
        }
        return "**** **** **** " + last4;
    }

}
