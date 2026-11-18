package com.example.livraria.service;

import com.example.livraria.domain.Editora;
import com.example.livraria.dto.CatalogoDtos.EditoraRequest;
import com.example.livraria.dto.CatalogoDtos.EditoraResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.EditoraRepository;
import com.example.livraria.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

import static com.example.livraria.service.AutorService.vazioParaNulo;

@Service
public class EditoraService {

    private final EditoraRepository repository;
    private final LivroRepository livroRepository;

    public EditoraService(EditoraRepository repository, LivroRepository livroRepository) {
        this.repository = repository;
        this.livroRepository = livroRepository;
    }

    @Transactional(readOnly = true)
    public List<EditoraResponse> listar(String busca) {
        List<Editora> lista = (busca == null || busca.isBlank())
                ? repository.findAllByOrderByNomeAsc()
                : repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(busca.trim());
        return lista.stream().map(EditoraResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public EditoraResponse buscar(Integer id) {
        return EditoraResponse.from(obter(id));
    }

    @Transactional
    public EditoraResponse criar(EditoraRequest req) {
        String nome = req.nome().trim();
        String cnpj = vazioParaNulo(req.cnpj());
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new RegraNegocioException("Ja existe a editora " + nome);
        }
        if (cnpj != null && repository.existsByCnpj(cnpj)) {
            throw new RegraNegocioException("Ja existe uma editora com o CNPJ " + cnpj);
        }
        Editora e = new Editora();
        aplicar(e, req);
        return EditoraResponse.from(repository.save(e));
    }

    @Transactional
    public EditoraResponse atualizar(Integer id, EditoraRequest req) {
        Editora e = obter(id);
        String cnpj = vazioParaNulo(req.cnpj());
        if (repository.existsByNomeIgnoreCaseAndIdNot(req.nome().trim(), id)) {
            throw new RegraNegocioException("Ja existe a editora " + req.nome().trim());
        }
        if (cnpj != null && repository.existsByCnpjAndIdNot(cnpj, id)) {
            throw new RegraNegocioException("Ja existe uma editora com o CNPJ " + cnpj);
        }
        aplicar(e, req);
        return EditoraResponse.from(repository.save(e));
    }

    @Transactional
    public void excluir(Integer id) {
        Editora e = obter(id);
        if (livroRepository.existsByEditoraId(id)) {
            throw new RegraNegocioException("Editora possui livros cadastrados e nao pode ser excluida");
        }
        repository.delete(e);
    }

    private Editora obter(Integer id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Editora", id));
    }

    private static void aplicar(Editora e, EditoraRequest req) {
        e.setNome(req.nome().trim());
        e.setCnpj(vazioParaNulo(req.cnpj()));
        e.setTelefone(vazioParaNulo(req.telefone()));
        String email = vazioParaNulo(req.email());
        e.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
        e.setSite(vazioParaNulo(req.site()));
    }
}
