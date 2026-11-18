package com.example.livraria.unit;

import com.example.livraria.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

/** Objetos de apoio para os testes unitarios. */
final class Fixtures {

    private Fixtures() {
    }

    static void logarComo(String email, String perfil) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority("ROLE_" + perfil))));
    }

    static void deslogar() {
        SecurityContextHolder.clearContext();
    }

    static Livro livro(int id, String titulo, String preco) {
        Livro l = new Livro();
        l.setId(id);
        l.setTitulo(titulo);
        l.setPreco(new BigDecimal(preco));
        l.setAtivo(true);
        return l;
    }

    static Cliente cliente(int id, String nome) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome(nome);
        return c;
    }

    static Pedido pedido(int id, StatusPedido status, Cliente cliente, Livro livro, int quantidade) {
        Pedido p = new Pedido();
        p.setId(id);
        p.setStatus(status);
        p.setCliente(cliente);
        p.adicionarItem(new ItemPedido(livro, quantidade));
        p.recalcularTotal();
        return p;
    }

    static Pagamento pagamento(int id, Pedido pedido, String valor, StatusPagamento status) {
        Pagamento pg = new Pagamento();
        pg.setId(id);
        pg.setPedido(pedido);
        pg.setValor(new BigDecimal(valor));
        pg.setFormaPagamento(FormaPagamento.PIX);
        pg.setStatus(status);
        pedido.getPagamentos().add(pg);
        return pg;
    }
}
