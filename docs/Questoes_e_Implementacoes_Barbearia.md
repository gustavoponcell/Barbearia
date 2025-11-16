# 1. Capa
**Título:** Barbearia — Questões do Trabalho e Implementações  
**Data de geração:** 16/11/2025  
**Resumo:** Sistema acadêmico completo para agenda/ordens de serviço, três estações físicas (uma com lavagem), controle de loja/estoque e vendas, finanças com contas/caixa/extratos automáticos e camadas de permissão por papel (ADMIN, COLABORADOR, BARBEIRO).

# 2. Como ler este documento
- **Seção 4** fornece um panorama rápido de conformidade por questão (Q1–Q18).
- **Seção 5** detalha cada requisito: enunciado resumido, implementação, snippet comentado, instruções de teste e status.
- **Seção 6** mapeia cada pacote (`system`, `model`, `persist`, `compare`, `value`, `enums`, `exceptions`, `util`).
- **Seção 7** descreve o que aparece no console e nos arquivos ao executar a simulação final.
- **Classe demonstrativa:** `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java` concentra as verificações sequenciais das 18 questões; `Sistema` encapsula as operações de negócio.

# 3. Ambiente e Build
- **JDK alvo:** 17 (`pom.xml` define `<maven.compiler.source>` e `<maven.compiler.target>` em 17).  
- **Bibliotecas:** Gson 2.10.1 para persistência JSON e JUnit 5.10.2 para testes (`pom.xml`, linhas 19–30).  
- **Plugins Maven relevantes:** `maven-clean-plugin`, `maven-compiler-plugin` (com `<release>17</release>`), `maven-resources-plugin`, `maven-surefire-plugin` (com `useModulePath=false`), `exec-maven-plugin` (classe principal padrão `br.ufvjm.barbearia.system.Main`) e `maven-javadoc-plugin`.  
- **Comandos úteis:**
  - `mvn -q -DskipTests clean package`
  - `mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.EntregaFinalMain`
  - `mvn -q javadoc:javadoc`

# 4. Resumo de Conformidade (tabela)
| Questão | Status | Evidência (arquivo:linhas) | Como validar |
| --- | --- | --- | --- |
| Q1 | OK | `EntregaFinalMain.java:85-135`, `Agendamento.java:50-143`, `ContaAtendimento.java:41-154` | Executar `EntregaFinalMain` e observar “Questao 1: OK” mostrando IDs relacionados; opcionalmente inspecionar classes mencionadas. |
| Q2 | OK | `Sistema.java:1003-1015`, `EntregaFinalMain.java:137-150` | Rodar `EntregaFinalMain` e verificar a tentativa de relatório pelo colaborador disparando `PermissaoNegadaException` (log Questao 2). |
| Q3 | OK | `Cliente.java:125-136`, `Servico.java:89-99`, `ContaAtendimento.java:223-236`, `Agendamento.java:196-208`, `Despesa.java` (`toString` análogo) | Questão 3 imprime os `toString()` de múltiplas classes no console. |
| Q4 | OK | `Cliente.java:58-66`, `Usuario.java:43-50` | Conferir construtores chamando `super(...)`; Questão 4 relata campos herdados preenchidos. |
| Q5 | OK | `Estacao.java:20-35` | Questão 5 checa `Estacao.ESTACOES.length==3` e lavagem na primeira posição. |
| Q6 | OK | `Sistema.java:244-267`, `EntregaFinalMain.java:179-204` | Questão 6 executa cadastro/edição/remoção e compara contagens via relatório operacional. |
| Q7 | OK | `Sistema.java:209-228`, `EntregaFinalMain.java:204-227` | Questão 7 cria/edita/remove cliente e imprime totais atualizados. |
| Q8 | OK | `Sistema.java:645-655`, `EntregaFinalMain.java:229-237` | Questão 8 lista e imprime OS por cliente com IDs exibidos. |
| Q9 | OK | `Sistema.java:194-208` e `Sistema.java:565-603`, `EntregaFinalMain.java:239-258` | Questão 9 movimenta listas/Deque e mostra `peek/pop` no console. |
| Q10 | OK | `Sistema.java:732-753`, `Sistema.java:760-787`, `Sistema.java:796-819`, `EntregaFinalMain.java:260-304` | Questão 10 fecha conta, registra venda e cancela OS, verificando arquivos em `data/extratos`. |
| Q11 | OK | `Servico.java:44-59`, `Sistema.java:94-191`, `Cliente.java:44-94`, `EntregaFinalMain.java:306-312` | Questão 11 exibe os dois contadores (encapsulado e protegido) alimentados no construtor de `Servico`. |
| Q12 | OK | `Sistema.java:525-545`, `Sistema.java:127-135`, `EntregaFinalMain.java:314-319` | Questão 12 compara `Sistema.getTotalOrdensServicoCriadas()` com o tamanho da lista. |
| Q13 | OK | `Sistema.java:231-242`, `Sistema.java:547-558`, `compare/*.java`, `EntregaFinalMain.java:321-356` | Questão 13 ordena clientes (por e-mail) e agendamentos (por nome do cliente). |
| Q14 | OK | `JsonStorage.java:60-95`, `ExtratoIO.java:25-49`, `Sistema.java:822-880`, `EntregaFinalMain.java:358-380` | Questão 14 salva/recupera snapshot com Gson + TypeAdapters e valida contagens. |
| Q15 | OK | `EntregaFinalMain.java:382-428` | Questão 15 imprime o passo a passo do `Iterator` e compara com `foreach`. |
| Q16 | OK | `EntregaFinalMain.java:431-485` | Questão 16 mostra `Collections.sort` com dois comparators diferentes para clientes e agendamentos. |
| Q17 | OK | `Sistema.java:103-125`, `EntregaFinalMain.java:487-534` | Questão 17 imprime índices retornados por `Sistema.find` vs `Collections.binarySearch`. |
| Q18 | OK | `EntregaFinalMain.java:536-774`, `Sistema.java:565-641`, `Sistema.java:706-879` | Questão 18 executa pipeline completo de 10 clientes, fila secundária, cancelamentos, extratos, vendas e persistência. |

