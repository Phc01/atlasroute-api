package com.atlasroute.api.user;

/** Dados públicos do usuário. Nunca inclui a senha nem o hash. */
public record UserResponse(Long id, String name, String email, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
