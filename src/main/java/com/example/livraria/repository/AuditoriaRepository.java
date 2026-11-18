package com.example.livraria.repository;

import com.example.livraria.domain.Auditoria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("""
            select a from Auditoria a
            where (:tabela = '' or a.tabela = :tabela)
              and (:operacao = '' or a.operacao = :operacao)
              and (:termo = ''
                   or lower(coalesce(a.usuario, '')) like lower(concat('%', :termo, '%'))
                   or lower(coalesce(a.detalhes, '')) like lower(concat('%', :termo, '%')))
            order by a.id desc""")
    List<Auditoria> filtrar(@Param("tabela") String tabela, @Param("operacao") String operacao,
                            @Param("termo") String termo, Pageable pagina);

    @Query("select distinct a.tabela from Auditoria a order by a.tabela")
    List<String> tabelas();
}
