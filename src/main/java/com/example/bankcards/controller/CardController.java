package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.service.CardService;
import com.example.bankcards.service.CardTransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService cardService;
    private final CardTransferService cardTransferService;

    public CardController(CardService cardService, CardTransferService cardTransferService) {
        this.cardService = cardService;
        this.cardTransferService = cardTransferService;
    }

    // ===== ADMIN =====

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CardDto createCard(@RequestBody @Valid CreateCardRequest request) {
        return cardService.createCard(request);
    }

    @PatchMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public CardDto changeCardStatus(@RequestParam("cardId") UUID cardId,
                                    @RequestParam("status") Card.CardStatus status) {
        return cardService.changeCardStatus(cardId, status);
    }

    @DeleteMapping("/{cardId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCard(@PathVariable(name = "cardId") UUID cardId) {
        cardService.deleteCard(cardId);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardDto> getAllCards(@RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "10") int size,
                                     @RequestParam(name = "sort", defaultValue = "ASC") String sort) {
        return cardService.getAllCards(page, size, sort);
    }

    // ===== USER =====

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Page<CardDto> getUserCards(@RequestParam(name = "page", defaultValue = "0") int page,
                                      @RequestParam(name = "size", defaultValue = "10") int size,
                                      @RequestParam(name = "sort", defaultValue = "ASC") String sort) {
        return cardService.getUserCards(page, size, sort);
    }

    @PostMapping("/{cardId}/block-request")
    @PreAuthorize("hasRole('USER')")
    public CardDto requestBlockCard(@PathVariable(name = "cardId") UUID cardId) {
        return cardService.requestBlockCard(cardId);
    }

    @GetMapping("/{cardId}/balance")
    @PreAuthorize("hasRole('USER')")
    public BigDecimal getBalance(@PathVariable(name = "cardId") UUID cardId) {
        return cardService.getBalance(cardId);
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasRole('USER')")
    public void transferBetweenOwnCards(@RequestParam(name = "fromCardId") @NotNull UUID fromCardId,
                                        @RequestParam(name = "toCardId") @NotNull UUID toCardId,
                                        @RequestParam(name = "amount") @NotNull String amount) {
        cardTransferService.transferBetweenOwnCards(fromCardId, toCardId, amount);
    }
}
