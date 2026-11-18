package com.example.livraria.controller;

import com.example.livraria.domain.StatusPedido;
import com.example.livraria.dto.PedidoDtos.PagarPedidoRequest;
import com.example.livraria.dto.PedidoDtos.PedidoRequest;
import com.example.livraria.dto.PedidoDtos.PedidoResponse;
import com.example.livraria.dto.PedidoDtos.StatusRequest;
import com.example.livraria.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PedidoResponse> listar(@RequestParam(name = "status", required = false) StatusPedido status,
                                       @RequestParam(name = "q", required = false) String busca) {
        return service.listar(status, busca);
    }

    @GetMapping("/{id}")
    public PedidoResponse buscar(@PathVariable Integer id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse criar(@Valid @RequestBody PedidoRequest req) {
        return service.criar(req);
    }

    @PutMapping("/{id}")
    public PedidoResponse atualizar(@PathVariable Integer id, @Valid @RequestBody PedidoRequest req) {
        return service.atualizar(id, req);
    }

    @PutMapping("/{id}/status")
    public PedidoResponse alterarStatus(@PathVariable Integer id, @Valid @RequestBody StatusRequest req) {
        return service.alterarStatus(id, req.status());
    }

    @PostMapping("/{id}/pagamento")
    public PedidoResponse pagar(@PathVariable Integer id, @Valid @RequestBody PagarPedidoRequest req) {
        return service.pagar(id, req.formaPagamento());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        service.excluir(id);
    }
}