# 5. Questões detalhadas (Q1 → Q18)
## Q1 — Classes e relações conforme diagrama
**Enunciado (resumo):** Instanciar as entidades-chave (Cliente, Usuário, Serviço, Agendamento, Conta, Despesa) preservando as relações do diagrama (cliente vinculado à OS, conta ligada à OS etc.).

**Implementação no projeto:** `EntregaFinalMain` cria objetos reais (`Cliente`, `Usuario`, `Servico`) e registra o agendamento e a conta (`Sistema.criarAgendamento` e `Sistema.criarContaAtendimento`), comprovando os vínculos exigidos. O próprio `Agendamento` mantém campos obrigatórios (`cliente`, `estacao`, `itens`) e o `ContaAtendimento` recebe o mesmo `Agendamento`, reforçando a associação (ver `Agendamento.java` e `ContaAtendimento.java`).

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java` (linhas 85-104)
```java
        Cliente clientePrincipal = new Cliente(UUID.randomUUID(), "Carlos Cliente", enderecoPadrao,
                telefoneCliente, emailCliente, CpfHash.fromMasked("123.456.789-09"), true);
        Usuario admin = new Usuario(UUID.randomUUID(), "Ana Admin", enderecoPadrao,
                telefoneAdmin, emailAdmin, Papel.ADMIN, "ana.admin", "hash-admin", true);
        Usuario colaborador = new Usuario(UUID.randomUUID(), "Caio Colaborador", enderecoPadrao,
                telefoneColaborador, emailColaborador, Papel.COLABORADOR, "caio.colab", "hash-colab", true);

        sistema.cadastrarCliente(clientePrincipal);

        Servico servicoCorte = new Servico(UUID.randomUUID(), "Corte Premium",
                Dinheiro.of(new BigDecimal("70.00"), BRL), 45, true);
        Servico servicoBarba = new Servico(UUID.randomUUID(), "Barba Express",
                Dinheiro.of(new BigDecimal("40.00"), BRL), 30, false);
        sistema.cadastrarServico(servicoCorte);
        sistema.cadastrarServico(servicoBarba);

        LocalDateTime inicioAtendimento = LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0);
        LocalDateTime fimAtendimento = inicioAtendimento.plusMinutes(75);
        Agendamento agendamentoPrincipal = sistema.criarAgendamento(UUID.randomUUID(), clientePrincipal,
                Estacao.ESTACOES[0], inicioAtendimento, fimAtendimento,
```

**Como testar:** `mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.EntregaFinalMain` e acompanhar o bloco “Questao 1”.

**Resultado esperado:** Console exibindo `Questao 1: OK - Relacionamentos principais: cliente->OS=<id>, conta->OS=<id>...`.

**Observações/Decisões de design:** `Agendamento` força período válido e itens obrigatórios antes de permitir faturamento (`Agendamento.java`, linhas 62-163), o que simplifica validações posteriores.

**Status final:** OK.

## Q2 — Papéis (ADMIN/COLABORADOR/BARBEIRO)
**Enunciado (resumo):** Restringir operações sensíveis a papéis específicos, demonstrando bloqueio para colaborador e sucesso para admin.

**Implementação no projeto:** `Sistema.assertAdmin` e `Sistema.assertColaboradorOuAdmin` validam o `Papel` antes de executar cada método. `EntregaFinalMain` chama `emitirRelatorioFinanceiro` com um colaborador para gerar a exceção e, em seguida, com um admin para confirmar o acesso.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 1003-1015)
```java
    private void assertAdmin(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario solicitante não pode ser nulo");
        if (usuario.getPapel() != Papel.ADMIN) {
            throw new PermissaoNegadaException("Operação permitida apenas para administradores");
        }
    }

    private void assertColaboradorOuAdmin(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario solicitante não pode ser nulo");
        Papel papel = usuario.getPapel();
        if (papel != Papel.ADMIN && papel != Papel.COLABORADOR) {
            throw new PermissaoNegadaException("Operação permitida apenas para administradores ou colaboradores");
        }
    }
```

**Como testar:** Executar `EntregaFinalMain`; o bloco “Questao 2” imprime a mensagem de bloqueio para o colaborador e a primeira linha do relatório financeiro para o admin.

**Resultado esperado:** `Questao 2: OK - Colaborador bloqueado: ... | Admin OK: Relatório Financeiro ...` no console.

**Observações/Decisões de design:** A centralização das verificações evita duplicação e facilita ampliar regras (ex.: adicionar BARBEIRO em métodos específicos). Exceções usam `PermissaoNegadaException` para diferenciar erro funcional de erro técnico.

**Status final:** OK.

## Q3 — toString() em todas as classes concretas
**Enunciado (resumo):** Garantir que classes de domínio imprimam descrições ricas para facilitar auditoria (Cliente, Serviço, Agendamento, Conta, etc.).

**Implementação no projeto:** Cada classe concreta sobrescreve `toString()` com os principais campos. A Questão 3 concatena cinco exemplos e imprime no console.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/model/Cliente.java` (linhas 125-136)
```java
    @Override
    public String toString() {
        return "Cliente{"
                + "cpf=" + cpf
                + ", ativo=" + ativo
                + ", extratosGerados=" + extratosGerados.size()
                + ", totalVeiculosProtegido=" + totalVeiculosProtegido
                + ", totalServicosProtegido=" + totalServicosProtegido
                + ", pessoa=" + super.toString()
                + '}';
    }
```

**Como testar:** Ver saída da Questão 3; todos os objetos criados no início do `main` possuem `toString()` específicos.

**Resultado esperado:** Impressão sequencial dos textos “Cliente: ...”, “Servico1: ...”, “Agendamento: ...” etc.

**Observações/Decisões de design:** Além de facilitar logs, os `toString()` listam contadores e referências internas, ajudando a validar persistência.

**Status final:** OK.

## Q4 — Uso de super(...) em subclasses
**Enunciado (resumo):** Subclasses de `Pessoa` (Cliente e Usuario) devem chamar `super(...)` passando nome, endereço, telefone e e-mail.

**Implementação no projeto:** Ambos construtores invocam o construtor protegido de `Pessoa`, garantindo consistência dos campos herdados. A Questão 4 valida que os dados foram propagados.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/model/Cliente.java` (linhas 58-66)
```java
    public Cliente(UUID id, String nome, Endereco endereco, Telefone telefone, Email email,
                   CpfHash cpf, boolean ativo) {
        super(id, nome, endereco, telefone, email);
        this.cpf = Objects.requireNonNull(cpf, "cpf não pode ser nulo");
        this.ativo = ativo;
    }
```
- `src/main/java/br/ufvjm/barbearia/model/Usuario.java` (linhas 43-58)
```java
    public Usuario(UUID id, String nome, Endereco endereco, Telefone telefone, Email email,
                   Papel papel, String login, String senhaHash, boolean ativo) {
        super(id, nome, endereco, telefone, email);
        this.papel = Objects.requireNonNull(papel, "papel não pode ser nulo");
        this.login = Objects.requireNonNull(login, "login não pode ser nulo");
        this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash não pode ser nulo");
        this.ativo = ativo;
    }
```

**Como testar:** Questão 4 imprime se os campos herdados batem com o endereço/telefone/e-mail cadastrados no início do cenário.

**Resultado esperado:** Mensagem `Cliente campos herdados OK=true, Usuario campos herdados OK=true`.

**Observações/Decisões de design:** A herança reduz duplicação de validações (null-checks) e permite evoluir dados de contato num só lugar (`Pessoa`).

**Status final:** OK.

## Q5 — Vetor estático com 3 estações
**Enunciado (resumo):** Manter vetor imutável com três estações físicas; a primeira precisa oferecer lavagem.

**Implementação no projeto:** `Estacao` expõe `public static final Estacao[] ESTACOES` preenchido em bloco estático. A Questão 5 confirma o tamanho e a propriedade `possuiLavagem` da primeira entrada.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/model/Estacao.java` (linhas 20-35)
```java
    public static final Estacao[] ESTACOES = new Estacao[3];

    static {
        ESTACOES[0] = new Estacao(1, true);
        ESTACOES[1] = new Estacao(2, false);
        ESTACOES[2] = new Estacao(3, false);
    }
```

**Como testar:** Questão 5 imprime `Total estações=3, primeira possui lavagem=true` quando `EntregaFinalMain` é executado.

**Resultado esperado:** Mensagem confirmando comprimento e requisito da primeira estação.

**Observações/Decisões de design:** Ao centralizar em um array, as reservas utilizam sempre posições válidas e fica simples expandir (ex.: se a faculdade exigir 4 estações no futuro).

**Status final:** OK.

## Q6 — CRUD de colaboradores com permissões
**Enunciado (resumo):** Somente ADMIN pode cadastrar/editar/remover `Usuario`; é preciso demonstrar o fluxo completo.

**Implementação no projeto:** `Sistema.cadastrarUsuario`, `editarUsuario` e `removerUsuario` invocam `assertAdmin`. A Questão 6 cria uma usuária, edita seus dados e a remove, medindo o total via relatório operacional.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 244-267)
```java
    public void cadastrarUsuario(Usuario solicitante, Usuario novoUsuario) {
        assertAdmin(solicitante);
        usuarios.add(Objects.requireNonNull(novoUsuario, "usuario não pode ser nulo"));
    }

    public void editarUsuario(Usuario solicitante, UUID id, Usuario novo) {
        assertAdmin(solicitante);
        Objects.requireNonNull(id, "id não pode ser nulo");
        Usuario usuarioAtualizado = Objects.requireNonNull(novo, "novo não pode ser nulo");
        if (!usuarioAtualizado.getId().equals(id)) {
            throw new IllegalArgumentException("ID do usuário não corresponde ao registro atualizado");
        }
        substituirUsuario(id, usuarioAtualizado);
    }

    public void removerUsuario(Usuario solicitante, UUID id) {
        assertAdmin(solicitante);
        Objects.requireNonNull(id, "id não pode ser nulo");
        boolean removido = usuarios.removeIf(u -> u.getId().equals(id));
        if (!removido) {
            throw new IllegalArgumentException("Usuário não encontrado: " + id);
        }
    }
```

