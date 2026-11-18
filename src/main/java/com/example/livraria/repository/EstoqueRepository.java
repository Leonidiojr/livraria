package com.example.livraria.repository;

import com.example.livraria.domain.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EstoqueRepository extends JpaRepository<Estoque, Integer> {

    @Query("select e from Estoque e join fetch e.livro l order by l.titulo")
    List<Estoque> listarComLivro();

    @Query("""
            select e from Estoque e join fetch e.livro l
            where lower(l.titulo) like lower(concat('%', :termo, '%')) or lower(l.isbn) like lower(concat('%', :termo, '%'))
            order by l.titulo""")
    List<Estoque> buscar(@Param("termo") String termo);

    Optional<Estoque> findByLivroId(Integer livroId);

    boolean existsByLivroId(Integer livroId);

    /** Trava a linha de estoque durante a venda para evitar vender a mesma unidade duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.livro.id = :livroId")
    Optional<Estoque> travarPorLivro(@Param("livroId") Integer livroId);
}
