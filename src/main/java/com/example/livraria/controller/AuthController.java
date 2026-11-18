package com.example.livraria.controller;

import com.example.livraria.dto.AuthDtos.LoginRequest;
import com.example.livraria.dto.AuthDtos.LoginResponse;
import com.example.livraria.dto.AuthDtos.RegistroRequest;
import com.example.livraria.dto.UsuarioDtos.UsuarioResponse;
import com.example.livraria.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest req) {
        return authService.registrar(req);
    }

    @GetMapping("/me")
    public UsuarioResponse me(Authentication auth) {
        return authService.usuarioAtual(auth.getName());
    }
}
