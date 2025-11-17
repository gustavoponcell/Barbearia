# Análise de Padrão de Projeto — Strategy no Sistema Barbearia

## Introdução
O sistema Barbearia é uma aplicação Java/Maven para gestão de uma barbearia com três estações fixas, agenda de ordens de serviço (OS), controle de loja/estoque, geração automática de extratos, permissões por papel e uma fila secundária (pilha LIFO) para realocar atendimentos. O núcleo (`Sistema`) mantém coleções em memória, faz a persistência via JSON e coordena regras como cancelamento com retenção de 35% e realocação de clientes.

O padrão Strategy define uma família de algoritmos, encapsula cada um e os torna intercambiáveis em tempo de execução, permitindo variar o comportamento sem alterar o cliente que os utiliza. É usado quando há múltiplas políticas para a mesma tarefa (ex.: ordenação, precificação) e evita condicionais extensas ao aderir ao Princípio Aberto/Fechado.

## Objetivo
Descrever como o Strategy aparece hoje no projeto (principalmente por meio dos `Comparator<T>`) e como pode ser aplicado em outros pontos para maximizar benefícios, como políticas de cancelamento/retensão, alocação de estações e precificação/descontos.

## Materiais e Métodos
A análise percorreu os diretórios `compare/**`, `system/**`, `model/**` e `persist/**`, inspecionando classes e pontos de uso. Os critérios para identificar Strategy foram: existência de um contrato comum, múltiplas implementações intercambiáveis, seleção em tempo de execução via parâmetro/atributo, redução de `if/switch` e aderência ao Open/Closed Principle.

## Resultados e Discussão
### 4.1 Evidências de Strategy já presentes
As estratégias de ordenação são implementadas via `Comparator<T>`, com seleção em tempo de execução:

1. **ClientePorNome** (`br.ufvjm.barbearia.compare.ClientePorNome`)
   - Arquivo/linhas: `src/main/java/br/ufvjm/barbearia/compare/ClientePorNome.java`:1-34. 【F:src/main/java/br/ufvjm/barbearia/compare/ClientePorNome.java†L1-L34】
   - Trecho:
     ```java
     @Override
     public int compare(Cliente cliente1, Cliente cliente2) {
         Objects.requireNonNull(cliente1, "cliente1 não pode ser nulo");
         Objects.requireNonNull(cliente2, "cliente2 não pode ser nulo");
         String nome1 = Objects.requireNonNull(cliente1.getNome(), "nome do cliente1 não pode ser nulo");
         String nome2 = Objects.requireNonNull(cliente2.getNome(), "nome do cliente2 não pode ser nulo");
         return COLLATOR.compare(nome1, nome2);
     }
     ```
   - Uso: `Sistema.listarClientesOrdenados` recebe `Comparator<Cliente>`; quando não informado, aplica `ClientePorNome` como padrão. 【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L231-L242】 Também é passado diretamente em `Main` para ordenar listagens e relatórios operacionais. 【F:src/main/java/br/ufvjm/barbearia/system/Main.java†L323-L356】
   - Por que é Strategy: `Comparator<Cliente>` é o contrato; políticas de ordenação são trocáveis em runtime sem alterar `Sistema` ou `Main`.

2. **ClientePorEmail** (`br.ufvjm.barbearia.compare.ClientePorEmail`)
   - Arquivo/linhas: `src/main/java/br/ufvjm/barbearia/compare/ClientePorEmail.java`:1-26. 【F:src/main/java/br/ufvjm/barbearia/compare/ClientePorEmail.java†L1-L26】
   - Trecho:
     ```java
     @Override
     public int compare(Cliente cliente1, Cliente cliente2) {
         Objects.requireNonNull(cliente1, "cliente1 não pode ser nulo");
         Objects.requireNonNull(cliente2, "cliente2 não pode ser nulo");
         Email email1 = Objects.requireNonNull(cliente1.getEmail(), "email do cliente1 não pode ser nulo");
         Email email2 = Objects.requireNonNull(cliente2.getEmail(), "email do cliente2 não pode ser nulo");
         return email1.getValor().compareToIgnoreCase(email2.getValor());
     }
     ```
   - Uso: passado a `Sistema.listarClientesOrdenados` para alterar a ordem padrão em fluxos de apresentação e relatório operacional. 【F:src/main/java/br/ufvjm/barbearia/system/Main.java†L328-L356】 Também demonstrado em `EntregaFinalMain` com `Collections.sort`. 【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L431-L480】
   - Por que é Strategy: mesma interface (`Comparator<Cliente>`), política alternativa selecionada em tempo de execução.

