package com.example.livraria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** DTOs de autenticacao. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail invalido") String email,
            @NotBlank(message = "Informe a senha") String senha) {
    }

    public record RegistroRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150) String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail invalido") @Size(max = 150) String email,
            @NotBlank(message = "Informe o CPF")
            @Pattern(regexp = "^$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF deve estar no formato 000.000.000-00") String cpf,
            @NotBlank(message = "Informe a senha") @Size(min = 6, max = 72, message = "A senha deve ter entre 6 e 72 caracteres") String senha) {
    }

    public record LoginResponse(String token, String tipo, Instant expiraEm, UsuarioDtos.UsuarioResponse usuario) {
    }
}
