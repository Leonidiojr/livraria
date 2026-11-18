package com.example.livraria.controller;

import com.example.livraria.domain.StatusPagamento;
import com.example.livraria.dto.PagamentoDtos.PagamentoRequest;
import com.example.livraria.dto.PagamentoDtos.PagamentoResponse;
import com.example.livraria.dto.PagamentoDtos.StatusPagamentoRequest;
import com.example.livraria.service.PagamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagamentos")
public class PagamentoController {

    private final PagamentoService service;

    public PagamentoController(PagamentoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PagamentoResponse> listar(@RequestParam(name = "status", required = false) StatusPagamento status,
                                          @RequestParam(name = "q", required = false) String busca) {
        return service.listar(status, busca);
    }

    @GetMapping("/{id}")
    public PagamentoResponse buscar(@PathVariable Integer id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PagamentoResponse registrar(@Valid @RequestBody PagamentoRequest req) {
        return service.registrar(req);
    }

    @PutMapping("/{id}/status")
    public PagamentoResponse alterarStatus(@PathVariable Integer id, @Valid @RequestBody StatusPagamentoRequest req) {
        return service.alterarStatus(id, req.status());
    }
}
