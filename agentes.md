# agentes.md — Sistema de Barbearia
**Data:** 2025-10-19 23:43  
**Projeto:** Barbearia (Agenda, Vendas, Estoque, Financeiro, Relatórios)  
**Contexto:** Documento que define os **agentes** (humanos e de software), suas **responsabilidades**, **permissões**, **eventos** e **pontos de integração** no sistema.

---

## 1) Visão rápida
- **Agentes humanos:** Administrador, Colaborador/Atendente, Barbeiro, Cliente (externo), Fornecedor (externo).
- **Agentes de software (camada de aplicação):** Sistema (núcleo), Serviços (Agenda/Atendimento/Estoque/Venda/Relatório/Despesa/Caixa/Auditoria), Persistência JSON.
- **Objetivo:** garantir clareza de **quem faz o quê**, **o que cada agente pode acessar** e **quais fluxos cada um dispara**.

---

## 2) Mapa dos agentes

### 2.1 Agentes humanos
| Agente | Descrição | Interfaces principais | UCs iniciados |
|---|---|---|---|
| **Administrador** | Dono do negócio; acesso total (exceto ações técnicas de código). | Login, Cadastros, Relatórios, Financeiro, Estoque. | UC02, UC03, UC04, UC06–UC07, UC08, UC17 (consulta), UC19–UC25, UC26–UC27 |
| **Colaborador/Atendente** | Operação do dia‑a‑dia: agenda, PDV, conta, clientes. | Login, Clientes, Agenda, PDV, Extratos. | UC01, UC04–UC05, UC08–UC18, UC22–UC23 |
| **Barbeiro** | Profissional que executa serviços. | Login, Agenda, Status/Consumo. | UC04, UC12–UC14 |
| **Cliente** (externo) | Consumidor de serviços/produtos. | Telefone/whatsapp; não entra no sistema. | Indireto via Colaborador (UC10, UC17, UC15, UC18) |
| **Fornecedor** (externo) | Origem de mercadorias. | NF/romaneio; não entra no sistema. | Indireto via Admin (UC19) |

### 2.2 Agentes de software
| Agente | Responsabilidade | Pacote/Classes |
|---|---|---|
| **Sistema (núcleo)** | Orquestra cadastro, agenda, pilha secundária, extratos, persistência; mantém contadores estáticos. | `br.ufvjm.barbearia.system.Sistema` |
| **Persistência JSON** | Salvar/recuperar snapshot e extratos em disco (Gson). | `br.ufvjm.barbearia.persist.JsonStorage`, `DataSnapshot`, `ExtratoIO` |
| **Serviços** | Regras de negócio por domínio. | `br.ufvjm.barbearia.service.*` (no projeto atual, concentramos no `Sistema`) |
| **Comparators** | Ordenação/consulta de listas. | `br.ufvjm.barbearia.compare.*` |
| **Auditoria** | Registro de ações críticas (before/after). | `br.ufvjm.barbearia.model.AuditoriaLog` (ou `service`/`persist`) |

---

## 3) Permissões por agente (RACI simplificado)
- **R (Responsible)** executa; **A (Accountable)** responde; **C (Consulted)** pode opinar; **I (Informed)** notificado.

| Recurso/Ação | Admin | Colaborador | Barbeiro | Cliente | Fornecedor |
|---|:---:|:---:|:---:|:---:|:---:|
| **Login** | A/R | A/R | A/R | – | – |
| **Clientes – CRUD** | A/R | R | – | I | – |
| **Usuários/Funcionários – CRUD** | A/R | – | – | – | – |
| **Serviços – CRUD** | A/R | – | – | I | – |
| **Produtos – CRUD** | A/R | – | – | I | C |
| **Estoque – Recebimento** | A/R | I | – | – | C |
| **Agenda – Verificar/Agendar** | A | R | C | I | – |
| **Fila secundária (pilha)** | A | R | C | I | – |
| **Status de atendimento** | I | R | R | I | – |
| **Consumo de produto** | I | R | R | I | – |
| **Conta/Nota atendimento** | A | R | C | I | – |
| **Venda (PDV)** | A | R | – | I | – |
| **Despesas – Lançar** | A/R | – | – | – | – |
| **Fechamento de caixa** | A | R | – | I | – |
| **Relatório Diário** | A/R | R | – | I | – |
| **Relatório Mensal** | A/R | I | – | I | – |
| **Balanço Mensal** | A/R | – | – | I | – |
| **Auditoria** | A/R | I | I | – | – |
| **Permissões** | A/R | – | – | – | – |

