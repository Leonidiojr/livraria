package com.example.livraria.repository;

import com.example.livraria.domain.Pedido;
import com.example.livraria.domain.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Integer> {

    /** Filtros opcionais: status, cliente e termo (nome/e-mail do cliente ou numero do pedido). */
    @Query("""
            select p from Pedido p join p.cliente c
            where (:status is null or p.status = :status)
              and (:clienteId is null or c.id = :clienteId)
              and (:termo = ''
                   or lower(c.nome) like lower(concat('%', :termo, '%'))
                   or lower(coalesce(c.email, '')) like lower(concat('%', :termo, '%'))
                   or cast(p.id as string) = :termo)
            order by p.dataPedido desc, p.id desc""")
    List<Pedido> filtrar(@Param("status") StatusPedido status,
                         @Param("clienteId") Integer clienteId,
                         @Param("termo") String termo);

    boolean existsByClienteId(Integer clienteId);
}
