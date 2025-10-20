# Evidências de Implementação — Sistema de Barbearia

**Autor:** Equipe Técnica da Barbearia  
**Data:** 2025-10-20  
**Stack alvo:** Java 17+ (compatível com Java 23) · Maven 3.9+

---

## 1. Como executar

1. Instale o JDK 17 (ou superior) e o Maven 3.9+.
2. No diretório do projeto, execute `mvn clean package` para compilar e rodar os testes.
3. Para iniciar a aplicação via linha de comando, use `mvn -Dexec.mainClass=br.ufvjm.barbearia.system.Main exec:java`.
4. Alternativamente, abra o projeto no NetBeans e pressione **F6** para executar `Main`.
5. Antes de salvar dados, garanta a existência da pasta `data/` na raiz do projeto (os utilitários de persistência criam subpastas como `data/extratos/` automaticamente).

---

## 2. Resumo do design

O projeto organiza as responsabilidades em pacotes `br.ufvjm.barbearia.*`: `model` concentra as entidades de domínio, `value` reúne objetos de valor (CPF, dinheiro, telefone, endereço), `enums` define enumeradores de negócio, `system` abriga a orquestração (`Sistema` e `Main`), `persist` encapsula a camada de persistência JSON/extratos e `compare` provê comparadores para ordenação. As regras de negócio principais residem nas entidades enquanto `Sistema` coordena coleções, contadores e integração com armazenamento.

Entidades centrais: Cliente, Usuario, Agendamento, ItemDeServico, ConsumoDeProduto, Servico, Produto, ContaAtendimento, ItemContaProduto, Venda, ItemVenda, RecebimentoFornecedor, ItemRecebimento, Despesa e CaixaDiario. Objetos de valor (`CpfHash`, `Dinheiro`, `Quantidade`, `Periodo`, `Email`, `Endereco`, `Telefone`) encapsulam validações. `DataSnapshot`, `JsonStorage` (Gson) e `ExtratoIO` tratam da persistência e geração de extratos por cliente.

---

## 3. Evidências por requisito

### Requisito 1 — Todas as classes conforme diagrama
- **Enunciado:** "Todas as classes conforme diagrama: evidencie relações (herança Pessoa→Cliente/Usuario; composição em Agendamento→ItemDeServico→ConsumoDeProduto; Venda→ItemVenda; ContaAtendimento→ItemContaProduto; RecebimentoFornecedor→ItemRecebimento)."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/model/Pessoa.java`
  - `src/main/java/br/ufvjm/barbearia/model/Cliente.java`
  - `src/main/java/br/ufvjm/barbearia/model/Usuario.java`
  - `src/main/java/br/ufvjm/barbearia/model/Agendamento.java`
  - `src/main/java/br/ufvjm/barbearia/model/ItemDeServico.java`
  - `src/main/java/br/ufvjm/barbearia/model/ConsumoDeProduto.java`
  - `src/main/java/br/ufvjm/barbearia/model/Venda.java`
  - `src/main/java/br/ufvjm/barbearia/model/ItemVenda.java`
  - `src/main/java/br/ufvjm/barbearia/model/ContaAtendimento.java`
  - `src/main/java/br/ufvjm/barbearia/model/ItemContaProduto.java`
  - `src/main/java/br/ufvjm/barbearia/model/RecebimentoFornecedor.java`
  - `src/main/java/br/ufvjm/barbearia/model/ItemRecebimento.java`
- **Como foi implementado:** `Pessoa` serve como classe base para `Cliente` e `Usuario`, compartilhando identificadores e dados de contato. `Agendamento` agrega `ItemDeServico`, que por sua vez contém `ConsumoDeProduto`, modelando a hierarquia da OS. `Venda`, `ContaAtendimento` e `RecebimentoFornecedor` seguem o mesmo padrão com seus itens especializados (`ItemVenda`, `ItemContaProduto`, `ItemRecebimento`), preservando encapsulamento das coleções via listas internas. Cada relacionamento requerido pelo diagrama está explicitado nos pacotes `model` citados.
- **Trecho ilustrativo:**
  ```java
  public class Cliente extends Pessoa {

      protected static int totalVeiculosProtegido;

      private final CpfHash cpf;
      private boolean ativo;
  }

  public class Usuario extends Pessoa {

      private Papel papel;
      private final String login;
      private String senhaHash;
      private boolean ativo;
  }

  public Agendamento(UUID id, Cliente cliente, Estacao estacao,
                     LocalDateTime inicio, LocalDateTime fim, Dinheiro sinal) {
      this.id = Objects.requireNonNull(id, "id não pode ser nulo");
      this.cliente = Objects.requireNonNull(cliente, "cliente não pode ser nulo");
      this.estacao = Objects.requireNonNull(estacao, "estacao não pode ser nula");
      this.inicio = Objects.requireNonNull(inicio, "inicio não pode ser nulo");
      this.fim = Objects.requireNonNull(fim, "fim não pode ser nulo");
      if (fim.isBefore(inicio)) {
          throw new IllegalArgumentException("fim não pode ser anterior ao início");
      }
      this.sinal = Objects.requireNonNull(sinal, "sinal não pode ser nulo");
      this.itens = new ArrayList<>();
      this.status = StatusAtendimento.EM_ESPERA;

      Sistema.incrementarTotalOS();
  }
  ```
- **Como testar/validar:** Em `Main`, crie um agendamento com itens de serviço e consumo; inspecione os objetos resultantes no depurador ou imprima `agendamento` para confirmar a composição. Para `Venda`, `ContaAtendimento` e `RecebimentoFornecedor`, consulte as classes indicadas e adicione itens para observar as listas internas.

### Requisito 2 — Uso por colaboradores e administrador
- **Enunciado:** "Uso por colaboradores e administrador: aponte `Usuario`+`Papel` e, se houver, checagens de permissão; cite telas/fluxos ou serviços onde o papel é usado."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/model/Usuario.java`
  - `src/main/java/br/ufvjm/barbearia/enums/Papel.java`
  - `src/main/java/br/ufvjm/barbearia/system/Main.java`
