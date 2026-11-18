package com.example.livraria.dto;

import com.example.livraria.domain.Categoria;
import com.example.livraria.domain.Editora;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** DTOs dos CRUDs de categorias e editoras. */
public final class CatalogoDtos {

    private CatalogoDtos() {
    }

    public record CategoriaRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 100, message = "Nome deve ter no maximo 100 caracteres") String nome,
            @Size(max = 255, message = "Descricao deve ter no maximo 255 caracteres") String descricao,
            Boolean ativo) {
    }

    public record CategoriaResponse(Integer id, String nome, String descricao, Boolean ativo, LocalDateTime dataCadastro) {
        public static CategoriaResponse from(Categoria c) {
            return new CategoriaResponse(c.getId(), c.getNome(), c.getDescricao(), c.getAtivo(), c.getDataCadastro());
        }
    }

    public record EditoraRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres") String nome,
            @Pattern(regexp = "^$|^\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}$", message = "CNPJ deve estar no formato 00.000.000/0000-00")
            String cnpj,
            @Size(max = 20, message = "Telefone deve ter no maximo 20 caracteres") String telefone,
            @Email(message = "E-mail invalido") @Size(max = 150) String email,
            @Size(max = 200, message = "Site deve ter no maximo 200 caracteres") String site) {
    }

    public record EditoraResponse(Integer id, String nome, String cnpj, String telefone, String email, String site) {
        public static EditoraResponse from(Editora e) {
            return new EditoraResponse(e.getId(), e.getNome(), e.getCnpj(), e.getTelefone(), e.getEmail(), e.getSite());
        }
    }
}
