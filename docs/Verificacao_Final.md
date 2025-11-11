# Verificação Final – Projeto Barbearia

**Percentual de conformidade estimado:** 100%

## Tabela de conformidade das questões

| Questão | Status | Evidências | Ações executadas | Resultado |
|---------|:------:|------------|------------------|-----------|
| 1 | OK | Estruturas de domínio mantendo relações (Cliente⇄Pessoa, Agendamento⇄Cliente/Conta)【F:src/main/java/br/ufvjm/barbearia/model/Cliente.java†L13-L137】【F:src/main/java/br/ufvjm/barbearia/model/Agendamento.java†L14-L214】【F:src/main/java/br/ufvjm/barbearia/model/ContaAtendimento.java†L13-L230】 | Revisada a modelagem; nenhum ajuste funcional necessário. | Conformidade confirmada. |
| 2 | OK | Verificações de papel via `assertAdmin`/`assertColaboradorOuAdmin` garantindo bloqueio de operações sensíveis.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L1003-L1015】 | Validado fluxo de autorização, sem alterações. | Conformidade confirmada. |
| 3 | OK | `toString()` implementado em entidades concretas (Cliente, Agendamento, Servico, Conta).【F:src/main/java/br/ufvjm/barbearia/model/Cliente.java†L127-L137】【F:src/main/java/br/ufvjm/barbearia/model/Agendamento.java†L196-L208】【F:src/main/java/br/ufvjm/barbearia/model/Servico.java†L89-L98】【F:src/main/java/br/ufvjm/barbearia/model/ContaAtendimento.java†L222-L230】 | Revisão textual apenas. | Conformidade confirmada. |
| 4 | OK | Construtores das subclasses invocando `super(...)` e preservando campos herdados.【F:src/main/java/br/ufvjm/barbearia/model/Cliente.java†L58-L63】【F:src/main/java/br/ufvjm/barbearia/model/Usuario.java†L24-L59】 | Verificação; nenhum ajuste. | Conformidade confirmada. |
| 5 | OK | Vetor estático de três estações documentado com lavagem na primeira posição.【F:src/main/java/br/ufvjm/barbearia/model/Estacao.java†L5-L71】 | Adicionado Javadoc detalhado para a estrutura fixa. | Conformidade reforçada. |
| 6 | OK | CRUD de usuários restrito a administradores com substituição segura.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L232-L306】 | Revisão; sem mudanças. | Conformidade confirmada. |
| 7 | OK | CRUD de clientes com validação de IDs e ordenação configurável.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L208-L231】 | Revisão; sem mudanças. | Conformidade confirmada. |
| 8 | OK | Listagem e impressão de OS por cliente disponíveis no núcleo.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L645-L655】 | Revisão; sem mudanças. | Conformidade confirmada. |
| 9 | OK | Estruturas dinâmicas (`List`, `Deque`) para agenda e fila secundária.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L493-L575】 | Documentado `realizarAgendamento` e fila secundária com Javadoc. | Conformidade reforçada. |
| 10 | OK | Extratos automáticos de serviço, venda e cancelamento com vinculação ao cliente.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L706-L753】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L755-L788】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L790-L819】 | Mantida lógica; adicionada documentação das rotinas de extrato. | Conformidade reforçada. |
| 11 | OK | Contadores estáticos (encapsulado e protegido) atualizados na criação do serviço e reidratados no carregamento.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L135-L166】【F:src/main/java/br/ufvjm/barbearia/model/Servico.java†L44-L120】【F:src/main/java/br/ufvjm/barbearia/model/Cliente.java†L44-L94】 | Revisão; sem ajustes. | Conformidade confirmada. |
| 12 | OK | Contador centralizado de OS com incremento único e getter público.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L127-L134】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L493-L507】 | Revisão; sem ajustes. | Conformidade confirmada. |
| 13 | OK | Comparators de cliente/agendamento usados nas listagens e no relatório operacional.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L321-L359】【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L431-L485】 | Revisão; sem ajustes. | Conformidade confirmada. |
| 14 | OK | Persistência JSON com Gson e reidratação de contadores via `loadAll`.【F:src/main/java/br/ufvjm/barbearia/persist/JsonStorage.java†L20-L83】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L812-L879】 | Revisão; sem ajustes. | Conformidade confirmada. |
| 15 | OK | Demonstração explícita do uso de `Iterator` e `foreach` no main dirigido.【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L401-L429】 | Revisão textual; sem mudanças. | Conformidade confirmada. |
| 16 | OK | `Collections.sort` aplicado com comparadores distintos para clientes e agendamentos.【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L431-L483】 | Revisão; sem ajustes. | Conformidade confirmada. |
| 17 | OK | Implementação de `Sistema.find` + comparação com `Collections.binarySearch` no roteiro final.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L103-L125】【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L487-L534】 | Revisão; sem ajustes. | Conformidade confirmada. |
| 18 | OK | Pipeline ponta-a-ponta com fila secundária, cancelamentos (35%), extratos e persistência final.【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L536-L774】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L571-L643】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L475-L503】 | Revisão geral; sem ajustes. | Conformidade confirmada. |

