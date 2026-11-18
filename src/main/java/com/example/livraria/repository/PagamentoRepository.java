package com.example.livraria.repository;

import com.example.livraria.domain.Pagamento;
import com.example.livraria.domain.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Integer> {

    @Query("""
            select p from Pagamento p join p.pedido pe join pe.cliente c
            where (:status is null or p.status = :status)
              and (:termo = ''
                   or lower(c.nome) like lower(concat('%', :termo, '%'))
                   or cast(pe.id as string) = :termo
                   or lower(coalesce(p.codigoTransacao, '')) like lower(concat('%', :termo, '%')))
            order by p.id desc""")
    List<Pagamento> filtrar(@Param("status") StatusPagamento status, @Param("termo") String termo);
}
