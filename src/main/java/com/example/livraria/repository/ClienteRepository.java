package com.example.livraria.repository;

import com.example.livraria.domain.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    List<Cliente> findAllByOrderByNomeAsc();

    @Query("""
            select c from Cliente c
            where lower(c.nome) like lower(concat('%', :termo, '%'))
               or lower(coalesce(c.email, '')) like lower(concat('%', :termo, '%'))
               or coalesce(c.cpf, '') like concat('%', :termo, '%')
            order by c.nome""")
    List<Cliente> buscar(@Param("termo") String termo);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Integer id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Integer id);

    Optional<Cliente> findFirstByEmailIgnoreCaseAndUsuarioIsNull(String email);

    Optional<Cliente> findByUsuarioId(Integer usuarioId);

    Optional<Cliente> findByUsuarioEmailIgnoreCase(String email);
}
