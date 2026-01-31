package com.example.bankcards.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "card_number_enc", nullable = false, length = 512)
    @NotBlank
    private String cardNumberEnc; // зашифрованный PAN

    @Column(name = "last4", nullable = false, length = 4)
    @Pattern(regexp = "\\d{4}")
    private String last4; // для маскирования

    @Column(name = "holder_name", nullable = false, length = 100)
    @NotBlank
    private String holderName;

    @Min(1)
    @Max(12)
    @Column(name = "expiry_month", nullable = false)
    private int expiryMonth;

    @Column(name = "expiry_year", nullable = false)
    private int expiryYear; // YYYY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    @DecimalMin(value = "0.00")
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency; // ISO 4217

    public enum CardStatus {
        ACTIVE,
        BLOCKED,
        EXPIRED
    }

    public boolean isExpired() {
        YearMonth now = YearMonth.now();
        return YearMonth.of(expiryYear, expiryMonth).isBefore(now);
    }
}