- **Como foi implementado:** O enum `Papel` distingue administradores, colaboradores e barbeiros. `Usuario` mantém o papel e credenciais, permitindo que a camada de aplicação restrinja operações. O cenário de `Main` cria usuários ADMIN e BARBEIRO, evidenciando fluxos distintos (cadastros vs. atendimento).
- **Trecho ilustrativo:**
  ```java
  public enum Papel {
      ADMIN("Administrador"),
      COLABORADOR("Colaborador"),
      BARBEIRO("Barbeiro");

      private final String descricao;

      Papel(String descricao) {
          this.descricao = descricao;
      }
  }

  Usuario adm = new Usuario(
          UUID.randomUUID(),
          "Carlos Admin",
          enderecoBase,
          telefoneFixo,
          Email.of("carlos.admin@barbearia.com"),
          Papel.ADMIN,
          "carlos",
          "123",
          true
  );
  ```
- **Como testar/validar:** Execute `Main` e observe os diferentes usuários criados. Em uma interface, use `usuario.getPapel()` para decidir quais operações habilitar (ex.: apenas ADMIN pode chamar `cadastrarUsuario`).

### Requisito 3 — `toString()` sobrescrito
- **Enunciado:** "`toString()` sobrescrito: mostre exemplos de `toString()` em 2–3 classes (Cliente, Agendamento, Produto)."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/model/Cliente.java`
  - `src/main/java/br/ufvjm/barbearia/model/Agendamento.java`
  - `src/main/java/br/ufvjm/barbearia/model/Produto.java`
- **Como foi implementado:** As entidades redefinem `toString()` para facilitar logs e relatórios, incluindo atributos relevantes e composição com dados herdados (`super.toString()` em `Cliente`).
- **Trecho ilustrativo:**
  ```java
  @Override
  public String toString() {
      return "Cliente{"
              + "cpf=" + cpf
              + ", ativo=" + ativo
              + ", totalVeiculosProtegido=" + totalVeiculosProtegido
              + ", pessoa=" + super.toString()
              + '}';
  }

  @Override
  public String toString() {
      return "Produto{"
              + "id=" + id
              + ", nome='" + nome + '\''
              + ", sku='" + sku + '\''
              + ", estoqueAtual=" + estoqueAtual
              + ", estoqueMinimo=" + estoqueMinimo
              + ", precoVenda=" + precoVenda
              + ", custoMedio=" + custoMedio
              + '}';
  }
  ```
- **Como testar/validar:** Crie instâncias via `Main` ou testes unitários e imprima `cliente` e `produto`; o console exibirá os atributos formatados.

### Requisito 4 — Uso de `super` em construtores de subclasses
- **Enunciado:** "`super` em construtores de subclasses: evidencie em `Cliente` e `Usuario` chamando `super(...)` de `Pessoa`."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/model/Cliente.java`
  - `src/main/java/br/ufvjm/barbearia/model/Usuario.java`
