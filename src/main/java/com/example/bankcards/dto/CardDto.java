package com.example.bankcards.dto;

import java.math.BigDecimal;

public record CardDto(String cardMask, String owner, int expiryMonth, int expireYear, String status, BigDecimal balance) {
}
