package com.example.livraria.service;

import com.example.livraria.domain.Pagamento;
import com.example.livraria.domain.Pedido;
import com.example.livraria.domain.StatusPagamento;
import com.example.livraria.domain.StatusPedido;
import com.example.livraria.dto.PagamentoDtos.PagamentoRequest;
import com.example.livraria.dto.PagamentoDtos.PagamentoResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.PagamentoRepository;
import com.example.livraria.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Regras de pagamento:
 * <ul>
 *   <li>so e possivel registrar pagamento para pedido PENDENTE, sem ultrapassar o saldo em aberto;</li>
 *   <li>PENDENTE -> APROVADO ou CANCELADO; APROVADO -> ESTORNADO;</li>
 *   <li>quando os pagamentos aprovados cobrem o total, o pedido passa a PAGO;</li>
 *   <li>estornar um pagamento de pedido PAGO devolve o pedido para PENDENTE (nao permitido apos o envio).</li>
 * </ul>
 */
@Service
public class PagamentoService {

    private final PagamentoRepository repository;
    private final PedidoRepository pedidoRepository;

    public PagamentoService(PagamentoRepository repository, PedidoRepository pedidoRepository) {
        this.repository = repository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<PagamentoResponse> listar(StatusPagamento status, String busca) {
        String termo = (busca == null || busca.isBlank()) ? "" : busca.trim(); // "" = sem filtro
        return repository.filtrar(status, termo).stream().map(PagamentoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PagamentoResponse buscar(Integer id) {
        return PagamentoResponse.from(obter(id));
    }

    @Transactional
    public PagamentoResponse registrar(PagamentoRequest req) {
        Pedido pedido = pedidoRepository.findById(req.pedidoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", req.pedidoId()));
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new RegraNegocioException("Pagamentos so podem ser registrados para pedidos pendentes");
        }
        BigDecimal valor = req.valor().setScale(2, RoundingMode.HALF_UP);
        BigDecimal emAberto = pedido.getValorTotal()
                .subtract(pedido.totalPagamentos(StatusPagamento.APROVADO, StatusPagamento.PENDENTE));
        if (valor.compareTo(emAberto) > 0) {
            throw new RegraNegocioException("Valor maior que o saldo em aberto do pedido (R$ " + emAberto + ")");
        }
        Pagamento p = new Pagamento();
        p.setPedido(pedido);
        p.setValor(valor);
        p.setFormaPagamento(req.formaPagamento());
        p.setStatus(StatusPagamento.PENDENTE);
        pedido.getPagamentos().add(p);
        return PagamentoResponse.from(repository.save(p));
    }

    @Transactional
    public PagamentoResponse alterarStatus(Integer id, StatusPagamento novo) {
        Pagamento p = obter(id);
        Pedido pedido = p.getPedido();
        StatusPagamento atual = p.getStatus();

        switch (novo) {
            case APROVADO -> {
                exigir(atual == StatusPagamento.PENDENTE, "Somente pagamentos pendentes podem ser aprovados");
                exigir(pedido.getStatus() == StatusPedido.PENDENTE, "O pedido nao esta pendente de pagamento");
                p.setStatus(StatusPagamento.APROVADO);
                p.setDataPagamento(LocalDateTime.now());
                p.setCodigoTransacao("TX-" + UUID.randomUUID().toString().substring(0, 13).toUpperCase(Locale.ROOT));
                if (pedido.totalPagamentos(StatusPagamento.APROVADO).compareTo(pedido.getValorTotal()) >= 0) {
                    pedido.setStatus(StatusPedido.PAGO);
                }
            }
            case CANCELADO -> {
                exigir(atual == StatusPagamento.PENDENTE, "Somente pagamentos pendentes podem ser cancelados");
                p.setStatus(StatusPagamento.CANCELADO);
            }
            case ESTORNADO -> {
                exigir(atual == StatusPagamento.APROVADO, "Somente pagamentos aprovados podem ser estornados");
                exigir(pedido.getStatus() == StatusPedido.PAGO || pedido.getStatus() == StatusPedido.PENDENTE,
                        "Nao e possivel estornar pagamento de pedido " + pedido.getStatus());
                p.setStatus(StatusPagamento.ESTORNADO);
                if (pedido.getStatus() == StatusPedido.PAGO) {
                    pedido.setStatus(StatusPedido.PENDENTE);
                }
            }
            default -> throw new RegraNegocioException("Status invalido: " + novo);
        }
        return PagamentoResponse.from(repository.save(p));
    }

    private Pagamento obter(Integer id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pagamento", id));
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new RegraNegocioException(mensagem);
        }
    }
}