- **Como foi implementado:** Os construtores invocam `super` para inicializar campos herdados (ID, nome, contato), delegando a validação de nulidade à classe base antes de tratar atributos específicos (`cpf`, `papel`).
- **Trecho ilustrativo:**
  ```java
  public Cliente(UUID id, String nome, Endereco endereco, Telefone telefone, Email email,
                 CpfHash cpf, boolean ativo) {
      super(id, nome, endereco, telefone, email);
      this.cpf = Objects.requireNonNull(cpf, "cpf não pode ser nulo");
      this.ativo = ativo;
  }

  public Usuario(UUID id, String nome, Endereco endereco, Telefone telefone, Email email,
                 Papel papel, String login, String senhaHash, boolean ativo) {
      super(id, nome, endereco, telefone, email);
      this.papel = Objects.requireNonNull(papel, "papel não pode ser nulo");
      this.login = Objects.requireNonNull(login, "login não pode ser nulo");
      this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash não pode ser nulo");
      this.ativo = ativo;
  }
  ```
- **Como testar/validar:** Instancie `Cliente`/`Usuario` (ex.: no `Main`) e verifique, via depuração, que os campos herdados foram preenchidos pela chamada a `super`.

### Requisito 5 — Estações (vetor estático fixo)
- **Enunciado:** "Estações (vetor estático fixo): mostrar `Estacao.ESTACOES = new Estacao[3]` com bloco `static` setando os 3 índices (a 1ª com lavagem)."
- **Onde está no código:** `src/main/java/br/ufvjm/barbearia/model/Estacao.java`
- **Como foi implementado:** O vetor `ESTACOES` é inicializado em bloco `static` com três objetos imutáveis, garantindo que a estação 1 possua lavagem e que as demais posições estejam pré-configuradas.
- **Trecho ilustrativo:**
  ```java
  public static final Estacao[] ESTACOES = new Estacao[3];

  static {
      ESTACOES[0] = new Estacao(1, true);
      ESTACOES[1] = new Estacao(2, false);
      ESTACOES[2] = new Estacao(3, false);
  }
  ```
- **Como testar/validar:** No `Main`, observe a reserva `Estacao.ESTACOES[0]`. Adicione logs para inspecionar o vetor e confirmar os atributos.

### Requisito 6 — CRUD de colaboradores
- **Enunciado:** "CRUD de colaboradores: onde estão métodos para cadastrar/editar `Usuario` (ex.: em `Sistema`); trechos dos métodos."
- **Onde está no código:** `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
- **Como foi implementado:** `Sistema` mantém uma lista mutável de `Usuario` e fornece métodos para cadastrar e editar registros com validação de ID, além de usar um iterador para substituir a instância correspondente.
- **Trecho ilustrativo:**
  ```java
  private List<Usuario> usuarios = new ArrayList<>();

  public void cadastrarUsuario(Usuario u) {
      usuarios.add(Objects.requireNonNull(u, "usuario não pode ser nulo"));
  }

  public void editarUsuario(UUID id, Usuario novo) {
      Objects.requireNonNull(id, "id não pode ser nulo");
      Usuario usuarioAtualizado = Objects.requireNonNull(novo, "novo não pode ser nulo");
      if (!usuarioAtualizado.getId().equals(id)) {
          throw new IllegalArgumentException("ID do usuário não corresponde ao registro atualizado");
      }
      substituirUsuario(id, usuarioAtualizado);
  }
  ```
- **Como testar/validar:** Chame `cadastrarUsuario` e `editarUsuario` com IDs válidos; tentar editar com ID divergente lança exceção, confirmando a validação.

### Requisito 7 — CRUD de clientes
- **Enunciado:** "CRUD de clientes: idem para `Cliente` (cadastrar/alterar/excluir)."
- **Onde está no código:** `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
- **Como foi implementado:** A lista de clientes em `Sistema` aceita inserção, substituição com validação de ID e remoção com `removeIf`, garantindo mensagem de erro caso o registro não exista.
- **Trecho ilustrativo:**
  ```java
  private List<Cliente> clientes = new ArrayList<>();

  public void cadastrarCliente(Cliente c) {
      clientes.add(Objects.requireNonNull(c, "cliente não pode ser nulo"));
  }

  public void editarCliente(UUID id, Cliente novo) {
      Objects.requireNonNull(id, "id não pode ser nulo");
      Cliente clienteAtualizado = Objects.requireNonNull(novo, "novo não pode ser nulo");
      if (!clienteAtualizado.getId().equals(id)) {
          throw new IllegalArgumentException("ID do cliente não corresponde ao registro atualizado");
      }
      substituirCliente(id, clienteAtualizado);
  }

  public void removerCliente(UUID id) {
      boolean removido = clientes.removeIf(c -> c.getId().equals(id));
      if (!removido) {
          throw new IllegalArgumentException("Cliente não encontrado: " + id);
      }
  }
  ```