3. **AgendamentoPorInicio** (`br.ufvjm.barbearia.compare.AgendamentoPorInicio`)
   - Arquivo/linhas: `src/main/java/br/ufvjm/barbearia/compare/AgendamentoPorInicio.java`:1-26. 【F:src/main/java/br/ufvjm/barbearia/compare/AgendamentoPorInicio.java†L1-L26】
   - Trecho:
     ```java
     @Override
     public int compare(Agendamento agendamento1, Agendamento agendamento2) {
         Objects.requireNonNull(agendamento1, "agendamento1 não pode ser nulo");
         Objects.requireNonNull(agendamento2, "agendamento2 não pode ser nulo");
         LocalDateTime inicio1 = Objects.requireNonNull(agendamento1.getInicio(), "início do agendamento1 não pode ser nulo");
         LocalDateTime inicio2 = Objects.requireNonNull(agendamento2.getInicio(), "início do agendamento2 não pode ser nulo");
         return inicio1.compareTo(inicio2);
     }
     ```
   - Uso: é o comparator padrão em `Sistema.listarAgendamentosOrdenados`. 【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L547-L558】 Chamado explicitamente em `Main` e `EntregaFinalMain` para ordenar saídas. 【F:src/main/java/br/ufvjm/barbearia/system/Main.java†L334-L356】【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L472-L480】
   - Por que é Strategy: contratos comuns, políticas de ordenação intercambiáveis em runtime.

4. **AgendamentoPorClienteNome** (`br.ufvjm.barbearia.compare.AgendamentoPorClienteNome`)
   - Arquivo/linhas: `src/main/java/br/ufvjm/barbearia/compare/AgendamentoPorClienteNome.java`:1-46. 【F:src/main/java/br/ufvjm/barbearia/compare/AgendamentoPorClienteNome.java†L1-L46】
   - Trecho:
     ```java
     @Override
     public int compare(Agendamento agendamento1, Agendamento agendamento2) {
         Objects.requireNonNull(agendamento1, "agendamento1 não pode ser nulo");
         Objects.requireNonNull(agendamento2, "agendamento2 não pode ser nulo");
         Cliente cliente1 = Objects.requireNonNull(agendamento1.getCliente(), "cliente do agendamento1 não pode ser nulo");
         Cliente cliente2 = Objects.requireNonNull(agendamento2.getCliente(), "cliente do agendamento2 não pode ser nulo");
         String nome1 = Objects.requireNonNull(cliente1.getNome(), "nome do cliente1 não pode ser nulo");
         String nome2 = Objects.requireNonNull(cliente2.getNome(), "nome do cliente2 não pode ser nulo");
         int comparacaoNome = COLLATOR.compare(nome1, nome2);
         if (comparacaoNome != 0) {
             return comparacaoNome;
         }
         LocalDateTime inicio1 = Objects.requireNonNull(agendamento1.getInicio(), "início do agendamento1 não pode ser nulo");
         LocalDateTime inicio2 = Objects.requireNonNull(agendamento2.getInicio(), "início do agendamento2 não pode ser nulo");
         return inicio1.compareTo(inicio2);
     }
     ```
   - Uso: passado a `Sistema.listarAgendamentosOrdenados` e a `Collections.sort` em execuções de demonstração. 【F:src/main/java/br/ufvjm/barbearia/system/Main.java†L341-L356】【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L472-L482】
   - Por que é Strategy: alternativa ao comparator padrão, plugável em runtime.