> **Regra:** Funcionários **não** têm acesso a **Despesas** e **Balanço Mensal**.

---

## 4) Eventos e fluxos por agente

### 4.1 Administrador
- **Gatilhos:** contratação/alteração de funcionário (UC02), definição de catálogo (UC06–UC07), compras (UC19), despesas (UC21), relatórios e balanço (UC23–UC25), permissões (UC27).
- **Fluxos críticos:** lançamento de despesas; recebimento de fornecedores (atualiza custo médio e estoque); homologação do fechamento de caixa.

### 4.2 Colaborador/Atendente
- **Gatilhos:** atendimento telefônico/recepção → verificar vaga (UC09), agendar (UC10), vincular barbeiro (UC11), criar lista de serviços (UC12), emitir conta (UC15), registrar venda (UC18).
- **Fluxos críticos:** cancelamento com **retenção 35%** (UC16), manutenção da **pilha** de espera (UC17), geração e **salvamento de extratos**.

### 4.3 Barbeiro
- **Gatilhos:** iniciar/finalizar atendimento (UC13), registrar consumo de produto (UC14).
- **Fluxos críticos:** atualização correta do **status** (transições válidas); consumo faturado vs. interno.

### 4.4 Sistema (núcleo)
- **Gatilhos:** criação de OS → **incrementar contador**; cancelamento → liberar slot e acionar pilha; persistência periódica/on‑demand.
- **Fluxos críticos:** atomicidade nas operações (estoque/financeiro/agenda).

---

## 5) Invariantes e regras por agente
- **Admin**: único com acesso a **UC21 (Despesas)** e **UC25 (Balanço)**; pode ver custos de produtos.
- **Colaborador**: não altera permissões/usuários; vê estoque (sem custo); cria e cancela OS (aplicando **35%**).
- **Barbeiro**: só muda status/consumo; não cria/edita clientes/usuários.
- **Sistema**: mantém `Estacao.ESTACOES[3]` fixo, contadores estáticos, e garantias de integridade (ex.: estação 1 para serviços com lavagem).

---

## 6) Entradas/Saídas por agente
| Agente | Entradas | Saídas |
|---|---|---|
| **Admin** | Dados de usuário/produto/serviço/compra/despesa | Relatórios, Balanço, estoque atualizado |
| **Colaborador** | Dados de cliente, agendamento, venda, cancelamento | OS criada/atualizada, extratos, conta/nota |
| **Barbeiro** | OS vigente, produtos a consumir | Status atualizado, consumo registrado |
| **Sistema** | Comandos do usuário (UI/CLI), dados JSON | Persistência (JSON), extratos, logs/auditoria |

---

## 7) Persistência e auditoria (responsabilidade do sistema)
- **Formato:** JSON (Gson).  
- **Entidades persistidas:** Clientes, Usuários, Serviços, Produtos, Agendamentos (OS), Vendas, Contas, Despesas, Recebimentos.  
- **Extratos:** um arquivo `.txt` por evento de serviço/venda, associado ao **Cliente**.  
- **Boas práticas:** `try-with-resources`, paths normalizados, validação e fallback de dados.  
- **Auditoria:** registrar ação, usuário, recurso e detalhes (before/after quando pertinente).

---

## 8) Pilha de atendimentos secundários (agente: Colaborador/Sistema)
- **Tipo:** `Deque<Agendamento>` (LIFO).  
- **Operações:** `push(agendamentoSecundario)`, `pop()` ao surgir vaga, `peek()` para inspeção.  
- **Regras:** somente **agendamentos de espera** entram na pilha; ao cancelar um primário, o sistema tenta alocar o **topo** da pilha.

