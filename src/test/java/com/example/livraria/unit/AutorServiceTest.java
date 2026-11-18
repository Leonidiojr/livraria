package com.example.livraria.unit;

import com.example.livraria.domain.Autor;
import com.example.livraria.dto.AutorDtos.AutorRequest;
import com.example.livraria.dto.AutorDtos.AutorResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.repository.AutorRepository;
import com.example.livraria.repository.LivroRepository;
import com.example.livraria.service.AutorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutorServiceTest {

    @Mock AutorRepository repository;
    @Mock LivroRepository livroRepository;
    @Mock com.example.livraria.repository.AutorFotoRepository fotoRepository;
    @InjectMocks AutorService service;

    private static Autor autor(int id, String nome) {
        Autor a = new Autor();
        a.setId(id);
        a.setNome(nome);
        return a;
    }

    @Test
    @DisplayName("Criar autor remove espacos e converte campos vazios em nulo")
    void criar() {
        when(repository.save(any(Autor.class))).thenAnswer(inv -> {
            Autor a = inv.getArgument(0);
            a.setId(1);
            return a;
        });

        AutorResponse r = service.criar(new AutorRequest("  Clarice Lispector ", " ", LocalDate.of(1920, 12, 10), ""));

        assertThat(r.id()).isEqualTo(1);
        assertThat(r.nome()).isEqualTo("Clarice Lispector");
        assertThat(r.nacionalidade()).isNull();
        assertThat(r.biografia()).isNull();
        assertThat(r.dataNascimento()).isEqualTo(LocalDate.of(1920, 12, 10));
    }

    @Test
    @DisplayName("Atualizar autor altera os dados do registro existente")
    void atualizar() {
        Autor existente = autor(2, "Nome antigo");
        when(repository.findById(2)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        AutorResponse r = service.atualizar(2, new AutorRequest("Jorge Amado", "Brasileira", null, "Baiano"));

        assertThat(r.nome()).isEqualTo("Jorge Amado");
        assertThat(existente.getNacionalidade()).isEqualTo("Brasileira");
        assertThat(existente.getBiografia()).isEqualTo("Baiano");
    }

    @Test
    @DisplayName("Atualizar/excluir autor inexistente gera RecursoNaoEncontradoException")
    void inexistente() {
        when(repository.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(9, new AutorRequest("X", null, null, null)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.excluir(9))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    @DisplayName("Excluir autor existente chama o repositorio")
    void excluir() {
        Autor a = autor(3, "Autor");
        when(repository.findById(3)).thenReturn(Optional.of(a));

        service.excluir(3);

        verify(repository).delete(a);
    }

    @Test
    @DisplayName("Listar com filtro usa busca por nome")
    void listar() {
        when(repository.findByNomeContainingIgnoreCaseOrderByNomeAsc("ama")).thenReturn(List.of(autor(1, "Jorge Amado")));

        assertThat(service.listar(" ama ")).extracting(AutorResponse::nome).containsExactly("Jorge Amado");
        verify(repository, never()).findAllByOrderByNomeAsc();
    }
}