**Como testar:** Questão 6 executa automaticamente esses três métodos e imprime as contagens antes/depois.

**Resultado esperado:** Texto `Usuários cadastrados -> após cadastro=X, após edição=X, após remoção=Y` demonstrando o CRUD.

**Observações/Decisões de design:** IDs são verificados no método de edição para evitar substituições acidentais caso o DTO chegue com outro identificador.

**Status final:** OK.

## Q7 — CRUD de clientes
**Enunciado (resumo):** Implementar cadastro, edição e remoção de clientes, mantendo validações básicas.

**Implementação no projeto:** `Sistema` expõe `cadastrarCliente`, `editarCliente` (valida id) e `removerCliente`. A Questão 7 percorre o fluxo completo e lista os registros ordenados para conferir as mudanças.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 209-228)
```java
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
        Objects.requireNonNull(id, "id não pode ser nulo");
        boolean removido = clientes.removeIf(c -> c.getId().equals(id));
        if (!removido) {
            throw new IllegalArgumentException("Cliente não encontrado: " + id);
        }
    }
```

**Como testar:** Questão 7 imprime `Clientes -> após cadastro=..., nome atualizado="...", após remoção=...`.

**Resultado esperado:** Mensagem confirmando atualização do nome e remoção posterior.

**Observações/Decisões de design:** O método `listarClientesOrdenados` (linha 231) permite paginação e uso de `Comparator` para consultas flexíveis.

