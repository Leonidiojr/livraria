-- Dados de exemplo: 50 livros de grande sucesso de vendas no Brasil, 10 por categoria.
--
-- Observacoes:
--  * ISBN: os valores 999-00-0000-NNN sao FICTICIOS (codigos internos de exemplo), nao os ISBNs reais das edicoes.
--  * Preco: preco de venda de referencia desta livraria (ilustrativo).
--  * Ano: ano da publicacao original da obra. Editora: editora de uma edicao brasileira da obra.
--  * Data de nascimento do autor so foi preenchida quando conhecida com seguranca.
SET search_path TO livraria;

-- Os contatos (CNPJ, telefone, e-mail) das 3 editoras da V2 eram valores de exemplo, nao dados reais: removidos.
UPDATE editora SET cnpj = NULL, telefone = NULL, email = NULL
WHERE nome IN ('Companhia das Letras', 'Novatec', 'Rocco');

-- ---------------------------------------------------------------- editoras
INSERT INTO editora (nome) VALUES
    ('Galera Record'), ('Record'), ('Todavia'), ('Bertrand Brasil'), ('Paralela'),
    ('HarperCollins Brasil'), ('WMF Martins Fontes'), ('Planeta Minotauro'), ('Suma'), ('Intrinseca'),
    ('Arqueiro'), ('Verus'), ('Alta Books'), ('Bookman'), ('GEN LTC'),
    ('L&PM'), ('Globo Livros'), ('Global Editora'), ('V&R Editoras'), ('Melhoramentos'),
    ('Casa Lygia Bojunga'), ('Salamandra'), ('Pequena Zahar'), ('José Olympio')
ON CONFLICT (nome) DO NOTHING;

-- ---------------------------------------------------------------- autores
INSERT INTO autor (nome, nacionalidade, data_nascimento)
SELECT v.nome, v.nacionalidade, v.nascimento::date
FROM (VALUES
    -- Romance
    ('Colleen Hoover', 'Norte-americana', '1979-12-11'),
    ('Itamar Vieira Junior', 'Brasileira', NULL),
    ('Matt Haig', 'Britânica', '1975-07-03'),
    ('Taylor Jenkins Reid', 'Norte-americana', '1983-12-20'),
    ('Carla Madeira', 'Brasileira', NULL),
    ('Paulo Coelho', 'Brasileira', '1947-08-24'),
    ('Machado de Assis', 'Brasileira', '1839-06-21'),
    ('Clarice Lispector', 'Brasileira', '1920-12-10'),
    ('Jeferson Tenório', 'Brasileira', NULL),
    -- Fantasia
    ('J.K. Rowling', 'Britânica', '1965-07-31'),
    ('J.R.R. Tolkien', 'Britânica', '1892-01-03'),
    ('C.S. Lewis', 'Britânica', '1898-11-29'),
    ('Sarah J. Maas', 'Norte-americana', '1986-03-05'),
    ('Rebecca Yarros', 'Norte-americana', NULL),
    ('George R. R. Martin', 'Norte-americana', '1948-09-20'),
    ('Rick Riordan', 'Norte-americana', '1964-06-05'),
    ('Suzanne Collins', 'Norte-americana', '1962-08-10'),
    ('Patrick Rothfuss', 'Norte-americana', '1973-06-06'),
    ('Eduardo Spohr', 'Brasileira', NULL),
    -- Tecnologia
    ('Robert C. Martin', 'Norte-americana', '1952-12-05'),
    ('Andrew Hunt', NULL, NULL),
    ('David Thomas', NULL, NULL),
    ('Aditya Y. Bhargava', NULL, NULL),
    ('Erich Gamma', 'Suíça', NULL),
    ('Richard Helm', 'Australiana', NULL),
    ('Ralph Johnson', 'Norte-americana', NULL),
    ('John Vlissides', 'Norte-americana', NULL),
    ('Martin Fowler', 'Britânica', NULL),
    ('Eric Evans', 'Norte-americana', NULL),
    ('Kathy Sierra', 'Norte-americana', NULL),
    ('Bert Bates', 'Norte-americana', NULL),
    ('Luciano Ramalho', 'Brasileira', NULL),
    ('Stuart Russell', 'Britânica', NULL),
    ('Peter Norvig', 'Norte-americana', NULL),
    ('Frederick P. Brooks Jr.', 'Norte-americana', '1931-04-19'),
    -- Historia
    ('Yuval Noah Harari', 'Israelense', '1976-02-24'),
    ('Laurentino Gomes', 'Brasileira', NULL),
    ('Anne Frank', 'Alemã', '1929-06-12'),
    ('Gilberto Freyre', 'Brasileira', '1900-03-15'),
    ('Sérgio Buarque de Holanda', 'Brasileira', '1902-07-11'),
    ('Lilia Moritz Schwarcz', 'Brasileira', NULL),
    ('Heloisa Murgel Starling', 'Brasileira', NULL),
    ('Jared Diamond', 'Norte-americana', '1937-09-10'),
    ('Fernando Morais', 'Brasileira', '1946-07-22'),
    ('Malala Yousafzai', 'Paquistanesa', '1997-07-12'),
    ('Antony Beevor', 'Britânica', '1946-12-14'),
    -- Infantil
    ('Antoine de Saint-Exupéry', 'Francesa', '1900-06-29'),
    ('Jeff Kinney', 'Norte-americana', '1971-02-19'),
    ('Ziraldo', 'Brasileira', '1932-10-24'),
    ('Monteiro Lobato', 'Brasileira', '1882-04-18'),
    ('R. J. Palacio', 'Norte-americana', '1963-07-13'),
    ('Roald Dahl', 'Britânica', '1916-09-13'),
    ('Lygia Bojunga', 'Brasileira', '1932-08-26'),
    ('Ruth Rocha', 'Brasileira', '1931-03-02'),
    ('Flávia Lins e Silva', 'Brasileira', NULL),
    ('Chico Buarque', 'Brasileira', '1944-06-19')
) AS v(nome, nacionalidade, nascimento)
WHERE NOT EXISTS (SELECT 1 FROM autor a WHERE a.nome = v.nome);

