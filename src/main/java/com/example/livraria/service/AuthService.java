package com.example.livraria.service;

import com.example.livraria.domain.Perfil;
import com.example.livraria.domain.Usuario;
import com.example.livraria.dto.AuthDtos.LoginRequest;
import com.example.livraria.dto.AuthDtos.LoginResponse;
import com.example.livraria.dto.AuthDtos.RegistroRequest;
import com.example.livraria.dto.UsuarioDtos.UsuarioRequest;
import com.example.livraria.dto.UsuarioDtos.UsuarioResponse;
import com.example.livraria.exception.CredenciaisInvalidasException;
import com.example.livraria.repository.UsuarioRepository;
import com.example.livraria.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditoriaService auditoriaService;
    private final ClienteService clienteService;

    public AuthService(UsuarioRepository usuarioRepository, UsuarioService usuarioService,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuditoriaService auditoriaService,
                       ClienteService clienteService) {
        this.clienteService = clienteService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public LoginResponse login(LoginRequest req) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .filter(u -> passwordEncoder.matches(req.senha(), u.getSenhaHash()))
                .orElseThrow(CredenciaisInvalidasException::new);

        auditoriaService.identificarNaTransacao(usuario.getEmail());
        usuario.setUltimoAcesso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        JwtService.TokenGerado token = jwtService.gerarToken(usuario);
        return new LoginResponse(token.token(), "Bearer", token.expiraEm(), UsuarioResponse.from(usuario));
    }

    /** Auto-cadastro publico: sempre cria um usuario com perfil CLIENTE. */
    @Transactional
    public UsuarioResponse registrar(RegistroRequest req) {
        auditoriaService.identificarNaTransacao("registro:" + req.email().trim().toLowerCase());
        // o mesmo cliente nao pode ter dois cadastros (ex.: ja cadastrado pela loja ou por outro login)
        clienteService.verificarDuplicidade(req.email(), req.cpf(), null);
        UsuarioResponse criado = usuarioService.criar(
                new UsuarioRequest(req.nome(), req.email(), req.senha(), Perfil.CLIENTE, true));
        clienteService.definirCpfDoUsuario(criado.id(), req.cpf());
        return criado;
    }

    @Transactional(readOnly = true)
    public UsuarioResponse usuarioAtual(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(UsuarioResponse::from)
                .orElseThrow(CredenciaisInvalidasException::new);
    }
}
