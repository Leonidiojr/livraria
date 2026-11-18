package com.example.livraria.unit;

import com.example.livraria.domain.Estoque;
import com.example.livraria.domain.Livro;
import com.example.livraria.dto.EstoqueDtos.EstoqueRequest;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.EstoqueRepository;
import com.example.livraria.repository.LivroRepository;
import com.example.livraria.service.EstoqueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.example.livraria.unit.Fixtures.livro;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock EstoqueRepository repository;
    @Mock LivroRepository livroRepository;
    @InjectMocks EstoqueService service;

    private final Livro livro = livro(1, "Sapiens", "69.90");

    @Test
    @DisplayName("Baixar estoque reduz a quantidade")
    void baixar() {
        Estoque e = new Estoque(livro, 10, 5);
        when(repository.travarPorLivro(1)).thenReturn(Optional.of(e));

        service.baixar(livro, 4);

        assertThat(e.getQuantidade()).isEqualTo(6);
        assertThat(e.abaixoDoMinimo()).isFalse();
    }

    @Test
    @DisplayName("Baixar mais do que o disponivel gera erro com a quantidade disponivel")
    void insuficiente() {
        Estoque e = new Estoque(livro, 2, 5);
        when(repository.travarPorLivro(1)).thenReturn(Optional.of(e));

        assertThatThrownBy(() -> service.baixar(livro, 3))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Estoque insuficiente").hasMessageContaining("disponivel: 2");
        assertThat(e.getQuantidade()).isEqualTo(2);
    }

    @Test
    @DisplayName("Livro sem registro de estoque nao pode ser vendido")
    void semEstoque() {
        when(repository.travarPorLivro(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.baixar(livro, 1)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Devolver estoque soma a quantidade")
    void devolver() {
        Estoque e = new Estoque(livro, 1, 5);
        when(repository.travarPorLivro(1)).thenReturn(Optional.of(e));

        service.devolver(livro, 3);

        assertThat(e.getQuantidade()).isEqualTo(4);
        assertThat(e.abaixoDoMinimo()).isTrue();
    }

    @Test
    @DisplayName("Entrada em livro que ja tem estoque soma a quantidade, sem criar segundo registro")
    void entradaSomaAoExistente() {
        Estoque e = new Estoque(livro, 3, 5);
        when(livroRepository.findById(1)).thenReturn(Optional.of(livro));
        when(repository.travarPorLivro(1)).thenReturn(Optional.of(e));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        service.criar(new EstoqueRequest(1, 10, null));

        assertThat(e.getQuantidade()).isEqualTo(13);
        assertThat(e.getEstoqueMinimo()).isEqualTo(5); // minimo mantido quando nao informado
        verify(repository).save(e);
    }

    @Test
    @DisplayName("Entrada com quantidade zero e recusada")
    void entradaZero() {
        when(livroRepository.findById(1)).thenReturn(Optional.of(livro));
        when(repository.travarPorLivro(1)).thenReturn(Optional.of(new Estoque(livro, 3, 5)));

        assertThatThrownBy(() -> service.criar(new EstoqueRequest(1, 0, 2))).isInstanceOf(RegraNegocioException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Filtro 'abaixo do minimo' retorna apenas os livros em falta")
    void filtroAbaixoDoMinimo() {
        when(repository.listarComLivro()).thenReturn(List.of(
                new Estoque(livro, 1, 5), new Estoque(livro(2, "Olga", "10"), 50, 5)));

        assertThat(service.listar(null, true)).hasSize(1);
        assertThat(service.listar(null, false)).hasSize(2);
    }
}
