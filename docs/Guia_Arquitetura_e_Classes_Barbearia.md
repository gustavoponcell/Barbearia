# Guia de Arquitetura e Classes – Sistema Barbearia

**Data de geração:** 2025-11-11

**Descrição:** Documento técnico completo do projeto Java/Maven "Barbearia", cobrindo arquitetura, pacotes, classes, fluxos de negócio, persistência, permissões, contadores e instruções de execução.

## Sumário
- [1. Capa e Sumário](#1-capa-e-sumário)
- [2. Visão Geral do Sistema](#2-visão-geral-do-sistema)
- [3. Mapa de Pacotes](#3-mapa-de-pacotes)
- [4. Referência de Classes](#4-referência-de-classes)
- [5. Fluxos de Negócio Chave](#5-fluxos-de-negócio-chave)
- [6. Permissões e Segurança](#6-permissões-e-segurança)
- [7. Ordenações e Busca](#7-ordenações-e-busca)
- [8. Persistência, Arquivos e Diretórios](#8-persistência-arquivos-e-diretórios)
- [9. Contadores Estáticos e Reidratação](#9-contadores-estáticos-e-reidratação)
- [10. Como Executar e Gerar Documentação](#10-como-executar-e-gerar-documentação)
- [11. Glossário](#11-glossário)

## 1. Capa e Sumário
(Seção atual.)

## 2. Visão Geral do Sistema
O sistema da barbearia provê uma aplicação desktop/offline para gerenciar agenda de ordens de serviço, vendas de produtos, finanças (contas, caixa diário, despesas, recebimentos), usuários com papéis e persistência completa em JSON. O núcleo (`Sistema`) mantém coleções em memória, orquestra regras de negócio e delega persistência a utilitários.

Principais módulos:
- **Agenda/OS:** `Sistema`, `Agendamento`, `Estacao`, `ContaAtendimento` controlam criação, cancelamentos e fechamento de atendimentos.
- **Estoque/Loja:** `Produto`, `Venda`, `ItemVenda`, `ConsumoDeProduto`, `ItemContaProduto` tratam movimentações e faturamento.
- **Finanças/Extratos/Caixa:** `CaixaDiario`, `Despesa`, `RecebimentoFornecedor`, geração de extratos via `ExtratoIO` e snapshots via `JsonStorage`.
- **Usuários/Papéis:** `Usuario` com enum `Papel` habilita validações de permissão em `Sistema`.
- **Persistência:** `JsonStorage`, `DataSnapshot` e adapters Gson garantem serialização de entidades e valores (`Dinheiro`, datas, períodos).

O salão possui três estações estáticas em `Estacao.ESTACOES`, sendo a primeira (índice 0) com lavagem obrigatória para serviços que exigem o recurso.

## 3. Mapa de Pacotes

### br.ufvjm.barbearia.system
- **Responsabilidade:** Orquestra regras de alto nível, coordena persistência, relatórios, fluxo de caixa e fila secundária.
- **Principais classes:** `Sistema`, `Main`, `EntregaFinalMain`.
- **Dependências internas:** Usa modelos (`model`), valores (`value`), enums, comparators, persistência (`persist`), exceções e utilitário de log. Exporta API pública para UI.

### br.ufvjm.barbearia.model
- **Responsabilidade:** Entidades de domínio (clientes, usuários, serviços, produtos, ordens, caixa, recebimentos, contas) e regras locais.
- **Principais classes:** `Pessoa`, `Cliente`, `Usuario`, `Servico`, `Agendamento`, `ContaAtendimento`, `CaixaDiario`, `Produto`, `Venda`, `RecebimentoFornecedor`, `Despesa`, `Estacao`.
- **Dependências internas:** Usa objetos de valor (`value`), enums (`enums`), e interage com `Sistema` para contadores e orquestração.

### br.ufvjm.barbearia.value
- **Responsabilidade:** Objetos imutáveis para valores: dinheiro, quantidade, CPF hash, período, email, telefone, endereço.
- **Principais classes:** `Dinheiro`, `Quantidade`, `CpfHash`, `Periodo`, `Email`, `Telefone`, `Endereco`.
- **Dependências internas:** Consumidos por `model`, `persist` e `system`.

### br.ufvjm.barbearia.enums
- **Responsabilidade:** Enumerações de papéis, status de atendimento, formas de pagamento, categorias de despesa, modo de consumo.
- **Principais enums:** `Papel`, `StatusAtendimento`, `FormaPagamento`, `CategoriaDespesa`, `ModoConsumoProduto`.
- **Dependências internas:** Usados por `model` e `system` para validações e fluxo.

### br.ufvjm.barbearia.compare
- **Responsabilidade:** Estratégias de ordenação para clientes e agendamentos.
- **Principais classes:** `ClientePorNome`, `ClientePorEmail`, `AgendamentoPorInicio`, `AgendamentoPorClienteNome`.
- **Dependências internas:** Usados por `Sistema` em relatórios e buscas ordenadas.

### br.ufvjm.barbearia.persist
- **Responsabilidade:** Persistência em disco: snapshots JSON, extratos e adapters.
- **Principais classes:** `JsonStorage`, `ExtratoIO`, `DataSnapshot`, `DinheiroAdapter`, `LocalDateAdapter`, `LocalDateTimeAdapter`, `YearMonthAdapter`.
- **Dependências internas:** Consomem `model` e `value`, utilizam Gson/NIO, devolvem DTOs para `Sistema`.

### br.ufvjm.barbearia.exceptions
- **Responsabilidade:** Exceções de domínio (ex.: permissões).
- **Principais classes:** `PermissaoNegadaException`.

### br.ufvjm.barbearia.util
- **Responsabilidade:** Utilitários de infraestrutura (logging).
- **Principais classes:** `Log`.

### Raiz (default package)
- **Responsabilidade:** Utilitário `QuickChecks` para smoke tests.


## 4. Referência de Classes

### 4.1 br.ufvjm.barbearia.system.Sistema – `src/main/java/br/ufvjm/barbearia/system/Sistema.java`
**Responsabilidade:** Núcleo da aplicação, gerenciando coleções em memória, contadores estáticos, fila secundária, fluxos financeiros, geração de extratos e persistência.

**Colabora com:** `Cliente`, `Usuario`, `Agendamento`, `ContaAtendimento`, `Venda`, `RecebimentoFornecedor`, `CaixaDiario`, `Servico`, `Produto`, `Despesa`, `JsonStorage`, `ExtratoIO`, comparators e `Log`.

**Padrões relevantes:** Service Facade, Transaction Script, Aggregator, Contador encapsulado.

**Atributos:**
| Nome | Tipo | Visibilidade | Mutabilidade | Padrão | Descrição |
|---|---|---|---|---|---|
| `totalOrdensServico` | `int` | `private static` | Mutável sincronizado | `0` | Contador global de ordens de serviço. |
| `totalServicos` | `int` | `private static` | Mutável sincronizado | `0` | Contador global de serviços criados. |
| `RETENCAO_CANCELAMENTO` | `BigDecimal` | `private static final` | Imutável | `0.35` | Percentual aplicado em cancelamentos. |
| `DATA_HORA_FORMATTER` | `DateTimeFormatter` | `private static final` | Imutável | `dd/MM/yyyy HH:mm` | Formatação de relatórios. |
| `DEFAULT_CLIENTE_COMPARATOR` | `ClientePorNome` | `private static final` | Imutável | instância padrão | Critério padrão de ordenação de clientes. |
| `DEFAULT_AGENDAMENTO_COMPARATOR` | `AgendamentoPorInicio` | `private static final` | Imutável | instância padrão | Critério padrão de ordenação de agendamentos. |
| `EXTRATOS_DIR` | `Path` | `private static final` | Imutável | `data/extratos` | Diretório alvo de extratos. |
| Coleções (`clientes`, `usuarios`, `servicos`, `produtos`, `agendamentos`, `vendas`, `contas`, `despesas`, `recebimentos`, `caixas`) | `List<...>` | `private` | Mutável | novas `ArrayList` | Estruturas em memória para cada agregado. |
| `filaSecundaria` | `Deque<Agendamento>` | `private` | Mutável | `ArrayDeque` | Pilha LIFO de espera. |

**Métodos (principais):**
| Assinatura | Parâmetros | Retorno | Lança | Efeitos colaterais | Regra/Permissão |
|---|---|---|---|---|---|
| `static <T> int find(List<T>, T, Comparator<? super T>)` | `lista`, `chave`, `cmp` | `int` | `NullPointerException` | Iteração linear | — |
| `static synchronized void incrementarTotalOS()` | — | `void` | — | ++ contador estático | — |
| `static synchronized int getTotalOrdensServicoCriadas()` | — | `int` | — | — | — |
| `static synchronized int getTotalServicos()` | — | `int` | — | — | — |
| `static synchronized void setTotalServicos(int)` | `total` | `void` | — | Atualiza contador | Reidratação |
| `static synchronized void redefinirTotalOrdensServico(int)` | `total` | `void` | — | Atualiza contador | Visível ao pacote |
| `cadastrarCliente(Cliente)` / `editarCliente(UUID, Cliente)` / `removerCliente(UUID)` | `cliente`/`id` | `void` | `IllegalArgumentException` | Mantém lista de clientes | — |
| `listarClientesOrdenados(...)` | `offset`, `limit`, `comparator` | `List<Cliente>` | — | Gera cópia ordenada | — |
| `cadastrarUsuario(Usuario solicitante, Usuario novo)` | `solicitante`, `novo` | `void` | `PermissaoNegadaException` | Adiciona usuário | Apenas ADMIN |
| `editarUsuario(...)`, `removerUsuario(...)` | `solicitante`, `id`, `novo` | `void` | `PermissaoNegadaException` | Substitui/ remove | ADMIN |
| `registrarDespesa(Usuario, Despesa)` / `listarDespesas` / `removerDespesa` | — | `void` / `List<Despesa>` | `PermissaoNegadaException` | Atualiza lista | ADMIN |
| `calcularBalancoMensal(Usuario, YearMonth, Currency)` | `solicitante`, `competencia`, `moeda` | `Dinheiro` | `PermissaoNegadaException` | Agrega receitas/despesas | ADMIN |
| `emitirRelatorioFinanceiro` / `emitirRelatorioOperacional` | conforme assinatura | `String` | — | Monta texto | Operacional livre; financeiro ADMIN |
| `cadastrarServico(Servico)` / `listarServicos()` | `servico` | `void` / `List<Servico>` | — | Adiciona | — |
| `cadastrarProduto(Produto)` / `listarProdutos()` | — | `void` / `List<Produto>` | — | — | — |
| `registrarVenda(Usuario, Venda)` | `solicitante`, `venda` | `void` | `PermissaoNegadaException` | Adiciona, gera extrato | ADMIN/COLABORADOR |
| `listarVendas(Usuario)` | `solicitante` | `List<Venda>` | `PermissaoNegadaException` | — | ADMIN |
| `criarContaAtendimento(Agendamento)` | `agendamento` | `ContaAtendimento` | — | Cria conta | — |
| `registrarConta` / `atualizarConta` / `removerConta` | — | `void` | `IllegalArgumentException` | Mantém lista | — |
| `criarAgendamento(...)` / `realizarAgendamento(Agendamento)` | dados da OS | `Agendamento` / `void` | — | Incrementa contador, log | — |
| `listarAgendamentosOrdenados(...)` | `offset`, `limit`, `comparator` | `List<Agendamento>` | — | Ordena cópia | — |
| `adicionarAgendamentoSecundario` / `inspecionarFilaSecundaria` / `recuperarAgendamentoSecundario` | `ag` | `void` / `Optional<Agendamento>` / `Agendamento` | `NoSuchElementException` | Manipula `Deque` | — |
| `cancelarAgendamento(Usuario, UUID)` | `solicitante`, `id` | `Agendamento.Cancelamento` | `PermissaoNegadaException` | Atualiza conta, caixa, extrato | ADMIN/COLABORADOR |
| `listarOrdensDeServicoDoCliente` / `imprimirOrdensDeServicoDoCliente` | `clienteId` | `List<Agendamento>` / `void` | — | Filtro / saída console | — |
| `registrarRecebimentoFornecedor(...)` (duas assinaturas) | `solicitante`, `recebimento`, `pagamento?`, `dataPagamento?` | `void` | `PermissaoNegadaException` | Atualiza estoque, caixa | ADMIN |
| `atualizarRecebimentoFornecedor` / `removerRecebimentoFornecedor` / `listarRecebimentos` | — | — / `List<RecebimentoFornecedor>` | `PermissaoNegadaException` | Mantém lista | ADMIN |
| `gerarExtratoServico(ContaAtendimento)` | `conta` | `void` | `UncheckedIOException` | Grava arquivo, marca extrato | — |
| `gerarExtratoVenda(Venda)` | `venda` | `void` | `UncheckedIOException` | Grava arquivo, marca venda | — |
| `saveAll(Usuario, Path)` | `solicitante`, `path` | `void` | `PermissaoNegadaException`, `UncheckedIOException` | Persiste snapshot JSON | ADMIN |
| `loadAll(Path)` | `path` | `void` | `UncheckedIOException` | Carrega snapshot, reidrata contadores | — |
| `abrirCaixa(LocalDate, Dinheiro)` | `data`, `saldo` | `CaixaDiario` | — | Cria caixa diário | — |
| `toString()` | — | `String` | — | — | — |
| Métodos auxiliares (`obterOuCriarCaixa`, `substituirCliente`, `assertAdmin`, etc.) | diversos | diversos | `IllegalArgumentException` | Atualizam coleções, validam estado | Implementam invariantes |

**Relações:** Agrega listas de entidades, coordena `JsonStorage`/`ExtratoIO`, valida permissões com `Usuario.getPapel()`, controla fila secundária (`Deque`).

**Invariantes/Validações:** IDs conferidos na edição; permissões para operações sensíveis; moedas coerentes nos balanços; nenhuma referência nula é aceita.

**Persistência:** Serializa todas as coleções via `DataSnapshot` e `JsonStorage`. Extratos gravados em `data/extratos` via `ExtratoIO`.

**toString()/equals()/hashCode():** `toString` resume contagens agregadas; equals/hashCode padrão (`Object`).

**Exemplo de uso:**
```java
Sistema sistema = new Sistema();
Usuario admin = criarAdmin();
Cliente cliente = criarCliente();
sistema.cadastrarCliente(cliente);
Agendamento ag = sistema.criarAgendamento(UUID.randomUUID(), cliente,
        Estacao.ESTACOES[0], inicio, fim, sinal);
ContaAtendimento conta = sistema.criarContaAtendimento(ag);
conta.calcularTotal(ag.totalServicos());
sistema.gerarExtratoServico(conta);
```

#### 4.1.1 br.ufvjm.barbearia.system.Sistema.ServicoTracker
**Responsabilidade:** Canal controlado para incrementar o contador encapsulado de serviços.

**Métodos:** `registrarCriacaoServico()` (incrementa contador estático); construtor privado implicito.

**Exemplo:**
```java
// Invocado indiretamente pelo construtor de Servico
Sistema.ServicoTracker.registrarCriacaoServico();
```

### 4.2 br.ufvjm.barbearia.system.Main – `src/main/java/br/ufvjm/barbearia/system/Main.java`
**Responsabilidade:** Roteiro de demonstração completo dos principais casos de uso.

**Métodos:**
| Assinatura | Parâmetros | Retorno | Lança | Efeitos | Regra |
|---|---|---|---|---|---|
| `main(String[] args)` | `args:String[]` | `void` | `UncheckedIOException` | Cria dados, gera extratos/snapshot, imprime resultados | Script didático |

**Exemplo:**
```java
public static void main(String[] args) {
    Sistema sistema = new Sistema();
    Usuario admin = criarAdminDemo();
    sistema.cadastrarUsuario(admin, admin);
    System.out.println(sistema.emitirRelatorioOperacional(admin));
}
```

### 4.3 br.ufvjm.barbearia.system.EntregaFinalMain – `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java`
**Responsabilidade:** Executar as 18 verificações da entrega final, demonstrando regras e evidências.

**Atributos:**
| Nome | Tipo | Visibilidade | Mutabilidade | Padrão | Descrição |
|---|---|---|---|---|---|
| `BRL` | `Currency` | `private static final` | Imutável | `Currency.getInstance("BRL")` | Moeda padrão das verificações. |

**Métodos principais:** `main(String[] args)`, `executarQuestao(int, Supplier<String>)`, `extrairContagem(String, String)`, além de auxiliares privados para listar arquivos e formatar saídas.

**Exemplo:**
```java
public static void main(String[] args) {
    Sistema sistema = new Sistema();
    Usuario admin = criarAdmin();
    executarQuestao(2, () -> sistema.emitirRelatorioFinanceiro(admin, YearMonth.now(), BRL));
}
```

### 4.4 br.ufvjm.barbearia.model.Pessoa – `src/main/java/br/ufvjm/barbearia/model/Pessoa.java`
**Responsabilidade:** Superclasse para entidades pessoais (clientes e usuários) com identidade, contato e endereço.

**Atributos:**
| Nome | Tipo | Visibilidade | Mutabilidade | Padrão | Descrição |
|---|---|---|---|---|---|
| `id` | `UUID` | `private final` | Imutável | construtor | Identificador único. |
| `nome` | `String` | `private` | Mutável (protected) | construtor | Nome completo. |
| `endereco` | `Endereco` | `private` | Mutável (protected) | construtor | Endereço principal. |
| `telefone` | `Telefone` | `private` | Mutável (protected) | construtor | Telefone principal. |
| `email` | `Email` | `private` | Mutável (protected) | construtor | E-mail válido. |

**Métodos:** getters, setters protegidos (`setNome`, `setEndereco`, `setTelefone`, `setEmail`), `equals`/`hashCode` por `id`, `toString()` detalhado.

**Exemplo:**
```java
UUID id = UUID.randomUUID();
Endereco endereco = Endereco.builder().logradouro("Rua A").numero("10").bairro("Centro")
        .cidade("Diamantina").estado("MG").cep("39100000").build();
Telefone telefone = Telefone.of("38 3531-0000");
Email email = Email.of("contato@dominio.com");
// Utilizada via subclasses Cliente ou Usuario
```

### 4.5 br.ufvjm.barbearia.model.Cliente – `src/main/java/br/ufvjm/barbearia/model/Cliente.java`
**Responsabilidade:** Entidade de cliente final com CPF mascarado, estado ativo e histórico de extratos. Também expõe contador protegido exigido pelo projeto.

**Atributos:**
| Nome | Tipo | Visibilidade | Mutabilidade | Padrão | Descrição |
|---|---|---|---|---|---|
| `totalVeiculosProtegido` | `int` | `protected static` | Mutável | `0` | Contador didático de veículos. |
| `totalServicosProtegido` | `int` | `protected static` | Mutável | `0` | Contador protegido de serviços. |
| `cpf` | `CpfHash` | `private final` | Imutável | construtor | CPF armazenado via hash/máscara. |
| `extratosGerados` | `List<String>` | `private final` | Mutável (conteúdo) | lista vazia | Referências de extratos emitidos. |
| `ativo` | `boolean` | `private` | Mutável | construtor | Estado de ativação. |

**Métodos:** `incrementarTotalServicosProtegido()`, `redefinirTotalServicosProtegido(int)`, `getTotalServicosProtegido()`, getters (`getCpf`, `isAtivo`, `getExtratosGerados`), comandos (`desativar`, `reativar`, `registrarExtrato`, `atualizarContato`), `toString()`.

**Exemplo:**
```java
Cliente cliente = new Cliente(id, "Ana", endereco, telefone,
        Email.of("ana@dominio.com"), CpfHash.fromMasked("123.456.789-09"), true);
cliente.registrarExtrato("data/extratos/extrato_ana.txt");
```

### 4.6 br.ufvjm.barbearia.model.Usuario – `src/main/java/br/ufvjm/barbearia/model/Usuario.java`
**Responsabilidade:** Representa usuário interno com papel, login imutável e senha hash.

**Atributos:** `papel` (`Papel`), `login` (`String` final), `senhaHash` (`String`), `ativo` (`boolean`).

**Métodos:** getters, `setPapel`, `alterarSenha`, `desativar`, `reativar`, `toString()`.

**Exemplo:**
```java
Usuario admin = new Usuario(UUID.randomUUID(), "Carlos", endereco, telefone,
        Email.of("admin@barbearia.com"), Papel.ADMIN, "carlos", "hashSeguro", true);
```

### 4.7 br.ufvjm.barbearia.model.Servico – `src/main/java/br/ufvjm/barbearia/model/Servico.java`
**Responsabilidade:** Item de catálogo de serviços com preço, duração e indicador de lavagem. Incrementa contadores encapsulado e protegido.

**Atributos:** `id` (`UUID`), `nome` (`String` validado), `preco` (`Dinheiro`), `duracaoMin` (`int`>0), `requerLavagem` (`boolean`).

**Métodos:** getters, `toString()`, `reidratarContadores(Iterable<? extends Servico>)` para sincronizar contadores, validações no construtor.

**Exemplo:**
```java
Servico corte = new Servico(UUID.randomUUID(), "Corte", Dinheiro.of(new BigDecimal("40"), brl), 30, false);
boolean precisaLavagem = corte.isRequerLavagem();
```

### 4.8 br.ufvjm.barbearia.model.Agendamento – `src/main/java/br/ufvjm/barbearia/model/Agendamento.java`
**Responsabilidade:** Ordem de serviço agendada com cliente, barbeiro, estação, itens, sinal e status controlado.

**Atributos:** `id`, `cliente`, `barbeiro`, `estacao`, `inicio`, `fim`, `itens`, `status`, `sinal`, `extratoCancelamentoGeradoEm`, `referenciaExtratoCancelamento`.

**Métodos:** `adicionarItemServico`, `associarBarbeiro`, `alterarStatus`, `cancelar(BigDecimal)`, `totalServicos`, `periodo`, `requerLavagem`, getters, métodos de extrato, `toString()`.

**Exemplo:**
```java
Agendamento ag = new Agendamento(UUID.randomUUID(), cliente, Estacao.ESTACOES[0], inicio, fim, sinal);
ag.adicionarItemServico(new ItemDeServico(servico, servico.getPreco(), servico.getDuracaoMin()));
ag.associarBarbeiro(barbeiro);
```

#### 4.8.1 Agendamento.Cancelamento
DTO com `percentualRetencao`, `totalServicos`, `valorRetencao`, `valorReembolso`; apenas getters e `toString()`.

### 4.9 br.ufvjm.barbearia.model.ItemDeServico – `src/main/java/br/ufvjm/barbearia/model/ItemDeServico.java`
**Responsabilidade:** Representa serviço escolhido com preço/duração efetivos e consumos de produto.

**Atributos:** `servico`, `preco`, `duracaoMin`, `consumos`.

**Métodos:** getters, `atualizarPreco`, `atualizarDuracao`, `registrarConsumo`, `subtotal`, `toString()`.

**Exemplo:**
```java
ItemDeServico item = new ItemDeServico(servico, servico.getPreco(), servico.getDuracaoMin());
item.registrarConsumo(new ConsumoDeProduto(produto, Quantidade.of(BigDecimal.ONE, "un"), ModoConsumoProduto.FATURADO));
```

### 4.10 br.ufvjm.barbearia.model.ConsumoDeProduto – `src/main/java/br/ufvjm/barbearia/model/ConsumoDeProduto.java`
**Responsabilidade:** Consumo de produto em atendimento, indicando quantidade e modo.

**Atributos:** `produto`, `quantidade`, `modo` (todos finais). Métodos: getters, `toString()`.

**Exemplo:**
```java
ConsumoDeProduto consumo = new ConsumoDeProduto(produto, Quantidade.of(new BigDecimal("0.5"), "fr"), ModoConsumoProduto.CONSUMO_INTERNO);
```

### 4.11 br.ufvjm.barbearia.model.ContaAtendimento – `src/main/java/br/ufvjm/barbearia/model/ContaAtendimento.java`
**Responsabilidade:** Controla faturamento de uma OS, somando serviços/produtos, ajustes, descontos, pagamentos e extratos.

**Atributos:** `id`, `agendamento`, `produtosFaturados`, `servicosAdicionais`, `ajustes`, `desconto`, `total`, `formaPagamento`, `cancelamentoRegistro`, `fechada`, `extratoServicoGeradoEm`, `referenciaExtratoServico`.

**Métodos:** adicionar itens (`adicionarProdutoFaturado`, `adicionarServicoFaturado`), ajustes (`registrarAjuste`, `registrarRetencaoCancelamento`), desconto (`aplicarDesconto`), cálculo (`calcularTotal(Dinheiro)` / `calcularTotal()`), liquidação (`liquidar`, `fecharConta`), getters, extrato (`marcarExtratoServicoGerado`), `toString()`.

**Exemplo:**
```java
ContaAtendimento conta = new ContaAtendimento(UUID.randomUUID(), agendamento);
conta.adicionarProdutoFaturado(new ItemContaProduto(produto, quantidade, produto.getPrecoVenda()));
conta.calcularTotal(agendamento.totalServicos());
conta.fecharConta(FormaPagamento.CARTAO_DEBITO);
```

##### 4.11.1 ContaAtendimento.AjusteConta
Classe estática com `Tipo` (CREDITO/DEBITO), `descricao`, `valor`; métodos fábrica `credito`/`debito`, getters, `toString()`.

##### 4.11.2 ContaAtendimento.CancelamentoRegistro
DTO com `percentualRetencao`, `valorRetencao`, `valorReembolso`, `totalServicos`; getters e `toString()`.

### 4.12 br.ufvjm.barbearia.model.ItemContaProduto – `src/main/java/br/ufvjm/barbearia/model/ItemContaProduto.java`
**Responsabilidade:** Produto faturado em conta de atendimento.

**Atributos:** `produto`, `quantidade`, `precoUnitario` (finais). Métodos: getters, `subtotal`, `toString()`.

**Exemplo:**
```java
ItemContaProduto item = new ItemContaProduto(produto, Quantidade.of(new BigDecimal("2"), "un"), produto.getPrecoVenda());
```

### 4.13 br.ufvjm.barbearia.model.ItemRecebimento – `src/main/java/br/ufvjm/barbearia/model/ItemRecebimento.java`
**Responsabilidade:** Item de nota de fornecedor com quantidade e custo unitário.

**Métodos:** getters, `subtotal()`, `toString()`.

**Exemplo:**
```java
ItemRecebimento item = new ItemRecebimento(produto, Quantidade.of(new BigDecimal("10"), "un"), Dinheiro.of(new BigDecimal("12"), brl));
```

### 4.14 br.ufvjm.barbearia.model.Produto – `src/main/java/br/ufvjm/barbearia/model/Produto.java`
**Responsabilidade:** Produto do estoque/PDV com controle de unidades, preço e custo médio.

**Atributos:** `id`, `nome`, `sku`, `estoqueAtual`, `estoqueMinimo`, `precoVenda`, `custoMedio`.

**Métodos:** validações no construtor, getters, `movimentarEntrada`, `movimentarSaida`, `abaixoDoMinimo`, `atualizarPrecoVenda`, `atualizarCustoMedio`, `atualizarNome`, `toString()`.

**Exemplo:**
```java
produto.movimentarEntrada(Quantidade.of(new BigDecimal("5"), "un"));
if (produto.abaixoDoMinimo()) {
    Log.warning("Estoque baixo para %s", produto.getNome());
}
```

### 4.15 br.ufvjm.barbearia.model.ItemVenda – `src/main/java/br/ufvjm/barbearia/model/ItemVenda.java`
**Responsabilidade:** Item de venda com produto, quantidade e preço unitário.

**Métodos:** getters, `subtotal`, `toString()`.

**Exemplo:**
```java
ItemVenda linha = new ItemVenda(produto, Quantidade.of(BigDecimal.ONE, "un"), produto.getPrecoVenda());
venda.adicionarItem(linha);
```

### 4.16 br.ufvjm.barbearia.model.Venda – `src/main/java/br/ufvjm/barbearia/model/Venda.java`
**Responsabilidade:** Venda de produtos com itens, desconto opcional, forma de pagamento e extrato.

**Atributos:** `id`, `cliente`, `dataHora`, `itens`, `formaPagamento`, `desconto`, `total`, `extratoGeradoEm`, `referenciaExtrato`.

**Métodos:** `adicionarItem`, `calcularTotal`, getters, métodos de extrato (`isExtratoGerado`, `marcarExtratoGerado`), `toString()`.

**Exemplo:**
```java
Venda venda = new Venda(UUID.randomUUID(), cliente, LocalDateTime.now(), FormaPagamento.PIX);
venda.adicionarItem(new ItemVenda(produto, quantidade, produto.getPrecoVenda()));
Dinheiro total = venda.calcularTotal();
```

### 4.17 br.ufvjm.barbearia.model.Despesa – `src/main/java/br/ufvjm/barbearia/model/Despesa.java`
**Responsabilidade:** Lançamento de despesa com categoria, valor, competência e data de pagamento opcional.

**Atributos:** `id`, `categoria`, `descricao`, `valor`, `competencia`, `dataPagamento`.

**Métodos:** getters, `estaPaga`, `registrarPagamento`, `toString()`.

**Exemplo:**
```java
Despesa aluguel = new Despesa(UUID.randomUUID(), CategoriaDespesa.ALUGUEL,
        "Aluguel do salão", Dinheiro.of(new BigDecimal("1500"), brl), YearMonth.of(2025, 1));
sistema.registrarDespesa(admin, aluguel);
```

### 4.18 br.ufvjm.barbearia.model.RecebimentoFornecedor – `src/main/java/br/ufvjm/barbearia/model/RecebimentoFornecedor.java`
**Responsabilidade:** Nota de entrada de fornecedor com itens, total, pagamentos registrados e saldo pendente.

**Atributos:** `id`, `fornecedor`, `dataHora`, `numeroNF`, `itens`, `total`, `pagamentoEfetuado`.

**Métodos:** `adicionarItem`, `calcularTotal`, `registrarPagamento`, `getSaldoPendente`, getters, `toString()`.

**Exemplo:**
```java
RecebimentoFornecedor recebimento = new RecebimentoFornecedor(UUID.randomUUID(), "Fornecedor X", dataHora, "NF123");
recebimento.adicionarItem(item);
recebimento.calcularTotal();
```

### 4.19 br.ufvjm.barbearia.model.CaixaDiario – `src/main/java/br/ufvjm/barbearia/model/CaixaDiario.java`
**Responsabilidade:** Controle de caixa diário com saldo de abertura, entradas, saídas, vendas, contas e movimentos.

**Atributos:** `data`, `saldoAbertura`, `entradas`, `saidas`, `saldoFechamento`, `vendas`, `contas`, `movimentos`.

**Métodos:** `registrarEntrada`, `registrarSaida`, `adicionarVenda`, `adicionarConta`, `consolidar`, `projetarBalanco`, getters, `toString()`.

**Exemplo:**
```java
CaixaDiario caixa = sistema.abrirCaixa(LocalDate.now(), Dinheiro.of(BigDecimal.ZERO, brl));
caixa.registrarEntrada(Dinheiro.of(new BigDecimal("50"), brl), "Retenção cancelamento");
Dinheiro saldo = caixa.consolidar();
```

#### 4.19.1 CaixaDiario.MovimentoCaixa
**Responsabilidade:** Representa movimento individual (entrada/saída) com motivo e timestamp.

**Atributos:** `tipo`, `valor`, `motivo`, `dataHora`.

**Métodos:** fábricas `entrada`/`saida`, getters, `toString()`.

**Exemplo:**
```java
CaixaDiario.MovimentoCaixa mov = CaixaDiario.MovimentoCaixa.entrada(valor, "Venda balcão");
```

### 4.20 br.ufvjm.barbearia.model.Estacao – `src/main/java/br/ufvjm/barbearia/model/Estacao.java`
**Responsabilidade:** Representa estação física; vetor estático `ESTACOES` fixa três posições (primeira com lavagem).

**Atributos:** `numero`, `possuiLavagem`, `ESTACOES` (array estático de três instâncias).

**Métodos:** `getNumero`, `isPossuiLavagem`, `toString`, `equals`, `hashCode`.

**Exemplo:**
```java
Estacao lavagem = Estacao.ESTACOES[0];
boolean temLavagem = lavagem.isPossuiLavagem();
```

### 4.21 br.ufvjm.barbearia.persist.DataSnapshot – `src/main/java/br/ufvjm/barbearia/persist/DataSnapshot.java`
**Responsabilidade:** DTO agregador com todas as listas persistidas.

**Atributos:** listas de `Cliente`, `Usuario`, `Servico`, `Produto`, `Agendamento`, `Venda`, `ContaAtendimento`, `Despesa`, `RecebimentoFornecedor`, `CaixaDiario`.

**Métodos:** construtores, getters que retornam cópias imutáveis, `toString()`, `builder()`.

**Exemplo:**
```java
DataSnapshot snapshot = DataSnapshot.builder()
        .withClientes(clientes)
        .withAgendamentos(agendamentos)
        .build();
```

#### 4.21.1 DataSnapshot.Builder
Builder fluente com métodos `withX` para cada lista e `build()`.

### 4.22 br.ufvjm.barbearia.persist.JsonStorage – `src/main/java/br/ufvjm/barbearia/persist/JsonStorage.java`
**Responsabilidade:** Salvar/carregar snapshots JSON usando Gson e adapters customizados.

**Atributos:** `GSON` configurado, `DEBUG_VIEW`.

**Métodos:** `save(DataSnapshot, Path)`, `load(Path)`, `description()`, `toString()`.

**Exemplo:**
```java
Path arquivo = Path.of("data/snapshots/barbearia.json");
JsonStorage.save(snapshot, arquivo);
DataSnapshot carregado = JsonStorage.load(arquivo);
```

### 4.23 br.ufvjm.barbearia.persist.ExtratoIO – `src/main/java/br/ufvjm/barbearia/persist/ExtratoIO.java`
**Responsabilidade:** Persistir extratos de serviço/venda em texto com timestamp.

**Métodos:** `saveExtrato(Cliente, String, Path)`, `description()`, `toString()`.

**Exemplo:**
```java
Path extrato = ExtratoIO.saveExtrato(cliente, conteudo, Path.of("data/extratos"));
cliente.registrarExtrato(extrato.toString());
```

### 4.24 br.ufvjm.barbearia.persist.adapters.* – `src/main/java/br/ufvjm/barbearia/persist/adapters/*.java`
**Responsabilidade:** Adapters Gson para `Dinheiro`, `LocalDate`, `LocalDateTime`, `YearMonth`.

**Métodos:** `write`/`read` em cada adapter validando formato; `toString()` descritivo.

**Exemplo:**
```java
Gson gson = new GsonBuilder()
        .registerTypeAdapter(Dinheiro.class, new DinheiroAdapter())
        .create();
```

### 4.25 br.ufvjm.barbearia.compare.* – `src/main/java/br/ufvjm/barbearia/compare/*.java`
**Responsabilidade:** Comparators para ordenação.

**Classes:** `ClientePorNome`, `ClientePorEmail`, `AgendamentoPorInicio`, `AgendamentoPorClienteNome` – todos implementam `Comparator`, validam nulos, possuem `toString()` explicativo.

**Exemplo:**
```java
List<Cliente> ordenados = clientes.stream()
        .sorted(new ClientePorNome())
        .toList();
```

### 4.26 br.ufvjm.barbearia.value.* – Objetos de valor
- **Dinheiro:** valor monetário com operações `somar`, `subtrair`, `multiplicar`; equals/hashCode por valor/moeda; valida mesma moeda.
- **Quantidade:** quantidade com unidade; escala padrão 3 casas; impede valores negativos.
- **CpfHash:** cria hash SHA-256 e máscara; garante 11 dígitos válidos.
- **Email:** valida via regex; imutável.
- **Telefone:** normaliza DDD + número; expõe formato `(DD) XXXX-XXXX`/`XXXXX-XXXX`.
- **Endereco:** builder imutável com validações (sigla UF, CEP com 8 dígitos).
- **Periodo:** intervalo entre `LocalDateTime`, com `contem` e formatação `dd/MM/yyyy HH:mm`.

**Exemplo:**
```java
Dinheiro valor = Dinheiro.of(new BigDecimal("50.00"), Currency.getInstance("BRL"));
Periodo periodo = Periodo.of(inicio, fim);
Endereco endereco = Endereco.builder().logradouro("Rua A").numero("10").bairro("Centro")
        .cidade("Diamantina").estado("MG").cep("39100000").build();
```

### 4.27 br.ufvjm.barbearia.enums.*
Enumerações com descrições amigáveis:
- `Papel` (ADMIN, COLABORADOR, BARBEIRO)
- `StatusAtendimento` (EM_ESPERA, EM_ATENDIMENTO, CONCLUIDO, CANCELADO)
- `FormaPagamento` (DINHEIRO, CARTAO_DEBITO, CARTAO_CREDITO, PIX, OUTRO)
- `CategoriaDespesa` (LIMPEZA, CAFE_FUNCIONARIOS, MATERIAIS, ALUGUEL, ENERGIA, AGUA, OUTRAS)
- `ModoConsumoProduto` (CONSUMO_INTERNO, FATURADO)

**Exemplo:**
```java
if (usuario.getPapel() == Papel.ADMIN) {
    sistema.registrarDespesa(usuario, despesa);
}
```

### 4.28 br.ufvjm.barbearia.exceptions.PermissaoNegadaException – `src/main/java/br/ufvjm/barbearia/exceptions/PermissaoNegadaException.java`
**Responsabilidade:** Sinalizar ausência de permissão.

**Métodos:** Construtores com mensagem/causa, `toString()` detalhando mensagem e causa.

**Exemplo:**
```java
throw new PermissaoNegadaException("Operação restrita a administradores");
```

### 4.29 br.ufvjm.barbearia.util.Log – `src/main/java/br/ufvjm/barbearia/util/Log.java`
**Responsabilidade:** Wrapper para `java.util.logging` com formatação customizada e flag de debug.

**Atributos:** `LOGGER`, `DEBUG_ENABLED`, `SimpleFormatter` interno.

**Métodos:** `info`, `warning`, `error`, `debug` (condicional), `format`. Classe interna `SimpleFormatter` formata `yyyy-MM-dd HH:mm:ss NÍVEL mensagem`.

**Exemplo:**
```java
Log.info("Agendamento registrado: %s", agendamento.getId());
```

### 4.30 QuickChecks – `src/main/java/QuickChecks.java`
**Responsabilidade:** Smoke test simples do `Sistema`.

**Métodos:** `main(String[] args)` (instancia `Sistema`, valida contador de OS >=0, imprime mensagem), `toString()` com descrição.

**Exemplo:**
```java
public static void main(String[] args) {
    Sistema s = new Sistema();
    assert s.getTotalOrdensServicoCriadas() >= 0;
    System.out.println("✅ Testes básicos OK");
}
```


## 5. Fluxos de Negócio Chave

### 5.1 Agendamento e alocação de estação
1. A UI chama `Sistema.criarAgendamento`, que instancia `Agendamento` com cliente, estação (`Estacao.ESTACOES`), período e sinal.
2. O método delega para `realizarAgendamento`, adicionando à lista interna e incrementando `totalOrdensServico`.
3. Serviços são associados via `Agendamento.adicionarItemServico`; barbeiro definido com `associarBarbeiro`.
4. Se algum `ItemDeServico` referencia `Servico.isRequerLavagem() == true`, deve-se usar `Estacao.ESTACOES[0]` (possui lavagem).

**Exemplo:**
```java
Agendamento ag = sistema.criarAgendamento(UUID.randomUUID(), cliente,
        Estacao.ESTACOES[0], inicio, fim, sinal);
ag.adicionarItemServico(new ItemDeServico(servico, servico.getPreco(), servico.getDuracaoMin()));
```

### 5.2 Cancelamento com retenção de 35%
1. Colaborador/Admin chama `Sistema.cancelarAgendamento` (valida papel via `assertColaboradorOuAdmin`).
2. O método localiza a OS, executa `Agendamento.cancelar(RETENCAO_CANCELAMENTO)` e obtém valores de retenção/reembolso.
3. Recupera/gera `ContaAtendimento`, registra retenção (`registrarRetencaoCancelamento`) e recalcula total.
4. Atualiza `CaixaDiario` do dia com `registrarEntrada` para o valor retido, garantindo associação da conta.
5. Gera extrato textual (`gerarExtratoCancelamento` -> `ExtratoIO.saveExtrato`) e marca em `Agendamento`.

**Exemplo:**
```java
Agendamento.Cancelamento info = sistema.cancelarAgendamento(colaborador, agendamentoId);
System.out.println("Retenção: " + info.getValorRetencao());
```

### 5.3 Fechamento do atendimento
1. `ContaAtendimento` recebe itens adicionais (`adicionarServicoFaturado`, `adicionarProdutoFaturado`).
2. Calcula o total com `calcularTotal(agendamento.totalServicos())`, aplicando descontos/ajustes.
3. Fecha via `fecharConta(FormaPagamento)` (chama `liquidar`).
4. `Sistema.gerarExtratoServico` gera comprovante, atualiza `ContaAtendimento` e `Cliente`.
5. Conta fechada pode ser adicionada ao `CaixaDiario` do dia (`adicionarConta`).

**Exemplo:**
```java
conta.calcularTotal(agendamento.totalServicos());
conta.fecharConta(FormaPagamento.PIX);
sistema.gerarExtratoServico(conta);
```

### 5.4 Venda (consumidor final)
1. `Venda` criada com cliente opcional e itens (`ItemVenda`).
2. Total calculado (`venda.calcularTotal()`); desconto opcional aplicado no construtor.
3. `Sistema.registrarVenda` valida papel, adiciona à lista e chama `gerarExtratoVenda`.
4. Extrato salvo via `ExtratoIO`, `Venda.marcarExtratoGerado` registra caminho.

**Exemplo:**
```java
Venda venda = new Venda(UUID.randomUUID(), cliente, LocalDateTime.now(), FormaPagamento.CARTAO_CREDITO);
venda.adicionarItem(new ItemVenda(produto, Quantidade.of(BigDecimal.ONE, "un"), produto.getPrecoVenda()));
sistema.registrarVenda(colaborador, venda);
```

### 5.5 Recebimento de fornecedor
1. Admin chama `Sistema.registrarRecebimentoFornecedor` com nota e itens.
2. Método calcula total (`RecebimentoFornecedor.calcularTotal()`), atualiza estoque (`Produto.movimentarEntrada`) e custo médio.
3. Se informado pagamento (`Dinheiro` + data), registra em `RecebimentoFornecedor.registrarPagamento` e no `CaixaDiario` (saída).
4. Registro permanece disponível via `listarRecebimentos`.

**Exemplo:**
```java
sistema.registrarRecebimentoFornecedor(admin, recebimento,
        Dinheiro.of(new BigDecimal("200"), brl), LocalDate.now());
```

### 5.6 Fila secundária (pilha LIFO)
1. Agendamentos sem vaga são inseridos com `Sistema.adicionarAgendamentoSecundario` (push).
2. Para inspecionar sem remover, usa `inspecionarFilaSecundaria()` (peek) que loga status.
3. Quando surge vaga, `recuperarAgendamentoSecundario()` promove o topo (pop) e pode ser passado a `realizarAgendamento`.

**Exemplo:**
```java
sistema.adicionarAgendamentoSecundario(agEspera);
Optional<Agendamento> proximo = sistema.inspecionarFilaSecundaria();
Agendamento promovido = sistema.recuperarAgendamentoSecundario();
```

### 5.7 Persistência JSON e reidratação de contadores
1. Admin chama `Sistema.saveAll(admin, caminho)` — monta `DataSnapshot` com listas e grava via `JsonStorage.save`.
2. Para restaurar, usa `Sistema.loadAll(caminho)` — lê snapshot, substitui listas e chama `Servico.reidratarContadores` + `redefinirTotalOrdensServico`.
3. Estratégia A (encapsulada) atualiza `Sistema.setTotalServicos`; estratégia B (protegida) usa `Cliente.redefinirTotalServicosProtegido`.
4. Após carga, contadores refletem quantidades reais mesmo após reinicialização.

**Exemplo:**
```java
sistema.saveAll(admin, Path.of("data/snapshots/barbearia.json"));
sistema.loadAll(Path.of("data/snapshots/barbearia.json"));
int totalServicos = Sistema.getTotalServicos();
```

## 6. Permissões e Segurança
- `Papel.ADMIN`: Pode cadastrar/editar/remover usuários, registrar despesas, emitir relatórios financeiros, registrar recebimentos e salvar snapshots.
- `Papel.COLABORADOR`: Pode manipular agenda, contas, vendas e cancelamentos (com retenção). Não acessa despesas nem balanços.
- `Papel.BARBEIRO`: Operações limitadas a status de atendimento/consumos (no roteiro demonstrativo).

Validações ocorrem em `Sistema.assertAdmin` e `Sistema.assertColaboradorOuAdmin`, lançando `PermissaoNegadaException`. Operações sensíveis: relatórios financeiros, despesas, recebimentos, persistência (`saveAll`). Senhas são armazenadas como hash em `Usuario.senhaHash`; CPF é guardado mascarado via `CpfHash`.

## 7. Ordenações e Busca
- Comparators disponíveis: `ClientePorNome`, `ClientePorEmail`, `AgendamentoPorInicio`, `AgendamentoPorClienteNome`.
- `Sistema.listarClientesOrdenados`/`listarAgendamentosOrdenados` aceitam offset/limit para paginação e comparator customizado.
- `Sistema.find` implementa busca linear usando `Iterator` (relaciona-se com exercícios sobre uso explícito de iteradores).
- Para usar `Collections.binarySearch`, garanta lista previamente ordenada pelo mesmo comparator; `find` funciona em qualquer ordem.

## 8. Persistência, Arquivos e Diretórios
- Snapshots JSON gravados em caminho informado (ex.: `data/snapshots/barbearia.json`) por `JsonStorage.save` com adapters (`LocalDateAdapter`, `LocalDateTimeAdapter`, `YearMonthAdapter`, `DinheiroAdapter`).
- Extratos de serviço/venda/cancelamento são salvos em `data/extratos/` com timestamp `yyyyMMddHHmmss` via `ExtratoIO.saveExtrato`.
- Uso consistente de `try-with-resources` (`JsonStorage`, `ExtratoIO`) garante fechamento de streams.
- `Sistema.loadAll` faz logging informativo (`Log.info`) sobre fontes de dados.

## 9. Contadores Estáticos e Reidratação
- **Estratégia A (encapsulada):** `Sistema.totalServicos` atualizado apenas via `Sistema.ServicoTracker` e `Sistema.setTotalServicos(int)` na reidratação. Mais seguro, centraliza controle.
- **Estratégia B (protegida):** `Cliente.totalServicosProtegido` acessível a subclasses/mesmo pacote; atualizado pelo construtor de `Servico` e por `Cliente.redefinirTotalServicosProtegido(int)`. Simples, porém mais exposta.
- `Sistema.totalOrdensServico` incrementado em `registrarAgendamento`; reidratação usa `redefinirTotalOrdensServico(contarElementos(agendamentos))`.
- Após `loadAll`, ambos contadores refletem quantidade de serviços persistidos e total de OS existentes.

## 10. Como Executar e Gerar Documentação
- **Build:** `mvn -q -DskipTests clean package`
- **Executar demonstração completa:** `mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.EntregaFinalMain`
- **Executar roteiro reduzido:** `mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.Main`
- **Gerar JavaDoc:** `mvn -q javadoc:javadoc` (saída em `target/site/apidocs/index.html`).
- Diretório `docs/` contém materiais complementares (`Evidencias_Implementacao_Barbearia.md`, diagramas do projeto original).

## 11. Glossário
- **OS (Ordem de Serviço):** Agendamento de atendimento com serviços, barbeiro e estação.
- **Extrato:** Comprovante textual de serviço ou venda salvo em disco.
- **Fila secundária:** Pilha LIFO de agendamentos aguardando vaga.
- **Retenção (35%):** Percentual cobrado em cancelamentos sobre o total de serviços contratados.
- **Snapshot:** Arquivo JSON com todas as listas persistidas.
