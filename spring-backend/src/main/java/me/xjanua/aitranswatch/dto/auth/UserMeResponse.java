package me.xjanua.aitranswatch.dto.auth;

import java.util.List;
import java.util.UUID;

import me.xjanua.aitranswatch.model.User;

public record UserMeResponse(UUID id, String username, String email, String fullName, List<String> roles) {
    public static UserMeResponse from(User user) {
        return new UserMeResponse(user.getId(), user.getUsername(), user.getEmail(), user.getFullName(),
                user.getRoles().stream().map(role -> role.getCode()).sorted().toList());
    }
}
