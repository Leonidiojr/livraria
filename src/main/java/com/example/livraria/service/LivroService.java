package com.example.livraria.service;

import com.example.livraria.domain.Autor;
import com.example.livraria.domain.Estoque;
import com.example.livraria.domain.Livro;
import com.example.livraria.dto.LivroDtos.LivroRequest;
import com.example.livraria.dto.LivroDtos.LivroResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.AutorRepository;
import com.example.livraria.repository.CategoriaRepository;
import com.example.livraria.repository.EditoraRepository;
import com.example.livraria.repository.LivroCapaRepository;
import com.example.livraria.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.example.livraria.service.AutorService.vazioParaNulo;

@Service
public class LivroService {

    private final LivroRepository livroRepository;
    private final CategoriaRepository categoriaRepository;
    private final EditoraRepository editoraRepository;
    private final AutorRepository autorRepository;

    private final LivroCapaRepository capaRepository;

    public LivroService(LivroRepository livroRepository, CategoriaRepository categoriaRepository, LivroCapaRepository capaRepository,
                        EditoraRepository editoraRepository, AutorRepository autorRepository) {
        this.livroRepository = livroRepository;
        this.categoriaRepository = categoriaRepository;
        this.editoraRepository = editoraRepository;
        this.autorRepository = autorRepository;
        this.capaRepository = capaRepository;
    }

    @Transactional(readOnly = true)
    public List<LivroResponse> listar(String busca) {
        List<Livro> livros = (busca == null || busca.isBlank())
                ? livroRepository.findAllByOrderByTituloAsc()
                : livroRepository.buscar(busca.trim());
        return livros.stream().map(LivroResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LivroResponse buscar(Integer id) {
        return LivroResponse.from(obter(id));
    }

    @Transactional
    public LivroResponse criar(LivroRequest req) {
        String isbn = normalizarIsbn(req.isbn());
        if (livroRepository.existsByIsbn(isbn)) {
            throw new RegraNegocioException("Ja existe um livro com o ISBN " + isbn);
        }
        Livro livro = new Livro();
        aplicar(livro, req, isbn);
        livro.setEstoque(new Estoque(livro, 0, Estoque.MINIMO_PADRAO)); // estoque nasce zerado
        return LivroResponse.from(livroRepository.save(livro));
    }

    @Transactional
    public LivroResponse atualizar(Integer id, LivroRequest req) {
        Livro livro = obter(id);
        String isbn = normalizarIsbn(req.isbn());
        if (livroRepository.existsByIsbnAndIdNot(isbn, id)) {
            throw new RegraNegocioException("Ja existe um livro com o ISBN " + isbn);
        }
        aplicar(livro, req, isbn);
        return LivroResponse.from(livroRepository.save(livro));
    }

    @Transactional
    public void excluir(Integer id) {
        Livro livro = obter(id);
        capaRepository.findById(id).ifPresent(capaRepository::delete); // a capa sai junto com o livro
        livroRepository.delete(livro);
        livroRepository.flush();
    }

    private Livro obter(Integer id) {
        return livroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Livro", id));
    }

    private void aplicar(Livro livro, LivroRequest req, String isbn) {
        livro.setIsbn(isbn);
        livro.setTitulo(req.titulo().trim());
        livro.setSubtitulo(vazioParaNulo(req.subtitulo()));
        livro.setDescricao(vazioParaNulo(req.descricao()));
        livro.setAnoPublicacao(req.anoPublicacao());
        livro.setNumeroPaginas(req.numeroPaginas());
        livro.setIdioma(vazioParaNulo(req.idioma()));
        livro.setPreco(req.preco().setScale(2, RoundingMode.HALF_UP));
        livro.setAtivo(req.ativo() == null || req.ativo());
        livro.setCategoria(categoriaRepository.findById(req.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", req.categoriaId())));
        livro.setEditora(editoraRepository.findById(req.editoraId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Editora", req.editoraId())));
        livro.setAutores(resolverAutores(req.autoresIds()));
    }

    private Set<Autor> resolverAutores(List<Integer> ids) {
        Set<Autor> autores = new LinkedHashSet<>();
        if (ids == null) {
            return autores;
        }
        for (Integer idAutor : new LinkedHashSet<>(ids)) {
            autores.add(autorRepository.findById(idAutor)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Autor", idAutor)));
        }
        return autores;
    }

    static String normalizarIsbn(String isbn) {
        return isbn.trim().toUpperCase();
    }
}
