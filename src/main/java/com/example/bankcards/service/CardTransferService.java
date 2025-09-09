package com.example.bankcards.service;

import java.util.UUID;

public interface CardTransferService {

    void transferBetweenOwnCards(UUID fromCardId, UUID toCardId, String amount);
}