-- ---------------------------------------------------------------- livros
INSERT INTO livro (isbn, titulo, descricao, ano_publicacao, idioma, preco, id_categoria, id_editora)
SELECT v.isbn, v.titulo, v.descricao, v.ano, 'Português', v.preco, c.id_categoria, e.id_editora
FROM (VALUES
    -- Romance
    ('999-00-0000-001', 'É Assim que Acaba', 'Lily e Ryle vivem um relacionamento que muda tudo o que ela acreditava sobre o amor.', 2016, 54.90, 'Romance', 'Galera Record'),
    ('999-00-0000-002', 'Verity', 'Uma escritora contratada para concluir uma série encontra uma autobiografia perturbadora.', 2018, 49.90, 'Romance', 'Galera Record'),
    ('999-00-0000-003', 'Torto Arado', 'A historia das irmas Bibiana e Belonisia no sertao da Chapada Diamantina.', 2019, 69.90, 'Romance', 'Todavia'),
    ('999-00-0000-004', 'A Biblioteca da Meia-Noite', 'Entre a vida e a morte, Nora visita as vidas que poderia ter vivido.', 2020, 54.90, 'Romance', 'Bertrand Brasil'),
    ('999-00-0000-005', 'Os Sete Maridos de Evelyn Hugo', 'Uma lenda de Hollywood revela a verdade sobre sua vida e seus casamentos.', 2017, 59.90, 'Romance', 'Paralela'),
    ('999-00-0000-006', 'Tudo é Rio', 'Um triangulo amoroso marcado por dor, culpa e perdao.', 2014, 49.90, 'Romance', 'Record'),
    ('999-00-0000-007', 'O Alquimista', 'Santiago, um jovem pastor, parte em busca de sua lenda pessoal.', 1988, 39.90, 'Romance', 'Paralela'),
    ('999-00-0000-008', 'Dom Casmurro', 'Bentinho narra sua historia com Capitu e a duvida que o acompanha.', 1899, 29.90, 'Romance', 'Companhia das Letras'),
    ('999-00-0000-009', 'A Hora da Estrela', 'A vida de Macabea, nordestina no Rio de Janeiro, contada por Rodrigo S.M.', 1977, 39.90, 'Romance', 'Rocco'),
    ('999-00-0000-010', 'O Avesso da Pele', 'Um filho reconstitui a historia do pai, professor negro morto em uma abordagem policial.', 2020, 64.90, 'Romance', 'Companhia das Letras'),
    -- Fantasia
    ('999-00-0000-011', 'Harry Potter e a Pedra Filosofal', 'Harry descobre que e bruxo e inicia seus estudos em Hogwarts.', 1997, 49.90, 'Fantasia', 'Rocco'),
    ('999-00-0000-012', 'O Hobbit', 'Bilbo Bolseiro parte em uma aventura com anoes e o mago Gandalf.', 1937, 59.90, 'Fantasia', 'HarperCollins Brasil'),
    ('999-00-0000-013', 'As Crônicas de Nárnia - Volume Único', 'Os sete livros da serie de Narnia reunidos em um volume.', 1950, 119.90, 'Fantasia', 'WMF Martins Fontes'),
    ('999-00-0000-014', 'Corte de Espinhos e Rosas', 'Feyre e levada a um reino magico governado por feericos.', 2015, 64.90, 'Fantasia', 'Galera Record'),
    ('999-00-0000-015', 'Quarta Asa', 'Violet ingressa em uma academia de cavaleiros de dragoes.', 2023, 79.90, 'Fantasia', 'Planeta Minotauro'),
    ('999-00-0000-016', 'A Guerra dos Tronos', 'Casas nobres disputam o Trono de Ferro dos Sete Reinos.', 1996, 89.90, 'Fantasia', 'Suma'),
    ('999-00-0000-017', 'Percy Jackson e o Ladrão de Raios', 'Percy descobre ser filho de um deus grego.', 2005, 44.90, 'Fantasia', 'Intrinseca'),
    ('999-00-0000-018', 'Jogos Vorazes', 'Katniss se oferece como tributo em um jogo mortal televisionado.', 2008, 49.90, 'Fantasia', 'Rocco'),
    ('999-00-0000-019', 'O Nome do Vento', 'Kvothe narra a propria historia, de crianca prodigio a lenda.', 2007, 79.90, 'Fantasia', 'Arqueiro'),
    ('999-00-0000-020', 'A Batalha do Apocalipse', 'O anjo renegado Ablon atravessa eras da humanidade ate o fim dos tempos.', 2010, 69.90, 'Fantasia', 'Verus'),
    -- Tecnologia
    ('999-00-0000-021', 'Código Limpo', 'Principios e praticas para escrever codigo legivel e sustentavel.', 2008, 99.90, 'Tecnologia', 'Alta Books'),
    ('999-00-0000-022', 'O Programador Pragmático', 'Conselhos praticos para a carreira e o oficio de programar.', 1999, 119.90, 'Tecnologia', 'Bookman'),
    ('999-00-0000-023', 'Entendendo Algoritmos', 'Guia ilustrado de algoritmos para programadores e curiosos.', 2016, 74.90, 'Tecnologia', 'Novatec'),
    ('999-00-0000-024', 'Padrões de Projeto', 'O catalogo classico de 23 padroes de projeto orientados a objetos.', 1994, 139.90, 'Tecnologia', 'Bookman'),
    ('999-00-0000-025', 'Refatoração', 'Tecnicas para melhorar o design de codigo existente com seguranca.', 1999, 109.90, 'Tecnologia', 'Novatec'),
    ('999-00-0000-026', 'Domain-Driven Design', 'Modelagem de software guiada pelo dominio do negocio.', 2003, 129.90, 'Tecnologia', 'Alta Books'),
    ('999-00-0000-027', 'Use a Cabeça! Java', 'Introducao visual e pratica a linguagem Java.', 2003, 119.90, 'Tecnologia', 'Alta Books'),
    ('999-00-0000-028', 'Python Fluente', 'Programacao Python idiomatica e recursos avancados da linguagem.', 2015, 149.90, 'Tecnologia', 'Novatec'),
    ('999-00-0000-029', 'Inteligência Artificial: Uma Abordagem Moderna', 'Referencia abrangente sobre os fundamentos da inteligencia artificial.', 1995, 299.90, 'Tecnologia', 'GEN LTC'),
    ('999-00-0000-030', 'O Mítico Homem-Mês', 'Ensaios classicos sobre gerenciamento de projetos de software.', 1975, 79.90, 'Tecnologia', 'Alta Books'),
    -- Historia
    ('999-00-0000-031', 'Sapiens: Uma Breve História da Humanidade', 'Da pre-historia ao seculo XXI, como o Homo sapiens dominou o planeta.', 2011, 69.90, 'Historia', 'L&PM'),
    ('999-00-0000-032', '1808', 'A fuga da corte portuguesa para o Brasil e suas consequencias.', 2007, 59.90, 'Historia', 'Globo Livros'),
    ('999-00-0000-033', 'O Diário de Anne Frank', 'O diario de uma adolescente judia escondida durante a Segunda Guerra.', 1947, 39.90, 'Historia', 'Record'),
    ('999-00-0000-034', 'Casa-Grande & Senzala', 'Ensaio sobre a formacao da sociedade brasileira.', 1933, 94.90, 'Historia', 'Global Editora'),
    ('999-00-0000-035', 'Raízes do Brasil', 'Classico sobre a formacao historica e cultural do pais.', 1936, 54.90, 'Historia', 'Companhia das Letras'),
    ('999-00-0000-036', 'Brasil: Uma Biografia', 'A historia do Brasil do periodo colonial aos dias atuais.', 2015, 109.90, 'Historia', 'Companhia das Letras'),
    ('999-00-0000-037', 'Armas, Germes e Aço', 'Por que algumas sociedades se desenvolveram mais rapido que outras.', 1997, 84.90, 'Historia', 'Record'),
    ('999-00-0000-038', 'Olga', 'A biografia de Olga Benario Prestes.', 1985, 64.90, 'Historia', 'Companhia das Letras'),
    ('999-00-0000-039', 'Eu Sou Malala', 'A jovem que defendeu o direito a educacao e foi atacada pelo Taliba.', 2013, 49.90, 'Historia', 'Companhia das Letras'),
    ('999-00-0000-040', 'A Segunda Guerra Mundial', 'Panorama completo do maior conflito do seculo XX.', 2012, 119.90, 'Historia', 'Record'),
    -- Infantil
    ('999-00-0000-041', 'O Pequeno Príncipe', 'Um aviador perdido no deserto conhece um principe de outro planeta.', 1943, 29.90, 'Infantil', 'HarperCollins Brasil'),
    ('999-00-0000-042', 'Diário de um Banana', 'As desventuras de Greg Heffley no ensino fundamental.', 2007, 44.90, 'Infantil', 'V&R Editoras'),
    ('999-00-0000-043', 'O Menino Maluquinho', 'O menino de panela na cabeca e muita imaginacao.', 1980, 49.90, 'Infantil', 'Melhoramentos'),
    ('999-00-0000-044', 'Reinações de Narizinho', 'As aventuras de Narizinho, Pedrinho e Emilia no Sitio do Picapau Amarelo.', 1931, 54.90, 'Infantil', 'Globo Livros'),
    ('999-00-0000-045', 'Extraordinário', 'Auggie, um menino com uma diferenca facial, vai a escola pela primeira vez.', 2012, 49.90, 'Infantil', 'Intrinseca'),
    ('999-00-0000-046', 'Matilda', 'Uma menina genial enfrenta pais negligentes e uma diretora tirana.', 1988, 49.90, 'Infantil', 'WMF Martins Fontes'),
    ('999-00-0000-047', 'A Bolsa Amarela', 'Raquel esconde seus desejos em uma bolsa amarela.', 1976, 44.90, 'Infantil', 'Casa Lygia Bojunga'),
    ('999-00-0000-048', 'Marcelo, Marmelo, Martelo', 'Historias divertidas sobre palavras, nomes e brincadeiras.', 1976, 42.90, 'Infantil', 'Salamandra'),
    ('999-00-0000-049', 'O Diário de Pilar na Grécia', 'Pilar viaja pela Grecia em uma rede magica.', 2010, 49.90, 'Infantil', 'Pequena Zahar'),
    ('999-00-0000-050', 'Chapeuzinho Amarelo', 'Uma menina que tinha medo de tudo aprende a enfrentar o Lobo.', 1979, 39.90, 'Infantil', 'José Olympio')
) AS v(isbn, titulo, descricao, ano, preco, categoria, editora)
JOIN categoria c ON c.nome = v.categoria
JOIN editora e ON e.nome = v.editora
ON CONFLICT (isbn) DO NOTHING;

