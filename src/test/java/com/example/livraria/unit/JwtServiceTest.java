package com.example.livraria.unit;

import com.example.livraria.domain.Perfil;
import com.example.livraria.domain.Usuario;
import com.example.livraria.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SEGREDO = "bGl2cmFyaWEtZGV2LXNlZ3JlZG8tand0LTMyLWJ5dGVzLW1pbmltby0xMjM0NTY=";
    private static final String OUTRO_SEGREDO = "b3V0cm8tc2VncmVkby1kaWZlcmVudGUtcGFyYS10ZXN0ZXMtMzItYnl0ZXM=";

    private static Usuario usuario() {
        Usuario u = new Usuario();
        u.setId(7);
        u.setNome("Maria");
        u.setEmail("maria@teste.com");
        u.setPerfil(new Perfil(1, Perfil.ADMIN));
        return u;
    }

    private static JwtService comRelogio(String segredo, Duration validade, Instant agora) throws Exception {
        Constructor<JwtService> c = JwtService.class.getDeclaredConstructor(String.class, Duration.class, Clock.class);
        c.setAccessible(true);
        return c.newInstance(segredo, validade, Clock.fixed(agora, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Token gerado contem o e-mail do usuario como subject")
    void geraEValidaToken() {
        JwtService jwt = new JwtService(SEGREDO, 60);
        JwtService.TokenGerado gerado = jwt.gerarToken(usuario());

        assertThat(gerado.token()).isNotBlank();
        assertThat(gerado.expiraEm()).isAfter(Instant.now());
        assertThat(jwt.extrairEmail(gerado.token())).contains("maria@teste.com");
    }

    @Test
    @DisplayName("Token assinado com outra chave e rejeitado")
    void rejeitaAssinaturaInvalida() {
        String token = new JwtService(OUTRO_SEGREDO, 60).gerarToken(usuario()).token();
        assertThat(new JwtService(SEGREDO, 60).extrairEmail(token)).isEmpty();
    }

    @Test
    @DisplayName("Token expirado e rejeitado")
    void rejeitaTokenExpirado() throws Exception {
        Instant emissao = Instant.parse("2025-01-01T10:00:00Z");
        String token = comRelogio(SEGREDO, Duration.ofMinutes(30), emissao).gerarToken(usuario()).token();

        JwtService depois = comRelogio(SEGREDO, Duration.ofMinutes(30), emissao.plus(Duration.ofHours(1)));
        assertThat(depois.extrairEmail(token)).isEmpty();
    }

    @Test
    @DisplayName("Texto qualquer nao e aceito como token")
    void rejeitaLixo() {
        JwtService jwt = new JwtService(SEGREDO, 60);
        assertThat(jwt.extrairEmail("abc.def.ghi")).isEmpty();
        assertThat(jwt.extrairEmail("")).isEmpty();
    }
}
