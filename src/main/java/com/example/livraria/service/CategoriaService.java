package com.example.livraria.service;

import com.example.livraria.domain.Categoria;
import com.example.livraria.dto.CatalogoDtos.CategoriaRequest;
import com.example.livraria.dto.CatalogoDtos.CategoriaResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.CategoriaRepository;
import com.example.livraria.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.livraria.service.AutorService.vazioParaNulo;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;
    private final LivroRepository livroRepository;

    public CategoriaService(CategoriaRepository repository, LivroRepository livroRepository) {
        this.repository = repository;
        this.livroRepository = livroRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(String busca) {
        List<Categoria> lista = (busca == null || busca.isBlank())
                ? repository.findAllByOrderByNomeAsc()
                : repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(busca.trim());
        return lista.stream().map(CategoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Integer id) {
        return CategoriaResponse.from(obter(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest req) {
        String nome = req.nome().trim();
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Ja existe a categoria " + nome);
        }
        Categoria c = new Categoria();
        aplicar(c, req);
        return CategoriaResponse.from(repository.save(c));
    }

    @Transactional
    public CategoriaResponse atualizar(Integer id, CategoriaRequest req) {
        Categoria c = obter(id);
        if (repository.existsByNomeIgnoreCaseAndIdNot(req.nome().trim(), id)) {
            throw new RegraNegocioException("Ja existe a categoria " + req.nome().trim());
        }
        aplicar(c, req);
        return CategoriaResponse.from(repository.save(c));
    }

    @Transactional
    public void excluir(Integer id) {
        Categoria c = obter(id);
        if (livroRepository.existsByCategoriaId(id)) {
            throw new RegraNegocioException("Categoria possui livros cadastrados; desative-a em vez de excluir");
        }
        repository.delete(c);
    }

    private Categoria obter(Integer id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }

    private static void aplicar(Categoria c, CategoriaRequest req) {
        c.setNome(req.nome().trim());
        c.setDescricao(vazioParaNulo(req.descricao()));
        c.setAtivo(req.ativo() == null || req.ativo());
    }
}
