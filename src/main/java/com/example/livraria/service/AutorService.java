package com.example.livraria.service;

import com.example.livraria.domain.Autor;
import com.example.livraria.dto.AutorDtos.AutorRequest;
import com.example.livraria.dto.AutorDtos.AutorResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.repository.AutorFotoRepository;
import com.example.livraria.repository.AutorRepository;
import com.example.livraria.repository.LivroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AutorService {

    private final AutorRepository autorRepository;
    private final LivroRepository livroRepository;

    private final AutorFotoRepository fotoRepository;

    public AutorService(AutorRepository autorRepository, LivroRepository livroRepository,
                        AutorFotoRepository fotoRepository) {
        this.fotoRepository = fotoRepository;
        this.autorRepository = autorRepository;
        this.livroRepository = livroRepository;
    }

    @Transactional(readOnly = true)
    public List<AutorResponse> listar(String busca) {
        List<Autor> autores = (busca == null || busca.isBlank())
                ? autorRepository.findAllByOrderByNomeAsc()
                : autorRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(busca.trim());
        return autores.stream().map(AutorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AutorResponse buscar(Integer id) {
        return AutorResponse.from(obter(id));
    }

    @Transactional
    public AutorResponse criar(AutorRequest req) {
        Autor autor = new Autor();
        aplicar(autor, req);
        return AutorResponse.from(autorRepository.save(autor));
    }

    @Transactional
    public AutorResponse atualizar(Integer id, AutorRequest req) {
        Autor autor = obter(id);
        aplicar(autor, req);
        return AutorResponse.from(autorRepository.save(autor));
    }

    @Transactional
    public void excluir(Integer id) {
        Autor autor = obter(id);
        // desfaz o vinculo com os livros antes de remover o autor (o livro continua existindo)
        livroRepository.findByAutoresId(id).forEach(l -> l.getAutores().remove(autor));
        fotoRepository.findById(id).ifPresent(fotoRepository::delete); // a foto sai junto com o autor
        autorRepository.delete(autor);
    }

    private Autor obter(Integer id) {
        return autorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Autor", id));
    }

    private static void aplicar(Autor autor, AutorRequest req) {
        autor.setNome(req.nome().trim());
        autor.setNacionalidade(vazioParaNulo(req.nacionalidade()));
        autor.setDataNascimento(req.dataNascimento());
        autor.setBiografia(vazioParaNulo(req.biografia()));
    }

    static String vazioParaNulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
