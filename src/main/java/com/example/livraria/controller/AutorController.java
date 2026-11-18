package com.example.livraria.controller;

import com.example.livraria.dto.AutorDtos.AutorRequest;
import com.example.livraria.dto.AutorDtos.AutorResponse;
import com.example.livraria.service.AutorService;
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
@RequestMapping("/api/autores")
public class AutorController {

    private final AutorService autorService;
    private final ImagemService imagemService;

    public AutorController(AutorService autorService, ImagemService imagemService) {
        this.autorService = autorService;
        this.imagemService = imagemService;
    }

    @GetMapping
    public List<AutorResponse> listar(@RequestParam(name = "q", required = false) String busca) {
        return autorService.listar(busca);
    }

    @GetMapping("/{id}")
    public AutorResponse buscar(@PathVariable Integer id) {
        return autorService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AutorResponse criar(@Valid @RequestBody AutorRequest req) {
        return autorService.criar(req);
    }

    @PutMapping("/{id}")
    public AutorResponse atualizar(@PathVariable Integer id, @Valid @RequestBody AutorRequest req) {
        return autorService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Integer id) {
        autorService.excluir(id);
    }

    // ------------------------------------------------------------------ foto

    /** Publico (sem token) para poder ser usado diretamente em tags img; URL versionada permite cache longo. */
    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Integer id) {
        ImagemService.Imagem img = imagemService.foto(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(img.tipo()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(img.conteudo());
    }

    @PutMapping(path = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AutorResponse salvarFoto(@PathVariable Integer id, @RequestParam("arquivo") MultipartFile arquivo) {
        imagemService.salvarFoto(id, arquivo);
        return autorService.buscar(id);
    }

    @DeleteMapping("/{id}/foto")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removerFoto(@PathVariable Integer id) {
        imagemService.removerFoto(id);
    }
}
