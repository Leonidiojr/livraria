package com.example.livraria.unit;

import com.example.livraria.domain.Perfil;
import com.example.livraria.domain.Usuario;
import com.example.livraria.dto.UsuarioDtos.UsuarioRequest;
import com.example.livraria.dto.UsuarioDtos.UsuarioResponse;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.PerfilRepository;
import com.example.livraria.repository.UsuarioRepository;
import com.example.livraria.service.ClienteService;
import com.example.livraria.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock PerfilRepository perfilRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ClienteService clienteService;
    @InjectMocks UsuarioService service;

    private final Perfil admin = new Perfil(1, Perfil.ADMIN);
    private final Perfil cliente = new Perfil(2, Perfil.CLIENTE);

    @BeforeEach
    void setUp() {
        when(perfilRepository.findByNome("ADMIN")).thenReturn(Optional.of(admin));
        when(perfilRepository.findByNome("CLIENTE")).thenReturn(Optional.of(cliente));
        when(passwordEncoder.encode(any())).thenAnswer(inv -> "HASH(" + inv.getArgument(0) + ")");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Usuario usuario(int id, String email, Perfil perfil) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("Usuario " + id);
        u.setEmail(email);
        u.setSenhaHash("HASH(antiga)");
        u.setPerfil(perfil);
        u.setAtivo(true);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    @Test
    @DisplayName("Criar usuario: e-mail normalizado, senha criptografada e perfil aplicado")
    void criar() {
        UsuarioResponse r = service.criar(new UsuarioRequest("Ana", "  ANA@Teste.com ", "segredo1", "cliente", null));

        assertThat(r.email()).isEqualTo("ana@teste.com");
        assertThat(r.perfil()).isEqualTo("CLIENTE");
        assertThat(r.ativo()).isTrue();
        verify(usuarioRepository).save(argThat(u -> u.getSenhaHash().equals("HASH(segredo1)")));
    }

    @Test
    @DisplayName("Criar usuario sem senha e rejeitado")
    void criarSemSenha() {
        assertThatThrownBy(() -> service.criar(new UsuarioRequest("Ana", "ana@teste.com", null, "CLIENTE", true)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("senha");
    }

    @Test
    @DisplayName("Criar usuario com e-mail ja cadastrado e rejeitado")
    void emailDuplicado() {
        when(usuarioRepository.existsByEmailIgnoreCase("ana@teste.com")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new UsuarioRequest("Ana", "ana@teste.com", "123456", "CLIENTE", true)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("e-mail");
    }

    @Test
    @DisplayName("Criar usuario com perfil inexistente e rejeitado")
    void perfilInvalido() {
        when(perfilRepository.findByNome("GERENTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new UsuarioRequest("Ana", "ana@teste.com", "123456", "GERENTE", true)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Perfil");
    }

    @Test
    @DisplayName("Atualizar sem senha mantem a senha atual")
    void atualizarMantemSenha() {
        Usuario u = usuario(5, "c@teste.com", cliente);

        service.atualizar(5, new UsuarioRequest("Novo Nome", "c@teste.com", null, "CLIENTE", true), "admin@teste.com");

        assertThat(u.getSenhaHash()).isEqualTo("HASH(antiga)");
        assertThat(u.getNome()).isEqualTo("Novo Nome");
    }

    @Test
    @DisplayName("Atualizar com senha troca o hash")
    void atualizarTrocaSenha() {
        Usuario u = usuario(5, "c@teste.com", cliente);

        service.atualizar(5, new UsuarioRequest("C", "c@teste.com", "novasenha", "CLIENTE", true), "admin@teste.com");

        assertThat(u.getSenhaHash()).isEqualTo("HASH(novasenha)");
    }

    @Test
    @DisplayName("Administrador nao pode rebaixar o proprio perfil")
    void naoRebaixaASiMesmo() {
        usuario(1, "admin@teste.com", admin);

        assertThatThrownBy(() -> service.atualizar(1,
                new UsuarioRequest("Admin", "admin@teste.com", null, "CLIENTE", true), "admin@teste.com"))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    @DisplayName("Nao e possivel desativar o ultimo administrador ativo")
    void ultimoAdmin() {
        usuario(2, "outro@teste.com", admin);
        when(usuarioRepository.countByPerfilNomeAndAtivoTrue(Perfil.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.atualizar(2,
                new UsuarioRequest("Outro", "outro@teste.com", null, "ADMIN", false), "admin@teste.com"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("administrador");
    }

    @Test
    @DisplayName("Usuario nao pode excluir a si mesmo")
    void naoExcluiASiMesmo() {
        usuario(1, "admin@teste.com", admin);

        assertThatThrownBy(() -> service.excluir(1, "ADMIN@teste.com"))
                .isInstanceOf(RegraNegocioException.class);
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Excluir cliente chama o repositorio")
    void excluirCliente() {
        Usuario u = usuario(8, "c@teste.com", cliente);

        service.excluir(8, "admin@teste.com");

        verify(usuarioRepository).delete(u);
    }
}