### 4.2 Outras oportunidades para Strategy (Propostas de extensão)
**Cancelamento/Retenção (Proposta de extensão)**
- Interface sugerida: `CancelamentoStrategy` com método `Dinheiro calcularRetencao(Dinheiro sinal)`.
- Implementações: `RetencaoFixa35` (regra atual de 35%), `RetencaoPromocional` (percentual reduzido em datas especiais), `RetencaoIsenta` (isenta retenção para erros operacionais).
- Integração: em `Sistema.cancelarAgendamento`, substituir cálculo fixo por uma estratégia recebida no construtor ou método, mantendo emissão de extrato. Benefício: flexibiliza regras sem alterar o fluxo; trade-off: mais classes e necessidade de seleção clara por contexto.

**Alocação de estação (Proposta de extensão)**
- Interface sugerida: `AlocacaoEstacaoStrategy` com método `Estacao selecionar(List<Estacao> estacoes, Agendamento agendamento)`.
- Implementações: “primeira livre”, “balanceamento” (rotações para distribuir carga), “por tipo de serviço” (adequação a serviços com lavagem obrigatória em estação 1).
- Integração: no fluxo de criação de OS (`Sistema.realizarAgendamento`), antes de persistir, delegar à estratégia a escolha da estação disponível. Benefício: encapsula a lógica de alocação e facilita testes A/B; trade-off: precisa de dados de disponibilidade e monitoramento de carga.

**Preço/Desconto (Proposta de extensão)**
- Interface sugerida: `PrecoStrategy` com método `Dinheiro calcular(Servico servico, Cliente cliente, ContextoPreco ctx)`.
- Implementações: preço base, promoção por período, fidelidade por histórico de visitas.
- Integração: no cálculo de contas e vendas (`ContaAtendimento`, `Venda`), aplicar a estratégia antes de somar itens. Benefício: evita condicionais; trade-off: requer parametrização clara e consistência com extratos.

### (a) Descrição detalhada do padrão Strategy
Strategy encapsula variações de algoritmo atrás de um contrato comum, permitindo escolher a política em tempo de execução. Ele atende ao Open/Closed Principle porque novas políticas são adicionadas como novas classes sem modificar o contexto. Também reduz condicionais longas, pois a escolha se desloca para a instanciação/injeção da estratégia.

### (c) Descrição textual do diagrama de classes (sem TikZ)
**Contexto existente**
- Contexto: `Sistema` — oferece métodos de listagem/ordenação que recebem uma estratégia (`Comparator`) e aplicam sobre coleções internas de clientes e agendamentos. Usa `ordenarERecortar` para aplicar a política.
- Associações: `Sistema` —usa→ `Comparator<Cliente>`; `Sistema` —usa→ `Comparator<Agendamento>`.

**Strategy (contrato)**
- Interface: `Comparator<T>`.
- Operação: `int compare(T a, T b)`.

**Concrete Strategies**
- `ClientePorNome` — implementa `Comparator<Cliente>` (critério: nome, com `Collator`).
- `ClientePorEmail` — implementa `Comparator<Cliente>` (critério: e-mail).
- `AgendamentoPorInicio` — implementa `Comparator<Agendamento>` (critério: início).
- `AgendamentoPorClienteNome` — implementa `Comparator<Agendamento>` (critério: nome do cliente, desempate por início).

**Colaborações (passo a passo)**
1. `Sistema` recebe um `Comparator<T>` como parâmetro em métodos de ordenação ou relatório operacional.
2. `Sistema` invoca `lista.sort(comparator)` ou `Collections.sort(lista, comparator)` dentro de `ordenarERecortar` ou em clientes como `Main`/`EntregaFinalMain`.
3. O comparator concreto define a política de ordenação aplicada.
4. O resultado ordenado é retornado ou impresso.

