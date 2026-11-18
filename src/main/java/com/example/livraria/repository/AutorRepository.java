package com.example.livraria.repository;

import com.example.livraria.domain.Autor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AutorRepository extends JpaRepository<Autor, Integer> {
    List<Autor> findAllByOrderByNomeAsc();

    List<Autor> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);
}
