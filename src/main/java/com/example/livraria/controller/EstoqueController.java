package com.example.livraria.controller;

import com.example.livraria.dto.EstoqueDtos.EstoqueMinimoRequest;
import com.example.livraria.dto.EstoqueDtos.EstoqueRequest;
import com.example.livraria.dto.EstoqueDtos.EstoqueResponse;
import com.example.livraria.service.EstoqueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @GetMapping
    public List<EstoqueResponse> listar(@RequestParam(name = "q", required = false) String busca,
                                        @RequestParam(name = "baixo", defaultValue = "false") boolean somenteBaixo) {
        return service.listar(busca, somenteBaixo);
    }

    @GetMapping("/{id}")
    public EstoqueResponse buscar(@PathVariable Integer id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EstoqueResponse criar(@Valid @RequestBody EstoqueRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public EstoqueResponse atualizar(@PathVariable Integer id, @Valid @RequestBody EstoqueMinimoRequest req) {
        return service.atualizar(id, req.estoqueMinimo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        service.excluir(id);
    }
}
