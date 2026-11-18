package com.example.livraria.dto;

import com.example.livraria.domain.Autor;
import com.example.livraria.service.ImagemService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** DTOs do CRUD de autores. */
public final class AutorDtos {

    private AutorDtos() {
    }

    public record AutorRequest(
            @NotBlank(message = "Informe o nome") @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres") String nome,
            @Size(max = 100, message = "Nacionalidade deve ter no maximo 100 caracteres") String nacionalidade,
            @Past(message = "A data de nascimento deve estar no passado") LocalDate dataNascimento,
            @Size(max = 5000, message = "Biografia deve ter no maximo 5000 caracteres") String biografia) {
    }

    public record AutorResponse(Integer id, String nome, String nacionalidade, LocalDate dataNascimento, String biografia,
                                String fotoUrl) {

        public static AutorResponse from(Autor a) {
            return new AutorResponse(a.getId(), a.getNome(), a.getNacionalidade(), a.getDataNascimento(), a.getBiografia(),
                    ImagemService.url("/api/autores/" + a.getId() + "/foto", a.getFotoAtualizadaEm()));
        }
    }
}
