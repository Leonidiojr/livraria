package com.example.livraria.service;

import com.example.livraria.domain.*;
import com.example.livraria.dto.PagamentoDtos.PagamentoRequest;
import com.example.livraria.dto.PagamentoDtos.PagamentoResponse;
import com.example.livraria.dto.PedidoDtos.ItemRequest;
import com.example.livraria.dto.PedidoDtos.PedidoRequest;
import com.example.livraria.dto.PedidoDtos.PedidoResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.ClienteRepository;
import com.example.livraria.repository.LivroRepository;
import com.example.livraria.repository.PedidoRepository;
import com.example.livraria.security.UsuarioLogado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Regras de venda:
 * <ul>
 *   <li>o preco de cada item e o preco atual do livro; o total e calculado no servidor;</li>
 *   <li>criar o pedido baixa o estoque; cancelar ou alterar devolve;</li>
 *   <li>itens so podem ser alterados enquanto o pedido esta PENDENTE;</li>
 *   <li>usuarios CLIENTE so enxergam e criam os proprios pedidos e so podem cancela-los enquanto pendentes.</li>
 * </ul>
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final LivroRepository livroRepository;
    private final EstoqueService estoqueService;
    private final PagamentoService pagamentoService;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                         LivroRepository livroRepository, EstoqueService estoqueService,
                         PagamentoService pagamentoService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.livroRepository = livroRepository;
        this.estoqueService = estoqueService;
        this.pagamentoService = pagamentoService;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(StatusPedido status, String busca) {
        Integer clienteId = ehCliente() ? clienteDoUsuarioLogado().getId() : null;
        String termo = (busca == null || busca.isBlank()) ? "" : busca.trim(); // "" = sem filtro
        return pedidoRepository.filtrar(status, clienteId, termo).stream().map(PedidoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscar(Integer id) {
        return PedidoResponse.from(obterVisivel(id));
    }

    @Transactional
    public PedidoResponse criar(PedidoRequest req) {
        Cliente cliente = ehCliente() ? clienteDoUsuarioLogado() : clienteInformado(req.clienteId());
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setObservacoes(AutorService.vazioParaNulo(req.observacoes()));
        pedido.setStatus(StatusPedido.PENDENTE);
        aplicarItens(pedido, req.itens());
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse atualizar(Integer id, PedidoRequest req) {
        Pedido pedido = obter(id);
        exigirStatus(pedido, StatusPedido.PENDENTE, "Somente pedidos pendentes podem ter os itens alterados");
        if (req.clienteId() != null) {
            pedido.setCliente(clienteInformado(req.clienteId()));
        }
        pedido.setObservacoes(AutorService.vazioParaNulo(req.observacoes()));

        devolverEstoque(pedido);
        pedido.getItens().clear();
        pedidoRepository.flush(); // remove os itens antigos antes de inserir os novos (chave unica pedido+livro)
        aplicarItens(pedido, req.itens());
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public PedidoResponse alterarStatus(Integer id, StatusPedido novo) {
        Pedido pedido = obterVisivel(id);
        StatusPedido atual = pedido.getStatus();

        if (ehCliente() && !(novo == StatusPedido.CANCELADO && atual == StatusPedido.PENDENTE)) {
            throw new RegraNegocioException("Clientes so podem cancelar pedidos pendentes");
        }
        if (novo == StatusPedido.PAGO) {
            throw new RegraNegocioException("O pedido passa a PAGO automaticamente quando os pagamentos sao aprovados");
        }
        if (!atual.podeIrManualmentePara(novo)) {
            throw new RegraNegocioException("Nao e possivel alterar o pedido de " + atual + " para " + novo);
        }
        if (novo == StatusPedido.CANCELADO) {
            cancelar(pedido);
        } else {
            pedido.setStatus(novo);
        }
        return PedidoResponse.from(pedidoRepository.save(pedido));
    }

    @Transactional
    public void excluir(Integer id) {
        Pedido pedido = obter(id);
        if (pedido.getStatus() != StatusPedido.PENDENTE && pedido.getStatus() != StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Somente pedidos pendentes ou cancelados podem ser excluidos");
        }
        if (!pedido.getPagamentos().isEmpty()) {
            throw new RegraNegocioException("O pedido possui pagamentos registrados; cancele-o em vez de excluir");
        }
        if (pedido.getStatus() == StatusPedido.PENDENTE) {
            devolverEstoque(pedido);
        }
        pedidoRepository.delete(pedido);
    }

    // ------------------------------------------------------------------ auxiliares

    private void cancelar(Pedido pedido) {
        devolverEstoque(pedido);
        for (Pagamento p : pedido.getPagamentos()) {
            if (p.getStatus() == StatusPagamento.PENDENTE) {
                p.setStatus(StatusPagamento.CANCELADO);
            } else if (p.getStatus() == StatusPagamento.APROVADO) {
                p.setStatus(StatusPagamento.ESTORNADO);
            }
        }
        pedido.setStatus(StatusPedido.CANCELADO);
    }

    private void aplicarItens(Pedido pedido, List<ItemRequest> itens) {
        // agrupa itens repetidos do mesmo livro
        Map<Integer, Integer> quantidades = new LinkedHashMap<>();
        for (ItemRequest i : itens) {
            quantidades.merge(i.livroId(), i.quantidade(), Integer::sum);
        }
        quantidades.forEach((livroId, qtd) -> {
            Livro livro = livroRepository.findById(livroId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Livro", livroId));
            if (!Boolean.TRUE.equals(livro.getAtivo())) {
                throw new RegraNegocioException("O livro '" + livro.getTitulo() + "' nao esta disponivel para venda");
            }
            estoqueService.baixar(livro, qtd);
            pedido.adicionarItem(new ItemPedido(livro, qtd));
        });
        pedido.recalcularTotal();
    }

    private void devolverEstoque(Pedido pedido) {
        pedido.getItens().forEach(i -> estoqueService.devolver(i.getLivro(), i.getQuantidade()));
    }

    private static void exigirStatus(Pedido p, StatusPedido status, String mensagem) {
        if (p.getStatus() != status) {
            throw new RegraNegocioException(mensagem);
        }
    }

    /**
     * Pagamento pelo proprio comprador (checkout): quita o saldo em aberto do pedido com a forma escolhida
     * e aprova o pagamento na hora. CLIENTE so paga os proprios pedidos.
     */
    @Transactional
    public PedidoResponse pagar(Integer id, FormaPagamento forma) {
        Pedido pedido = obterVisivel(id);
        if (pedido.getStatus() != StatusPedido.PENDENTE) {
            throw new RegraNegocioException("Somente pedidos pendentes podem ser pagos");
        }
        BigDecimal emAberto = pedido.getValorTotal()
                .subtract(pedido.totalPagamentos(StatusPagamento.APROVADO, StatusPagamento.PENDENTE));
        if (emAberto.signum() <= 0) {
            throw new RegraNegocioException("Este pedido nao possui saldo em aberto");
        }
        PagamentoResponse pagamento = pagamentoService.registrar(new PagamentoRequest(pedido.getId(), forma, emAberto));
        pagamentoService.alterarStatus(pagamento.id(), StatusPagamento.APROVADO);
        return PedidoResponse.from(pedido);
    }

    private Pedido obter(Integer id) {
        return pedidoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    /** Para usuarios CLIENTE, pedidos de outros clientes se comportam como inexistentes. */
    private Pedido obterVisivel(Integer id) {
        Pedido p = obter(id);
        if (ehCliente() && !p.getCliente().getId().equals(clienteDoUsuarioLogado().getId())) {
            throw new RecursoNaoEncontradoException("Pedido", id);
        }
        return p;
    }

    private Cliente clienteInformado(Integer clienteId) {
        if (clienteId == null) {
            throw new RegraNegocioException("Informe o cliente do pedido");
        }
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", clienteId));
    }

    private Cliente clienteDoUsuarioLogado() {
        String email = UsuarioLogado.email().orElseThrow(() -> new RegraNegocioException("Usuario nao autenticado"));
        return clienteRepository.findByUsuarioEmailIgnoreCase(email)
                .orElseThrow(() -> new RegraNegocioException("Seu usuario nao possui cadastro de cliente"));
    }

    private static boolean ehCliente() {
        return UsuarioLogado.temPerfil(Perfil.CLIENTE);
    }
}
