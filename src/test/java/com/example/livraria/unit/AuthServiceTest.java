package com.example.livraria.unit;

import com.example.livraria.service.ClienteService;
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
import com.example.livraria.service.AuditoriaService;
import com.example.livraria.service.AuthService;
import com.example.livraria.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock UsuarioService usuarioService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuditoriaService auditoriaService;
    @Mock ClienteService clienteService;
    @InjectMocks AuthService service;

    private Usuario usuario(boolean ativo) {
        Usuario u = new Usuario();
        u.setId(1);
        u.setNome("Ana");
        u.setEmail("ana@teste.com");
        u.setSenhaHash("hash");
        u.setAtivo(ativo);
        u.setPerfil(new Perfil(2, Perfil.CLIENTE));
        return u;
    }

    @Test
    @DisplayName("Login valido retorna token e registra ultimo acesso")
    void loginValido() {
        Usuario u = usuario(true);
        when(usuarioRepository.findByEmailIgnoreCase("ana@teste.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("123456", "hash")).thenReturn(true);
        when(jwtService.gerarToken(u)).thenReturn(new JwtService.TokenGerado("tok", Instant.now()));

        LoginResponse r = service.login(new LoginRequest(" ana@teste.com ", "123456"));

        assertThat(r.token()).isEqualTo("tok");
        assertThat(r.tipo()).isEqualTo("Bearer");
        assertThat(r.usuario().email()).isEqualTo("ana@teste.com");
        assertThat(u.getUltimoAcesso()).isNotNull();
        verify(usuarioRepository).save(u);
    }

    @Test
    @DisplayName("Senha incorreta gera CredenciaisInvalidasException")
    void senhaErrada() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@teste.com")).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("errada", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@teste.com", "errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(jwtService, never()).gerarToken(any());
    }

    @Test
    @DisplayName("Usuario inexistente gera CredenciaisInvalidasException")
    void usuarioInexistente() {
        when(usuarioRepository.findByEmailIgnoreCase("x@teste.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("x@teste.com", "123456")))
                .isInstanceOf(CredenciaisInvalidasException.class);
    }

    @Test
    @DisplayName("Usuario inativo nao consegue autenticar")
    void usuarioInativo() {
        when(usuarioRepository.findByEmailIgnoreCase("ana@teste.com")).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> service.login(new LoginRequest("ana@teste.com", "123456")))
                .isInstanceOf(CredenciaisInvalidasException.class);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    @DisplayName("Auto-cadastro sempre cria usuario com perfil CLIENTE")
    void registroSempreCliente() {
        when(usuarioService.criar(any())).thenReturn(
                new UsuarioResponse(7, "Ana", "ana@teste.com", true, Perfil.CLIENTE, null, null));
        service.registrar(new RegistroRequest("Ana", "ana@teste.com", "529.982.247-25", "123456"));

        verify(clienteService).verificarDuplicidade("ana@teste.com", "529.982.247-25", null);
        verify(clienteService).definirCpfDoUsuario(7, "529.982.247-25");

        ArgumentCaptor<UsuarioRequest> captor = ArgumentCaptor.forClass(UsuarioRequest.class);
        verify(usuarioService).criar(captor.capture());
        assertThat(captor.getValue().perfil()).isEqualTo(Perfil.CLIENTE);
        assertThat(captor.getValue().ativo()).isTrue();
    }
}
