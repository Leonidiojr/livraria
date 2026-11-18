package com.example.livraria.unit;

import com.example.livraria.domain.*;
import com.example.livraria.dto.PedidoDtos.ItemRequest;
import com.example.livraria.dto.PedidoDtos.PedidoRequest;
import com.example.livraria.dto.PedidoDtos.PedidoResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.ClienteRepository;
import com.example.livraria.repository.LivroRepository;
import com.example.livraria.repository.PedidoRepository;
import com.example.livraria.service.EstoqueService;
import com.example.livraria.service.PagamentoService;
import com.example.livraria.service.PedidoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static com.example.livraria.unit.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock LivroRepository livroRepository;
    @Mock EstoqueService estoqueService;
    @Mock PagamentoService pagamentoService;
    @InjectMocks PedidoService service;

    private final Livro dom = livro(1, "Dom Casmurro", "30.00");
    private final Livro hobbit = livro(2, "O Hobbit", "50.00");
    private final Cliente ana = cliente(10, "Ana");

    @BeforeEach
    void setUp() {
        logarComo("vendedor@teste.com", Perfil.VENDEDOR);
        when(livroRepository.findById(1)).thenReturn(Optional.of(dom));
        when(livroRepository.findById(2)).thenReturn(Optional.of(hobbit));
        when(clienteRepository.findById(10)).thenReturn(Optional.of(ana));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        deslogar();
    }

    @Test
    @DisplayName("Criar pedido: usa o preco do livro, soma itens repetidos, baixa estoque e calcula o total")
    void criar() {
        PedidoResponse r = service.criar(new PedidoRequest(10, List.of(
                new ItemRequest(1, 2), new ItemRequest(2, 1), new ItemRequest(1, 1)), "  "));

        assertThat(r.status()).isEqualTo(StatusPedido.PENDENTE);
        assertThat(r.clienteNome()).isEqualTo("Ana");
        assertThat(r.itens()).hasSize(2);
        assertThat(r.itens().get(0).quantidade()).isEqualTo(3);
        assertThat(r.valorTotal()).isEqualByComparingTo("140.00"); // 3 x 30 + 1 x 50
        assertThat(r.saldo()).isEqualByComparingTo("140.00");
        assertThat(r.observacoes()).isNull();
        verify(estoqueService).baixar(dom, 3);
        verify(estoqueService).baixar(hobbit, 1);
    }

    @Test
    @DisplayName("Equipe precisa informar o cliente do pedido")
    void semCliente() {
        assertThatThrownBy(() -> service.criar(new PedidoRequest(null, List.of(new ItemRequest(1, 1)), null)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("cliente");
    }

    @Test
    @DisplayName("Livro inativo nao pode ser vendido")
    void livroInativo() {
        dom.setAtivo(false);
        assertThatThrownBy(() -> service.criar(new PedidoRequest(10, List.of(new ItemRequest(1, 1)), null)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("nao esta disponivel");
        verify(estoqueService, never()).baixar(any(), anyInt());
    }

    @Test
    @DisplayName("Usuario CLIENTE sempre compra para o proprio cadastro, ignorando o clienteId enviado")
    void clienteCompraParaSi() {
        logarComo("joao@teste.com", Perfil.CLIENTE);
        Cliente joao = cliente(20, "Joao");
        when(clienteRepository.findByUsuarioEmailIgnoreCase("joao@teste.com")).thenReturn(Optional.of(joao));

        PedidoResponse r = service.criar(new PedidoRequest(10, List.of(new ItemRequest(2, 1)), null));

        assertThat(r.clienteId()).isEqualTo(20);
    }

    @Test
    @DisplayName("Cancelar pedido devolve o estoque, cancela pagamentos pendentes e estorna os aprovados")
    void cancelar() {
        Pedido p = pedido(5, StatusPedido.PAGO, ana, dom, 2);
        Pagamento pendente = pagamento(1, p, "10.00", StatusPagamento.PENDENTE);
        Pagamento aprovado = pagamento(2, p, "50.00", StatusPagamento.APROVADO);
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(p));

        PedidoResponse r = service.alterarStatus(5, StatusPedido.CANCELADO);

        assertThat(r.status()).isEqualTo(StatusPedido.CANCELADO);
        assertThat(pendente.getStatus()).isEqualTo(StatusPagamento.CANCELADO);
        assertThat(aprovado.getStatus()).isEqualTo(StatusPagamento.ESTORNADO);
        verify(estoqueService).devolver(dom, 2);
    }

    @Test
    @DisplayName("Transicoes invalidas de status sao rejeitadas")
    void transicoesInvalidas() {
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, StatusPedido.PENDENTE, ana, dom, 1)));
        when(pedidoRepository.findById(6)).thenReturn(Optional.of(pedido(6, StatusPedido.ENTREGUE, ana, dom, 1)));

        assertThatThrownBy(() -> service.alterarStatus(5, StatusPedido.ENVIADO)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.alterarStatus(5, StatusPedido.PAGO))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("automaticamente");
        assertThatThrownBy(() -> service.alterarStatus(6, StatusPedido.CANCELADO)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Fluxo de entrega: PAGO -> ENVIADO -> ENTREGUE")
    void fluxoEntrega() {
        Pedido p = pedido(7, StatusPedido.PAGO, ana, dom, 1);
        when(pedidoRepository.findById(7)).thenReturn(Optional.of(p));

        assertThat(service.alterarStatus(7, StatusPedido.ENVIADO).status()).isEqualTo(StatusPedido.ENVIADO);
        assertThat(service.alterarStatus(7, StatusPedido.ENTREGUE).status()).isEqualTo(StatusPedido.ENTREGUE);
    }

    @Test
    @DisplayName("CLIENTE nao enxerga pedido de outro cliente e so pode cancelar os proprios pendentes")
    void restricoesDoCliente() {
        logarComo("joao@teste.com", Perfil.CLIENTE);
        Cliente joao = cliente(20, "Joao");
        when(clienteRepository.findByUsuarioEmailIgnoreCase("joao@teste.com")).thenReturn(Optional.of(joao));
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, StatusPedido.PENDENTE, ana, dom, 1)));
        when(pedidoRepository.findById(8)).thenReturn(Optional.of(pedido(8, StatusPedido.PAGO, joao, dom, 1)));

        assertThatThrownBy(() -> service.buscar(5)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.alterarStatus(8, StatusPedido.CANCELADO))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("pendentes");
    }

    @Test
    @DisplayName("Alterar itens so e permitido em pedido pendente")
    void atualizarSomentePendente() {
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(pedido(5, StatusPedido.PAGO, ana, dom, 1)));

        assertThatThrownBy(() -> service.atualizar(5, new PedidoRequest(10, List.of(new ItemRequest(1, 1)), null)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Alterar itens devolve o estoque antigo e baixa o novo")
    void atualizarItens() {
        Pedido p = pedido(5, StatusPedido.PENDENTE, ana, dom, 2);
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(p));

        PedidoResponse r = service.atualizar(5, new PedidoRequest(null, List.of(new ItemRequest(2, 3)), "novo"));

        verify(estoqueService).devolver(dom, 2);
        verify(estoqueService).baixar(hobbit, 3);
        assertThat(r.valorTotal()).isEqualByComparingTo("150.00");
        assertThat(r.observacoes()).isEqualTo("novo");
    }

    @Test
    @DisplayName("Pedido com pagamentos nao pode ser excluido")
    void excluirComPagamentos() {
        Pedido p = pedido(5, StatusPedido.PENDENTE, ana, dom, 1);
        pagamento(1, p, "10.00", StatusPagamento.CANCELADO);
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(p));

        assertThatThrownBy(() -> service.excluir(5)).isInstanceOf(RegraNegocioException.class);
        verify(pedidoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Excluir pedido pendente devolve o estoque")
    void excluirPendente() {
        Pedido p = pedido(5, StatusPedido.PENDENTE, ana, dom, 4);
        when(pedidoRepository.findById(5)).thenReturn(Optional.of(p));

        service.excluir(5);

        verify(estoqueService).devolver(dom, 4);
        verify(pedidoRepository).delete(p);
    }
}
