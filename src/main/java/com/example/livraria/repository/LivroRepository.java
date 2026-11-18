package com.example.livraria.repository;

import com.example.livraria.domain.Livro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LivroRepository extends JpaRepository<Livro, Integer> {

    boolean existsByIsbn(String isbn);

    boolean existsByIsbnAndIdNot(String isbn, Integer id);

    List<Livro> findAllByOrderByTituloAsc();

    List<Livro> findByAutoresId(Integer autorId);

    boolean existsByCategoriaId(Integer categoriaId);

    boolean existsByEditoraId(Integer editoraId);

    @Query("""
            select distinct l from Livro l left join l.autores a
            where lower(l.titulo) like lower(concat('%', :termo, '%'))
               or lower(l.isbn) like lower(concat('%', :termo, '%'))
               or lower(a.nome) like lower(concat('%', :termo, '%'))
            order by l.titulo
            """)
    List<Livro> buscar(@Param("termo") String termo);
}
