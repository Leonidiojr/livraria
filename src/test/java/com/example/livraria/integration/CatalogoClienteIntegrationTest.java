package com.example.livraria.integration;

import com.example.livraria.DadosTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** CRUDs de editoras, categorias, clientes e estoque. */
class CatalogoClienteIntegrationTest extends IntegrationTestBase {

    @Test
    @DisplayName("CRUD de editora com validacao e unicidade de CNPJ")
    void editora() throws Exception {
        String admin = tokenAdmin();
        Integer id = id(mvc.perform(json(post("/api/editoras"), admin, """
                        {"nome": "Editora IT", "cnpj": "11.222.333/0001-81", "email": "CONTATO@EDITORA.IT", "site": "https://editora.it"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("contato@editora.it"))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(comToken(get("/api/editoras").param("q", "editora it"), admin))
                .andExpect(jsonPath("$[*].id", hasItem(id)));
        mvc.perform(json(put("/api/editoras/" + id), admin, """
                        {"nome": "Editora IT Alterada", "cnpj": "11.222.333/0001-81"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Editora IT Alterada"))
                .andExpect(jsonPath("$.site").doesNotExist());

        mvc.perform(json(post("/api/editoras"), admin, """
                        {"nome": "Outra IT", "cnpj": "11.222.333/0001-81"}"""))
                .andExpect(status().isConflict());
        mvc.perform(json(post("/api/editoras"), admin, """
                        {"nome": "Formato IT", "cnpj": "123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cnpj").exists());

        mvc.perform(comToken(delete("/api/editoras/" + id), admin)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Editora e categoria com livros nao podem ser excluidas")
    void exclusaoBloqueada() throws Exception {
        String admin = tokenAdmin();
        // categoria 1 e editora 1 possuem livros de exemplo
        mvc.perform(comToken(delete("/api/categorias/1"), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("livros")));
        mvc.perform(comToken(delete("/api/editoras/1"), admin)).andExpect(status().isConflict());
    }

    @Test
    @DisplayName("CRUD de categoria com nome unico")
    void categoria() throws Exception {
        String admin = tokenAdmin();
        Integer id = id(mvc.perform(json(post("/api/categorias"), admin, """
                        {"nome": "Poesia IT", "descricao": "Poemas"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativo").value(true))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(json(post("/api/categorias"), admin, "{\"nome\": \"poesia it\"}")).andExpect(status().isConflict());
        mvc.perform(json(put("/api/categorias/" + id), admin, """
                        {"nome": "Poesia IT", "ativo": false}"""))
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(comToken(delete("/api/categorias/" + id), admin)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/categorias/" + id), admin)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("CRUD de cliente com validacao de CPF, UF e CEP")
    void cliente() throws Exception {
        String vendedor = tokenVendedor();
        Integer id = id(mvc.perform(json(post("/api/clientes"), vendedor, """
                        {"nome": "Maria IT", "email": "Maria@IT.com", "cpf": "111.444.777-35", "estado": "sp",
                         "cep": "01310-100", "cidade": "Sao Paulo"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("SP"))
                .andExpect(jsonPath("$.email").value("maria@it.com"))
                .andReturn().getResponse().getContentAsString());

        mvc.perform(comToken(get("/api/clientes").param("q", "111.444"), vendedor))
                .andExpect(jsonPath("$[*].id", hasItem(id)));

        // CPF com digito verificador errado (409), formato invalido (400), duplicado (409)
        mvc.perform(json(post("/api/clientes"), vendedor, """
                        {"nome": "X", "email": "x.it@teste.com", "cpf": "111.444.777-36"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("CPF invalido")));
        mvc.perform(json(post("/api/clientes"), vendedor, """
                        {"nome": "X", "cpf": "11144477735", "estado": "SPX", "cep": "123"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.cpf").exists())
                .andExpect(jsonPath("$.campos.estado").exists())
                .andExpect(jsonPath("$.campos.cep").exists());
        mvc.perform(json(post("/api/clientes"), vendedor, """
                        {"nome": "Y", "email": "y.it@teste.com", "cpf": "111.444.777-35"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cliente ja possui cadastro ativo com o CPF 111.444.777-35"));
        // e-mail ja usado por outro cliente (sem diferenciar maiusculas)
        mvc.perform(json(post("/api/clientes"), vendedor, """
                        {"nome": "Z", "email": "MARIA@it.com", "cpf": "%s"}""".formatted(DadosTeste.cpf())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cliente ja possui cadastro ativo com o e-mail maria@it.com"));
        // e-mail e CPF sao obrigatorios
        mvc.perform(json(post("/api/clientes"), vendedor, """
                        {"nome": "Sem Dados"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.cpf").exists());

        // alterar o proprio cadastro mantendo CPF/e-mail nao e duplicidade
        mvc.perform(json(put("/api/clientes/" + id), vendedor, """
                        {"nome": "Maria IT Silva", "email": "maria@it.com", "cpf": "111.444.777-35"}"""))
                .andExpect(jsonPath("$.nome").value("Maria IT Silva"))
                .andExpect(jsonPath("$.cidade").doesNotExist());
        mvc.perform(comToken(delete("/api/clientes/" + id), vendedor)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Cliente com pedidos nao pode ser excluido")
    void clienteComPedido() throws Exception {
        String admin = tokenAdmin();
        Integer cliente = id(corpo(json(post("/api/clientes"), admin, """
                {"nome": "Com Pedido IT", "email": "%s", "cpf": "%s"}""".formatted(DadosTeste.email("pedido.it"), DadosTeste.cpf()))));
        Integer livro = livroExemplo(admin, 1);
        mvc.perform(json(post("/api/pedidos"), admin, """
                        {"clienteId": %d, "itens": [{"livroId": %d, "quantidade": 1}]}""".formatted(cliente, livro)))
                .andExpect(status().isCreated());

        mvc.perform(comToken(delete("/api/clientes/" + cliente), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("pedidos")));
    }

    @Test
    @DisplayName("Novo livro nasce com estoque zero; estoque pode ser ajustado e filtrado por 'abaixo do minimo'")
    void estoque() throws Exception {
        String admin = tokenAdmin();
        Integer livro = id(mvc.perform(json(post("/api/livros"), admin, """
                        {"isbn": "978-2222222222", "titulo": "Livro Estoque IT", "preco": 10, "categoriaId": 1, "editoraId": 1}"""))
                .andExpect(jsonPath("$.quantidadeEstoque").value(0))
                .andReturn().getResponse().getContentAsString());
        Integer estoque = estoqueDoLivro(admin, livro);

        mvc.perform(comToken(get("/api/estoque").param("baixo", "true"), admin))
                .andExpect(jsonPath("$[*].id", hasItem(estoque)));

        // a edicao altera so o estoque minimo; a quantidade enviada e ignorada
        mvc.perform(json(put("/api/estoque/" + estoque), admin, "{\"quantidade\": 20, \"estoqueMinimo\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(0))
                .andExpect(jsonPath("$.estoqueMinimo").value(3))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(true));
        mvc.perform(json(put("/api/estoque/" + estoque), admin, "{\"estoqueMinimo\": -1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(put("/api/estoque/" + estoque), admin, "{\"quantidade\": 20}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.estoqueMinimo").exists());
        // entrada de estoque soma ao saldo do mesmo registro
        mvc.perform(json(post("/api/estoque"), admin, "{\"livroId\": %d, \"quantidade\": 5}".formatted(livro)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(estoque))
                .andExpect(jsonPath("$.quantidade").value(5))
                .andExpect(jsonPath("$.estoqueMinimo").value(3))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(false));
        mvc.perform(json(post("/api/estoque"), admin, "{\"livroId\": %d, \"quantidade\": 0}".formatted(livro)))
                .andExpect(status().isConflict());

        // excluir o livro remove o estoque junto
        mvc.perform(comToken(delete("/api/livros/" + livro), admin)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/estoque/" + estoque), admin)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Usuario CLIENTE criado pelo admin ganha cadastro de cliente; ao excluir o usuario o cliente permanece")
    void usuarioClienteVinculado() throws Exception {
        String admin = tokenAdmin();
        Integer usuario = id(corpo(json(post("/api/usuarios"), admin, """
                {"nome": "Leitor IT", "email": "leitor.it@teste.com", "senha": "leitor123", "perfil": "CLIENTE"}""")));

        mvc.perform(comToken(get("/api/clientes").param("q", "leitor.it@teste.com"), admin))
                .andExpect(jsonPath("$[0].usuarioId").value(usuario));

        mvc.perform(comToken(delete("/api/usuarios/" + usuario), admin)).andExpect(status().isNoContent());
        mvc.perform(comToken(get("/api/clientes").param("q", "leitor.it@teste.com"), admin))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].usuarioId").doesNotExist());
    }

    @Test
    @DisplayName("Cliente cadastrado pela loja nao pode se cadastrar de novo pela tela de login (CPF ou e-mail)")
    void duplicidadeEntreLojaEAutoCadastro() throws Exception {
        String admin = tokenAdmin();
        String cpf = DadosTeste.cpf();
        String email = DadosTeste.email("duplicado.it");
        mvc.perform(json(post("/api/clientes"), admin, """
                        {"nome": "Cliente Loja", "email": "%s", "cpf": "%s"}""".formatted(email, cpf)))
                .andExpect(status().isCreated());

        // mesmo CPF, outro e-mail
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": "Cliente Loja", "email": "%s", "cpf": "%s", "senha": "123456"}"""
                        .formatted(DadosTeste.email("outro.it"), cpf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cliente ja possui cadastro ativo com o CPF " + cpf));
        // mesmo e-mail, outro CPF
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": "Cliente Loja", "email": "%s", "cpf": "%s", "senha": "123456"}"""
                        .formatted(email.toUpperCase(), DadosTeste.cpf())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cliente ja possui cadastro ativo com o e-mail " + email));

        // e o contrario: quem se cadastrou pela tela de login nao pode ser cadastrado de novo pela loja
        String cpf2 = DadosTeste.cpf();
        String email2 = DadosTeste.email("auto.it");
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome": "Auto Cadastro", "email": "%s", "cpf": "%s", "senha": "123456"}""".formatted(email2, cpf2)))
                .andExpect(status().isCreated());
        mvc.perform(comToken(get("/api/clientes").param("q", email2), admin))
                .andExpect(jsonPath("$[0].cpf").value(cpf2));
        mvc.perform(json(post("/api/clientes"), admin, """
                        {"nome": "Auto Cadastro", "email": "%s", "cpf": "%s"}""".formatted(DadosTeste.email("x.it"), cpf2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cliente ja possui cadastro ativo com o CPF " + cpf2));
    }

    @Test
    @DisplayName("Usuario CLIENTE criado pelo admin com e-mail de cliente ja cadastrado e vinculado a ele (sem duplicar)")
    void usuarioClienteVinculaCadastroExistente() throws Exception {
        String admin = tokenAdmin();
        String email = DadosTeste.email("vinculo.it");
        Integer cliente = id(corpo(json(post("/api/clientes"), admin, """
                {"nome": "Cliente Vinculo", "email": "%s", "cpf": "%s"}""".formatted(email, DadosTeste.cpf()))));
        Integer usuario = id(corpo(json(post("/api/usuarios"), admin, """
                {"nome": "Cliente Vinculo", "email": "%s", "senha": "vinculo123", "perfil": "CLIENTE"}""".formatted(email))));

        mvc.perform(comToken(get("/api/clientes").param("q", email), admin))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(cliente))
                .andExpect(jsonPath("$[0].usuarioId").value(usuario));
    }
}