## Boas práticas de POO
- Classes de domínio documentadas com responsabilidades e invariantes, reforçando encapsulamento e baixo acoplamento.【F:src/main/java/br/ufvjm/barbearia/model/Cliente.java†L13-L137】【F:src/main/java/br/ufvjm/barbearia/model/Servico.java†L8-L120】
- Núcleo `Sistema` centraliza regras transversais (fila, contadores, persistência) seguindo SRP e evitando duplicação.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L47-L503】

## Comentários e JavaDoc
- Inseridos comentários e JavaDoc explicativos em `Estacao`, nas rotinas da fila secundária e nos geradores de extratos para facilitar manutenção.【F:src/main/java/br/ufvjm/barbearia/model/Estacao.java†L5-L89】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L521-L575】【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L706-L804】
- `EntregaFinalMain` recebeu documentação detalhada do fluxo de execução e das funções utilitárias.【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L1-L44】【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L768-L780】

## Build / CI
- Tentativas de `mvn -q -DskipTests clean package` e `mvn -q -DskipTests package` falharam por erro 403 ao baixar plugins do Maven Central (ambiente offline).【0aa981†L1-L11】【61dfb8†L1-L11】
- Execução de `mvn -q exec:java` e `mvn -q javadoc:javadoc` igualmente bloqueadas pela indisponibilidade dos plugins.【b41977†L1-L9】【ac8f24†L1-L9】
- Incluído `docs/build-offline.md` com instruções de mirror/offline para contornar o problema.【F:docs/build-offline.md†L1-L32】

## Documentação
- Atualizado `README.md` e `docs/EntregaFinal.md` com orientações de execução e mapa das evidências.【F:README.md†L1-L27】【F:docs/EntregaFinal.md†L1-L32】
- Adicionados `docs/README.md`, `docs/cenarios.md`, `docs/diagramas/README.md` e o próprio `docs/Verificacao_Final.md` consolidando materiais e cenários.【F:docs/README.md†L1-L23】【F:docs/cenarios.md†L1-L30】【F:docs/diagramas/README.md†L1-L11】

## Arquivos alterados
- `.gitignore` – ignorar `target/` e `data/` para evitar rastrear artefatos de build.【F:.gitignore†L1-L2】
- `pom.xml` – fixadas versões de `maven-clean-plugin` e `maven-resources-plugin` para estabilidade em ambientes restritos.【F:pom.xml†L33-L58】
- `README.md` – guia principal reorganizado com comandos de build e referências de documentação.【F:README.md†L1-L27】
- `docs/EntregaFinal.md`, `docs/README.md`, `docs/cenarios.md`, `docs/diagramas/README.md`, `docs/build-offline.md`, `docs/Verificacao_Final.md` – documentação ampliada para entrega final.【F:docs/EntregaFinal.md†L1-L32】【F:docs/README.md†L1-L23】【F:docs/cenarios.md†L1-L30】【F:docs/diagramas/README.md†L1-L11】【F:docs/build-offline.md†L1-L32】
- `src/main/java/br/ufvjm/barbearia/model/Estacao.java` – Javadoc completo das estações fixas.【F:src/main/java/br/ufvjm/barbearia/model/Estacao.java†L5-L89】
- `src/main/java/br/ufvjm/barbearia/system/Sistema.java` – documentação ampliada da fila secundária, cancelamentos e extratos.【F:src/main/java/br/ufvjm/barbearia/system/Sistema.java†L521-L804】
- `src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java` – documentação e esclarecimentos das utilidades de verificação.【F:src/main/java/br/ufvjm/barbearia/system/EntregaFinalMain.java†L1-L780】
