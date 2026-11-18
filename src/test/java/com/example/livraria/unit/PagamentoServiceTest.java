package com.example.livraria.unit;

import com.example.livraria.domain.*;
import com.example.livraria.dto.PagamentoDtos.PagamentoRequest;
import com.example.livraria.dto.PagamentoDtos.PagamentoResponse;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.PagamentoRepository;
import com.example.livraria.repository.PedidoRepository;
import com.example.livraria.service.PagamentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static com.example.livraria.unit.Fixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagamentoServiceTest {

    @Mock PagamentoRepository repository;
    @Mock PedidoRepository pedidoRepository;
    @InjectMocks PagamentoService service;

    private Pedido pedido; // total R$ 100,00

    @BeforeEach
    void setUp() {
        pedido = pedido(1, StatusPedido.PENDENTE, cliente(1, "Ana"), livro(1, "Livro", "50.00"), 2);
        when(pedidoRepository.findById(1)).thenReturn(Optional.of(pedido));
        when(repository.save(any(Pagamento.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pagamento registrado(int id, String valor, StatusPagamento status) {
        Pagamento p = pagamento(id, pedido, valor, status);
        when(repository.findById(id)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    @DisplayName("Registrar pagamento cria com status PENDENTE")
    void registrar() {
        PagamentoResponse r = service.registrar(new PagamentoRequest(1, FormaPagamento.PIX, new BigDecimal("40")));

        assertThat(r.status()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(r.valor()).isEqualByComparingTo("40.00");
        assertThat(pedido.getPagamentos()).hasSize(1);
    }

    @Test
    @DisplayName("Nao aceita valor acima do saldo em aberto (considerando pendentes e aprovados)")
    void acimaDoSaldo() {
        registrado(9, "70.00", StatusPagamento.PENDENTE);

        assertThatThrownBy(() -> service.registrar(new PagamentoRequest(1, FormaPagamento.PIX, new BigDecimal("30.01"))))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("saldo");
    }

    @Test
    @DisplayName("Nao aceita pagamento para pedido que nao esta pendente")
    void pedidoNaoPendente() {
        pedido.setStatus(StatusPedido.CANCELADO);
        assertThatThrownBy(() -> service.registrar(new PagamentoRequest(1, FormaPagamento.BOLETO, BigDecimal.TEN)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Aprovacao parcial mantem o pedido PENDENTE; ao quitar, o pedido vira PAGO")
    void aprovacao() {
        registrado(1, "40.00", StatusPagamento.PENDENTE);
        registrado(2, "60.00", StatusPagamento.PENDENTE);

        PagamentoResponse r1 = service.alterarStatus(1, StatusPagamento.APROVADO);
        assertThat(r1.codigoTransacao()).startsWith("TX-");
        assertThat(r1.dataPagamento()).isNotNull();
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PENDENTE);

        service.alterarStatus(2, StatusPagamento.APROVADO);
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PAGO);
    }

    @Test
    @DisplayName("Estornar pagamento de pedido PAGO devolve o pedido para PENDENTE")
    void estorno() {
        registrado(1, "100.00", StatusPagamento.APROVADO);
        pedido.setStatus(StatusPedido.PAGO);

        service.alterarStatus(1, StatusPagamento.ESTORNADO);

        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.PENDENTE);
    }

    @Test
    @DisplayName("Nao e possivel estornar pagamento de pedido ja enviado")
    void estornoAposEnvio() {
        registrado(1, "100.00", StatusPagamento.APROVADO);
        pedido.setStatus(StatusPedido.ENVIADO);

        assertThatThrownBy(() -> service.alterarStatus(1, StatusPagamento.ESTORNADO))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Transicoes de pagamento invalidas sao rejeitadas")
    void transicoesInvalidas() {
        registrado(1, "10.00", StatusPagamento.APROVADO);
        registrado(2, "10.00", StatusPagamento.PENDENTE);

        assertThatThrownBy(() -> service.alterarStatus(1, StatusPagamento.CANCELADO)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.alterarStatus(1, StatusPagamento.APROVADO)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.alterarStatus(2, StatusPagamento.ESTORNADO)).isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.alterarStatus(2, StatusPagamento.PENDENTE)).isInstanceOf(RegraNegocioException.class);
    }
}
