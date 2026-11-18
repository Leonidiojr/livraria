package com.example.livraria.security;

import com.example.livraria.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Carrega o usuario do banco para o Spring Security (perfil -> ROLE_&lt;PERFIL&gt;). */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(u -> User.withUsername(u.getEmail())
                        .password(u.getSenhaHash())
                        .disabled(!Boolean.TRUE.equals(u.getAtivo()))
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().getNome())))
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException(email));
    }
}