**Observações de navegabilidade/multiplicidade**
- `Sistema` depende de múltiplos `Comparator<T>` (1..*) para clientes e agendamentos.
- Cada `ConcreteStrategy` implementa `Comparator<T>` e é conhecida apenas pelo contrato.
- `Sistema` não conhece detalhes internos das estratégias, mantendo baixo acoplamento.

**Propostas de extensão (mesmo formato textual)**
- `CancelamentoStrategy`: contexto `Sistema.cancelarAgendamento` usa→ estratégias (`RetencaoFixa35`, `RetencaoPromocional`, `RetencaoIsenta`); seleção definiria quanto reter do sinal; multiplicidade 1..* estratégias possíveis.
- `AlocacaoEstacaoStrategy`: contexto `Sistema.realizarAgendamento` usa→ (`PrimeiraLivre`, `Balanceamento`, `PorTipoServico`); define estação escolhida antes de salvar; múltiplas estratégias plugáveis.
- `PrecoStrategy`: contexto `ContaAtendimento`/`Venda` usa→ (`PrecoBase`, `PrecoPromocional`, `PrecoFidelidade`); define o valor aplicado por item.

### (d) Implementação em Java (real + proposta)
- **ConcreteStrategy existente** (`ClientePorNome`):
  ```java
  public int compare(Cliente cliente1, Cliente cliente2) {
      Objects.requireNonNull(cliente1, "cliente1 não pode ser nulo");
      Objects.requireNonNull(cliente2, "cliente2 não pode ser nulo");
      String nome1 = Objects.requireNonNull(cliente1.getNome(), "nome do cliente1 não pode ser nulo");
      String nome2 = Objects.requireNonNull(cliente2.getNome(), "nome do cliente2 não pode ser nulo");
      return COLLATOR.compare(nome1, nome2);
  }
  ```
  【F:src/main/java/br/ufvjm/barbearia/compare/ClientePorNome.java†L21-L28】

- **Contexto aplicando a estratégia** (`Sistema`):
  ```java
  public List<Cliente> listarClientesOrdenados(Comparator<Cliente> comparator, int offset, int limit) {
      Comparator<Cliente> criterio = comparator != null ? comparator : DEFAULT_CLIENTE_COMPARATOR;
      return ordenarERecortar(clientes, criterio, offset, limit);
  }
  ```
  【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L239-L242】

- **Uso externo demonstrando troca em runtime** (`Main`):
  ```java
  sistema.listarClientesOrdenados(new ClientePorNome(), 0, 2);
  sistema.listarClientesOrdenados(new ClientePorEmail(), 1, 2);
  sistema.listarAgendamentosOrdenados(new AgendamentoPorInicio(), 0, 3);
  sistema.listarAgendamentosOrdenados(new AgendamentoPorClienteNome(), 1, 2);
  ```
  【F:src/main/java/br/ufvjm/barbearia/system/Main.java†L323-L344】

- **Proposta de extensão (esqueleto mínimo)**
  ```java
  // Proposta de extensão
  interface CancelamentoStrategy {
      Dinheiro calcularRetencao(Dinheiro sinal);
  }
  class RetencaoFixa35 implements CancelamentoStrategy {
      public Dinheiro calcularRetencao(Dinheiro sinal) { return sinal.multiplicar(new BigDecimal("0.35")); }
  }
  class RetencaoIsenta implements CancelamentoStrategy {
      public Dinheiro calcularRetencao(Dinheiro sinal) { return Dinheiro.of(BigDecimal.ZERO, sinal.getMoeda()); }
  }
  // Integração sugerida: injetar CancelamentoStrategy em Sistema.cancelarAgendamento
  ```

### (e) Vantagens e Desvantagens
**Vantagens**
- Aderência ao Open/Closed Principle: novas políticas de ordenação ou negócio são adicionadas sem alterar `Sistema`.
- Testabilidade: cada estratégia pode ser testada isoladamente.
- Reutilização e plugabilidade: o mesmo comparator é reutilizado em relatórios, buscas (`Sistema.find`) e apresentações.
- Redução de condicionais: evita `if/switch` com seleção explícita de critérios.
- Clareza de responsabilidade: regras variáveis ficam concentradas em classes pequenas e nomeadas.

