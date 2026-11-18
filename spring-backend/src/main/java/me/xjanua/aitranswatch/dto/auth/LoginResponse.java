package me.xjanua.aitranswatch.dto.auth;

import java.util.List;

public record LoginResponse(String accessToken, String tokenType, long expiresIn, UserMeResponse user) {
    public static LoginResponse bearer(String token, long expiresIn, UserMeResponse user) {
        return new LoginResponse(token, "Bearer", expiresIn, user);
    }
}