-- ---------------------------------------------------------------- livro x autor
INSERT INTO livro_autor (id_livro, id_autor)
SELECT l.id_livro, a.id_autor
FROM (VALUES
    ('999-00-0000-001', 'Colleen Hoover'), ('999-00-0000-002', 'Colleen Hoover'),
    ('999-00-0000-003', 'Itamar Vieira Junior'), ('999-00-0000-004', 'Matt Haig'),
    ('999-00-0000-005', 'Taylor Jenkins Reid'), ('999-00-0000-006', 'Carla Madeira'),
    ('999-00-0000-007', 'Paulo Coelho'), ('999-00-0000-008', 'Machado de Assis'),
    ('999-00-0000-009', 'Clarice Lispector'), ('999-00-0000-010', 'Jeferson Tenório'),
    ('999-00-0000-011', 'J.K. Rowling'), ('999-00-0000-012', 'J.R.R. Tolkien'),
    ('999-00-0000-013', 'C.S. Lewis'), ('999-00-0000-014', 'Sarah J. Maas'),
    ('999-00-0000-015', 'Rebecca Yarros'), ('999-00-0000-016', 'George R. R. Martin'),
    ('999-00-0000-017', 'Rick Riordan'), ('999-00-0000-018', 'Suzanne Collins'),
    ('999-00-0000-019', 'Patrick Rothfuss'), ('999-00-0000-020', 'Eduardo Spohr'),
    ('999-00-0000-021', 'Robert C. Martin'),
    ('999-00-0000-022', 'Andrew Hunt'), ('999-00-0000-022', 'David Thomas'),
    ('999-00-0000-023', 'Aditya Y. Bhargava'),
    ('999-00-0000-024', 'Erich Gamma'), ('999-00-0000-024', 'Richard Helm'),
    ('999-00-0000-024', 'Ralph Johnson'), ('999-00-0000-024', 'John Vlissides'),
    ('999-00-0000-025', 'Martin Fowler'), ('999-00-0000-026', 'Eric Evans'),
    ('999-00-0000-027', 'Kathy Sierra'), ('999-00-0000-027', 'Bert Bates'),
    ('999-00-0000-028', 'Luciano Ramalho'),
    ('999-00-0000-029', 'Stuart Russell'), ('999-00-0000-029', 'Peter Norvig'),
    ('999-00-0000-030', 'Frederick P. Brooks Jr.'),
    ('999-00-0000-031', 'Yuval Noah Harari'), ('999-00-0000-032', 'Laurentino Gomes'),
    ('999-00-0000-033', 'Anne Frank'), ('999-00-0000-034', 'Gilberto Freyre'),
    ('999-00-0000-035', 'Sérgio Buarque de Holanda'),
    ('999-00-0000-036', 'Lilia Moritz Schwarcz'), ('999-00-0000-036', 'Heloisa Murgel Starling'),
    ('999-00-0000-037', 'Jared Diamond'), ('999-00-0000-038', 'Fernando Morais'),
    ('999-00-0000-039', 'Malala Yousafzai'), ('999-00-0000-040', 'Antony Beevor'),
    ('999-00-0000-041', 'Antoine de Saint-Exupéry'), ('999-00-0000-042', 'Jeff Kinney'),
    ('999-00-0000-043', 'Ziraldo'), ('999-00-0000-044', 'Monteiro Lobato'),
    ('999-00-0000-045', 'R. J. Palacio'), ('999-00-0000-046', 'Roald Dahl'),
    ('999-00-0000-047', 'Lygia Bojunga'), ('999-00-0000-048', 'Ruth Rocha'),
    ('999-00-0000-049', 'Flávia Lins e Silva'), ('999-00-0000-050', 'Chico Buarque')
) AS v(isbn, autor)
JOIN livro l ON l.isbn = v.isbn
JOIN autor a ON a.nome = v.autor
ON CONFLICT DO NOTHING;
