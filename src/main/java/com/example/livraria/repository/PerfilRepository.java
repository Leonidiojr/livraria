package com.example.livraria.repository;

import com.example.livraria.domain.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PerfilRepository extends JpaRepository<Perfil, Integer> {
    Optional<Perfil> findByNome(String nome);

    List<Perfil> findAllByOrderByNomeAsc();
}
