package com.example.livraria.controller;

import com.example.livraria.dto.UsuarioDtos.UsuarioRequest;
import com.example.livraria.dto.UsuarioDtos.UsuarioResponse;
import com.example.livraria.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/api/usuarios")
    public List<UsuarioResponse> listar(@RequestParam(name = "q", required = false) String busca) {
        return usuarioService.listar(busca);
    }

    @GetMapping("/api/usuarios/{id}")
    public UsuarioResponse buscar(@PathVariable Integer id) {
        return usuarioService.buscar(id);
    }

    @PostMapping("/api/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse criar(@Valid @RequestBody UsuarioRequest req) {
        return usuarioService.criar(req);
    }

    @PutMapping("/api/usuarios/{id}")
    public UsuarioResponse atualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioRequest req, Authentication auth) {
        return usuarioService.atualizar(id, req, auth.getName());
    }

    @DeleteMapping("/api/usuarios/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id, Authentication auth) {
        usuarioService.excluir(id, auth.getName());
    }

    @GetMapping("/api/perfis")
    public List<String> perfis() {
        return usuarioService.perfis();
    }
}
