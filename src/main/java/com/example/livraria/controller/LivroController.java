package com.example.livraria.controller;

import com.example.livraria.dto.LivroDtos.LivroRequest;
import com.example.livraria.dto.LivroDtos.LivroResponse;
import com.example.livraria.service.LivroService;
import com.example.livraria.service.ImagemService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/livros")
public class LivroController {

    private final LivroService livroService;
    private final ImagemService imagemService;

    public LivroController(LivroService livroService, ImagemService imagemService) {
        this.livroService = livroService;
        this.imagemService = imagemService;
    }

    @GetMapping
    public List<LivroResponse> listar(@RequestParam(name = "q", required = false) String busca) {
        return livroService.listar(busca);
    }

    @GetMapping("/{id}")
    public LivroResponse buscar(@PathVariable Integer id) {
        return livroService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LivroResponse criar(@Valid @RequestBody LivroRequest req) {
        return livroService.criar(req);
    }

    @PutMapping("/{id}")
    public LivroResponse atualizar(@PathVariable Integer id, @Valid @RequestBody LivroRequest req) {
        return livroService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        livroService.excluir(id);
    }

    // ------------------------------------------------------------------ capa

    /** Publico (sem token) para poder ser usado diretamente em tags img; URL versionada permite cache longo. */
    @GetMapping("/{id}/capa")
    public ResponseEntity<byte[]> capa(@PathVariable Integer id) {
        ImagemService.Imagem img = imagemService.capa(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.tipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(img.conteudo());
    }

    @PutMapping(path = "/{id}/capa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public LivroResponse salvarCapa(@PathVariable Integer id, @RequestParam("arquivo") MultipartFile arquivo) {
        imagemService.salvarCapa(id, arquivo);
        return livroService.buscar(id);
    }

    @DeleteMapping("/{id}/capa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerCapa(@PathVariable Integer id) {
        imagemService.removerCapa(id);
    }
}
