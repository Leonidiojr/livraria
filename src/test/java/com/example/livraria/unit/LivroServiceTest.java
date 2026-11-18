package com.example.livraria.unit;

import com.example.livraria.domain.Autor;
import com.example.livraria.domain.Categoria;
import com.example.livraria.domain.Editora;
import com.example.livraria.domain.Livro;
import com.example.livraria.dto.LivroDtos.LivroRequest;
import com.example.livraria.dto.LivroDtos.LivroResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.AutorRepository;
import com.example.livraria.repository.CategoriaRepository;
import com.example.livraria.repository.EditoraRepository;
import com.example.livraria.repository.LivroRepository;
import com.example.livraria.service.LivroService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LivroServiceTest {

    @Mock LivroRepository livroRepository;
    @Mock CategoriaRepository categoriaRepository;
    @Mock EditoraRepository editoraRepository;
    @Mock AutorRepository autorRepository;
    @Mock com.example.livraria.repository.LivroCapaRepository capaRepository;
    @InjectMocks LivroService service;

    private Categoria categoria;
    private Editora editora;
    private Autor autor;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(1);
        categoria.setNome("Romance");
        editora = new Editora();
        editora.setId(2);
        editora.setNome("Rocco");
        autor = new Autor();
        autor.setId(3);
        autor.setNome("Machado de Assis");
    }

    private static LivroRequest req(String isbn, List<Integer> autores) {
        return new LivroRequest(isbn, "  Dom Casmurro ", "", null, 1899, 256, "Portugues",
                new BigDecimal("39.9"), 1, 2, autores, null);
    }

    @Test
    @DisplayName("Criar livro: normaliza campos, resolve relacionamentos e salva")
    void criaLivro() {
        when(livroRepository.existsByIsbn("978-85-359-0277-9")).thenReturn(false);
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(editoraRepository.findById(2)).thenReturn(Optional.of(editora));
        when(autorRepository.findById(3)).thenReturn(Optional.of(autor));
        when(livroRepository.save(any(Livro.class))).thenAnswer(inv -> {
            Livro l = inv.getArgument(0);
            l.setId(10);
            return l;
        });

        LivroResponse r = service.criar(req("978-85-359-0277-9", List.of(3, 3)));

        ArgumentCaptor<Livro> captor = ArgumentCaptor.forClass(Livro.class);
        verify(livroRepository).save(captor.capture());
        Livro salvo = captor.getValue();
        assertThat(salvo.getTitulo()).isEqualTo("Dom Casmurro");
        assertThat(salvo.getSubtitulo()).isNull();
        assertThat(salvo.getPreco()).isEqualByComparingTo("39.90");
        assertThat(salvo.getPreco().scale()).isEqualTo(2);
        assertThat(salvo.getAtivo()).isTrue();
        assertThat(salvo.getAutores()).hasSize(1);

        assertThat(r.id()).isEqualTo(10);
        assertThat(r.categoriaNome()).isEqualTo("Romance");
        assertThat(r.editoraNome()).isEqualTo("Rocco");
        assertThat(r.autores()).extracting("nome").containsExactly("Machado de Assis");
    }

    @Test
    @DisplayName("Criar livro com ISBN duplicado gera RegraNegocioException")
    void isbnDuplicado() {
        when(livroRepository.existsByIsbn("123-4567890")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(req("123-4567890", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ISBN");
        verify(livroRepository, never()).save(any());
    }

    @Test
    @DisplayName("Criar livro com categoria inexistente gera RecursoNaoEncontradoException")
    void categoriaInexistente() {
        when(livroRepository.existsByIsbn(any())).thenReturn(false);
        when(categoriaRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(req("1234567890", null)))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Categoria");
    }

    @Test
    @DisplayName("Criar livro com autor inexistente gera RecursoNaoEncontradoException")
    void autorInexistente() {
        when(livroRepository.existsByIsbn(any())).thenReturn(false);
        when(categoriaRepository.findById(1)).thenReturn(Optional.of(categoria));
        when(editoraRepository.findById(2)).thenReturn(Optional.of(editora));
        when(autorRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(req("1234567890", List.of(99))))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Autor");
    }

    @Test
    @DisplayName("Atualizar livro com ISBN de outro livro gera conflito")
    void atualizarIsbnDeOutro() {
        Livro existente = new Livro();
        existente.setId(5);
        when(livroRepository.findById(5)).thenReturn(Optional.of(existente));
        when(livroRepository.existsByIsbnAndIdNot("1234567890", 5)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(5, req("1234567890", null)))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Buscar/excluir livro inexistente gera RecursoNaoEncontradoException")
    void livroInexistente() {
        when(livroRepository.findById(404)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(404)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.excluir(404)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(livroRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Listar com termo usa a busca; sem termo lista tudo")
    void listar() {
        when(livroRepository.findAllByOrderByTituloAsc()).thenReturn(List.of());
        when(livroRepository.buscar("machado")).thenReturn(List.of());

        service.listar(null);
        service.listar("  machado ");

        verify(livroRepository).findAllByOrderByTituloAsc();
        verify(livroRepository).buscar("machado");
    }
}