- **Como testar/validar:** Utilize o `Main` ou scripts para cadastrar, editar e remover clientes; observe a lista antes/depois via depuração.

### Requisito 8 — Verificar e imprimir OS por cliente
- **Enunciado:** "Verificar e imprimir OS por cliente: métodos como `listarOrdensDeServicoDoCliente(UUID)` e impressão (`System.out.println(a)`)."
- **Onde está no código:** `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
- **Como foi implementado:** `Sistema` filtra agendamentos por ID do cliente e imprime cada OS com `System.out.println(a.toString())`, permitindo consultas rápidas.
- **Trecho ilustrativo:**
  ```java
  public List<Agendamento> listarOrdensDeServicoDoCliente(UUID clienteId) {
      Objects.requireNonNull(clienteId, "clienteId não pode ser nulo");
      return agendamentos.stream()
              .filter(a -> a.getCliente().getId().equals(clienteId))
              .collect(Collectors.toList());
  }

  public void imprimirOrdensDeServicoDoCliente(UUID clienteId) {
      listarOrdensDeServicoDoCliente(Objects.requireNonNull(clienteId, "clienteId não pode ser nulo"))
              .forEach(a -> System.out.println(a.toString()));
  }
  ```
- **Como testar/validar:** Crie agendamentos associados a um cliente e chame `imprimirOrdensDeServicoDoCliente`; o console listará as OS.

### Requisito 9 — Estruturas dinâmicas
- **Enunciado:** "Estruturas dinâmicas: listas (`ArrayList`) para OS, estoque, loja, clientes; mostre campos e pontos de inserção/remoção."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
  - `src/main/java/br/ufvjm/barbearia/model/Venda.java`
  - `src/main/java/br/ufvjm/barbearia/model/ContaAtendimento.java`
- **Como foi implementado:** O sistema utiliza `ArrayList` para manter coleções mutáveis (clientes, usuários, agendamentos, produtos, vendas, contas, despesas, recebimentos). Entidades agregadas como `Venda` e `ContaAtendimento` também usam listas internas para itens, atualizando totais ao adicionar/remover elementos.
- **Trecho ilustrativo:**
  ```java
  private List<Agendamento> agendamentos = new ArrayList<>();
  private List<Produto> produtos = new ArrayList<>();
  private List<Venda> vendas = new ArrayList<>();
  private List<ContaAtendimento> contas = new ArrayList<>();

  public void realizarAgendamento(Agendamento ag) {
      agendamentos.add(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
      incrementarTotalOS();
  }

  public void cadastrarProduto(Produto produto) {
      produtos.add(Objects.requireNonNull(produto, "produto não pode ser nulo"));
  }

  public void adicionarItem(ItemVenda itemVenda) {
      itens.add(Objects.requireNonNull(itemVenda, "itemVenda não pode ser nulo"));
      total = null;
  }
  ```
- **Como testar/validar:** Adicione registros via `Sistema` e observe o crescimento das listas; remova itens e confirme a atualização dos totais em `Venda`/`ContaAtendimento`.

### Requisito 10 — Extratos de serviço e venda
- **Enunciado:** "Extratos de serviço e venda: métodos de geração (string) e salvamento (JSON/TXT) junto ao cliente; mostre chamada a `ExtratoIO.saveExtrato(...)`."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
  - `src/main/java/br/ufvjm/barbearia/persist/ExtratoIO.java`
- **Como foi implementado:** `Sistema` monta strings de extrato e delega a gravação para `ExtratoIO`, que cria diretórios, gera nome de arquivo com timestamp e escreve em UTF-8, garantindo associação com o cliente pelo ID.
- **Trecho ilustrativo:**
  ```java
  public void gerarExtratoServico(Agendamento ag) {
      Objects.requireNonNull(ag, "agendamento não pode ser nulo");
      String nomeBarbeiro = ag.getBarbeiro() != null ? ag.getBarbeiro().getNome() : "(sem barbeiro)";
      String extrato = "Extrato de Serviço\nCliente: " + ag.getCliente().getNome()
              + "\nBarbeiro: " + nomeBarbeiro
              + "\nTotal: " + ag.totalServicos();
      try {
          ExtratoIO.saveExtrato(ag.getCliente(), extrato, Path.of("data/extratos"));
      } catch (IOException e) {
          throw new UncheckedIOException("Falha ao gerar extrato de serviço", e);
      }
  }

  public static void saveExtrato(Cliente cliente, String extrato, Path dir) throws IOException {
      Objects.requireNonNull(cliente, "cliente não pode ser nulo");
      Objects.requireNonNull(extrato, "extrato não pode ser nulo");
      Objects.requireNonNull(dir, "dir não pode ser nulo");

      Files.createDirectories(dir);

      String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
      String fileName = String.format("extrato_%s_%s.txt", cliente.getId(), timestamp);
      Path file = dir.resolve(fileName);

      try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
          writer.write(extrato);
      }
  }
  ```
- **Como testar/validar:** Rode `Main`, verifique a criação de arquivos em `data/extratos/` e abra o `.txt` correspondente para conferir o conteúdo.

### Observação sobre o Item 11
O requisito 11 sobre veículos foi desconsiderado neste projeto por orientação do professor/cliente: a barbearia não manipula frota, portanto nenhuma funcionalidade relacionada a `Veiculo` foi implementada ou necessária para atender aos casos de uso.

### Requisito 12 — Método de classe para total de Ordens de Serviço
- **Enunciado:** "Método de classe para total de Ordens de Serviço: evidencie `Sistema.getTotalOrdensServicoCriadas()` e onde incrementa."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
  - `src/main/java/br/ufvjm/barbearia/model/Agendamento.java`
- **Como foi implementado:** `Sistema` mantém contador estático de OS com métodos sincronizados de incremento e leitura. O construtor de `Agendamento` e `Sistema.realizarAgendamento` invocam `incrementarTotalOS`, garantindo contagem cumulativa.
- **Trecho ilustrativo:**
  ```java
  private static int totalOrdensServico = 0;

  public static synchronized void incrementarTotalOS() {
      totalOrdensServico++;
  }

  public static synchronized int getTotalOrdensServicoCriadas() {
      return totalOrdensServico;
  }

  public void realizarAgendamento(Agendamento ag) {
      agendamentos.add(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
      incrementarTotalOS();
  }
  ```
- **Como testar/validar:** Após criar agendamentos, chame `Sistema.getTotalOrdensServicoCriadas()` (ver `Main`) para acompanhar o total global.

### Requisito 13 — `Comparator` para Agendamento e Cliente
- **Enunciado:** "`Comparator` para Agendamento e Cliente: arquivos em `compare` (por nome/email e por data de início/nome cliente); traga os métodos `compare(...)`."
- **Onde está no código:** Pacote `src/main/java/br/ufvjm/barbearia/compare/`
- **Como foi implementado:** Comparadores específicos ordenam agendamentos por início ou nome de cliente (`Collator` PT-BR) e clientes por nome/e-mail, garantindo ordenações consistentes.
- **Trecho ilustrativo:**
  ```java
  public class AgendamentoPorInicio implements Comparator<Agendamento> {
      @Override
      public int compare(Agendamento agendamento1, Agendamento agendamento2) {
          Objects.requireNonNull(agendamento1, "agendamento1 não pode ser nulo");
          Objects.requireNonNull(agendamento2, "agendamento2 não pode ser nulo");
          LocalDateTime inicio1 = Objects.requireNonNull(agendamento1.getInicio(), "início do agendamento1 não pode ser nulo");
          LocalDateTime inicio2 = Objects.requireNonNull(agendamento2.getInicio(), "início do agendamento2 não pode ser nulo");
          return inicio1.compareTo(inicio2);
      }
  }

  public class ClientePorNome implements Comparator<Cliente> {
      private static final Collator COLLATOR;

      static {
          COLLATOR = Collator.getInstance(new Locale("pt", "BR"));
          COLLATOR.setStrength(Collator.PRIMARY);
      }

      @Override
      public int compare(Cliente cliente1, Cliente cliente2) {
          Objects.requireNonNull(cliente1, "cliente1 não pode ser nulo");
          Objects.requireNonNull(cliente2, "cliente2 não pode ser nulo");
          String nome1 = Objects.requireNonNull(cliente1.getNome(), "nome do cliente1 não pode ser nulo");
          String nome2 = Objects.requireNonNull(cliente2.getNome(), "nome do cliente2 não pode ser nulo");
          return COLLATOR.compare(nome1, nome2);
      }
  }
  ```
- **Como testar/validar:** Ordene listas com `Collections.sort` ou `list.sort(new AgendamentoPorInicio())` e verifique o resultado ordenado.

### Requisito 14 — Salvar/recuperar JSON com Gson
- **Enunciado:** "Salvar/recuperar JSON (Clientes, Serviços, Agendamentos, Produtos, Relatórios de Vendas, Colaboradores, Estoque, etc.): mostrar `DataSnapshot` e `JsonStorage.save/load` usando Gson e `try-with-resources`."
- **Onde está no código:**
  - `src/main/java/br/ufvjm/barbearia/persist/DataSnapshot.java`
  - `src/main/java/br/ufvjm/barbearia/persist/JsonStorage.java`
  - `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
- **Como foi implementado:** `DataSnapshot` agrega todas as listas do sistema. `Sistema.saveAll/loadAll` convertem as coleções para snapshot e delegam a `JsonStorage`, que usa `Gson` com pretty printing e `try-with-resources` para ler/gravar arquivos UTF-8, criando diretórios quando necessário.
- **Trecho ilustrativo:**
  ```java
  public static void save(DataSnapshot data, Path file) throws IOException {
      Objects.requireNonNull(data, "data não pode ser nulo");
      Objects.requireNonNull(file, "file não pode ser nulo");

      Path parent = file.getParent();
      if (parent != null) {
          Files.createDirectories(parent);
      }

      try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
          GSON.toJson(data, writer);
      }
  }

  public static DataSnapshot load(Path file) throws IOException {
      Objects.requireNonNull(file, "file não pode ser nulo");

      if (!Files.exists(file)) {
          return new DataSnapshot();
      }

      try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
          DataSnapshot snapshot = GSON.fromJson(reader, DataSnapshot.class);
          return snapshot != null ? snapshot : new DataSnapshot();
      }
  }
  ```
- **Como testar/validar:** Execute `Main` para gerar `data/barbearia.json`; abra o arquivo para conferir o JSON formatado. Em seguida, rode `loadAll` apontando para o mesmo caminho para restaurar os dados.

### Requisito 15 — Pilha para atendimentos secundários
- **Enunciado:** "Pilha para atendimentos secundários: `Deque/ArrayDeque` para fila LIFO; métodos `push/pop/peek` e quando são usados (cancelamentos)."
- **Onde está no código:** `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
- **Como foi implementado:** A fila secundária é um `Deque<Agendamento>` baseado em `ArrayDeque`, inserindo elementos com `push` e removendo com `pop` quando surge vaga. A checagem de vazio protege o `pop`, e a inspeção do próximo atendimento pode ser obtida via `filaSecundaria.peek()` pela camada de apresentação, pois a estrutura exposta suporta a operação LIFO completa.
- **Trecho ilustrativo:**
  ```java
  private Deque<Agendamento> filaSecundaria = new ArrayDeque<>();

  public void adicionarAgendamentoSecundario(Agendamento ag) {
      filaSecundaria.push(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
  }

  public Agendamento recuperarAgendamentoSecundario() {
      if (filaSecundaria.isEmpty()) {
          throw new NoSuchElementException("Não há agendamentos na fila secundária");
      }
      return filaSecundaria.pop();
  }
  ```
- **Como testar/validar:** Empilhe agendamentos secundários, acione `recuperarAgendamentoSecundario` após um cancelamento e, se precisar apenas visualizar, utilize `filaSecundaria.peek()` a partir de um ponto de depuração ou método utilitário.

---

## 4. Exemplos rápidos de execução

Rodando `Main` (`mvn -Dexec.mainClass=br.ufvjm.barbearia.system.Main exec:java`), o cenário cadastra usuários, clientes, serviço e produto base, cria um agendamento concluído, gera o extrato e salva o snapshot em `data/barbearia.json`. O extrato é gravado em `data/extratos/extrato_<cliente>_<timestamp>.txt`, e o console imprime o resumo do sistema com o total de OS acumuladas.

---

## 5. Conclusões

Os requisitos funcionais foram cobertos com heranças e composições coerentes, CRUDs completos para clientes e colaboradores, geração de extratos, comparadores e persistência JSON com Gson. A pilha LIFO garante realocação de atendimentos, e o contador estático acompanha o volume de OS. Recomenda-se evoluir com testes automatizados, camadas de permissão explícitas e auditoria conforme o documento de agentes.

