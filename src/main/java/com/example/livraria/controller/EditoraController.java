package com.example.livraria.controller;

import com.example.livraria.dto.CatalogoDtos.EditoraRequest;
import com.example.livraria.dto.CatalogoDtos.EditoraResponse;
import com.example.livraria.service.EditoraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/editoras")
public class EditoraController {

    private final EditoraService service;

    public EditoraController(EditoraService service) {
        this.service = service;
    }

    @GetMapping
    public List<EditoraResponse> listar(@RequestParam(name = "q", required = false) String busca) {
        return service.listar(busca);
    }

    @GetMapping("/{id}")
    public EditoraResponse buscar(@PathVariable Integer id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EditoraResponse criar(@Valid @RequestBody EditoraRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public EditoraResponse atualizar(@PathVariable Integer id, @Valid @RequestBody EditoraRequest req) {
        return service.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        service.excluir(id);
    }
}
