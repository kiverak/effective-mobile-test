package com.example.bankcards.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCardRequest {

    @NotNull(message = "Owner UUID cannot be null")
    private UUID ownerUid;

    @NotBlank(message = "Encrypted number cannot be blank")
    private String encryptedNumber;

    @NotBlank(message = "Last 4 digits cannot be blank")
    @Size(min = 4, max = 4, message = "Last 4 digits must be exactly 4 characters")
    private String last4;

    @NotBlank(message = "Holder name cannot be blank")
    private String holderName;

    @Min(value = 1, message = "Expiry month must be between 1 and 12")
    @Max(value = 12, message = "Expiry month must be between 1 and 12")
    private int expiryMonth;

    @Min(value = 2024, message = "Expiry year must be in the future")
    private int expiryYear;

    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    private BigDecimal initialBalance;
}
