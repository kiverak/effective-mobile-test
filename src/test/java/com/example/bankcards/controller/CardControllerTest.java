package com.example.bankcards.controller;

import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.RoleRepository;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.service.CardTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockitoBean
    private CardTransferService cardTransferService;

    private User adminUser;
    private User regularUser;
    private Card card1;
    private Card card2;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        roleRepository.deleteAll();
        cardRepository.deleteAll();
        userRepository.deleteAll();

        adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        userRole = new Role();
        userRole.setName("ROLE_USER");
        roleRepository.saveAll(Set.of(adminRole, userRole));

        adminUser = User.builder()
                .username("admin")
                .password("password")
                .roles(Set.of(adminRole))
                .status(User.UserStatus.ACTIVE)
                .build();
        regularUser = User.builder()
                .username("user")
                .password("password")
                .roles(Set.of(userRole))
                .status(User.UserStatus.ACTIVE)
                .build();
        userRepository.saveAll(Set.of(adminUser, regularUser));

        card1 = Card.builder()
                .cardNumberEnc("1111222233334444")
                .last4("4444")
                .balance(new BigDecimal("1000.00"))
                .owner(regularUser)
                .holderName("holderName")
                .expiryMonth(12)
                .expiryYear(2028)
                .status(Card.CardStatus.ACTIVE)
                .currency("USD")
                .build();
        cardRepository.save(card1);

        card2 = Card.builder()
                .cardNumberEnc("5555666677778888")
                .last4("8888")
                .balance(new BigDecimal("500.00"))
                .owner(regularUser)
                .holderName("holderName")
                .expiryMonth(12)
                .expiryYear(2028)
                .status(Card.CardStatus.ACTIVE)
                .currency("USD")
                .build();
        cardRepository.save(card2);
    }

    // --- ADMIN TESTS ---

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void createCard_asAdmin_shouldSucceed() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                regularUser.getId(),
                "enc123",
                "1234",
                "Test Holder",
                12,
                2025,
                "USD",
                BigDecimal.TEN);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.owner").value(regularUser.getUsername()));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void changeCardStatus_asAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(patch("/api/cards/status")
                        .param("cardId", card1.getId().toString())
                        .param("status", Card.CardStatus.BLOCKED.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(Card.CardStatus.BLOCKED.name()));

        Card updated = cardRepository.findById(card1.getId()).orElseThrow();
        assertEquals(Card.CardStatus.BLOCKED, updated.getStatus());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteCard_asAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(delete("/api/cards/" + card1.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cards/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllCards_asAdmin_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/cards/all")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user")
    void getAllCards_asNonAdmin_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/cards/all")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "ASC"))
                .andExpect(status().isForbidden());
    }

    // --- USER TESTS ---

    @Test
    @WithMockUser(username = "user")
    void getUserCards_asUser_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user")
    void requestBlockCard_asUser_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/cards/" + card1.getId() + "/block-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(Card.CardStatus.BLOCKED.name()));
    }

    @Test
    @WithMockUser(username = "user")
    void getBalance_asUser_shouldSucceed() throws Exception {
        mockMvc.perform(get("/api/cards/" + card1.getId() + "/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(card1.getBalance().doubleValue()));
    }

    @Test
    @WithMockUser(username = "user")
    void transferBetweenOwnCards_asUser_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/cards/transfer")
                        .param("fromCardId", card1.getId().toString())
                        .param("toCardId", card2.getId().toString())
                        .param("amount", "100.00"))
                .andExpect(status().isOk());

        Mockito.verify(cardTransferService)
                .transferBetweenOwnCards(card1.getId(), card2.getId(), "100.00");
    }

    @Test
    @WithMockUser(username = "user")
    void transferBetweenOwnCards_missingAmount_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/cards/transfer")
                        .param("fromCardId", card1.getId().toString())
                        .param("toCardId", card2.getId().toString()))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(cardTransferService);
    }

    @Test
    @WithMockUser(username = "user")
    void transferBetweenOwnCards_missingFromCardId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/cards/transfer")
                        .param("toCardId", card2.getId().toString())
                        .param("amount", "100.00"))
                .andExpect(status().isBadRequest());

        Mockito.verifyNoInteractions(cardTransferService);
    }
}