package com.example.livraria.dto;

import com.example.livraria.domain.Autor;
import com.example.livraria.domain.Livro;
import com.example.livraria.service.ImagemService;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/** DTOs do CRUD de livros. */
public final class LivroDtos {

    private LivroDtos() {
    }

    public record LivroRequest(
            @NotBlank(message = "Informe o ISBN")
            @Pattern(regexp = "^[0-9Xx-]{10,17}$", message = "ISBN invalido (use 10 ou 13 digitos, com ou sem hifens)")
            String isbn,
            @NotBlank(message = "Informe o titulo") @Size(max = 250, message = "Titulo deve ter no maximo 250 caracteres") String titulo,
            @Size(max = 250, message = "Subtitulo deve ter no maximo 250 caracteres") String subtitulo,
            @Size(max = 5000, message = "Descricao deve ter no maximo 5000 caracteres") String descricao,
            @Min(value = 1000, message = "Ano de publicacao invalido") @Max(value = 2100, message = "Ano de publicacao invalido") Integer anoPublicacao,
            @Positive(message = "Numero de paginas deve ser positivo") Integer numeroPaginas,
            @Size(max = 50, message = "Idioma deve ter no maximo 50 caracteres") String idioma,
            @NotNull(message = "Informe o preco")
            @DecimalMin(value = "0.01", message = "O preco deve ser maior que zero")
            @Digits(integer = 8, fraction = 2, message = "Preco invalido")
            BigDecimal preco,
            @NotNull(message = "Informe a categoria") Integer categoriaId,
            @NotNull(message = "Informe a editora") Integer editoraId,
            List<Integer> autoresIds,
            Boolean ativo) {
    }

    public record AutorResumo(Integer id, String nome) {
    }

    public record LivroResponse(Integer id, String isbn, String titulo, String subtitulo, String descricao,
                                Integer anoPublicacao, Integer numeroPaginas, String idioma, BigDecimal preco,
                                Integer categoriaId, String categoriaNome, Integer editoraId, String editoraNome,
                                List<AutorResumo> autores, Boolean ativo, LocalDateTime dataCadastro,
                                Integer quantidadeEstoque, String capaUrl) {

        public static LivroResponse from(Livro l) {
            List<AutorResumo> autores = l.getAutores().stream()
                    .sorted(Comparator.comparing(Autor::getNome))
                    .map(a -> new AutorResumo(a.getId(), a.getNome()))
                    .toList();
            return new LivroResponse(l.getId(), l.getIsbn(), l.getTitulo(), l.getSubtitulo(), l.getDescricao(),
                    l.getAnoPublicacao(), l.getNumeroPaginas(), l.getIdioma(), l.getPreco(),
                    l.getCategoria().getId(), l.getCategoria().getNome(),
                    l.getEditora().getId(), l.getEditora().getNome(),
                    autores, l.getAtivo(), l.getDataCadastro(), l.quantidadeEmEstoque(),
                    ImagemService.url("/api/livros/" + l.getId() + "/capa", l.getCapaAtualizadaEm()));
        }
    }

    /** Item simples para listas de selecao (categorias, editoras, perfis). */
    public record OpcaoResponse(Integer id, String nome) {
    }
}
