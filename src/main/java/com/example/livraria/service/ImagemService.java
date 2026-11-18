package com.example.livraria.service;

import com.example.livraria.domain.Autor;
import com.example.livraria.domain.AutorFoto;
import com.example.livraria.domain.Livro;
import com.example.livraria.domain.LivroCapa;
import com.example.livraria.exception.ArquivoInvalidoException;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.repository.AutorFotoRepository;
import com.example.livraria.repository.AutorRepository;
import com.example.livraria.repository.LivroCapaRepository;
import com.example.livraria.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Capas de livros e fotos de autores. O tipo da imagem e identificado pelos primeiros bytes
 * do arquivo (assinatura), e nao pelo nome ou pelo Content-Type informado pelo navegador.
 */
@Service
public class ImagemService {

    public static final int TAMANHO_MAXIMO = 2 * 1024 * 1024; // 2 MB

    public record Imagem(byte[] conteudo, String tipo) {
    }

    private final LivroRepository livroRepository;
    private final AutorRepository autorRepository;
    private final LivroCapaRepository capaRepository;
    private final AutorFotoRepository fotoRepository;

    public ImagemService(LivroRepository livroRepository, AutorRepository autorRepository,
                         LivroCapaRepository capaRepository, AutorFotoRepository fotoRepository) {
        this.livroRepository = livroRepository;
        this.autorRepository = autorRepository;
        this.capaRepository = capaRepository;
        this.fotoRepository = fotoRepository;
    }

    /** Nome original do arquivo, sem caminho e limitado ao tamanho da coluna. */
    static String nomeArquivo(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null || nome.isBlank()) {
            return null;
        }
        nome = nome.replace("\\", "/");
        nome = nome.substring(nome.lastIndexOf('/') + 1).trim();
        return nome.length() > 255 ? nome.substring(nome.length() - 255) : nome;
    }

    // ------------------------------------------------------------------ capa do livro

    @Transactional
    public Livro salvarCapa(Integer livroId, MultipartFile arquivo) {
        Livro livro = livro(livroId);
        Imagem img = validar(arquivo);
        LivroCapa capa = capaRepository.findById(livroId).orElseGet(LivroCapa::new);
        capa.setLivroId(livroId);
        capa.setConteudo(img.conteudo());
        capa.setTipo(img.tipo());
        capa.setNomeArquivo(nomeArquivo(arquivo));
        capa.setTamanhoBytes(img.conteudo().length);
        capa.setDataEnvio(LocalDateTime.now());
        capaRepository.save(capa);
        livro.setCapaAtualizadaEm(LocalDateTime.now());
        return livro;
    }

    @Transactional(readOnly = true)
    public Imagem capa(Integer livroId) {
        return capaRepository.findById(livroId)
                .map(c -> new Imagem(c.getConteudo(), c.getTipo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Capa do livro", livroId));
    }

    @Transactional
    public void removerCapa(Integer livroId) {
        Livro livro = livro(livroId);
        capaRepository.findById(livroId).ifPresent(capaRepository::delete);
        livro.setCapaAtualizadaEm(null);
    }

    // ------------------------------------------------------------------ foto do autor

    @Transactional
    public Autor salvarFoto(Integer autorId, MultipartFile arquivo) {
        Autor autor = autor(autorId);
        Imagem img = validar(arquivo);
        AutorFoto foto = fotoRepository.findById(autorId).orElseGet(AutorFoto::new);
        foto.setAutorId(autorId);
        foto.setConteudo(img.conteudo());
        foto.setTipo(img.tipo());
        foto.setNomeArquivo(nomeArquivo(arquivo));
        foto.setTamanhoBytes(img.conteudo().length);
        foto.setDataEnvio(LocalDateTime.now());
        fotoRepository.save(foto);
        autor.setFotoAtualizadaEm(LocalDateTime.now());
        return autor;
    }

    @Transactional(readOnly = true)
    public Imagem foto(Integer autorId) {
        return fotoRepository.findById(autorId)
                .map(f -> new Imagem(f.getConteudo(), f.getTipo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Foto do autor", autorId));
    }

    @Transactional
    public void removerFoto(Integer autorId) {
        Autor autor = autor(autorId);
        fotoRepository.findById(autorId).ifPresent(fotoRepository::delete);
        autor.setFotoAtualizadaEm(null);
    }

    // ------------------------------------------------------------------ apoio

    /** URL publica da imagem com a versao (data de atualizacao) para invalidar o cache do navegador. */
    public static String url(String caminho, LocalDateTime atualizadaEm) {
        return atualizadaEm == null ? null
                : caminho + "?v=" + atualizadaEm.toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public static Imagem validar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Selecione um arquivo de imagem");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new ArquivoInvalidoException("A imagem deve ter no maximo 2 MB");
        }
        byte[] bytes;
        try {
            bytes = arquivo.getBytes();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Nao foi possivel ler o arquivo enviado");
        }
        String tipo = tipoPelaAssinatura(bytes);
        if (tipo == null) {
            throw new ArquivoInvalidoException("Formato nao suportado: envie uma imagem JPG, PNG ou WEBP");
        }
        return new Imagem(bytes, tipo);
    }

    static String tipoPelaAssinatura(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private Livro livro(Integer id) {
        return livroRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Livro", id));
    }

    private Autor autor(Integer id) {
        return autorRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Autor", id));
    }
}
