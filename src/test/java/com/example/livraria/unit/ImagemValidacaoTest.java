package com.example.livraria.unit;

import com.example.livraria.Imagens;
import com.example.livraria.exception.ArquivoInvalidoException;
import com.example.livraria.service.ImagemService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagemValidacaoTest {

    private static MockMultipartFile arquivo(String nome, String tipoInformado, byte[] bytes) {
        return new MockMultipartFile("arquivo", nome, tipoInformado, bytes);
    }

    @Test
    @DisplayName("Reconhece PNG, JPEG e WEBP pela assinatura dos bytes")
    void tiposAceitos() {
        assertThat(ImagemService.validar(arquivo("a.png", "image/png", Imagens.png(10, 10))).tipo()).isEqualTo("image/png");
        assertThat(ImagemService.validar(arquivo("a.jpg", "image/jpeg", Imagens.jpeg(10, 10))).tipo()).isEqualTo("image/jpeg");
        byte[] webp = "RIFF0000WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
        assertThat(ImagemService.validar(arquivo("a.webp", "image/webp", webp)).tipo()).isEqualTo("image/webp");
    }

    @Test
    @DisplayName("O tipo vem do conteudo, nao do nome nem do Content-Type informado")
    void ignoraTipoInformado() {
        // PNG verdadeiro enviado com nome e tipo de JPEG
        assertThat(ImagemService.validar(arquivo("foto.jpg", "image/jpeg", Imagens.png(5, 5))).tipo()).isEqualTo("image/png");
        // texto disfarcado de PNG e recusado
        assertThatThrownBy(() -> ImagemService.validar(arquivo("capa.png", "image/png", "<script>".getBytes())))
                .isInstanceOf(ArquivoInvalidoException.class).hasMessageContaining("Formato nao suportado");
    }

    @Test
    @DisplayName("Recusa arquivo vazio, ausente ou maior que 2 MB")
    void tamanho() {
        assertThatThrownBy(() -> ImagemService.validar(null)).isInstanceOf(ArquivoInvalidoException.class);
        assertThatThrownBy(() -> ImagemService.validar(arquivo("a.png", "image/png", new byte[0])))
                .isInstanceOf(ArquivoInvalidoException.class);
        byte[] grande = new byte[ImagemService.TAMANHO_MAXIMO + 1];
        System.arraycopy(Imagens.png(2, 2), 0, grande, 0, 8);
        assertThatThrownBy(() -> ImagemService.validar(arquivo("a.png", "image/png", grande)))
                .isInstanceOf(ArquivoInvalidoException.class).hasMessageContaining("2 MB");
    }

    @Test
    @DisplayName("URL da imagem inclui versao; sem imagem a URL e nula")
    void url() {
        assertThat(ImagemService.url("/api/livros/1/capa", null)).isNull();
        assertThat(ImagemService.url("/api/livros/1/capa", LocalDateTime.of(2026, 1, 1, 0, 0)))
                .isEqualTo("/api/livros/1/capa?v=1767225600000");
    }
}