**Status final:** OK.

## Q8 — Verificar/imprimir OS por cliente
**Enunciado (resumo):** Buscar todas as ordens de serviço de um cliente e imprimir no console.

**Implementação no projeto:** `listarOrdensDeServicoDoCliente` filtra `agendamentos` por `clienteId`; `imprimirOrdensDeServicoDoCliente` faz `forEach` imprimindo cada `toString()`. A Questão 8 combina os dois métodos.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 645-655)
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

**Como testar:** Questão 8 executa os dois métodos e imprime quantas OS foram encontradas (IDs concatenados).

**Resultado esperado:** Linha `OS do cliente Carlos Cliente: N registro(s) [...]` seguida pela listagem textual das OS.

**Observações/Decisões de design:** A busca retorna `List<Agendamento>` para permitir outras operações (exportar, imprimir, etc.).

**Status final:** OK.

## Q9 — Estruturas dinâmicas para entidades
**Enunciado (resumo):** Usar coleções dinâmicas (listas/deques) em vez de arrays fixos para clientes, usuários, agendamentos etc., além de pilha LIFO para fila secundária.

**Implementação no projeto:** `Sistema` mantém `ArrayList` para cada entidade e um `ArrayDeque` como fila secundária. Métodos `adicionarAgendamentoSecundario`, `inspecionarFilaSecundaria` e `recuperarAgendamentoSecundario` manipulam a pilha. Questão 9 cria uma OS, envia para a fila, faz `peek/pop` e relata o estado.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 194-208)
```java
    private List<Cliente> clientes = new ArrayList<>();
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Servico> servicos = new ArrayList<>();
    private List<Produto> produtos = new ArrayList<>();
    private List<Agendamento> agendamentos = new ArrayList<>();
    private List<Venda> vendas = new ArrayList<>();
    private List<ContaAtendimento> contas = new ArrayList<>();
    private List<Despesa> despesas = new ArrayList<>();
    private List<RecebimentoFornecedor> recebimentos = new ArrayList<>();
    private List<CaixaDiario> caixas = new ArrayList<>();

    private Deque<Agendamento> filaSecundaria = new ArrayDeque<>();
```

**Como testar:** Questão 9 mostra `topo antes pop=<id>`, `recuperado=<id>`, `fila vazia após pop=true`.

**Resultado esperado:** Confirmação textual de que `peek` enxergou o elemento antes do `pop` e que, após o `pop`, a fila ficou vazia.

**Observações/Decisões de design:** O `Deque` permite expansão futura para políticas FIFO (bastaria usar `addLast/pollFirst`).

**Status final:** OK.

## Q10 — Extratos automáticos (serviço, venda, cancelamento)
**Enunciado (resumo):** Gerar extratos automáticos em disco ao fechar contas, registrar vendas e cancelar OS (retenção 35%).

**Implementação no projeto:** `Sistema.gerarExtratoServico`, `gerarExtratoVenda` e `gerarExtratoCancelamento` montam strings ricas e usam `ExtratoIO.saveExtrato` (com `try-with-resources`) para salvar em `data/extratos`. A retenção de 35% é aplicada em `cancelarAgendamento` (constante `RETENCAO_CANCELAMENTO`). Questão 10 limpa o diretório, provoca fechamento, venda e cancelamento e verifica se os arquivos existem.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 732-749)
```java
        String extrato = new StringBuilder()
                .append("Extrato de Serviço\n")
                .append("OS: ").append(ag.getId()).append('\n')
                .append("Cliente: ").append(nomeCliente).append('\n')
                .append("Barbeiro: ").append(nomeBarbeiro).append('\n')
                .append("Total serviços: ").append(ag.totalServicos()).append('\n')
                .append("Total conta: ").append(totalConta).append('\n')
                .append("Forma de pagamento: ").append(formaPagamentoTexto)
                .toString();

        try {
            Path arquivo = ExtratoIO.saveExtrato(cliente, extrato, EXTRATOS_DIR);
            conta.marcarExtratoServicoGerado(LocalDateTime.now(), arquivo.toString());
            if (cliente != null) {
                cliente.registrarExtrato(arquivo.toString());
            }
            Log.info("Extrato de serviço gerado em %s para %s", arquivo.toAbsolutePath(), nomeCliente);
        } catch (IOException e) {
            Log.error("Falha ao gerar extrato de serviço", e);
            throw new UncheckedIOException("Falha ao gerar extrato de serviço", e);
        }
```