**Desvantagens**
- Aumento de classes: cada variação gera um novo tipo.
- Custo de orquestração/seleção: é preciso decidir e passar a estratégia correta em cada uso.
- Risco de microestratégias redundantes: políticas muito específicas podem proliferar sem padronização.
- Necessidade de convenção de nomes e pacotes para manter encontrabilidade.

### (f) Conclusão — Padrão ou antipadrão neste contexto?
Strategy é adequado ao projeto: os `Comparator<T>` existentes já cumprem o padrão, permitindo múltiplas políticas de ordenação plugáveis em `Sistema`, `Main` e `EntregaFinalMain`. As propostas (cancelamento, alocação, preço) expandem o benefício ao encapsular variações de negócio sem alterar fluxos centrais. Não usar Strategy quando não há variação prevista, a regra é estável por longo período ou a complexidade adicional não se justifica.

### Referências internas ao código (Evidências)
| Classe/Arquivo | Método | Linhas (aprox.) | Observação |
| --- | --- | --- | --- |
| `ClientePorNome` | `compare` | 21-28 | Ordenação por nome. 【F:src/main/java/br/ufvjm/barbearia/compare/ClientePorNome.java†L21-L28】 |
| `ClientePorEmail` | `compare` | 13-20 | Ordenação por e-mail. 【F:src/main/java/br/ufvjm/barbearia/compare/ClientePorEmail.java†L13-L20】 |
| `AgendamentoPorInicio` | `compare` | 13-19 | Ordenação por início. 【F:src/main/java/br/ufvjm/barbearia/compare/AgendamentoPorInicio.java†L13-L19】 |
| `AgendamentoPorClienteNome` | `compare` | 23-39 | Ordenação por nome do cliente; desempate por início. 【F:src/main/java/br/ufvjm/barbearia/compare/AgendamentoPorClienteNome.java†L23-L39】 |
| `Sistema` | `listarClientesOrdenados` | 231-242 | Recebe `Comparator<Cliente>` e aplica em tempo de execução. 【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L231-L242】 |
| `Sistema` | `listarAgendamentosOrdenados` | 547-558 | Recebe `Comparator<Agendamento>` e aplica em tempo de execução. 【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L547-L558】 |
| `Main` | Uso de comparators | 323-356 | Seleção manual de estratégias em runtime para clientes/agendamentos. 【F:src/main/java/br/ufvjm/barbearia/system/Main.java†L323-L356】 |
| `EntregaFinalMain` | Uso de comparators | 431-482 | Demonstra troca de estratégias com `Collections.sort`. 【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L431-L482】 |

### Apêndice A — Guia rápido de uso do Strategy existente
- Ordenar clientes por nome (padrão): `sistema.listarClientesOrdenados(new ClientePorNome(), 0, n);`
- Ordenar clientes por e-mail: `sistema.listarClientesOrdenados(new ClientePorEmail(), offset, limit);`
- Ordenar agendamentos por início: `sistema.listarAgendamentosOrdenados(new AgendamentoPorInicio(), 0, n);`
- Ordenar agendamentos por nome do cliente: `sistema.listarAgendamentosOrdenados(new AgendamentoPorClienteNome(), offset, limit);`

### Apêndice B — Propostas de Strategy (se aplicável)
- **CancelamentoStrategy**: `RetencaoFixa35`, `RetencaoPromocional`, `RetencaoIsenta`; integrar em `Sistema.cancelarAgendamento`.
- **AlocacaoEstacaoStrategy**: `PrimeiraLivre`, `Balanceamento`, `PorTipoServico`; integrar em `Sistema.realizarAgendamento` antes de persistir.
- **PrecoStrategy**: `PrecoBase`, `PrecoPromocional`, `PrecoFidelidade`; integrar em cálculo de `ContaAtendimento` e `Venda`.
