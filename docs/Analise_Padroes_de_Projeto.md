# Análise de Padrões de Projeto do Sistema de Barbearia

**Data:** 2025-11-17

## Resumo do objetivo
Avaliar a presença dos padrões solicitados no código do projeto Barbearia (Java/Maven), justificar cada ocorrência com evidências e recomendar adoções úteis.

## Sumário
- [Metodologia de Detecção](#metodologia-de-detecção)
- [Mapa Rápido (Tabela de Achados)](#mapa-rápido-tabela-de-achados)
- [Análise Detalhada por Padrão](#análise-detalhada-por-padrão)
- [Top Recomendações](#top-recomendações)
- [Tabela Benefício vs Complexidade](#tabela-benefício-vs-complexidade)
- [Passos de Adoção Recomendados (Backlog)](#passos-de-adoção-recomendados-backlog)
- [Conclusão](#conclusão)

## Metodologia de Detecção
- Varredura manual dos pacotes `system`, `model`, `compare`, `persist`, `value`, `enums` e utilitários presentes, lendo classes-chave com foco nos sinais típicos de cada padrão.
- Critérios de evidência: **forte** (estrutura clássica completa, interfaces específicas e uso claro), **média** (parcial ou utilitários que cumprem o papel sem separação formal), **fraca** (apenas indícios ou comentários).
- Heurísticas: interfaces + múltiplas implementações para Strategy/Adapter; objetos imutáveis com construtor privado + estático para Factory Method; enums + validação de transições para State; builders para agregados grandes; classes de persistência isoladas para DAO.

## Mapa Rápido (Tabela de Achados)
| Padrão | Detectado? | Confiança | Onde (arquivo:linhas) | Comentário curto |
| --- | --- | --- | --- | --- |
| Composite | Não | – | – | Estruturas usam listas simples, sem interface composta. |
| Decorator | Não | – | – | Não há envelopamento incremental de comportamento. |
| Factory Method | Parcial | Média | `value/Dinheiro.java`:24-52 | Métodos estáticos `of` criam instâncias validadas. |
| Abstract Factory | Não | – | – | Não há fábricas famílias/coerentes. |
| Adapter | Sim | Alta | `persist/adapters/DinheiroAdapter.java`:13-79 | Gson TypeAdapters adaptam VO para JSON. |
| Chain of Responsibility | Não | – | – | Fluxos diretos sem cadeia de handlers. |
| Prototype | Não | – | – | Sem clonagem profunda/protótipos. |
| Singleton | Não | – | – | `Sistema` é instanciável; sem controle de instância única. |
| DAO | Parcial | Média | `persist/JsonStorage.java`:22-105 | Classe dedicada de acesso a armazenamento JSON. |
| Bridge | Não | – | – | Não há separação de abstração/implementação. |
| Memento | Não | – | – | Sem snapshots reversíveis além de DTO estático. |
| Command | Não | – | – | Operações são métodos diretos, sem objetos comando. |
| State | Parcial | Média | `model/Agendamento.java`:121-153 | Enum + validação de transições encapsulam estados. |
| Strategy | Sim | Alta | `compare/*`:11-25 | Comparators intercambiáveis para ordenação. |
| Observer | Não | – | – | Não há publicação/assinatura de eventos. |
| Builder | Sim | Alta | `persist/DataSnapshot.java`:129-207 | Builder encadeado para montar snapshot. |
| Flyweight | Não | – | – | Nenhum compartilhamento interno otimizado. |
| Interpreter | Não | – | – | Não há gramáticas/parse custom. |

## Análise Detalhada por Padrão
### Composite
- **Definição:** composição recursiva de objetos (parte-todo) com interface unificada.
- **Sinais procurados:** interfaces comuns para item/contêiner, operações delegadas recursivamente.
- **Evidências:** não identificadas; listas de itens em `ContaAtendimento` e `Agendamento` são agregações simples sem interface composta.
- **Avaliação:** não implementado.
- **Recomendação:** apenas se surgir hierarquia real (ex.: pacotes de serviços). Complexidade média; hoje não é necessário.

### Decorator
- **Definição:** adiciona responsabilidades dinamicamente via composição.
- **Sinais procurados:** classes que recebem componente e repassam chamadas.
- **Evidências:** ausentes.
- **Avaliação:** não aplicável.
- **Recomendação:** sem necessidade atual.

### Factory Method
- **Definição:** método que cria objetos, permitindo controle/extensão do tipo retornado.
- **Sinais procurados:** construtor privado + métodos estáticos nomeados.
- **Evidências:** `Dinheiro.of` valida entradas e controla criação de VO monetário, encapsulando arredondamento e obrigatoriedade de moeda.【F:src/main/java/br/ufvjm/barbearia/value/Dinheiro.java†L11-L52】 Métodos `AjusteConta.credito/debito` e `ContaAtendimento.Cancelamento` seguem ideia, mas sem hierarquia.
- **Avaliação:** uso utilitário (evidência média); não há sobrecarga por subtipo.
- **Recomendação:** manter; considerar fábricas nomeadas para outras entidades com validação (p.ex. `Agendamento.createComValidacao`). Complexidade baixa.

### Abstract Factory
- **Definição:** cria famílias de objetos relacionados sem expor classes concretas.
- **Sinais procurados:** interfaces fábrica com múltiplas implementações (ex.: persistência em JSON vs BD).
- **Evidências:** não há.
- **Avaliação:** não implementado.
- **Recomendação:** poderia ser útil se surgir persistência alternativa (JSON/SQL). Complexidade média.

### Adapter
- **Definição:** converte interface de um tipo para outra esperada pelo cliente.
- **Sinais procurados:** classes “Adapter” delegando conversão.
- **Evidências:** Gson `TypeAdapter` para `Dinheiro` adapta VO para JSON primitivo e vice-versa.【F:src/main/java/br/ufvjm/barbearia/persist/adapters/DinheiroAdapter.java†L13-L79】 Similar para `LocalDate`, `LocalDateTime`, `YearMonth` (mesmo padrão de registro em `JsonStorage`).
- **Avaliação:** implementação correta; reduz acoplamento de VO ao framework de serialização.
- **Recomendação:** nenhum ajuste imediato; manter adapters separados para novos VOs.

### Chain of Responsibility
- **Definição:** cadeia de handlers para processar requisições até alguém tratá-las.
- **Evidências:** não presentes; validações e regras são diretas.
- **Recomendação:** pode ser útil para pipelines de autorização/validação em `Sistema` (ex.: cancelamento). Complexidade média.

### Prototype
- **Definição:** cria novos objetos copiando protótipos existentes.
- **Evidências:** inexistentes.
- **Recomendação:** desnecessário; entidades são simples e identificadas por UUID.

### Singleton
- **Definição:** garante única instância acessível globalmente.
- **Evidências:** `Sistema` é instanciável e não controla unicidade; utilitários estáticos (`JsonStorage`, `ExtratoIO`) não mantêm estado.
- **Recomendação:** manter sem singleton para favorecer testes; se interface gráfica precisar, injetar instância explicitamente. Complexidade baixa, benefício baixo.

### DAO (Data Access Object)
- **Definição:** encapsula acesso a dados, isolando infraestrutura do domínio.
- **Sinais procurados:** classe responsável por CRUD/persistência de entidades ou agregados.
- **Evidências:** `JsonStorage` centraliza salvar/carregar `DataSnapshot`, abstraindo `Gson` e filesystem do domínio.【F:src/main/java/br/ufvjm/barbearia/persist/JsonStorage.java†L21-L105】 Contudo, mistura conversão (DTO) e acesso a arquivo, sem interface.
- **Avaliação:** implementação parcial (um DAO global). Falta especialização por entidade e interface para troca de tecnologia.
- **Recomendação:** extrair interface `SnapshotRepository` e separar concern de serialização (mapper) do I/O. Complexidade média.

### Bridge
- **Definição:** separa abstração da implementação permitindo variações independentes.
- **Evidências:** inexistentes.
- **Recomendação:** não necessário no escopo atual.

### Memento
- **Definição:** captura e restaura estado interno sem violar encapsulamento.
- **Evidências:** `DataSnapshot` é DTO persistido, mas não há mecanismo de restauração incremental/undo; é mais uma transferência de estado.
- **Avaliação:** não implementado.
- **Recomendação:** só faria sentido se fosse necessário rollback/undo de operações. Complexidade média/alta.

### Command
- **Definição:** encapsula uma operação como objeto, permitindo fila, undo, logging.
- **Evidências:** operações críticas (`registrarVenda`, `fecharContaAtendimento`) são métodos diretos em `Sistema`, sem objetos comando.
- **Avaliação:** não implementado.
- **Recomendação:** ver Top Recomendações para encapsular operações sensíveis. Complexidade média.

### State
- **Definição:** altera comportamento conforme estado interno encapsulado em objetos/enum.
- **Sinais procurados:** enum/objetos de estado com regras de transição.
- **Evidências:** `Agendamento` mantém `StatusAtendimento` e valida transições permitidas (`EM_ESPERA → EM_ATENDIMENTO → CONCLUIDO` ou cancelamento a qualquer momento) no método `alterarStatus`/`transicaoValida`.【F:src/main/java/br/ufvjm/barbearia/model/Agendamento.java†L121-L153】
- **Avaliação:** abordagem simples (enum + guarda). Poderia evoluir para classes de estado com regras específicas (ex.: side effects de cancelamento).
- **Recomendação:** manter, mas extrair lógica de transição para estratégia de estado se regras crescerem.

### Strategy
- **Definição:** encapsula algoritmos intercambiáveis sob a mesma interface.
- **Evidências:** Comparators (`AgendamentoPorInicio`, `ClientePorNome`, etc.) implementam `Comparator` e são injetados em listagens/relatórios no `Sistema`, permitindo ordenações variadas.【F:src/main/java/br/ufvjm/barbearia/compare/AgendamentoPorInicio.java†L1-L25】
- **Avaliação:** implementação clara; uso adequado em relatórios.
- **Recomendação:** manter; adicionar políticas de precificação/retencao como estratégias se necessário.

### Observer
- **Definição:** notifica múltiplos interessados sobre eventos.
- **Evidências:** inexistentes; geração de extratos é chamada diretamente após operações.
- **Recomendação:** ver Top Recomendações para automatizar disparos (ex.: `VendaRegistradaEvent`). Complexidade média.

### Builder
- **Definição:** construção passo a passo de objetos complexos, preservando imutabilidade.
- **Evidências:** `DataSnapshot.Builder` com métodos `with*` e `build` encadeados para montar snapshot completo sem construtor gigante.【F:src/main/java/br/ufvjm/barbearia/persist/DataSnapshot.java†L129-L207】
- **Avaliação:** implementação correta e útil para testes/persistência.
- **Recomendação:** ampliar para entidades com muitos campos opcionais (ex.: `ContaAtendimento` configurada antes de uso). Complexidade baixa.

### Flyweight
- **Definição:** compartilha dados imutáveis para reduzir consumo de memória.
- **Evidências:** não há; entidades são instanciadas diretamente.
- **Recomendação:** desnecessário hoje.

### Interpreter
- **Definição:** define gramática e interpretador para linguagem específica.
- **Evidências:** ausentes.
- **Recomendação:** não aplicável.

## Top Recomendações
1. **Command para operações críticas (registrar venda, fechar conta, cancelar com retenção)**
   - **Problema:** lógica sensível concentrada em `Sistema`, difícil de auditar/undo.
   - **Onde aplicar:** novo pacote `br.ufvjm.barbearia.command` com comandos `RegistrarVendaCommand`, `FecharContaCommand`, `CancelarAgendamentoCommand`; invocação em `Sistema`.
   - **Esqueleto:**
     ```java
     public interface Command<R> { R execute(); }

     public final class RegistrarVendaCommand implements Command<Venda> {
         private final Sistema sistema; private final Usuario solicitante; private final Venda venda;
         public RegistrarVendaCommand(Sistema sistema, Usuario solicitante, Venda venda) { ... }
         @Override public Venda execute() {
             sistema.registrarVenda(solicitante, venda);
             return venda;
         }
     }
     ```
   - **Impacto:** melhora auditabilidade, permite fila/undo futuro; complexidade **média**.
   - **Plano incremental:** criar interface + 1 comando piloto (`RegistrarVendaCommand`), ajustar `Sistema` a usá-lo internamente, expandir para outras operações.

2. **Observer para eventos de negócio (venda registrada, conta fechada, cancelamento)**
   - **Problema:** acoplamento entre operação e geração de extratos/peristência; difícil adicionar novos efeitos.
   - **Onde aplicar:** criar `EventBus` simples em `br.ufvjm.barbearia.system` e eventos `VendaRegistrada`, `ContaFechada`, `AgendamentoCancelado` publicados em `Sistema`; handlers em `persist`/`service`.
   - **Esqueleto:**
     ```java
     public interface Event {}
     public interface EventHandler<E extends Event> { void handle(E event); }

     public final class SimpleEventBus {
         private final Map<Class<?>, List<EventHandler<?>>> handlers = new ConcurrentHashMap<>();
         public <E extends Event> void register(Class<E> type, EventHandler<E> handler) { ... }
         public <E extends Event> void publish(E event) { ... }
     }
     ```
   - **Impacto:** acoplamento menor, fácil adicionar logging/auditoria; complexidade **média**.
   - **Plano incremental:** criar bus, publicar evento em `registrarVenda`, adicionar handler que chama `gerarExtratoVenda`.

3. **State explícito para `Agendamento` (classes de estado)**
   - **Problema:** regras de transição estão como condicionais; expansão pode ficar complexa.
   - **Onde aplicar:** pacote `model.state` com classes `EmEsperaState`, `EmAtendimentoState`, `ConcluidoState`, `CanceladoState`; `Agendamento` delega operações a objeto de estado.
   - **Esqueleto:**
     ```java
     interface EstadoAgendamento {
         void avançar(Agendamento ctx);
         void cancelar(Agendamento ctx, BigDecimal retencao);
     }

     final class EmEsperaState implements EstadoAgendamento {
         public void avançar(Agendamento ctx) { ctx.setEstado(new EmAtendimentoState()); }
         public void cancelar(Agendamento ctx, BigDecimal retencao) { ctx.registrarCancelamento(retencao); ctx.setEstado(new CanceladoState()); }
     }
     ```
   - **Impacto:** facilita adicionar regras/efeitos por estado; complexidade **média**.
   - **Plano incremental:** introduzir interface de estado + conversor simples, migrar validações, testar transições.

4. **DAO mais granular com interface**
   - **Problema:** `JsonStorage` é utilitário monolítico; difícil trocar persistência ou testar.
   - **Onde aplicar:** criar `SnapshotRepository` (interface) e implementação `JsonSnapshotRepository` em `persist`; `Sistema` depende da interface.
   - **Esqueleto:**
     ```java
     public interface SnapshotRepository {
         void save(DataSnapshot snapshot, Path destino) throws IOException;
         DataSnapshot load(Path origem) throws IOException;
     }

     public final class JsonSnapshotRepository implements SnapshotRepository {
         @Override public void save(DataSnapshot snapshot, Path destino) throws IOException { JsonStorage.save(snapshot, destino); }
         @Override public DataSnapshot load(Path origem) throws IOException { return JsonStorage.load(origem); }
     }
     ```
   - **Impacto:** facilita mocks e múltiplas implementações (SQL, REST); complexidade **baixa/média**.

## Tabela Benefício vs Complexidade
| Padrão | Benefício p/ o projeto | Complexidade | Locais propostos | Observações |
| --- | --- | --- | --- | --- |
| Command | Alta: auditar/encadear operações críticas | Média | `system` (novos comandos) | Prepara para undo/logging. |
| Observer | Média/Alta: desacoplamento de efeitos (extratos/persistência) | Média | `system` (EventBus), `persist` (handlers) | Evita chamadas diretas a IO. |
| State (classes) | Média: clareza das regras de status | Média | `model` (`Agendamento`) | Útil se regras crescerem. |
| DAO interface | Média: testabilidade e troca de storage | Baixa/Média | `persist` (`SnapshotRepository`) | Passo natural para camadas limp as. |

## Passos de Adoção Recomendados (Backlog)
- **P0 (imediato):**
  - Introduzir `SnapshotRepository` e adaptar `Sistema.saveAll/loadAll` a usar a interface.
  - Criar `Command` base e mover `registrarVenda` para `RegistrarVendaCommand`.
- **P1 (curto prazo):**
  - Implementar `SimpleEventBus` com handler de extrato de venda; publicar em `registrarVenda` e `fecharContaAtendimento`.
  - Adicionar testes unitários para comandos e evento de venda.
- **P2 (médio prazo):**
  - Evoluir `Agendamento` para state pattern explícito; migrar validações.
  - Avaliar cadeias de validação (Chain of Responsibility) para autorização/políticas de cancelamento.

## Conclusão
O projeto já utiliza **Strategy** (comparators), **Adapter** (Gson TypeAdapters), **Builder** (DataSnapshot) e **Factory Method** utilitário, além de uma forma simplificada de **State** e um DAO monolítico para snapshots. Adoções futuras mais valiosas são **Command** e **Observer** para desacoplar operações críticas, um **DAO** com interface para testabilidade e uma evolução do **State** em `Agendamento` para acomodar regras crescentes, evitando over-engineering nos demais padrões.
