package com.example.livraria.config;

import com.example.livraria.domain.Perfil;
import com.example.livraria.domain.Usuario;
import com.example.livraria.repository.PerfilRepository;
import com.example.livraria.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cria o administrador inicial quando o banco ainda nao possui nenhum ADMIN. */
@Component
public class AdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminInitializer(UsuarioRepository usuarioRepository, PerfilRepository perfilRepository,
                            PasswordEncoder passwordEncoder,
                            @Value("${app.admin.email}") String email,
                            @Value("${app.admin.password}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.countByPerfilNomeAndAtivoTrue(Perfil.ADMIN) > 0
                || usuarioRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Perfil admin = perfilRepository.findByNome(Perfil.ADMIN)
                .orElseThrow(() -> new IllegalStateException("Perfil ADMIN nao encontrado (verifique as migracoes)"));
        Usuario u = new Usuario();
        u.setNome("Administrador");
        u.setEmail(email.toLowerCase());
        u.setSenhaHash(passwordEncoder.encode(senha));
        u.setPerfil(admin);
        u.setAtivo(true);
        usuarioRepository.save(u);
        log.info("Usuario administrador criado: {}", email);
    }
}
