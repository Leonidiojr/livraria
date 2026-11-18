package com.example.livraria.repository;

import com.example.livraria.domain.LivroCapa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LivroCapaRepository extends JpaRepository<LivroCapa, Integer> {
}
