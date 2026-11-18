package com.example.livraria.service;

import com.example.livraria.domain.Perfil;
import com.example.livraria.domain.Usuario;
import com.example.livraria.dto.UsuarioDtos.UsuarioRequest;
import com.example.livraria.dto.UsuarioDtos.UsuarioResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.PerfilRepository;
import com.example.livraria.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteService clienteService;

    public UsuarioService(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
                          PasswordEncoder passwordEncoder, ClienteService clienteService) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.clienteService = clienteService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar(String busca) {
        List<Usuario> usuarios = (busca == null || busca.isBlank())
                ? usuarioRepository.findAllByOrderByNomeAsc()
                : usuarioRepository.findByNomeContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNomeAsc(busca.trim(), busca.trim());
        return usuarios.stream().map(UsuarioResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Integer id) {
        return UsuarioResponse.from(obter(id));
    }

    @Transactional
    public UsuarioResponse criar(UsuarioRequest req) {
        if (req.senha() == null || req.senha().isBlank()) {
            throw new RegraNegocioException("A senha e obrigatoria no cadastro");
        }
        String email = normalizarEmail(req.email());
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Ja existe um usuario com o e-mail " + email);
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setPerfil(obterPerfil(req.perfil()));
        u.setAtivo(req.ativo() == null || req.ativo());
        Usuario salvo = usuarioRepository.save(u);
        if (Perfil.CLIENTE.equals(salvo.getPerfil().getNome())) {
            clienteService.garantirClienteDoUsuario(salvo); // permite ao cliente fazer os proprios pedidos
        }
        return UsuarioResponse.from(salvo);
    }

    @Transactional
    public UsuarioResponse atualizar(Integer id, UsuarioRequest req, String emailLogado) {
        Usuario u = obter(id);
        String email = normalizarEmail(req.email());
        if (usuarioRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new RegraNegocioException("Ja existe um usuario com o e-mail " + email);
        }
        Perfil novoPerfil = obterPerfil(req.perfil());
        boolean novoAtivo = req.ativo() == null ? Boolean.TRUE.equals(u.getAtivo()) : req.ativo();

        boolean deixaDeSerAdminAtivo = u.isAdmin() && Boolean.TRUE.equals(u.getAtivo())
                && (!Perfil.ADMIN.equals(novoPerfil.getNome()) || !novoAtivo);
        if (deixaDeSerAdminAtivo) {
            if (u.getEmail().equalsIgnoreCase(emailLogado)) {
                throw new RegraNegocioException("Voce nao pode remover o seu proprio acesso de administrador");
            }
            garantirOutroAdminAtivo();
        }

        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setPerfil(novoPerfil);
        u.setAtivo(novoAtivo);
        if (req.senha() != null && !req.senha().isBlank()) {
            u.setSenhaHash(passwordEncoder.encode(req.senha()));
        }
        Usuario salvo = usuarioRepository.save(u);
        if (Perfil.CLIENTE.equals(novoPerfil.getNome())) {
            clienteService.garantirClienteDoUsuario(salvo);
        }
        return UsuarioResponse.from(salvo);
    }

    @Transactional
    public void excluir(Integer id, String emailLogado) {
        Usuario u = obter(id);
        if (u.getEmail().equalsIgnoreCase(emailLogado)) {
            throw new RegraNegocioException("Voce nao pode excluir o proprio usuario");
        }
        if (u.isAdmin() && Boolean.TRUE.equals(u.getAtivo())) {
            garantirOutroAdminAtivo();
        }
        clienteService.desvincularUsuario(u.getId()); // o cadastro de cliente (e seus pedidos) permanece
        usuarioRepository.delete(u);
        usuarioRepository.flush();
    }

    @Transactional(readOnly = true)
    public List<String> perfis() {
        return perfilRepository.findAllByOrderByNomeAsc().stream().map(Perfil::getNome).toList();
    }

    private void garantirOutroAdminAtivo() {
        if (usuarioRepository.countByPerfilNomeAndAtivoTrue(Perfil.ADMIN) <= 1) {
            throw new RegraNegocioException("O sistema precisa de pelo menos um administrador ativo");
        }
    }

    private Usuario obter(Integer id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario", id));
    }

    private Perfil obterPerfil(String nome) {
        return perfilRepository.findByNome(nome.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new RegraNegocioException("Perfil invalido: " + nome));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
