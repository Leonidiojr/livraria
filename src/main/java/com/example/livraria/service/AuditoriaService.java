package com.example.livraria.service;

import com.example.livraria.dto.AuditoriaResponse;
import com.example.livraria.repository.AuditoriaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class AuditoriaService {

    private static final int LIMITE_PADRAO = 200;

    private final AuditoriaRepository repository;

    @PersistenceContext
    private EntityManager em;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AuditoriaResponse> listar(String tabela, String operacao, String termo, Integer limite) {
        int max = (limite == null || limite < 1 || limite > 1000) ? LIMITE_PADRAO : limite;
        return repository.filtrar(vazio(tabela), vazio(operacao).toUpperCase(Locale.ROOT),
                        vazio(termo), PageRequest.of(0, max))
                .stream().map(AuditoriaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<String> tabelas() {
        return repository.tabelas();
    }

    /**
     * Define o usuario gravado pela auditoria apenas na transacao atual. Usado quando a operacao
     * acontece antes de existir um usuario autenticado (login e auto-cadastro).
     */
    @Transactional
    public void identificarNaTransacao(String usuario) {
        em.createNativeQuery("select set_config('app.usuario', :u, true)")
                .setParameter("u", usuario)
                .getSingleResult();
    }

    private static String vazio(String s) {
        return (s == null || s.isBlank()) ? "" : s.trim(); // "" = sem filtro (evita parametro nulo sem tipo no PostgreSQL)
    }
}
