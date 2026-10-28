
package com.golu.expense.dto;

public class AuthResponse {

    private String token;
    private String tokenType;
    private long expiresInMs;
    private UserResponse user;

    public AuthResponse() {
    }

    public AuthResponse(String token, String tokenType,
                        long expiresInMs, UserResponse user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInMs() {
        return expiresInMs;
    }

    public UserResponse getUser() {
        return user;
    }
}