**Como testar:** Rodar Questão 10; o método imprime `Extratos gerados: [...] | serviço=true, venda=true, cancelamento=true`.

**Resultado esperado:** Arquivos `.txt` criados dentro de `data/extratos`, um para cada evento, e paths registrados em `Cliente.registrarExtrato`.

**Observações/Decisões de design:** O mesmo diretório é reaproveitado por Questão 18; `limparDiretorio` garante estado previsível antes dos testes.

**Status final:** OK.

## Q11 — Dois contadores estáticos de Serviço
**Enunciado (resumo):** Manter dois contadores de serviços criados: um encapsulado no `Sistema` (private static + getters) e outro `protected` exposto via `Cliente` para demonstrar prós/contras.

**Implementação no projeto:** O construtor de `Servico` chama `Sistema.ServicoTracker.registrarCriacaoServico()` (encapsulado) e `Cliente.incrementarTotalServicosProtegido()` (protected). O primeiro só pode ser manipulado via `Sistema` (com `setTotalServicos` usado na reidratação), enquanto o segundo fica exposto para subclasses/mesmo pacote. Questão 11 imprime ambos os totais.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/model/Servico.java` (linhas 44-59)
```java
    public Servico(UUID id, String nome, Dinheiro preco, int duracaoMin, boolean requerLavagem) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.nome = validarNome(nome);
        this.preco = Objects.requireNonNull(preco, "preço não pode ser nulo");
        if (duracaoMin <= 0) {
            throw new IllegalArgumentException("duração deve ser positiva");
        }
        this.duracaoMin = duracaoMin;
        this.requerLavagem = requerLavagem;

        Sistema.ServicoTracker.registrarCriacaoServico();
        Cliente.incrementarTotalServicosProtegido();
    }
