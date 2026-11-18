package com.example.livraria.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Optional;

/** Acesso ao usuario autenticado da requisicao atual. */
public final class UsuarioLogado {

    private UsuarioLogado() {
    }

    public static Optional<String> email() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    public static boolean temPerfil(String... perfis) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> Arrays.stream(perfis).anyMatch(p -> a.getAuthority().equals("ROLE_" + p)));
    }
}
