package com.example.livraria.service;

import com.example.livraria.domain.Cliente;
import com.example.livraria.domain.Usuario;
import com.example.livraria.dto.ClienteDtos.ClienteRequest;
import com.example.livraria.dto.ClienteDtos.ClienteResponse;
import com.example.livraria.exception.RecursoNaoEncontradoException;
import com.example.livraria.exception.RegraNegocioException;
import com.example.livraria.repository.ClienteRepository;
import com.example.livraria.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

import static com.example.livraria.service.AutorService.vazioParaNulo;

@Service
public class ClienteService {

    private final ClienteRepository repository;
    private final PedidoRepository pedidoRepository;

    public ClienteService(ClienteRepository repository, PedidoRepository pedidoRepository) {
        this.repository = repository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(String busca) {
        List<Cliente> lista = (busca == null || busca.isBlank())
                ? repository.findAllByOrderByNomeAsc()
                : repository.buscar(busca.trim());
        return lista.stream().map(ClienteResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Integer id) {
        return ClienteResponse.from(obter(id));
    }

    @Transactional
    public ClienteResponse criar(ClienteRequest req) {
        String cpf = validarCpf(req.cpf());
        verificarDuplicidade(req.email(), cpf, null);
        Cliente c = new Cliente();
        aplicar(c, req, cpf);
        return ClienteResponse.from(repository.save(c));
    }

    @Transactional
    public ClienteResponse atualizar(Integer id, ClienteRequest req) {
        Cliente c = obter(id);
        String cpf = validarCpf(req.cpf());
        verificarDuplicidade(req.email(), cpf, id);
        aplicar(c, req, cpf);
        return ClienteResponse.from(repository.save(c));
    }

    @Transactional
    public void excluir(Integer id) {
        Cliente c = obter(id);
        if (pedidoRepository.existsByClienteId(id)) {
            throw new RegraNegocioException("Cliente possui pedidos e nao pode ser excluido");
        }
        repository.delete(c);
    }

    /**
     * Impede cadastro em duplicidade: o mesmo CPF ou e-mail nao pode pertencer a dois clientes,
     * venha o cadastro da equipe (aba Clientes) ou do auto-cadastro da tela de login.
     *
     * @param idIgnorar cliente sendo alterado (null em inclusoes)
     */
    @Transactional(readOnly = true)
    public void verificarDuplicidade(String email, String cpf, Integer idIgnorar) {
        String cpfValido = validarCpf(cpf);
        if (cpfValido != null && (idIgnorar == null
                ? repository.existsByCpf(cpfValido) : repository.existsByCpfAndIdNot(cpfValido, idIgnorar))) {
            throw new RegraNegocioException("Cliente ja possui cadastro ativo com o CPF " + cpfValido);
        }
        String emailNormalizado = vazioParaNulo(email);
        if (emailNormalizado != null) {
            emailNormalizado = emailNormalizado.toLowerCase(Locale.ROOT);
            if (idIgnorar == null
                    ? repository.existsByEmailIgnoreCase(emailNormalizado)
                    : repository.existsByEmailIgnoreCaseAndIdNot(emailNormalizado, idIgnorar)) {
                throw new RegraNegocioException("Cliente ja possui cadastro ativo com o e-mail " + emailNormalizado);
            }
        }
    }

    /** Auto-cadastro: grava o CPF informado no cadastro de cliente criado para o usuario. */
    @Transactional
    public void definirCpfDoUsuario(Integer usuarioId, String cpf) {
        String cpfValido = validarCpf(cpf);
        repository.findByUsuarioId(usuarioId).ifPresent(c -> c.setCpf(cpfValido));
    }

    /**
     * Cria o cadastro de cliente vinculado a um usuario com perfil CLIENTE (se ainda nao existir).
     * Se a equipe ja cadastrou um cliente com o mesmo e-mail (sem login), o login e vinculado a ele
     * em vez de criar um segundo cadastro.
     */
    @Transactional
    public void garantirClienteDoUsuario(Usuario usuario) {
        if (usuario.getId() != null && repository.findByUsuarioId(usuario.getId()).isPresent()) {
            return;
        }
        var existente = repository.findFirstByEmailIgnoreCaseAndUsuarioIsNull(usuario.getEmail());
        if (existente.isPresent()) {
            existente.get().setUsuario(usuario);
            return;
        }
        Cliente c = new Cliente();
        c.setNome(usuario.getNome());
        c.setEmail(usuario.getEmail());
        c.setUsuario(usuario);
        repository.save(c);
    }

    /** Desfaz o vinculo do cliente com o usuario (antes de excluir o usuario). */
    @Transactional
    public void desvincularUsuario(Integer usuarioId) {
        repository.findByUsuarioId(usuarioId).ifPresent(c -> c.setUsuario(null));
    }

    private Cliente obter(Integer id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Cliente", id));
    }

    private static String validarCpf(String cpf) {
        String valor = vazioParaNulo(cpf);
        if (valor != null && !Cpf.valido(valor)) {
            throw new RegraNegocioException("CPF invalido: " + valor);
        }
        return valor;
    }

    private static void aplicar(Cliente c, ClienteRequest req, String cpf) {
        c.setNome(req.nome().trim());
        String email = vazioParaNulo(req.email());
        c.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
        c.setCpf(cpf);
        c.setTelefone(vazioParaNulo(req.telefone()));
        c.setEndereco(vazioParaNulo(req.endereco()));
        c.setNumero(vazioParaNulo(req.numero()));
        c.setComplemento(vazioParaNulo(req.complemento()));
        c.setBairro(vazioParaNulo(req.bairro()));
        c.setCidade(vazioParaNulo(req.cidade()));
        String uf = vazioParaNulo(req.estado());
        c.setEstado(uf == null ? null : uf.toUpperCase(Locale.ROOT));
        c.setCep(vazioParaNulo(req.cep()));
        c.setDataNascimento(req.dataNascimento());
    }
}