---

## 9) Métricas de agente (SLOs sugeridos)
- **Verificação de vagas:** < 2s para disponibilidade do dia.  
- **Persistência JSON:** < 200ms para salvar snapshot até 5k registros.  
- **Geração de extrato:** imediata (< 50ms) após OS/venda.  
- **Integridade:** 0 erros de estoque negativo; 0 OS “concluída” sem antes “em atendimento”.

---

## 10) Segurança e privacidade
- **Login/Perfil:** checagens por papel (Admin/Colaborador/Barbeiro).  
- **CPF:** armazenado como **hash + máscara** (`CpfHash`), **nunca** o valor puro.  
- **Senhas:** armazenadas com **hash** (não reversível).  
- **Logs:** trilha de ações críticas (CRUD, estoque, financeiro).

---

## 11) Checklist por agente (implementação)

### Administrador
- [ ] CRUD de usuários (UC02) com `super(...)` em `Usuario`  
- [ ] Serviços/Produtos (UC06–UC07)  
- [ ] Recebimento fornecedor (UC19) impactando custo médio  
- [ ] Despesas (UC21) e Relatórios/Balanço (UC23–UC25)  
- [ ] Permissões (UC27)

### Colaborador/Atendente
- [ ] Clientes CRUD (UC01/UC05)  
- [ ] Agenda (UC09–UC12) + Cancelamento (UC16)  
- [ ] Consumo/Conta (UC14–UC15) + Vendas (UC18)  
- [ ] Pilha secundária (UC17)  
- [ ] Extratos impressos e salvos

### Barbeiro
- [ ] Atualizar status (UC13) com transição válida  
- [ ] Registrar consumo (UC14)

### Sistema
- [ ] Vetor fixo de estações (`Estacao.ESTACOES`)  
- [ ] Contadores estáticos (OS e veículos)  
- [ ] Save/Load JSON + Extratos + Auditoria  
- [ ] Comparator para Cliente e Agendamento  
- [ ] Impressão de OS por cliente

---

## 12) Glossário de termos
- **OS (Ordem de Serviço):** equivalente a **Agendamento** com lista de serviços, status e recursos alocados.  
- **Fila secundária:** pilha LIFO de clientes aguardando vaga após cancelamentos.  
- **Extrato:** resumo textual de um serviço/venda (impresso e salvo).  
- **Snapshot:** conjunto completo de listas persistidas em JSON.

---

## 13) Ligações com o diagrama de classes
- **Herança:** `Pessoa` (abstrata) → `Cliente`, `Usuario`.  
- **Composição:** `Agendamento` ◼ `ItemDeServico` ◼ `ConsumoDeProduto`; `Venda` ◼ `ItemVenda`; `ContaAtendimento` ◼ `ItemContaProduto`; `RecebimentoFornecedor` ◼ `ItemRecebimento`.  
- **Agregação:** `CaixaDiario` ◇ `Venda` e `ContaAtendimento`.  
- **Enums/VOs:** `StatusAtendimento`, `FormaPagamento`, `Dinheiro`, `Quantidade`, `Periodo`, `CpfHash` etc.

---

## 14) Apêndice — Contadores de Veículo (exigência didática)
- **Encapsulado (em `Sistema`)**: `private static int totalVeiculosEncapsulado;` + getters/setters — (+) controle e validação; (–) mais verboso.  
- **Protected (em `Cliente`)**: `protected static int totalVeiculosProtegido;` — (+) simples; (–) menos seguro (exposto a subclasses/pacote).  
- **`Veiculo`** incrementa **ambos** no construtor.

---

## 15) Referências internas
- **Relatório completo:** `Relatorio_Projeto_Barbearia.md`  
- **Diagrama de classes (completo):** `uml_barbearia_classes.svg` / `.png`  
- **Diagrama light (model – PDF):** `uml_barbearia_model_light.pdf`