```

**Como testar:** Questão 11 imprime `Total Serviços (encapsulado)=X, (protegido)=X`.

**Resultado esperado:** Valores idênticos, demonstrando que ambas estratégias acompanham o catálogo.

**Observações/Decisões de design:** O contador protegido é mais suscetível a alterações indevidas (qualquer classe no pacote `model` poderia incrementá-lo), enquanto o encapsulado exige passar pelo `Sistema`, permitindo validações e logging.

**Status final:** OK.

## Q12 — Método estático para total de OS
**Enunciado (resumo):** Disponibilizar método estático para obter o total de OS e garantir incremento único em toda criação/promoção.

**Implementação no projeto:** `Sistema.realizarAgendamento` delega para `registrarAgendamento`, que incrementa o contador estático `totalOrdensServico` via `incrementarTotalOS()`. Questão 12 compara o contador com `agendamentos.size()`.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 525-543)
```java
    public void realizarAgendamento(Agendamento ag) {
        registrarAgendamento(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
    }

    private void registrarAgendamento(Agendamento ag) {
        agendamentos.add(ag);
        incrementarTotalOS();
        String clienteNome = ag.getCliente() != null ? ag.getCliente().getNome() : "(sem cliente)";
        Log.info("Agendamento registrado: %s para %s", ag.getId(), clienteNome);
    }
```

**Como testar:** Questão 12 imprime `Total OS contador=<n>, lista=<n>`.

**Resultado esperado:** Os valores devem coincidir, comprovando que promoções da fila secundária (também via `realizarAgendamento`) não quebram a contagem.

**Observações/Decisões de design:** Métodos `incrementarTotalOS` e `redefinirTotalOrdensServico` são `synchronized` para suportar cenários multi-thread (ex.: UI com threads). Reidratação chama `redefinirTotalOrdensServico` com o tamanho atual.

**Status final:** OK.

## Q13 — Comparator para Agendamento e Cliente
**Enunciado (resumo):** Implementar comparators específicos e utilizá-los em operações práticas (ordenação/paginação).

**Implementação no projeto:** `Sistema.listarClientesOrdenados` aceita `Comparator<Cliente>` opcional, default `ClientePorNome`. `listarAgendamentosOrdenados` faz o mesmo para `Agendamento`. Questão 13 cadastra clientes adicionais, cria OS e usa `ClientePorEmail` e `AgendamentoPorClienteNome` para ordenar.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 231-242)
```java
    public List<Cliente> listarClientesOrdenados(Comparator<Cliente> comparator, int offset, int limit) {
        Comparator<Cliente> criterio = comparator != null ? comparator : DEFAULT_CLIENTE_COMPARATOR;
        return ordenarERecortar(clientes, criterio, offset, limit);
    }
```

**Como testar:** Questão 13 imprime `Clientes ordenados por e-mail=[...] | Agendamentos por cliente=[...]`.

**Resultado esperado:** Listas ordenadas conforme o comparator escolhido (e-mail ascendente e nomes dos clientes em agendamentos).

**Observações/Decisões de design:** Os comparators (`compare/ClientePorEmail`, `compare/AgendamentoPorClienteNome`, etc.) usam `Collator` brasileiro para respeitar acentuação.

**Status final:** OK.

## Q14 — Persistência JSON com Gson + TypeAdapters
**Enunciado (resumo):** Salvar/carregar snapshots completos em JSON usando Gson, com adapters para `LocalDate`, `LocalDateTime`, `YearMonth` e `Dinheiro`, além de `try-with-resources`.

**Implementação no projeto:** `JsonStorage.createGson()` registra os adapters customizados e o método `save` usa `BufferedWriter` em bloco `try`. `Sistema.saveAll` monta um `DataSnapshot`, `Sistema.loadAll` lê o arquivo, recria `ArrayList` e reidrata contadores (`Servico.reidratarContadores`). Questão 14 salva em `target/snapshot-entrega.json`, carrega em nova instância e compara contagens.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/persist/JsonStorage.java` (linhas 60-83)
```java
    private static Gson createGson() {
        GsonBuilder builder = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .registerTypeAdapter(YearMonth.class, new YearMonthAdapter())
                .registerTypeAdapter(Dinheiro.class, new DinheiroAdapter())
                .setPrettyPrinting();
        return builder.create();
    }

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
```

**Como testar:** Questão 14 imprime `Persistência: clientes X->X, agendamentos Y->Y, ...`, confirmando a simetria.

**Resultado esperado:** Contagens idênticas antes e depois da carga, provando que o snapshot contém todos os domínios.

**Observações/Decisões de design:** `ExtratoIO` utiliza timestamp `yyyyMMddHHmmss`, garantindo nomes únicos de arquivo, e os adapters (`persist/adapters/*.java`) evitam problemas de serialização com `java.time` e `Dinheiro`.

**Status final:** OK.

## Q15 — Iterator + while + comparação com foreach
**Enunciado (resumo):** Demonstrar o uso explícito de `Iterator` com `while (hasNext())` e explicar sua equivalência ao `foreach`.

**Implementação no projeto:** Questão 15 cria uma lista temporária de clientes, percorre com `Iterator` manual (registrando índice) e depois com `foreach`, adicionando explicação textual.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java` (linhas 401-421)
```java
            Iterator<Cliente> iterator = clientesIterator.iterator();
            int indice = 0;
            while (iterator.hasNext()) {
                Cliente clienteAtual = iterator.next();
                demonstracao.append("[Iterator] cursor avança para índice ")
                        .append(indice)
                        .append(": ")
                        .append(clienteAtual.getNome())
                        .append(System.lineSeparator());
                indice++;
            }

            demonstracao.append("O Iterator mantém um cursor entre os elementos; hasNext() verifica se há próximo e next()")
                    .append(" desloca o cursor, retornando o registro atual...")
                    .append(System.lineSeparator());

            demonstracao.append("O foreach compila para o mesmo mecanismo de Iterator, mas com sintaxe mais simples:")
                    .append(System.lineSeparator());
            for (Cliente cliente : clientesIterator) {
                demonstracao.append("[foreach] Encontrado: ")
                        .append(cliente.getNome())
                        .append(System.lineSeparator());
            }
```

**Como testar:** Questão 15 imprime o texto completo, evidenciando ambos estilos de iteração.

**Resultado esperado:** Mensagem detalhada explicando o cursor do iterator, seguida pela listagem `[foreach] Encontrado: ...`.

**Observações/Decisões de design:** Comentários contextualizam o risco de modificar a lista fora do `Iterator` (pode lançar `ConcurrentModificationException`).

**Status final:** OK.

## Q16 — Comparator/sort explícito
**Enunciado (resumo):** Demonstrar duas ordenações distintas usando `Collections.sort(lista, comparator)` para clientes e agendamentos.

**Implementação no projeto:** Questão 16 cria três clientes e três agendamentos, exibe a ordem original e aplica `ClientePorNome`, depois `ClientePorEmail`, e, para agendamentos, `AgendamentoPorInicio` seguido de `AgendamentoPorClienteNome`.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java` (linhas 448-457)
```java
            System.out.println("Clientes - ordem original: " + clientesComparators.stream()
                    .map(Cliente::getNome)
                    .collect(Collectors.joining(", ")));
            Collections.sort(clientesComparators, new ClientePorNome());
            System.out.println("Ordenado por nome: " + clientesComparators.stream()
                    .map(Cliente::getNome)
                    .collect(Collectors.joining(", ")));
            Collections.sort(clientesComparators, new ClientePorEmail());
            System.out.println("Ordenado por email: " + clientesComparators.stream()
                    .map(c -> c.getEmail().getValor())
                    .collect(Collectors.joining(", ")));
```
- `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java` (linhas 472-482)
```java
            System.out.println("Agendamentos - ordem original: " + agendamentosComparators.stream()
                    .map(a -> a.getCliente().getNome() + " @ " + a.getInicio().toLocalTime())
                    .collect(Collectors.joining(", ")));
            Collections.sort(agendamentosComparators, new AgendamentoPorInicio());
            System.out.println("Agendamentos ordenados por início: " + agendamentosComparators.stream()
                    .map(a -> a.getCliente().getNome() + " @ " + a.getInicio().toLocalTime())
                    .collect(Collectors.joining(", ")));
            Collections.sort(agendamentosComparators, new AgendamentoPorClienteNome());
            System.out.println("Agendamentos ordenados por nome do cliente: " + agendamentosComparators.stream()
                    .map(a -> a.getCliente().getNome() + " @ " + a.getInicio().toLocalTime())
                    .collect(Collectors.joining(", ")));
```

**Como testar:** Observar Questão 16; ela imprime as três sequências (original + duas ordenações) para cada lista.

**Resultado esperado:** Logs mostrando mudanças reais de ordem (clientes reorganizados alfabeticamente, agendamentos agrupados por cliente/hora).

**Observações/Decisões de design:** As ordenações usam `Collections.sort` diretamente para tornar evidente a aplicação do comparator (mesmo `List.sort` estaria disponível, mas a exigência cita `Collections.sort`).

**Status final:** OK.

## Q17 — find custom vs Collections.binarySearch
**Enunciado (resumo):** Implementar um método `find` com `Iterator` e compará-lo empiricamente com `Collections.binarySearch` numa lista previamente ordenada.

**Implementação no projeto:** `Sistema.find` percorre a lista comparando elementos via `Comparator`. Questão 17 ordena `clientesBusca` e pesquisa uma chave existente e outra inexistente com ambos os métodos, imprimindo os índices retornados.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` (linhas 108-125)
```java
    public static <T> int find(List<T> lista, T chave, Comparator<? super T> cmp) {
        Objects.requireNonNull(lista, "lista não pode ser nula");
        Objects.requireNonNull(chave, "chave não pode ser nula");
        Objects.requireNonNull(cmp, "comparador não pode ser nulo");

        int idx = 0;
        for (Iterator<T> it = lista.iterator(); it.hasNext(); idx++) {
            T atual = it.next();
            if (cmp.compare(atual, chave) == 0) {
                return idx;
            }
        }
        return -1;
    }
```

**Como testar:** Questão 17 imprime `Questao 17 - chave existente -> find=0, binarySearch=0` e `chave inexistente -> find=-1, binarySearch=-5` (exemplo), além da lista ordenada.

**Resultado esperado:** Demonstração textual de que `find` retorna -1 quando não acha, enquanto `binarySearch` retorna posição negativa conforme contrato (`-(inserção) - 1`).

**Observações/Decisões de design:** O método `find` aceita qualquer `Comparator`, podendo ser usado até para objetos que não implementam `Comparable`.

**Status final:** OK.

## Q18 — Simulação ponta-a-ponta para 10 clientes
**Enunciado (resumo):** Simular pipeline completo: cadastro de 10 clientes, criação de OS com fila secundária (peek/pop), cancelamentos com retenção 35%, promoção de fila, fechamento de contas, vendas adicionais, extratos automáticos e persistência final.

**Implementação no projeto:** Questão 18 constrói listas de clientes/serviços/produtos, agenda sete clientes na agenda principal e empilha os demais, realiza cancelamentos e promoções, registra consumo de produtos, fecha contas com formas de pagamento variadas, dispara vendas adicionais, lista extratos por cliente, salva snapshot final e reidrata o sistema. Métodos de `Sistema` (fila secundária, extratos, save/load) são reutilizados.

**Arquivos e trechos relevantes:**
- `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java` (linhas 599-618)
```java
            for (int i = 0; i < clientesPipeline.size(); i++) {
                Cliente cliente = clientesPipeline.get(i);
                Servico servicoPrincipal = servicosDisponiveis.get(i % servicosDisponiveis.size());
                LocalDateTime inicio = baseFluxo.plusHours(i);
                LocalDateTime fim = inicio.plusMinutes(servicoPrincipal.getDuracaoMin());
                Dinheiro sinal = Dinheiro.of(BigDecimal.valueOf(20 + (i * 5L)), BRL);

                Agendamento agendamento = new Agendamento(UUID.randomUUID(), cliente,
                        Estacao.ESTACOES[i % Estacao.ESTACOES.length], inicio, fim, sinal);
                agendamento.associarBarbeiro(colaborador);

                ItemDeServico itemPrincipal = new ItemDeServico(servicoPrincipal, servicoPrincipal.getPreco(),
                        servicoPrincipal.getDuracaoMin());
                agendamento.adicionarItemServico(itemPrincipal);
                if (i % 3 == 0) {
                    Servico adicional = servicosDisponiveis.get((i + 1) % servicosDisponiveis.size());
                    agendamento.adicionarItemServico(new ItemDeServico(adicional, adicional.getPreco(),
                            adicional.getDuracaoMin()));
                }
```

**Como testar:** Executar `EntregaFinalMain`; após “Questao 18: ...” o console exibe o pipeline (cadastros, envio à fila, cancelamentos, promoções, fechamentos, vendas, extratos, snapshot salvo/recarregado).

**Resultado esperado:**
- Mensagens `Cliente cadastrado (01/10): ...`, `Agendamento confirmado ...`, `Agendamento ... enviado para fila secundária.`
- Logs de `inspecionarFilaSecundaria`, `Promovido da fila secundária`, `Retenção cancelamento`.
- Totais: `Totais globais -> OS: ... | Serviços catalogados: ...`, `Reidratação concluída -> clientes=... | OS=... | Serviços=... | Extratos=...`.
- Arquivo `data/snapshot_final.json` criado.

**Observações/Decisões de design:** A simulação consome produtos via `produto.movimentarSaida` e registra `ConsumoDeProduto` dentro dos itens de serviço, garantindo que estoque e faturamento sejam afetados antes do fechamento. A fila secundária usa LIFO (push/pop) para priorizar o cliente mais recente aguardando vaga.

**Status final:** OK.

# 6. Mapa de pacotes e onde cada questão toca
- **`br.ufvjm.barbearia.system`** — Núcleo de regras (`Sistema`) e cenários (`EntregaFinalMain`, `Main`). Lida com quase todas as questões: permissões (Q2), CRUDs (Q6–Q7), fila secundária (Q9, Q18), extratos (Q10), contadores (Q11–Q12), comparators/paginadores (Q13), persistência (Q14), find (Q17). `Main` oferece um roteiro alternativo de uso completo.
- **`br.ufvjm.barbearia.model`** — Entidades do domínio: `Cliente`, `Usuario`, `Servico`, `Agendamento`, `ContaAtendimento`, `Produto`, `Venda`, `RecebimentoFornecedor`, `CaixaDiario`, `Estacao`, etc. Impactam Q1 (relações), Q3 (toString), Q4 (herança), Q5 (estações), Q10 (extratos dependem de `Conta`/`Agendamento`), Q11 (contadores), Q18 (itens de serviço/produtos).
  - `Agendamento` controla status, cancelamentos (Q10/Q18) e itens.  
  - `ContaAtendimento` agrega serviços/produtos e gera extratos (Q10).  
  - `Produto`/`Quantidade` modelam estoque usado em vendas e consumos (Q18).  
  - `Estacao` fornece recursos fixos (Q5, Q18).
- **`br.ufvjm.barbearia.persist`** — Infra de persistência (`JsonStorage`, `DataSnapshot`, `ExtratoIO`) e adapters. Atende Q10 (extratos), Q14 (snapshot) e parte final da Q18 (salvar/recarregar). `DataSnapshot` encapsula listas para JSON.
- **`br.ufvjm.barbearia.compare`** — Comparators de clientes/agendamentos usados em Q13 e Q16; também os padrões do `Sistema` (Q13) e no relatório operacional (`emitirRelatorioOperacional`).
- **`br.ufvjm.barbearia.value`** — Objetos de valor (`Dinheiro`, `Quantidade`, `CpfHash`, `Endereco`, `Telefone`, `Periodo`, `Email`). Necessários para várias regras: validações (Q1/Q4), adapters (Q14), cálculos monetários (Q10/Q18).
- **`br.ufvjm.barbearia.enums`** — Enumerados (`Papel`, `StatusAtendimento`, `FormaPagamento`, `CategoriaDespesa`, `ModoConsumoProduto`). Suportam permissões (Q2), fluxo de atendimento (Q18) e geração de extratos (Q10).
- **`br.ufvjm.barbearia.exceptions`** — `PermissaoNegadaException` diferencia violações de acesso (Q2) das demais exceções.
- **`br.ufvjm.barbearia.util`** — `Log` padroniza mensagens usadas em CRUDs, fila secundária e extratos (Q9–Q10–Q18).

# 7. Evidências de execução
- **Extratos automáticos:** Questão 10 e Questão 18 chamam `ExtratoIO.saveExtrato`, gravando arquivos como `data/extratos/extrato_<cliente>_<timestamp>.txt` e imprimindo `Extrato de serviço gerado em ...` (Sistema.java, linhas 732-749; 760-787; 796-819). O console também lista os paths (Questão 10) e, na Questão 18, cada cliente mostra `Extratos gerados para ...: <lista>`.
- **Peek da fila secundária:** `sistema.inspecionarFilaSecundaria()` loga `Inspeção fila secundária: ...` e Questão 18 complementa com `System.out.printf("Agendamento %s enviado para fila secundária.%n", ...)`, `Promovido da fila secundária: ...` (EntregaFinalMain.java, linhas 619-681).
- **Ordenações com dois comparators:** Questões 13 e 16 imprimem `Clientes - ordem original: ...`, `Ordenado por nome: ...`, `Ordenado por email: ...`, e `Agendamentos ordenados por início/...` (EntregaFinalMain.java, linhas 321-356 e 431-485).
- **Índices de find x binarySearch:** Questão 17 imprime `Questao 17 - chave existente -> find=..., binarySearch=...` e a mensagem explicativa `find percorre a lista linearmente (O(n)). Já binarySearch ...` (EntregaFinalMain.java, linhas 523-528).
- **Totais finais e estoque:** Questão 18 mostra `Totais globais -> OS: %d | Serviços catalogados: %d` e a listagem `Estoque restante ... - <produto> (<sku>): <quantidade>` (EntregaFinalMain.java, linhas 741-749). Após salvar e recarregar, aparece `Reidratação concluída -> clientes=... | OS=... | Serviços=... | Extratos=...` (linhas 763-770).

# 8. Observações finais e próximos passos
- **Cobertura de testes automatizados:** Atualmente a validação está concentrada em `EntregaFinalMain`. Adicionar testes de unidade para `Sistema` (ex.: `cancelarAgendamento`, `gerarExtratoVenda`) facilitaria regressões.
- **Logs estruturados:** O wrapper `Log` já existe; vale mapear mensagens críticas (cancelamentos, geração de extratos) para níveis distintos e, futuramente, enviar para arquivos/observabilidade.
- **Documentação adicional:** Incluir diagramas (já citados em `agentes.md`) na pasta `docs/diagramas` exportados em PDF pode auxiliar revisores visuais.
- **Internacionalização:** Strings de console/extrato estão em português fixo; parametrizar via resource bundles ajudaria deployments em outras regiões.
