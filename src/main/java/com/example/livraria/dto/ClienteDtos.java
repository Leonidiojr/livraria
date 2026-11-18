package com.example.livraria.dto;

import com.example.livraria.domain.Cliente;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public final class ClienteDtos {

    private ClienteDtos() {
    }

    public record ClienteRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres") String nome,
            @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail invalido") @Size(max = 150) String email,
            @NotBlank(message = "Informe o CPF")
            @Pattern(regexp = "^$|^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$", message = "CPF deve estar no formato 000.000.000-00") String cpf,
            @Size(max = 20, message = "Telefone deve ter no maximo 20 caracteres") String telefone,
            @Size(max = 255) String endereco,
            @Size(max = 20) String numero,
            @Size(max = 100) String complemento,
            @Size(max = 100) String bairro,
            @Size(max = 100) String cidade,
            @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "UF deve ter 2 letras") String estado,
            @Pattern(regexp = "^$|^\\d{5}-\\d{3}$", message = "CEP deve estar no formato 00000-000") String cep,
            @Past(message = "A data de nascimento deve estar no passado") LocalDate dataNascimento) {
    }

    public record ClienteResponse(Integer id, String nome, String email, String cpf, String telefone,
                                  String endereco, String numero, String complemento, String bairro,
                                  String cidade, String estado, String cep, LocalDate dataNascimento,
                                  Integer usuarioId) {
        public static ClienteResponse from(Cliente c) {
            return new ClienteResponse(c.getId(), c.getNome(), c.getEmail(), c.getCpf(), c.getTelefone(),
                    c.getEndereco(), c.getNumero(), c.getComplemento(), c.getBairro(), c.getCidade(),
                    c.getEstado(), c.getCep(), c.getDataNascimento(),
                    c.getUsuario() != null ? c.getUsuario().getId() : null);
        }
    }
}
