package com.example.livraria.service;

import com.example.livraria.domain.Estoque;
import com.example.livraria.domain.Livro;
import com.example.livraria.dto.EstoqueDtos.EstoqueRequest;
import com.example.livraria.dto.EstoqueDtos.EstoqueResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.EstoqueRepository;
import com.example.livraria.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstoqueService {

    private final EstoqueRepository repository;
    private final LivroRepository livroRepository;

    public EstoqueService(EstoqueRepository repository, LivroRepository livroRepository) {
        this.repository = repository;
        this.livroRepository = livroRepository;
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponse> listar(String busca, boolean somenteAbaixoDoMinimo) {
        List<Estoque> lista = (busca == null || busca.isBlank())
                ? repository.listarComLivro()
                : repository.buscar(busca.trim());
        return lista.stream()
                .filter(e -> !somenteAbaixoDoMinimo || e.abaixoDoMinimo())
                .map(EstoqueResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EstoqueResponse buscar(Integer id) {
        return EstoqueResponse.from(obter(id));
    }

    /**
     * Entrada de estoque: soma a quantidade recebida ao saldo do livro.
     * Todo livro ja nasce com registro de estoque (zerado); se por algum motivo nao tiver, o registro e criado.
     * O estoque minimo so muda quando informado.
     */
    @Transactional
    public EstoqueResponse criar(EstoqueRequest req) {
        if (req.livroId() == null) {
            throw new RegraNegocioException("Informe o livro");
        }
        Livro livro = livroRepository.findById(req.livroId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro", req.livroId()));
        var existente = repository.travarPorLivro(livro.getId());
        if (existente.isPresent()) {
            if (req.quantidade() < 1) {
                throw new RegraNegocioException("Informe a quantidade de entrada (maior que zero)");
            }
            Estoque e = existente.get();
            e.setQuantidade(e.getQuantidade() + req.quantidade());
            if (req.estoqueMinimo() != null) {
                e.setEstoqueMinimo(req.estoqueMinimo());
            }
            return EstoqueResponse.from(repository.save(e));
        }
        Estoque e = new Estoque(livro, req.quantidade(),
                req.estoqueMinimo() == null ? Estoque.MINIMO_PADRAO : req.estoqueMinimo());
        livro.setEstoque(e);
        return EstoqueResponse.from(repository.save(e));
    }

    /**
     * Edicao do estoque: altera somente o estoque minimo. A quantidade nunca e definida diretamente;
     * ela so muda por entradas de estoque, vendas e cancelamentos.
     */
    @Transactional
    public EstoqueResponse atualizar(Integer id, Integer estoqueMinimo) {
        Estoque e = obter(id);
        e.setEstoqueMinimo(estoqueMinimo);
        return EstoqueResponse.from(repository.save(e));
    }

    @Transactional
    public void excluir(Integer id) {
        Estoque e = obter(id);
        e.getLivro().setEstoque(null);
        repository.delete(e);
    }

    // ------------------------------------------------------------- uso pelas vendas

    /** Retira unidades do estoque do livro (com trava de linha). */
    @Transactional
    public void baixar(Livro livro, int quantidade) {
        Estoque e = repository.travarPorLivro(livro.getId())
                .orElseThrow(() -> new RegraNegocioException("O livro '" + livro.getTitulo() + "' nao possui estoque"));
        if (e.getQuantidade() < quantidade) {
            throw new RegraNegocioException("Estoque insuficiente para '" + livro.getTitulo()
                    + "' (disponivel: " + e.getQuantidade() + ", solicitado: " + quantidade + ")");
        }
        e.setQuantidade(e.getQuantidade() - quantidade);
    }

    /** Devolve unidades ao estoque (cancelamento ou alteracao de pedido). */
    @Transactional
    public void devolver(Livro livro, int quantidade) {
        repository.travarPorLivro(livro.getId())
                .ifPresent(e -> e.setQuantidade(e.getQuantidade() + quantidade));
    }

    private Estoque obter(Integer id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Estoque", id));
    }
}
