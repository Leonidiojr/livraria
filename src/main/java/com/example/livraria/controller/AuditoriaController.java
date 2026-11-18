package com.example.livraria.controller;

import com.example.livraria.dto.AuditoriaResponse;
import com.example.livraria.service.AuditoriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Consulta (somente leitura) dos registros gravados pelos triggers de auditoria. */
@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) {
        this.service = service;
    }

    @GetMapping
    public List<AuditoriaResponse> listar(@RequestParam(name = "tabela", required = false) String tabela,
                                          @RequestParam(name = "operacao", required = false) String operacao,
                                          @RequestParam(name = "q", required = false) String termo,
                                          @RequestParam(name = "limite", required = false) Integer limite) {
        return service.listar(tabela, operacao, termo, limite);
    }

    @GetMapping("/tabelas")
    public List<String> tabelas() {
        return service.tabelas();
    }
}
