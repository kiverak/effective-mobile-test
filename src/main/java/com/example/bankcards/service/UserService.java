package com.example.bankcards.service;

import com.example.bankcards.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserService {

    User getCurrentUser();

    String getCurrentUsername();

    User getByUsername(String username);

    Optional<User> getById(UUID userId);
}
