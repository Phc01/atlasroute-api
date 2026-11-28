package com.atlasroute.api.user;

/**
 * Perfil de acesso do usuário.
 *
 * <p>Salvo como texto na coluna {@code users.role}; a migration tem um CHECK com os mesmos
 * valores, então um perfil novo aqui exige uma migration nova.
 */
public enum Role {
    /** Administra cadastros, romaneios e otimizações. */
    ADMIN,
    /** Motorista: vê e executa os próprios romaneios. */
    DRIVER
}
