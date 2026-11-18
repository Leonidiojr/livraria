package com.example.livraria.repository;

import com.example.livraria.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    List<Usuario> findAllByOrderByNomeAsc();

    List<Usuario> findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNomeAsc(String nome, String email);

    long countByPerfilNomeAndAtivoTrue(String perfil);
}
