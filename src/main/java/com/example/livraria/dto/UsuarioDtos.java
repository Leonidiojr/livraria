package com.example.livraria.dto;

import com.example.livraria.domain.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** DTOs do CRUD de usuarios. */
public final class UsuarioDtos {

    private UsuarioDtos() {
    }

    /**
     * Requisicao de criacao/atualizacao. A senha e obrigatoria na criacao e opcional na
     * atualizacao (quando vazia, a senha atual e mantida).
     */
    public record UsuarioRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail invalido") @Size(max = 150) String email,
            @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha,
            @NotBlank(message = "Informe o perfil") String perfil,
            Boolean ativo) {
    }

    public record UsuarioResponse(Integer id, String nome, String email, Boolean ativo, String perfil,
                                  LocalDateTime dataCadastro, LocalDateTime ultimoAcesso) {

        public static UsuarioResponse from(Usuario u) {
            return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getAtivo(),
                    u.getPerfil() != null ? u.getPerfil().getNome() : null,
                    u.getDataCadastro(), u.getUltimoAcesso());
        }
    }
}
