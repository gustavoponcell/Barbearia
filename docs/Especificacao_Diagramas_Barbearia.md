# Especificação de Diagramas do Sistema Barbearia

## 1. Capa e Sumário
**Data:** 2025-02-14  
**Descrição:** Documento de referência para projetar os diagramas de casos de uso, cenários, classes e estados do sistema Java da barbearia, alinhado ao código-fonte e aos requisitos já registrados.

### Sumário
- [2. Contexto e Escopo](#2-contexto-e-escopo)
- [3. Diagrama de Casos de Uso — Ficha Técnica](#3-diagrama-de-casos-de-uso--ficha-técnica)
  - [3.1 Atores](#31-atores)
  - [3.2 Casos de Uso (com IDs)](#32-casos-de-uso-com-ids)
  - [3.3 Matriz Atores x Casos de Uso](#33-matriz-atores-x-casos-de-uso)
  - [3.4 Notas de desenho](#34-notas-de-desenho)
- [4. Cenários (Fluxos de Eventos) — Todos os Casos de Uso](#4-cenários-fluxos-de-eventos--todos-os-casos-de-uso)
- [5. Diagrama de Classes — Blueprint Textual](#5-diagrama-de-classes--blueprint-textual)
  - [5.1 Pacotes](#51-pacotes)
  - [5.2 Classes do Domínio](#52-classes-do-domínio)
  - [5.3 Associações & Multiplicidades](#53-associações--multiplicidades)
  - [5.4 Restrições e Invariantes](#54-restrições-e-invariantes)
- [6. Diagrama de Estados — Entidades-Chave](#6-diagrama-de-estados--entidades-chave)
- [7. Rastreamento Requisito ↔ Artefato](#7-rastreamento-requisito--artefato)
- [8. Apêndice — Notas para o Desenho em Ferramentas UML](#8-apêndice--notas-para-o-desenho-em-ferramentas-uml)

## 2. Contexto e Escopo
- **Resumo do problema:** Barbearia com três estações fixas (`Estacao.ESTACOES[0..2]`) compartilhadas entre atendimentos, sendo a estação 1 (índice 0) a única com lavagem. O salão opera agenda de ordens de serviço com sinal, possui fila secundária (pilha LIFO) para encaixes, loja de produtos com estoque mínimo, registra vendas, contas de atendimento, despesas, recebimentos de fornecedor, extratos e balanço mensal. Persistência local em JSON e extratos em arquivos texto.
- **Atores internos:** administrador (opera cadastros críticos, finanças e permissões), colaborador/atendente (cadastros operacionais, agenda, vendas, cancelamentos, extratos), barbeiro (atualiza status e consumos), cliente (via telefone/registro manual) e fornecedor (entrada de produtos e notas fiscais).
- **Escopo funcional coberto:** cadastro e manutenção de clientes, usuários e serviços; agenda, cancelamentos com retenção de 35%; criação de contas e vendas; controle de estoque e recebimentos; relatórios diário/mensal; balanço mensal; gerenciamento de fila secundária; geração de extratos e persistência JSON.
- **Restrições/Políticas:** autenticação com perfis (`Papel`), permissões aplicadas em `Sistema.assertAdmin`/`assertColaboradorOuAdmin` antes de operações sensíveis; cancelamento retém 35% do valor (`Sistema.RETENCAO_CANCELAMENTO`); vetor estático de estações (`Estacao.ESTACOES`); fila secundária com `Deque` (`ArrayDeque`) para encaixes; persistência exclusiva em JSON (`JsonStorage`) e extratos em disco (`ExtratoIO`); objetos de valor evitam dados inconsistentes (`CpfHash`, `Dinheiro`, `Quantidade`, `Periodo`, `Email`, `Telefone`, `Endereco`).

## 3. Diagrama de Casos de Uso — Ficha Técnica
### 3.1 Atores
| Ator | Responsabilidade principal |
| --- | --- |
| Administrador | Controla cadastros críticos (usuários, serviços, produtos), finanças, relatórios e balanço. |
| Colaborador/Atendente | Mantém clientes, agenda, fila secundária, contas, cancelamentos e vendas diárias. |
| Barbeiro | Atualiza status de atendimento e registra consumo de produtos nos serviços. |
| Cliente | Solicita agendamentos, cancelamentos e extratos via atendimento humano. |
| Fornecedor | Entrega produtos e notas fiscais para atualização de estoque e contas a pagar. |
| Sistema de Autenticação (intra) | Valida login/logout e armazena sessão (representado pelo núcleo `Sistema`). |

### 3.2 Casos de Uso (com IDs)
Cada caso lista atores envolvidos, objetivo, pré/pós-condições, regras e relacionamentos.

#### UC-01 – Incluir Cliente
- **Atores:** Colaborador (primário), Administrador (alternativo).
- **Objetivo:** Registrar um cliente com endereço, contato e CPF hash.
- **Pré-condições:** Usuário autenticado com papel Colaborador ou Administrador; cliente não existente (verificação prévia opcional).
- **Pós-condições:** Cliente presente em `Sistema.clientes`; ID definido via `UUID.randomUUID()` e persistido.
- **Regras/Restrições:** Campos obrigatórios validados por `Pessoa`/`Cliente` (nome, endereço, telefone, email, CPF hash); clientes iniciam com status ativo.
- **Relacionamentos:** <<include>> UC-09 (Verificar se existe cliente cadastrado) quando a rotina exige consulta prévia.

#### UC-02 – Editar Cliente
- **Atores:** Colaborador, Administrador.
- **Objetivo:** Atualizar dados de contato do cliente.
- **Pré-condições:** Usuário autenticado com permissão; cliente existente.
- **Pós-condições:** Cliente substituído em lista via `Sistema.editarCliente` com verificação de ID.
- **Regras/Restrições:** ID informado deve coincidir com o objeto atualizado; validações de nulos em setters protegidos; mantém histórico de extratos.
- **Relacionamentos:** <<include>> UC-09 para localizar cliente.

#### UC-03 – Remover Cliente
- **Atores:** Administrador (primário), Colaborador (apenas se política permitir remoção lógica, não implementada – recomenda-se Administrador).
- **Objetivo:** Excluir cliente da base ativa.
- **Pré-condições:** Usuário com permissão (idealmente Administrador); cliente existente sem agendamentos futuros.
- **Pós-condições:** Registro removido de `Sistema.clientes` ou exceção se inexistente.
- **Regras/Restrições:** Operação lança `IllegalArgumentException` se ID não encontrado.

#### UC-04 – Gerenciar Funcionário (Incluir/Editar/Remover)
- **Atores:** Administrador.
- **Objetivo:** Cadastrar e manter usuários internos (colaboradores, barbeiros, administradores).
- **Pré-condições:** Login ativo como Administrador.
- **Pós-condições:** Usuário criado/editado/removido de `Sistema.usuarios`.
- **Regras/Restrições:** Uso obrigatório de `Sistema.cadastrarUsuario/editarUsuario/removerUsuario`; validação de papel (`Papel`), login único e senha hash; apenas Administrador pode executar (lança `PermissaoNegadaException`).
- **Relacionamentos:** <<include>> UC-14 (Autenticar usuário) para garantir sessão.

#### UC-05 – Alterar Senha de Administrador
- **Atores:** Administrador (próprio ou superior).
- **Objetivo:** Atualizar hash de senha do administrador.
- **Pré-condições:** Usuário autenticado e autorizado; usuário alvo ativo.
- **Pós-condições:** `Usuario.alterarSenha` aplicado; senha anterior inválida.
- **Regras/Restrições:** Hash validado como não nulo; ideal registrar auditoria (fora do escopo atual).

#### UC-06 – Verificar Produto no Estoque
- **Atores:** Colaborador, Administrador.
- **Objetivo:** Consultar quantidade atual e status de mínimo.
- **Pré-condições:** Usuário autenticado.
- **Pós-condições:** Consulta retorna dados de `Produto`; nenhuma alteração.
- **Regras/Restrições:** Uso de `Produto.abaixoDoMinimo`; não expõe custo médio a colaboradores (política de UI).

#### UC-07 – Receber Produtos de Fornecedor e Atualizar Estoque
- **Atores:** Administrador (primário), Fornecedor (secundário).
- **Objetivo:** Registrar notas fiscais com itens e atualizar estoque/custos.
- **Pré-condições:** Administrador autenticado; produtos cadastrados.
- **Pós-condições:** `RecebimentoFornecedor` adicionado; estoque dos produtos movimentado; caixa registra saída se informado.
- **Regras/Restrições:** `Sistema.registrarRecebimentoFornecedor` exige papel Admin; validações de `ItemRecebimento` (quantidade/custo positivos) e `Produto.movimentarEntrada`; pagamento não pode exceder total.
- **Relacionamentos:** <<include>> UC-06 para verificação de estoque; <<include>> UC-18 para atualizar caixa.

#### UC-08 – Verificar Vaga na Agenda
- **Atores:** Colaborador.
- **Objetivo:** Conferir disponibilidade de estação em período solicitado.
- **Pré-condições:** Cliente identificado; período válido; usuário autenticado.
- **Pós-condições:** Disponibilidade retornada; em caso de conflito, opção de fila secundária.
- **Regras/Restrições:** Estação deve existir (`Estacao.ESTACOES`); para serviços com lavagem, selecionar estação 1.
- **Relacionamentos:** <<include>> UC-11 (Gerenciar fila secundária) quando indisponível.

#### UC-09 – Verificar Cliente Cadastrado
- **Atores:** Colaborador.
- **Objetivo:** Confirmar existência de cliente pelo ID/nome antes de operações.
- **Pré-condições:** Usuário autenticado; chave de busca fornecida.
- **Pós-condições:** Cliente localizado ou exceção.
- **Regras/Restrições:** Uso de comparadores (`ClientePorNome`, `ClientePorEmail`) quando necessário.

#### UC-10 – Realizar Agendamento
- **Atores:** Colaborador (primário), Cliente (inicia pedido), Barbeiro (opcional se definido).
- **Objetivo:** Criar ordem de serviço com serviços e estação.
- **Pré-condições:** Cliente ativo; período livre; usuário autenticado; serviços disponíveis.
- **Pós-condições:** `Agendamento` criado via `Sistema.criarAgendamento`/`realizarAgendamento`; contador de OS incrementado; itens associados.
- **Regras/Restrições:** Serviços requerem lavagem respeitando estação; sinal registrado (`Dinheiro`); status inicia em `EM_ESPERA`.
- **Relacionamentos:** <<include>> UC-09; <<include>> UC-12 (Criar lista de serviços); <<extend>> UC-11 (Fila secundária) se slot indisponível.

#### UC-11 – Gerenciar Fila Secundária
- **Atores:** Colaborador, Sistema.
- **Objetivo:** Incluir agendamentos de espera e promover encaixes.
- **Pré-condições:** Usuário autenticado; agendamento sem slot disponível.
- **Pós-condições:** `Sistema.filaSecundaria` atualizada (push/pop/peek).
- **Regras/Restrições:** Estrutura LIFO (`ArrayDeque`); somente agendamentos aguardando vaga.

#### UC-12 – Criar Lista de Serviços para Atendimento
- **Atores:** Colaborador, Barbeiro.
- **Objetivo:** Montar itens de serviço com preços e consumos.
- **Pré-condições:** Agendamento criado; serviços cadastrados.
- **Pós-condições:** `Agendamento.itens` preenchidos com `ItemDeServico` e `ConsumoDeProduto`.
- **Regras/Restrições:** Duração e preço positivos; consumo referencia produtos existentes.
- **Relacionamentos:** <<include>> UC-06 para validar estoque; <<include>> UC-13 para associar barbeiro.

#### UC-13 – Atualizar Status de Atendimento
- **Atores:** Barbeiro (primário), Colaborador (pode disparar início/fim), Sistema.
- **Objetivo:** Mudar status de OS conforme progresso.
- **Pré-condições:** Agendamento existente e atribuído; usuário autenticado com papel permitido.
- **Pós-condições:** `Agendamento.status` atualizado (EM_ESPERA → EM_ATENDIMENTO → CONCLUIDO ou CANCELADO).
- **Regras/Restrições:** `Agendamento.alterarStatus` valida transições.

#### UC-14 – Autenticar Usuário (Login/Logout)
- **Atores:** Administrador, Colaborador, Barbeiro.
- **Objetivo:** Validar credenciais e encerrar sessão.
- **Pré-condições:** Usuário cadastrado ativo.
- **Pós-condições:** Sessão permitida para demais UCs; logout limpa sessão (tratado pela camada de interface; `Sistema` assume usuário recebido já autenticado).
- **Regras/Restrições:** Login imutável; senha armazenada como hash.

#### UC-15 – Emitir Conta/Extrato de Atendimento
- **Atores:** Colaborador (primário), Cliente (recebe), Sistema (gera arquivo).
- **Objetivo:** Calcular total da conta, registrar forma de pagamento e gerar extrato textual.
- **Pré-condições:** Conta existente; itens de serviço/produto registrados; usuário autenticado com permissão.
- **Pós-condições:** `ContaAtendimento` fechada (`fechada=true`), extrato gravado via `Sistema.gerarExtratoServico` e arquivo associado ao cliente.
- **Regras/Restrições:** Total deve ser calculado antes de fechar; forma de pagamento obrigatória; extrato único por conta.

#### UC-16 – Cancelar Agendamento (retém 35%)
- **Atores:** Colaborador, Administrador.
- **Objetivo:** Cancelar OS aplicando retenção de 35% e liberar vaga.
- **Pré-condições:** Agendamento existente em status não cancelado; usuário com permissão.
- **Pós-condições:** Status `CANCELADO`; retenção registrada em `ContaAtendimento.CancelamentoRegistro`; extrato de cancelamento salvo; fila secundária reavaliada.
- **Regras/Restrições:** Percentual fixo (`Sistema.RETENCAO_CANCELAMENTO`); valores calculados via `Agendamento.cancelar`; geração de extrato obriga `cliente.registrarExtrato`.


#### UC-17 – Registrar Venda de Produtos/Serviços
- **Atores:** Colaborador (primário), Administrador (consulta), Cliente (destinatário).
- **Objetivo:** Registrar venda independente de atendimento.
- **Pré-condições:** Produtos cadastrados; usuário autenticado com permissão.
- **Pós-condições:** `Venda` adicionada; extrato gerado (`Sistema.gerarExtratoVenda`).
- **Regras/Restrições:** `Venda.calcularTotal` exige itens e verifica desconto; extrato único.

#### UC-18 – Atualizar Caixa Diário
- **Atores:** Colaborador (primário), Administrador (consulta/consolidação).
- **Objetivo:** Registrar entradas/saídas vinculadas a contas, vendas e recebimentos.
- **Pré-condições:** Caixa aberto (`Sistema.abrirCaixa` ou obtido); usuário autenticado conforme permissão.
- **Pós-condições:** `CaixaDiario` atualizado com movimentos, contas e vendas; saldo recalculado.
- **Regras/Restrições:** Valores não negativos; motivos obrigatórios; consolidação calcula saldo de fechamento.

#### UC-19 – Emitir Relatórios de Vendas/Serviços (Dia e Mês)
- **Atores:** Administrador (primário), Colaborador (visualização operacional).
- **Objetivo:** Gerar relatórios textual-operacional e financeiro.
- **Pré-condições:** Usuário autenticado; dados existentes.
- **Pós-condições:** Texto de relatório produzido (`Sistema.emitirRelatorioOperacional` e `Sistema.emitirRelatorioFinanceiro`).
- **Regras/Restrições:** Relatório financeiro exige papel Admin; validação de moeda.

#### UC-20 – Gerar Balanço Mensal (Serviços e Vendas)
- **Atores:** Administrador.
- **Objetivo:** Consolidar receitas de vendas e despesas para o mês.
- **Pré-condições:** Admin autenticado; competência informada.
- **Pós-condições:** Valor `Dinheiro` retornado (`Sistema.calcularBalancoMensal`).
- **Regras/Restrições:** Moedas coerentes; despesas filtradas por `YearMonth`.

#### UC-21 – Emitir Extrato do Cliente
- **Atores:** Colaborador (primário), Cliente.
- **Objetivo:** Disponibilizar histórico de extratos e comprovantes.
- **Pré-condições:** Cliente localizado; conta/venda com extrato gerado.
- **Pós-condições:** Referências armazenadas no cliente fornecidas/impresas; nenhuma alteração de estado.
- **Regras/Restrições:** Extratos persistidos em `data/extratos`; somente uma geração por evento.

#### UC-22 – Verificar Produto para Consumo
- **Atores:** Barbeiro (primário), Colaborador (suporte).
- **Objetivo:** Confirmar disponibilidade de produto para consumo interno durante atendimento.
- **Pré-condições:** Agendamento em andamento; produto cadastrado; usuário autenticado como Barbeiro/Colaborador.
- **Pós-condições:** Consumo registrado em `ItemDeServico.registrarConsumo`.
- **Regras/Restrições:** Quantidade compatível com estoque; modo de consumo indica faturado ou interno.

#### UC-23 – Registrar Despesa
- **Atores:** Administrador.
- **Objetivo:** Lançar despesas operacionais no sistema.
- **Pré-condições:** Categoria e valor informados.
- **Pós-condições:** `Despesa` adicionada à lista; pagamento opcional registrado.
- **Regras/Restrições:** Categoria obrigatória; descrição não vazia; pagamento armazena data.

#### UC-24 – Persistir/Restaurar Dados
- **Atores:** Administrador, Sistema (secundário).
- **Objetivo:** Salvar snapshot em JSON e recarregar dados.
- **Pré-condições:** Admin autenticado para salvar; arquivo disponível para carregar.
- **Pós-condições:** Arquivo JSON atualizado (`JsonStorage.save`); dados reidratados (`JsonStorage.load`, `Servico.reidratarContadores`).
- **Regras/Restrições:** Diretórios criados automaticamente; exceções encapsuladas em `UncheckedIOException`.

#### UC-25 – Imprimir Ordens de Serviço do Cliente
- **Atores:** Colaborador.
- **Objetivo:** Listar agendamentos associados ao cliente para comunicação.
- **Pré-condições:** Cliente localizado; usuário autenticado.
- **Pós-condições:** Impressão via console (`Sistema.imprimirOrdensDeServicoDoCliente`); nenhuma alteração em dados.
- **Regras/Restrições:** Usa `Sistema.listarOrdensDeServicoDoCliente` para recuperar dados ordenados.

### 3.3 Matriz Atores x Casos de Uso
Legenda: **R** (Responsável), **S** (Suporta/participa), **V** (Visualiza/consulta).

| Caso de Uso | Administrador | Colaborador | Barbeiro | Cliente | Fornecedor | Sistema |
| --- | --- | --- | --- | --- | --- | --- |
| UC-01 Incluir Cliente | S | R |  | V |  | S |
| UC-02 Editar Cliente | S | R |  | V |  | S |
| UC-03 Remover Cliente | R |  |  | I |  | S |
| UC-04 Gerenciar Funcionário | R |  |  |  |  | S |
| UC-05 Alterar Senha Admin | R |  |  |  |  | S |
| UC-06 Verificar Produto | R | R |  |  |  | S |
| UC-07 Receber Fornecedor | R | S |  |  | S | R |
| UC-08 Verificar Vaga Agenda | S | R | S | V |  | S |
| UC-09 Verificar Cliente | S | R |  | V |  | S |
| UC-10 Realizar Agendamento | S | R | S | V |  | R |
| UC-11 Gerenciar Fila | S | R | S | V |  | R |
| UC-12 Criar Lista Serviços | S | R | R | V |  | S |
| UC-13 Atualizar Status | S | S | R | V |  | S |
| UC-14 Autenticar Usuário | R | R | R |  |  | S |
| UC-15 Emitir Conta/Extrato | S | R | S | V |  | R |
| UC-16 Cancelar Agendamento | R | R | S | V |  | R |
| UC-17 Registrar Venda | R | R |  | V |  | R |
| UC-18 Atualizar Caixa | R | R |  |  |  | R |
| UC-19 Emitir Relatórios | R | S |  | V |  | S |
| UC-20 Gerar Balanço | R |  |  | V |  | S |
| UC-21 Emitir Extrato Cliente | S | R |  | V |  | R |
| UC-22 Verificar Produto Consumo | S | S | R |  |  | S |
| UC-23 Registrar Despesa | R |  |  |  |  | S |
| UC-24 Persistir/Restaurar | R |  |  |  |  | R |
| UC-25 Imprimir OS Cliente | S | R |  | V |  | R |

### 3.4 Notas de desenho
- **Fronteira do sistema:** delimitar o núcleo `Sistema` envolvendo operações de agenda, estoque, finanças e persistência; atores externos (Cliente, Fornecedor) ficam fora da fronteira.
- **Agrupamentos:** separar casos de uso em clusters “Cadastros”, “Agenda/Atendimento”, “Financeiro/Relatórios”, “Persistência”.
- **Estereótipos sugeridos:** `<<include>>` entre UC-09 e operações que exigem cliente; `<<extend>>` de UC-11 sobre UC-10; `<<include>>` de UC-14 (autenticação) para casos administrados.
- **Posicionamento:** colocar Administrador à esquerda (cadastros/financeiro), Colaborador central (agenda/vendas), Barbeiro à direita (status/consumo), Cliente e Fornecedor abaixo como externos passivos.

## 4. Cenários (Fluxos de Eventos) — Todos os Casos de Uso
Para cada UC, listar objetivo, atores, pré-condições, fluxos principais, alternativos, exceções, pós-condições, dados manipulados, regras e observações de UI.

### UC-01 – Incluir Cliente
- **Objetivo:** Registrar novo cliente ativo.
- **Ator Primário:** Colaborador. **Secundários:** Administrador, Sistema.
- **Pré-condições:** Sessão autenticada; cliente informado; campos obrigatórios validados (`Pessoa`, `Cliente`).
- **Fluxo Principal:**
  1. Operador seleciona “Novo Cliente”.
  2. Preenche nome, endereço completo, telefone, email, CPF mascarado.
  3. Sistema valida dados (construtor de `Cliente`).
  4. `Sistema.cadastrarCliente` adiciona à lista.
  5. Sistema confirma inclusão e disponibiliza ID.
- **Fluxos Alternativos:**
  - 2a. Email inválido → `Email.of` rejeita; operador corrige.
  - 2b. CPF inválido → `CpfHash.fromMasked` lança erro; operador ajusta.
- **Fluxos de Exceção:**
  - 3e. Campo nulo → `Objects.requireNonNull` interrompe; UI sinaliza obrigatoriedade.
- **Pós-condições:** Cliente ativo com lista de extratos vazia; contadores protegidos inalterados.
- **Dados manipulados:** `Cliente`, `Endereco`, `Telefone`, `Email`, `CpfHash`.
- **Regras:** Nenhum campo pode ser nulo; ID imutável; cliente começa ativo.
- **Observações de UI:** Apresentar máscara de CPF; informar ID gerado.

### UC-02 – Editar Cliente
- **Objetivo:** Atualizar endereço/contato.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Cliente existente; autenticação válida.
- **Fluxo Principal:**
  1. Operador busca cliente (UC-09).
  2. Atualiza campos de contato.
  3. Sistema valida (`Cliente.atualizarContato`).
  4. `Sistema.editarCliente` substitui registro se ID coincidir.
  5. Sistema confirma atualização.
- **Fluxos Alternativos:**
  - 4a. ID divergente → exceção e operação abortada.
- **Fluxos de Exceção:**
  - 2e. Campo vazio → validação em objetos de valor.
- **Pós-condições:** Registro refletindo novos dados.
- **Dados:** `Cliente`, `Endereco`, `Telefone`, `Email`.
- **Regras:** Não alterar ID; manter histórico de extratos.
- **UI:** Destacar campos obrigatórios; mostrar extratos existentes.

### UC-03 – Remover Cliente
- **Objetivo:** Excluir cliente.
- **Ator Primário:** Administrador.
- **Pré-condições:** Cliente localizado; confirmar ausência de OS futuras.
- **Fluxo Principal:**
  1. Admin seleciona cliente.
  2. Sistema valida permissão (`assertAdmin`).
  3. `Sistema.removerCliente` remove por ID.
  4. Sistema confirma exclusão ou informa inexistência.
- **Fluxos Alternativos:**
  - 3a. Cliente com agendamento futuro → política recomenda desativação (fora do escopo atual).
- **Fluxos de Exceção:**
  - 3e. ID inexistente → `IllegalArgumentException`.
- **Pós-condições:** Cliente ausente da lista.
- **Dados:** `Cliente` (ID).
- **Regras:** Apenas Administrador.
- **UI:** Alertar sobre perda de histórico lógico (arquivos de extrato permanecem).

### UC-04 – Gerenciar Funcionário (CRUD)
- **Objetivo:** Cadastrar/editar/remover usuários internos.
- **Ator Primário:** Administrador.
- **Pré-condições:** Sessão de Administrador.
- **Fluxo Principal (Incluir):**
  1. Admin acessa módulo de usuários.
  2. Preenche dados pessoais, papel, login e senha hash.
  3. Sistema valida (`Usuario` construtor).
  4. `Sistema.cadastrarUsuario` adiciona à lista.
  5. Interface confirma criação.
- **Fluxos Alternativos:**
  - Editar: `Sistema.editarUsuario` valida ID antes de substituir.
  - Remover: `Sistema.removerUsuario` elimina registro.
- **Fluxos de Exceção:**
  - Tentativa por colaborador → `PermissaoNegadaException`.
- **Pós-condições:** Lista de usuários atualizada.
- **Dados:** `Usuario`, `Papel`, `Email`, `Endereco`, `Telefone`.
- **Regras:** Login imutável; senhas em hash; usuários podem ser desativados.
- **UI:** Bloquear edição de login; indicar papel claramente.

### UC-05 – Alterar Senha de Administrador
- **Objetivo:** Atualizar credencial.
- **Ator Primário:** Administrador.
- **Pré-condições:** Usuário alvo existente e ativo.
- **Fluxo Principal:**
  1. Admin seleciona perfil.
  2. Informa novo hash de senha.
  3. Sistema valida não nulidade.
  4. `Usuario.alterarSenha` substitui valor.
  5. Confirmar alteração.
- **Fluxos de Exceção:**
  - Hash vazio → `Objects.requireNonNull`.
- **Pós-condições:** Nova senha vigente.
- **Dados:** `Usuario.senhaHash`.
- **Regras:** Registrar auditoria futura (não implementado).
- **UI:** Solicitar confirmação dupla da senha.

### UC-06 – Verificar Produto no Estoque
- **Objetivo:** Consultar saldo e status mínimo.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Produto cadastrado; usuário autenticado.
- **Fluxo Principal:**
  1. Operador filtra produto.
  2. Sistema exibe `Produto.getEstoqueAtual`, `abaixoDoMinimo`, preços.
- **Fluxos Alternativos:**
  - 1a. Produto inexistente → informar ausência.
- **Pós-condições:** Nenhuma mudança.
- **Dados:** `Produto`, `Quantidade`, `Dinheiro`.
- **Regras:** Estoque não negativo; custo médio visível apenas a Administrador (política de UI).
- **UI:** Destacar alerta se abaixo do mínimo.

### UC-07 – Receber Produtos de Fornecedor
- **Objetivo:** Registrar entrada de mercadorias e atualizar estoque.
- **Ator Primário:** Administrador.
- **Pré-condições:** Produtos existentes; NF disponível.
- **Fluxo Principal:**
  1. Admin abre módulo de recebimento.
  2. Informa fornecedor, NF, data/hora.
  3. Adiciona itens (produto, quantidade, custo unitário).
  4. Sistema valida itens (`ItemRecebimento` construtor).
  5. `Sistema.registrarRecebimentoFornecedor` atualiza estoque (`Produto.movimentarEntrada`) e registra no caixa se pagamento informado.
  6. Sistema armazena `RecebimentoFornecedor` e, se aplicável, registra pagamento parcial.
- **Fluxos Alternativos:**
  - 5a. Pagamento parcial → `RecebimentoFornecedor.registrarPagamento` calcula saldo.
- **Fluxos de Exceção:**
  - Quantidade negativa → `IllegalArgumentException`.
  - Pagamento > total → exceção.
- **Pós-condições:** Estoque ajustado; recebimento salvo; caixa atualizado se pagamento.
- **Dados:** `RecebimentoFornecedor`, `ItemRecebimento`, `Produto`, `CaixaDiario`.
- **Regras:** Apenas Administrador; moeda consistente; custo médio atualizado via política interna.
- **UI:** Permitir anexar comprovante; indicar saldo pendente.

### UC-08 – Verificar Vaga na Agenda
- **Objetivo:** Confirmar disponibilidade.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Dados de serviço e período.
- **Fluxo Principal:**
  1. Operador escolhe período e estação.
  2. Sistema consulta conflitos na lista de agendamentos.
  3. Se livre, indica possibilidade de agendar; senão, sugere fila secundária.
- **Fluxos Alternativos:**
  - Serviço requer lavagem → UI sugere estação 1.
- **Pós-condições:** Nenhuma alteração.
- **Dados:** `Agendamento.periodo`, `Estacao`.
- **Regras:** Fim após início; serviços com lavagem na estação apropriada.
- **UI:** Visual de agenda destacando estações.

### UC-09 – Verificar Cliente Cadastrado
- **Objetivo:** Encontrar cliente.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Chave informada.
- **Fluxo Principal:**
  1. Operador pesquisa por nome/email.
  2. Sistema ordena via comparator e retorna resultado.
- **Fluxos de Exceção:**
  - Cliente não encontrado → informar e permitir cadastro (UC-01).
- **Pós-condições:** Cliente selecionado.
- **Dados:** `Sistema.listarClientesOrdenados`, `ClientePorNome`.
- **UI:** Auto-complete ou lista paginada.

### UC-10 – Realizar Agendamento
- **Objetivo:** Criar OS.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Cliente ativo; vaga disponível; serviços definidos.
- **Fluxo Principal:**
  1. Seleciona cliente (UC-09).
  2. Escolhe serviços (UC-12) e verifica necessidade de lavagem.
  3. Seleciona estação e horário; verifica disponibilidade (UC-08).
  4. Sistema cria `Agendamento`, adiciona itens, atribui barbeiro se informado.
  5. Sistema incrementa contador e adiciona à lista.
- **Fluxos Alternativos:**
  - Sem vaga → direciona para UC-11.
- **Fluxos de Exceção:**
  - Período inválido → `IllegalArgumentException`.
- **Pós-condições:** Agendamento em `EM_ESPERA`, sinal registrado.
- **Dados:** `Agendamento`, `ItemDeServico`, `Dinheiro`, `Estacao`, `Usuario` (barbeiro).
- **Regras:** Itens obrigatórios; sinal com moeda válida; vetor de estações fixo.
- **UI:** Mostrar resumo com período, estação e serviços.

### UC-11 – Gerenciar Fila Secundária
- **Objetivo:** Administrar agendamentos em espera.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Agendamento sem vaga.
- **Fluxo Principal:**
  1. Operador decide enviar agendamento para fila.
  2. `Sistema.adicionarAgendamentoSecundario` empilha no topo.
  3. Ao liberar vaga, operador chama `Sistema.recuperarAgendamentoSecundario` para encaixe.
- **Fluxos de Exceção:**
  - Pop em fila vazia → `NoSuchElementException`.
- **Pós-condições:** Fila atualizada; agendamento promovido quando possível.
- **Dados:** `Deque<Agendamento>`.
- **Regras:** LIFO; apenas espera.
- **UI:** Painel mostrando ordem atual e tempo aguardado.

### UC-12 – Criar Lista de Serviços
- **Objetivo:** Definir itens de serviço.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Serviços cadastrados; agendamento em edição.
- **Fluxo Principal:**
  1. Operador seleciona serviço do catálogo.
  2. Define preço e duração (pode ajustar).
  3. Registra consumos de produto por item (`registrarConsumo`).
  4. Item é associado ao agendamento.
- **Fluxos de Exceção:**
  - Duração <= 0 → erro.
- **Pós-condições:** Lista de itens pronta; base para contas.
- **Dados:** `ItemDeServico`, `Servico`, `ConsumoDeProduto`, `Produto`, `Quantidade`.
- **Regras:** Preço `Dinheiro`; consumos indicam modo (`ModoConsumoProduto`).
- **UI:** Permitir adicionar múltiplos consumos por item.

### UC-13 – Atualizar Status
- **Objetivo:** Evoluir atendimento.
- **Ator Primário:** Barbeiro.
- **Pré-condições:** Agendamento associado; autenticação válida.
- **Fluxo Principal:**
  1. Barbeiro inicia atendimento → `alterarStatus(EM_ATENDIMENTO)`.
  2. Ao finalizar, marca `CONCLUIDO`.
- **Fluxos Alternativos:**
  - Cancelamento solicitado → UC-16.
- **Fluxos de Exceção:**
  - Transição inválida → `IllegalStateException`.
- **Pós-condições:** Status atualizado.
- **Dados:** `Agendamento.status`.
- **Regras:** Sequência definida.
- **UI:** Botões contextuais conforme status atual.

### UC-14 – Autenticar Usuário
- **Objetivo:** Garantir permissões.
- **Ator Primário:** Usuário interno.
- **Pré-condições:** Usuário cadastrado e ativo.
- **Fluxo Principal:**
  1. Usuário informa login/senha.
  2. Camada de UI valida hash e papel.
  3. Sessão criada; papel usado nas demais chamadas.
- **Fluxos de Exceção:**
  - Usuário inativo → negar acesso.
- **Pós-condições:** Papel disponível para validações.
- **Dados:** `Usuario.login`, `Usuario.senhaHash`, `Papel`.
- **Regras:** Senhas criptografadas; login único.
- **UI:** Mensagens genéricas de erro (segurança).

### UC-15 – Emitir Conta/Extrato
- **Objetivo:** Faturar atendimento.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Conta existente; serviços consumidos; autenticação.
- **Fluxo Principal:**
  1. Operador abre conta vinculada ao agendamento.
  2. Confere itens, descontos e ajustes.
  3. `ContaAtendimento.calcularTotal` soma serviços, produtos, ajustes e descontos.
  4. Seleciona forma de pagamento; `fecharConta` registra.
  5. `Sistema.gerarExtratoServico` cria arquivo e referencia em cliente/conta.
- **Fluxos Alternativos:**
  - Cancelamento antes de fechar → `registrarRetencaoCancelamento` (UC-16).
- **Fluxos de Exceção:**
  - Total não calculado → `IllegalStateException` ao fechar.
  - Desconto > total → exceção.
- **Pós-condições:** Conta fechada; extrato salvo.
- **Dados:** `ContaAtendimento`, `ItemContaProduto`, `ItemDeServico`, `Dinheiro`, `FormaPagamento`.
- **Regras:** Apenas um extrato por conta; ajustes podem ser crédito ou débito.
- **UI:** Informar caminho do extrato gerado.

### UC-16 – Cancelar Agendamento com Retenção
- **Objetivo:** Cancelar OS com penalidade.
- **Ator Primário:** Colaborador ou Administrador.
- **Pré-condições:** Agendamento existente, não cancelado.
- **Fluxo Principal:**
  1. Operador seleciona agendamento.
  2. Sistema calcula retenção `totalServicos * 0.35`.
  3. `Agendamento.cancelar` altera status e retorna objeto `Cancelamento`.
  4. `ContaAtendimento.registrarRetencaoCancelamento` registra crédito.
  5. `Sistema.gerarExtratoCancelamento` grava extrato e marca flags.
  6. Vaga liberada; sistema tenta promover topo da fila (UC-11).
- **Fluxos de Exceção:**
  - Agendamento já cancelado → erro.
- **Pós-condições:** Registro de cancelamento anexado; extrato armazenado.
- **Dados:** `Agendamento`, `ContaAtendimento.CancelamentoRegistro`, `Dinheiro`.
- **Regras:** Percentual fixo 35%; extrato único.
- **UI:** Informar valores de retenção e reembolso.

### UC-17 – Registrar Venda
- **Objetivo:** Registrar vendas de balcão.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Produtos selecionados; cliente opcional.
- **Fluxo Principal:**
  1. Operador abre venda.
  2. Adiciona itens (`ItemVenda`).
  3. Calcula total (`Venda.calcularTotal`).
  4. `Sistema.registrarVenda` valida permissão e salva.
  5. Sistema gera extrato (`gerarExtratoVenda`).
- **Fluxos de Exceção:**
  - Venda sem itens → erro.
  - Desconto > total → erro.
- **Pós-condições:** Venda com extrato.
- **Dados:** `Venda`, `ItemVenda`, `Dinheiro`, `FormaPagamento`.
- **Regras:** Apenas colaboradores/admins registram; extrato único.
- **UI:** Indicar extrato salvo e permitir impressão.

### UC-18 – Atualizar Caixa Diário
- **Objetivo:** Controlar fluxo de caixa.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Caixa aberto para data.
- **Fluxo Principal:**
  1. Operador cria ou seleciona `CaixaDiario`.
  2. Registra entradas/saídas (`registrarEntrada/registrarSaida`).
  3. Associa vendas e contas concluídas.
  4. Consolida (`consolidar`) ao fim do dia.
- **Fluxos de Exceção:**
  - Valor negativo → erro.
  - Caixa duplicado → `IllegalStateException` na abertura.
- **Pós-condições:** Saldo final calculado; movimentos registrados.
- **Dados:** `CaixaDiario`, `MovimentoCaixa`, `Dinheiro`, `Venda`, `ContaAtendimento`.
- **Regras:** Moeda consistente; consolidar antes de gerar relatório financeiro.
- **UI:** Mostrar resumo (entradas, saídas, saldo projetado).

### UC-19 – Emitir Relatórios
- **Objetivo:** Gerar relatórios diário/mensal.
- **Ator Primário:** Administrador.
- **Pré-condições:** Dados existentes; competência informada para financeiro.
- **Fluxo Principal:**
  1. Admin escolhe tipo (operacional ou financeiro).
  2. Sistema monta relatório via `emitirRelatorioOperacional` ou `emitirRelatorioFinanceiro`.
  3. Resultado exibido/exportado.
- **Fluxos de Exceção:**
  - Moeda nula → erro.
- **Pós-condições:** Texto pronto para impressão.
- **Dados:** Listas de clientes, agendamentos, vendas, despesas; contadores.
- **Regras:** Relatório financeiro restrito a Admin; validação de moeda.
- **UI:** Oferecer filtros de offset/limit.

### UC-20 – Gerar Balanço Mensal
- **Objetivo:** Calcular saldo do mês.
- **Ator Primário:** Administrador.
- **Pré-condições:** Competência informada; dados de vendas e despesas.
- **Fluxo Principal:**
  1. Admin seleciona competência e moeda.
  2. `Sistema.calcularBalancoMensal` agrega receitas e despesas.
  3. Resultado exibido e opcionalmente salvo.
- **Pós-condições:** Valor consolidado disponível.
- **Dados:** `Venda`, `Despesa`, `Dinheiro`, `YearMonth`.
- **Regras:** Moeda coerente; exceções se permissão insuficiente.
- **UI:** Mostrar detalhamento por categoria.

### UC-21 – Emitir Extrato do Cliente
- **Objetivo:** Reapresentar extratos gerados.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Cliente localizado; extratos existentes.
- **Fluxo Principal:**
  1. Operador abre ficha do cliente.
  2. UI lista `Cliente.getExtratosGerados` com caminhos.
  3. Operador imprime/abre arquivo.
- **Pós-condições:** Nenhuma alteração; apenas consulta.
- **Dados:** `Cliente.extratosGerados`.
- **Regras:** Extratos criados nos UCs 15, 16, 17; manter histórico.
- **UI:** Permitir abrir diretório `data/extratos`.

### UC-22 – Verificar Produto para Consumo
- **Objetivo:** Registrar consumo durante atendimento.
- **Ator Primário:** Barbeiro.
- **Pré-condições:** Agendamento em andamento; produto disponível.
- **Fluxo Principal:**
  1. Barbeiro seleciona serviço em execução.
  2. Escolhe produto e quantidade.
  3. `ItemDeServico.registrarConsumo` associa `ConsumoDeProduto`.
  4. UI registra saída de estoque conforme política.
- **Fluxos de Exceção:**
  - Produto indisponível → colaborador consulta UC-06.
- **Pós-condições:** Consumo listado na conta.
- **Dados:** `ConsumoDeProduto`, `Produto`, `Quantidade`, `ModoConsumoProduto`.
- **Regras:** Quantidade positiva; modo indica faturado ou interno.
- **UI:** Escolher se consumo será cobrado na conta.

### UC-23 – Registrar Despesa
- **Objetivo:** Lançar despesas.
- **Ator Primário:** Administrador.
- **Pré-condições:** Categoria e valor informados.
- **Fluxo Principal:**
  1. Admin preenche dados da despesa.
  2. Sistema valida (descrição não vazia, valor positivo).
  3. `Sistema.registrarDespesa` adiciona à lista.
- **Fluxos Alternativos:**
  - Registrar pagamento posterior (`Despesa.registrarPagamento`).
- **Pós-condições:** Despesa disponível para balanço.
- **Dados:** `Despesa`, `Dinheiro`, `CategoriaDespesa`, `YearMonth`.
- **Regras:** Apenas Admin; data de pagamento opcional.
- **UI:** Permitir filtro por competência.

### UC-24 – Persistir/Restaurar Dados
- **Objetivo:** Salvar/Carregar snapshot.
- **Ator Primário:** Administrador.
- **Pré-condições:** Caminho válido; permissão admin.
- **Fluxo Principal (Salvar):**
  1. Admin aciona “Salvar snapshot”.
  2. `Sistema.saveAll` monta `DataSnapshot` e invoca `JsonStorage.save`.
  3. Sistema confirma persistência.
- **Fluxo Principal (Restaurar):**
  1. Admin indica arquivo.
  2. `Sistema.loadAll` chama `JsonStorage.load`.
  3. Sistema substitui listas, recalcula contadores (`Servico.reidratarContadores`, `Sistema.redefinirTotalOrdensServico`).
- **Fluxos de Exceção:**
  - Falha IO → `UncheckedIOException`.
- **Pós-condições:** Snapshot persistido ou dados recarregados.
- **Dados:** `DataSnapshot`, `JsonStorage`, `ExtratoIO`, `Path`.
- **Regras:** Diretórios criados automaticamente; somente Admin salva.
- **UI:** Exibir caminho padrão `data/sistema.json`.

### UC-25 – Imprimir Ordens do Cliente
- **Objetivo:** Disponibilizar histórico de OS.
- **Ator Primário:** Colaborador.
- **Pré-condições:** Cliente localizado; agendamentos existentes.
- **Fluxo Principal:**
  1. Operador seleciona cliente.
  2. `Sistema.listarOrdensDeServicoDoCliente` retorna ordens.
  3. `Sistema.imprimirOrdensDeServicoDoCliente` exibe no console.
- **Pós-condições:** Histórico apresentado; sem alterações.
- **Dados:** `Agendamento`, `Cliente`.
- **Regras:** Lista ordenada por início quando comparator informado.
- **UI:** Permitir exportar para PDF/planilha futuramente.

## 5. Diagrama de Classes — Blueprint Textual
### 5.1 Pacotes
| Pacote | Responsabilidade | Conteúdo-chave | Interações |
| --- | --- | --- | --- |
| `br.ufvjm.barbearia.system` | Orquestra regras globais, permissões, fila secundária, relatórios e persistência. | `Sistema`, `Main`, `EntregaFinalMain`. | Usa `model`, `value`, `enums`, `compare`, `persist`, `exceptions`, `util`. |
| `br.ufvjm.barbearia.model` | Entidades do domínio (clientes, usuários, serviços, agenda, finanças, estoque). | `Pessoa`, `Cliente`, `Usuario`, `Servico`, `Agendamento`, `ContaAtendimento`, `Produto`, `Venda`, `RecebimentoFornecedor`, `Despesa`, `CaixaDiario`, `Estacao`, `ItemDeServico`, `ConsumoDeProduto`, `ItemContaProduto`, `ItemVenda`, `ItemRecebimento`. | Consome objetos de valor e enums; manipulado por `Sistema`; persistido via `JsonStorage`. |
| `br.ufvjm.barbearia.value` | Objetos de valor imutáveis para segurança e consistência. | `Dinheiro`, `Quantidade`, `CpfHash`, `Email`, `Telefone`, `Endereco`, `Periodo`. | Usados por entidades e adapters de persistência. |
| `br.ufvjm.barbearia.enums` | Enumerações de papéis, status, formas de pagamento, categorias, modo de consumo. | `Papel`, `StatusAtendimento`, `FormaPagamento`, `CategoriaDespesa`, `ModoConsumoProduto`. | Referenciados em modelos e sistema. |
| `br.ufvjm.barbearia.compare` | Estratégias de ordenação e busca. | `ClientePorNome`, `ClientePorEmail`, `AgendamentoPorInicio`, `AgendamentoPorClienteNome`. | Usados por `Sistema` em relatórios e consultas. |
| `br.ufvjm.barbearia.persist` | Persistência JSON e extratos. | `JsonStorage`, `DataSnapshot`, `ExtratoIO`, adapters Gson. | Chamados por `Sistema` para salvar/carregar dados e extratos. |
| `br.ufvjm.barbearia.persist.adapters` | Adapters Gson para tipos customizados. | `LocalDateAdapter`, `LocalDateTimeAdapter`, `YearMonthAdapter`, `DinheiroAdapter`. | Registrados em `JsonStorage`. |
| `br.ufvjm.barbearia.exceptions` | Exceções de domínio. | `PermissaoNegadaException`. | Lançada por `Sistema` ao validar permissões. |
| `br.ufvjm.barbearia.util` | Infraestrutura auxiliar. | `Log`. | Usado por `Sistema` para registrar eventos. |

### 5.2 Classes do Domínio
A seguir, cada classe relevante com responsabilidade, atributos, métodos principais, relacionamentos, padrões e notas.

#### `br.ufvjm.barbearia.system.Sistema`
- **Responsabilidade:** Núcleo orquestrador (cadastros, agenda, finanças, relatórios, fila secundária, persistência).
- **Atributos:**
| Nome | Tipo | Visib. | Multiplicidade | Nulável | Descrição | Regras |
| --- | --- | --- | --- | --- | --- | --- |
| `totalOrdensServico` | `int` | `private static` | 1 | Não | Contador global de OS. | Incrementado em criação; sincronizado. |
| `totalServicos` | `int` | `private static` | 1 | Não | Contador global de serviços. | Mantido via `ServicoTracker`. |
| `RETENCAO_CANCELAMENTO` | `BigDecimal` | `private static final` | 1 | Não | Percentual 0.35 para cancelamentos. | Usado em UC-16. |
| `DATA_HORA_FORMATTER` | `DateTimeFormatter` | `private static final` | 1 | Não | Formato `dd/MM/yyyy HH:mm`. | Relatórios. |
| `DEFAULT_CLIENTE_COMPARATOR` | `ClientePorNome` | `private static final` | 1 | Não | Comparator padrão. | Substituído se nulo. |
| `DEFAULT_AGENDAMENTO_COMPARATOR` | `AgendamentoPorInicio` | `private static final` | 1 | Não | Comparator padrão de agendamentos. | |
| `EXTRATOS_DIR` | `Path` | `private static final` | 1 | Não | Diretório `data/extratos`. | Criado sob demanda. |
| Coleções (`clientes`, `usuarios`, `servicos`, `produtos`, `agendamentos`, `vendas`, `contas`, `despesas`, `recebimentos`, `caixas`) | `List<...>` | `private` | 0..* | Não | Coleções principais em memória. | Inicializadas com `ArrayList`; expostas como cópia. |
| `filaSecundaria` | `Deque<Agendamento>` | `private` | 0..* | Não | Pilha LIFO para espera. | `ArrayDeque`. |
- **Métodos principais:**
| Assinatura | Retorno | Lança | Efeitos | Regras |
| --- | --- | --- | --- | --- |
| `static <T> int find(List<T>, T, Comparator)` | `int` | `NullPointerException` | Busca linear. | Parâmetros não nulos. |
| `incrementarTotalOS()` / `getTotalOrdensServicoCriadas()` | `void` / `int` | — | Incrementa/consulta contador. | Sincronizado. |
| `getTotalServicos()` / `setTotalServicos(int)` | `int` / `void` | — | Consulta/atualiza contador encapsulado. | Sincronizado; valores não negativos. |
| `ServicoTracker.registrarCriacaoServico()` | `void` | — | Incrementa `totalServicos`. | Usado por `Servico`. |
| CRUD de clientes (`cadastrar`, `editar`, `remover`, `listarOrdenados`) | variado | `IllegalArgumentException` | Mantém lista. | IDs conferidos; comparators opcionais. |
| CRUD de usuários (`cadastrarUsuario`, `editarUsuario`, `removerUsuario`) | `void` | `PermissaoNegadaException` | Mantém usuários. | Apenas Admin. |
| `registrarDespesa/listarDespesas/removerDespesa` | `void`/`List` | `PermissaoNegadaException` | Mantém despesas. | Apenas Admin. |
| `calcularBalancoMensal` / `emitirRelatorioFinanceiro` | `Dinheiro` / `String` | `PermissaoNegadaException` | Consolida finanças. | Moeda coerente; Admin. |
| `emitirRelatorioOperacional` | `String` | — | Monta resumo textual. | Offset/limit validados. |
| CRUD de serviços/produtos | `void`/`List` | — | Mantém catálogo. | Objetos não nulos. |
| `registrarVenda` / `listarVendas` | `void` / `List` | `PermissaoNegadaException` | Armazena venda e gera extrato. | Venda exige colaborador/admin. |
| `criarContaAtendimento`, `fecharContaAtendimento` | `ContaAtendimento` / `ContaAtendimento` | `PermissaoNegadaException` | Cria/fecha contas com extrato. | Forma de pagamento obrigatória; total calculado. |
| `abrirCaixa`, `listarCaixas`, `obterCaixa` | `CaixaDiario` / `List` / `CaixaDiario` | `PermissaoNegadaException` | Mantém caixa diário. | Data única. |
| `criarAgendamento`, `realizarAgendamento`, `cancelarAgendamento` | `Agendamento` / `void` / `Agendamento.Cancelamento` | `PermissaoNegadaException` | Gerencia OS, fila e extratos de cancelamento. | Percentual fixo; fila secundária atualizada. |
| `adicionarAgendamentoSecundario`, `recuperarAgendamentoSecundario`, `inspecionarFilaSecundaria` | `void` / `Agendamento` / `Optional` | `NoSuchElementException` | Manipula pilha LIFO. | Agendamentos não nulos. |
| `registrarRecebimentoFornecedor` (sobrecargas) | `void` | `PermissaoNegadaException` | Atualiza estoque/caixa. | Apenas Admin. |
| `gerarExtratoServico/Venda/Cancelamento` | `void` | `UncheckedIOException` | Persiste extratos e marca entidades. | Apenas um extrato por evento. |
| `saveAll` / `loadAll` | `void` | `PermissaoNegadaException`, `UncheckedIOException` | Salva/carrega snapshot. | Admin salva; load recalcula contadores. |
- **Relacionamentos:** agrega listas de entidades (`Cliente`, `Usuario`, `Servico`, `Produto`, `Agendamento`, `ContaAtendimento`, `Venda`, `Despesa`, `RecebimentoFornecedor`, `CaixaDiario`); depende de `JsonStorage`, `ExtratoIO`, `DataSnapshot`; usa comparators e `Log`; lança `PermissaoNegadaException`.
- **Padrões:** Service Facade; Transaction Script; encapsulamento de contador; pilha LIFO.
- **Persistência:** Serializa coleções via `saveAll`; extratos em `data/extratos`.
- **Notas de desenho:** Representar como controlador central com associações para agregados e dependências para utilitários.

#### `br.ufvjm.barbearia.system.Main`
- **Responsabilidade:** Script demonstrativo que instancia o sistema, cria dados de exemplo e imprime relatórios.
- **Atributos:** Nenhum campo de instância; apenas constantes/métodos locais.
- **Métodos:**
| Assinatura | Retorno | Lança | Efeitos | Regras |
| --- | --- | --- | --- | --- |
| `main(String[] args)` | `void` | `UncheckedIOException` | Executa cenário completo (cadastros, agenda, vendas, extratos, snapshot). | Uso didático; orquestra métodos de `Sistema`. |
- **Relacionamentos:** Depende de `Sistema`, `Usuario`, `Cliente`, `Servico`, `Produto`, `Agendamento`, valores e enums.
- **Notas:** Representar como <<boundary>> de demonstração.

#### `br.ufvjm.barbearia.system.EntregaFinalMain`
- **Responsabilidade:** Executar checklist da entrega final, verificando regras e imprimindo evidências.
- **Atributos:**
| Nome | Tipo | Visib. | Multiplicidade | Nulável | Descrição |
| --- | --- | --- | --- | --- | --- |
| `BRL` | `Currency` | `private static final` | 1 | Não | Moeda padrão das verificações. |
- **Métodos principais:** `main`, `executarQuestao`, `extrairContagem`, utilitários de IO e formatação.
- **Relacionamentos:** Usa `Sistema`, `Path`, APIs de arquivos e streams.
- **Notas:** Estereótipo <<control>> de auditoria; não participa diretamente do modelo, mas confirma regras.

#### `br.ufvjm.barbearia.model.Pessoa`
- **Responsabilidade:** Superclasse abstrata de pessoas com identificação e contato.
- **Atributos:**
| Nome | Tipo | Visib. | Multiplicidade | Nulável | Descrição | Regras |
| --- | --- | --- | --- | --- | --- | --- |
| `id` | `UUID` | `private final` | 1 | Não | Identificador imutável. | Obrigatório. |
| `nome` | `String` | `private` | 1 | Não | Nome completo. | Não nulo; normalizado externamente. |
| `endereco` | `Endereco` | `private` | 1 | Não | Endereço completo. | Não nulo. |
| `telefone` | `Telefone` | `private` | 1 | Não | Telefone principal. | Não nulo. |
| `email` | `Email` | `private` | 1 | Não | E-mail válido. | Não nulo. |
- **Métodos:** getters, setters protegidos, `equals/hashCode` por ID, `toString` descritivo.
- **Relacionamentos:** Superclasse de `Cliente` e `Usuario`; utiliza objetos de valor (`Endereco`, `Telefone`, `Email`).
- **Notas:** Estereótipo <<abstract>>.

#### `br.ufvjm.barbearia.model.Cliente`
- **Responsabilidade:** Representa cliente com CPF hash, status e histórico de extratos.
- **Atributos:**
| Nome | Tipo | Visib. | Multiplicidade | Nulável | Descrição | Regras |
| --- | --- | --- | --- | --- | --- | --- |
| `cpf` | `CpfHash` | `private final` | 1 | Não | CPF mascarado/hasheado. | `CpfHash.fromMasked` valida formato. |
| `extratosGerados` | `List<String>` | `private final` | 0..* | Não | Referências a extratos salvos. | Lista imutável externamente. |
| `ativo` | `boolean` | `private` | 1 | Não | Situação do cliente. | Pode ser desativado/reabilitado. |
| `totalVeiculosProtegido`, `totalServicosProtegido` | `protected static int` | 1 | Não | Contadores didáticos. | Reidratação via métodos estáticos. |
- **Métodos:** `desativar`, `reativar`, `registrarExtrato`, `getExtratosGerados`, `atualizarContato`, incrementos e redefinições de contadores.
- **Relacionamentos:** Herda `Pessoa`; associado a `Agendamento`, `ContaAtendimento`, `Venda`, `Cliente.registrarExtrato` é chamado por `Sistema` e `ExtratoIO`.
- **Padrões:** Contadores estáticos demonstrando encapsulamento vs. protected.
- **Notas:** Extratos referenciam caminhos do filesystem.

#### `br.ufvjm.barbearia.model.Usuario`
- **Responsabilidade:** Usuário interno com papel e credenciais.
- **Atributos:**
| Nome | Tipo | Visib. | Multiplicidade | Nulável | Descrição | Regras |
| --- | --- | --- | --- | --- | --- | --- |
| `papel` | `Papel` | `private` | 1 | Não | Papel do usuário. | Não nulo. |
| `login` | `String` | `private final` | 1 | Não | Login imutável. | Não nulo. |
| `senhaHash` | `String` | `private` | 1 | Não | Hash da senha. | Não nulo; alterado via método. |
| `ativo` | `boolean` | `private` | 1 | Não | Status do usuário. | Permite desativação/reativação. |
- **Métodos:** `setPapel`, `alterarSenha`, `desativar`, `reativar`, getters e `toString`.
- **Relacionamentos:** Herda `Pessoa`; utilizado por `Sistema` para permissões; pode ser associado como barbeiro em `Agendamento`.
- **Notas:** Papéis disponíveis em `Papel` (Admin, Colaborador, Barbeiro).

#### `br.ufvjm.barbearia.model.Servico`
- **Responsabilidade:** Serviço ofertado com preço, duração e requisito de lavagem.
- **Atributos:** `id`, `nome`, `preco`, `duracaoMin`, `requerLavagem` (todos `private final`).
- **Métodos:** getters, `reidratarContadores`, `toString`; construtor valida nome/duração e incrementa contadores (`Sistema.ServicoTracker`, `Cliente.incrementarTotalServicosProtegido`).
- **Relacionamentos:** Utilizado por `ItemDeServico`; influencia seleção de `Estacao` em `Agendamento`.
- **Notas:** Representar dependência com `Sistema` e `Cliente` para contadores.

#### `br.ufvjm.barbearia.model.Agendamento`
- **Responsabilidade:** Ordem de serviço com cliente, barbeiro, estação, período, itens, status e sinal.
- **Atributos principais:**
| Nome | Tipo | Nulável | Descrição |
| --- | --- | --- | --- |
| `id` | `UUID` | Não | Identificador da OS. |
| `cliente` | `Cliente` | Não | Cliente vinculado. |
| `barbeiro` | `Usuario` | Sim | Barbeiro responsável (pode ser atribuído depois). |
| `estacao` | `Estacao` | Não | Estação reservada. |
| `inicio` / `fim` | `LocalDateTime` | Não | Janela do atendimento. |
| `itens` | `List<ItemDeServico>` | Não | Serviços planejados. |
| `status` | `StatusAtendimento` | Não | Estado atual. |
| `sinal` | `Dinheiro` | Não | Valor de sinal recebido. |
| Flags de extrato | `LocalDateTime`/`String` | Sim | Controle de extrato de cancelamento. |
- **Métodos:** `adicionarItemServico`, `associarBarbeiro`, `alterarStatus` (valida transições), `cancelar` (retorna `Cancelamento` com valores), `totalServicos`, `periodo`, `requerLavagem`, `marcarExtratoCancelamentoGerado`.
- **Relacionamentos:** Associação 1..1 com `Cliente` e `Estacao`; 0..1 com `Usuario` (barbeiro); composição com `ItemDeServico`; relação 1..1 com `ContaAtendimento` (via busca em `Sistema`); participa da fila secundária.
- **Notas:** Classe interna `Cancelamento` com valores de retenção e reembolso.

#### `br.ufvjm.barbearia.model.ContaAtendimento`
- **Responsabilidade:** Conta financeira agregando serviços, produtos, ajustes, descontos, cancelamentos e extratos.
- **Atributos principais:**
| Nome | Tipo | Nulável | Descrição |
| --- | --- | --- | --- |
| `id` | `UUID` | Não | Identificador da conta. |
| `agendamento` | `Agendamento` | Não | OS atendida. |
| `produtosFaturados` | `List<ItemContaProduto>` | Não | Produtos cobrados. |
| `servicosAdicionais` | `List<ItemDeServico>` | Não | Serviços extra. |
| `ajustes` | `List<AjusteConta>` | Não | Créditos/débitos manuais. |
| `desconto` | `Dinheiro` | Sim | Desconto opcional. |
| `total` | `Dinheiro` | Sim | Total calculado. |
| `formaPagamento` | `FormaPagamento` | Sim | Forma de pagamento após liquidação. |
| `cancelamentoRegistro` | `CancelamentoRegistro` | Sim | Dados da retenção. |
| `fechada` | `boolean` | Não | Indica encerramento. |
| Flags de extrato | `LocalDateTime`/`String` | Sim | Controle do extrato de serviço. |
- **Métodos:** `aplicarDesconto`, `adicionarProdutoFaturado`, `adicionarServicoFaturado`, `registrarAjuste`, `registrarRetencaoCancelamento`, `calcularTotal(Dinheiro)`, `liquidar`, `fecharConta`, `marcarExtratoServicoGerado`.
- **Relacionamentos:** Composição com `ItemContaProduto` e `ItemDeServico`; associação 1..1 com `Agendamento`; agregação com `CaixaDiario`; classes internas `AjusteConta` e `CancelamentoRegistro` (<<valueObject>>).
- **Notas:** `baseParaCalculo` zera total quando cancelado; garante descontos não excedentes.

#### `br.ufvjm.barbearia.model.Produto`
- **Responsabilidade:** Produto comercializado ou consumido, com controle de estoque, preços e custo médio.
- **Atributos:** `id`, `nome`, `sku`, `estoqueAtual`, `estoqueMinimo`, `precoVenda`, `custoMedio` (validados contra nulos e unidades compatíveis).
- **Métodos:** `movimentarEntrada`, `movimentarSaida` (impede estoque negativo), `abaixoDoMinimo`, `atualizarPrecoVenda`, `atualizarCustoMedio`, `atualizarNome`.
- **Relacionamentos:** Referenciado por `ItemDeServico`, `ItemContaProduto`, `ItemVenda`, `ItemRecebimento`, `ConsumoDeProduto`, `RecebimentoFornecedor`.

#### `br.ufvjm.barbearia.model.Venda`
- **Responsabilidade:** Venda avulsa com itens, desconto opcional, total e extrato.
- **Atributos principais:** `id`, `cliente` (opcional), `dataHora`, `itens`, `formaPagamento`, `desconto`, `total`, flags de extrato.
- **Métodos:** `adicionarItem`, `calcularTotal`, `marcarExtratoGerado`, getters.
- **Relacionamentos:** Associação opcional com `Cliente`; itens referenciam `Produto`; vinculado ao `CaixaDiario` e extratos.

#### `br.ufvjm.barbearia.model.RecebimentoFornecedor`
- **Responsabilidade:** Registrar notas de fornecedor, itens recebidos, total e pagamentos.
- **Atributos principais:** `id`, `fornecedor`, `dataHora`, `numeroNF`, `itens`, `total`, `pagamentoEfetuado`.
- **Métodos:** `adicionarItem`, `calcularTotal`, `registrarPagamento`, `getSaldoPendente`.
- **Relacionamentos:** Itens referenciam `Produto`; integra com `Sistema` e `CaixaDiario` para saída de caixa.

#### `br.ufvjm.barbearia.model.Despesa`
- **Responsabilidade:** Lançamento de despesas por categoria, valor e competência.
- **Atributos:** `id`, `categoria`, `descricao`, `valor`, `competencia`, `dataPagamento`.
- **Métodos:** `estaPaga`, `registrarPagamento`, `toString`.
- **Relacionamentos:** Utilizada por `Sistema` para relatórios e balanço; categoria definida por `CategoriaDespesa`.

#### `br.ufvjm.barbearia.model.CaixaDiario`
- **Responsabilidade:** Controlar saldo diário com entradas, saídas, vendas, contas e movimentos.
- **Atributos principais:** `data`, `saldoAbertura`, `entradas`, `saidas`, `saldoFechamento`, listas de `Venda`, `ContaAtendimento`, `MovimentoCaixa`.
- **Métodos:** `registrarEntrada`, `registrarSaida`, `adicionarVenda`, `adicionarConta`, `consolidar`, `projetarBalanco`.
- **Relacionamentos:** Associado a vendas, contas, recebimentos (saídas); `MovimentoCaixa` interno com tipos ENTRADA/SAIDA.

#### `br.ufvjm.barbearia.model.Estacao`
- **Responsabilidade:** Representar estação física com número e suporte a lavagem.
- **Atributos:** `numero`, `possuiLavagem`; vetor estático `ESTACOES[3]` inicializado com três posições fixas.
- **Métodos:** getters, `equals`, `hashCode`, `toString`.
- **Relacionamentos:** Referenciada por `Agendamento`; vetor usado diretamente por `Sistema` e UI.

#### Itens e Consumos
- **`ItemDeServico`**: associa `Servico` a preço e duração aplicados na OS; mantém lista de `ConsumoDeProduto`.
- **`ConsumoDeProduto`**: registra produto, quantidade (`Quantidade`) e modo (`ModoConsumoProduto`), informando se será faturado ou consumo interno.
- **`ItemContaProduto`**: item de produto faturado em conta; armazena `Produto`, `Quantidade`, `Dinheiro` unitário; `subtotal` multiplica quantidade.
- **`ItemVenda`**: item de venda avulsa; mesma estrutura de `ItemContaProduto`.
- **`ItemRecebimento`**: item de recebimento de fornecedor; guarda `Produto`, `Quantidade`, custo unitário (`Dinheiro`).

#### Objetos de Valor (`br.ufvjm.barbearia.value`)
- **`Dinheiro`**: valor monetário imutável com moeda; operações `somar`, `subtrair`, `multiplicar` com validação de moeda e escala fixa.
- **`Quantidade`**: quantidade imutável com unidade e escala; proíbe negativos.
- **`CpfHash`**: encapsula hash SHA-256 do CPF e máscara exibível; valida formato e repetição.
- **`Email`**: valida formato por regex e armazena string normalizada.
- **`Telefone`**: extrai DDD e número; fornece formato `(DD) 99999-9999`.
- **`Endereco`**: builder imutável com logradouro, número, complemento opcional, bairro, cidade, estado (sigla) e CEP formatado.
- **`Periodo`**: representa intervalo entre duas datas/horas; garante `fim` ≥ `inicio` e oferece método `contem`.

#### Enums (`br.ufvjm.barbearia.enums`)
- **`Papel`**: `ADMIN`, `COLABORADOR`, `BARBEIRO` — direciona permissões.
- **`StatusAtendimento`**: `EM_ESPERA`, `EM_ATENDIMENTO`, `CONCLUIDO`, `CANCELADO` — controla fluxo da OS.
- **`FormaPagamento`**: formas aceitas (`DINHEIRO`, `CARTAO_DEBITO`, `CARTAO_CREDITO`, `PIX`, `OUTRO`).
- **`CategoriaDespesa`**: categorias de despesas (limpeza, materiais, aluguel, etc.).
- **`ModoConsumoProduto`**: `CONSUMO_INTERNO`, `FATURADO` — identifica abatimento no estoque.

#### Comparators (`br.ufvjm.barbearia.compare`)
- **`ClientePorNome` / `ClientePorEmail`**: ordenam clientes por nome ou email.
- **`AgendamentoPorInicio` / `AgendamentoPorClienteNome`**: ordenam agendamentos por data/hora de início ou nome do cliente.
- **Uso:** injetados em relatórios e listagens do `Sistema`.

#### Persistência (`br.ufvjm.barbearia.persist`)
- **`DataSnapshot`**: DTO com listas de todas as entidades persistidas; builder facilita montagem.
- **`JsonStorage`**: utilitário `save`/`load` com `Gson` e adapters (`LocalDateAdapter`, `LocalDateTimeAdapter`, `YearMonthAdapter`, `DinheiroAdapter`); cria diretórios automaticamente.
- **`ExtratoIO`**: grava extratos `.txt` com timestamp `yyyyMMddHHmmss` em diretório configurável.
- **Adapters**: serialização customizada para datas e valores monetários.

#### Exceções e Utilitários
- **`PermissaoNegadaException`**: `RuntimeException` lançada quando papel insuficiente tenta executar operação protegida; sobrescreve `toString` para incluir causa.
- **`Log`** (`util`): wrapper de `java.util.logging` com formato padronizado e nível configurável via flag `barbearia.debug`.

### 5.3 Associações & Multiplicidades
| Classe A | Relacionamento | Classe B | Multiplicidade A→B | Multiplicidade B→A | Notas |
| --- | --- | --- | --- | --- | --- |
| `Sistema` | agrega | `Cliente` | 1 → 0..* | cada `Cliente` pertence a um `Sistema` | Coleção `clientes`. |
| `Sistema` | agrega | `Usuario` | 1 → 0..* | idem | Permissões aplicadas por papel. |
| `Sistema` | agrega | `Servico` | 1 → 0..* | idem | Contador sincronizado. |
| `Sistema` | agrega | `Produto` | 1 → 0..* | idem | Estoque em memória. |
| `Sistema` | agrega | `Agendamento` | 1 → 0..* | cada `Agendamento` pertence ao sistema | Fila secundária compartilha referência. |
| `Sistema` | agrega | `ContaAtendimento` | 1 → 0..* | idem | Conta vinculada a agendamento. |
| `Sistema` | agrega | `Venda` | 1 → 0..* | idem | Extratos de venda. |
| `Sistema` | agrega | `Despesa` | 1 → 0..* | idem | Apenas Admin acessa. |
| `Sistema` | agrega | `RecebimentoFornecedor` | 1 → 0..* | idem | Atualiza estoque e caixa. |
| `Sistema` | agrega | `CaixaDiario` | 1 → 0..* | cada caixa é único por data | `abrirCaixa` garante exclusividade. |
| `Cliente` | associado a | `Agendamento` | 1 → 0..* | Cada `Agendamento` tem 1 cliente | Relação obrigatória. |
| `Cliente` | associado a | `ContaAtendimento` | 1 → 0..* | Cada conta referencia 1 cliente via agendamento | Extratos por cliente. |
| `Cliente` | associado a | `Venda` | 0..1 → 0..* | Venda pode ser para consumidor final | Cliente opcional. |
| `Agendamento` | composição | `ItemDeServico` | 1 → 1..* | Itens pertencem à OS | Remoção da OS remove itens. |
| `ItemDeServico` | associação | `ConsumoDeProduto` | 1 → 0..* | Consumo pertence ao item | Modo de consumo indica faturamento. |
| `Agendamento` | associado a | `Usuario` (barbeiro) | 1 → 0..1 | Barbeiro pode atender várias OS | Opcional. |
| `Agendamento` | associado a | `Estacao` | 1 → 1 | Estação fixa por OS | Estações pré-definidas. |
| `ContaAtendimento` | composição | `ItemContaProduto` | 1 → 0..* | Produto faturado pertence à conta | Subtotal acumula no total. |
| `ContaAtendimento` | composição | `ItemDeServico` (adicionais) | 1 → 0..* | Serviços adicionais faturados | Compartilha definição. |
| `ContaAtendimento` | agregação | `CaixaDiario` | 0..* → 0..* | Conta pode compor caixa | Registrada ao fechar. |
| `Venda` | agregação | `CaixaDiario` | 0..* → 0..* | Vendas entram no caixa | Integrado via `Sistema`. |
| `RecebimentoFornecedor` | composição | `ItemRecebimento` | 1 → 1..* | Itens pertencem ao recebimento | Atualizam estoque. |
| `RecebimentoFornecedor` | associação | `Produto` | 1..* → 1 | Produto referenciado por item | Quantidade somada ao estoque. |
| `Produto` | associado a | `ConsumoDeProduto` | 1 → 0..* | Consumo referencia produto | Saída manual. |
| `Sistema` | dependência | `JsonStorage`, `ExtratoIO`, `DataSnapshot` | — | — | Persistência e extratos. |

### 5.4 Restrições e Invariantes
- **Status de atendimento:** Transições permitidas: `EM_ESPERA → EM_ATENDIMENTO → CONCLUIDO`; cancelamento pode ocorrer a partir de qualquer estado não cancelado.
- **Retenção de cancelamento:** Percentual fixo de 35% aplicado sobre `Agendamento.totalServicos`; extrato deve ser gerado imediatamente.
- **Estoque:** `Produto.movimentarSaida` impede estoque negativo; recebimentos garantem unidade compatível.
- **Fila secundária:** Implementada como pilha LIFO (`Deque`); apenas agendamentos sem vaga podem entrar; pop em fila vazia gera exceção.
- **Extratos:** Cada `ContaAtendimento` e `Venda` permite apenas um extrato; cancelamento também tem flag exclusiva.
- **Persistência:** `JsonStorage.save` cria diretórios e usa UTF-8; `load` retorna snapshot vazio quando arquivo inexistente.
- **Contadores:** `Sistema.totalOrdensServico` e `totalServicos` devem ser sincronizados com dados carregados; `Cliente.totalServicosProtegido` sincronizado via `Servico.reidratarContadores`.
- **Caixa Diário:** Apenas um caixa por data; `abrirCaixa` lança erro se duplicado; valores de movimentos não podem ser negativos.

## 6. Diagrama de Estados — Entidades-Chave

### Agendamento
- **Estados:**
  - `Criado`: instanciado e adicionado ao sistema (`EM_ESPERA`).
  - `Confirmado`: slot reservado e barbeiro definido (ainda `EM_ESPERA`).
  - `EmAtendimento`: status `EM_ATENDIMENTO` após início.
  - `Concluido`: status `CONCLUIDO` com atendimento finalizado.
  - `Cancelado`: status `CANCELADO` após execução de `cancelar` (retenção 35%).
- **Transições:**
| Origem | Evento | Guarda | Ação | Destino |
| --- | --- | --- | --- | --- |
| Criado | confirmar slot | Estação disponível | Associar barbeiro/serviços | Confirmado |
| Confirmado | iniciar atendimento | Usuário com papel Barbeiro/Colaborador | `alterarStatus(EM_ATENDIMENTO)` | EmAtendimento |
| EmAtendimento | finalizar atendimento | Serviços concluídos | `alterarStatus(CONCLUIDO)` | Concluido |
| Qualquer não cancelado | cancelar agendamento | Usuário com permissão; percentual 0.35 | `cancelar`, gerar extrato de cancelamento | Cancelado |
| Cancelado | reativar (não suportado) | — | — | — |
- **Ações de entrada/saída:**
  - Entrada em `Cancelado`: registrar retenção em `ContaAtendimento` e gerar extrato.
  - Saída de `Concluido`: habilita fechamento de conta.
- **Regras:** Serviços que exigem lavagem só podem ser confirmados na estação 1; fila secundária movimentada após cancelamento.

### ContaAtendimento
- **Estados:**
  - `Aberta`: criada para agendamento, aguardando itens e cálculo.
  - `Calculada`: `calcularTotal` executado, total disponível.
  - `Fechada`: `fecharConta` chamada com forma de pagamento; extrato gerado.
  - `Cancelada`: Conta ajustada por cancelamento (retenção registrada) sem cobrança adicional.
- **Transições:**
| Origem | Evento | Guarda | Ação | Destino |
| --- | --- | --- | --- | --- |
| Aberta | adicionar item | — | Acrescenta serviços/produtos | Aberta |
| Aberta | calcular total | Agendamento possui itens | `calcularTotal(totalServicos)` | Calculada |
| Calculada | fechar conta | Forma de pagamento informada | `fecharConta`, gerar extrato | Fechada |
| Qualquer | registrar cancelamento | `Agendamento` cancelado | `registrarRetencaoCancelamento`, zera base | Cancelada |
| Cancelada | gerar extrato cancelamento | Cancelamento registrado | `Sistema.gerarExtratoCancelamento` | Cancelada |
- **Ações:**
  - Entrada em `Fechada`: `ContaAtendimento.isFechada` true; extrato salvo.
  - Entrada em `Cancelada`: ajustes crédito aplicados e extrato específico.
- **Regras:** Desconto não pode exceder total; forma de pagamento obrigatória antes de fechar.

### Produto (Estoque)
- **Estados:**
  - `EstoqueNormal`: quantidade ≥ mínimo.
  - `EstoqueBaixo`: quantidade < mínimo.
  - `SemEstoque`: quantidade == 0.
- **Transições:**
| Origem | Evento | Guarda | Ação | Destino |
| --- | --- | --- | --- | --- |
| EstoqueNormal | consumo/saída | nova quantidade < mínimo | `movimentarSaida` | EstoqueBaixo |
| EstoqueBaixo | entrada | nova quantidade ≥ mínimo | `movimentarEntrada` | EstoqueNormal |
| Qualquer | consumo total | nova quantidade = 0 | `movimentarSaida` | SemEstoque |
| SemEstoque | recebimento | quantidade > 0 | `movimentarEntrada` | EstoqueBaixo ou EstoqueNormal (conforme mínimo) |
- **Regras:** Unidades devem coincidir; consumo durante atendimento deve considerar modo (`ConsumoDeProduto`).

## 7. Rastreamento Requisito ↔ Artefato
| Requisito | Casos de Uso | Classes/Métodos | Estados/Transições | Evidências |
| --- | --- | --- | --- | --- |
| CRUD de clientes e ID definido | UC-01, UC-02, UC-03, UC-25 | `Sistema.cadastrarCliente/editarCliente/removerCliente`, `Cliente` | — | `Sistema` linhas de CRUD; `Cliente` construtor e métodos. |
| CRUD de funcionários (colaborador/admin) | UC-04 | `Sistema.cadastrarUsuario/editarUsuario/removerUsuario`, `Usuario` | — | Permissão `assertAdmin`. |
| Alterar senha de administrador | UC-05 | `Usuario.alterarSenha`, `Sistema` valida permissão | — | Métodos de `Usuario`. |
| Verificar produto no estoque | UC-06 | `Sistema.listarProdutos`, `Produto.abaixoDoMinimo` | Estado Produto (`EstoqueBaixo`) | Classe `Produto`. |
| Verificar vaga na agenda | UC-08 | `Sistema.listarAgendamentosOrdenados`, `Agendamento.periodo` | Estados `Criado/Confirmado` | `Agendamento` período e status. |
| Realizar agendamentos | UC-10 | `Sistema.criarAgendamento`, `Agendamento` | Estados `Criado`, `Confirmado` | `Sistema` criação de OS. |
| Criar lista de serviços | UC-12 | `Agendamento.adicionarItemServico`, `ItemDeServico` | — | `ItemDeServico` e `ConsumoDeProduto`. |
| Verificar cliente cadastrado | UC-09 | `Sistema.listarClientesOrdenados`, `ClientePorNome` | — | Comparators. |
| Cancelar agendamento com retenção 35% | UC-16 | `Sistema.cancelarAgendamento`, `Agendamento.cancelar`, `ContaAtendimento.registrarRetencaoCancelamento` | Transição `→ Cancelado` | `Sistema.RETENCAO_CANCELAMENTO`. |
| Receber fornecedores e atualizar estoque | UC-07, UC-23 | `Sistema.registrarRecebimentoFornecedor`, `Produto.movimentarEntrada`, `RecebimentoFornecedor` | Estado Produto (`SemEstoque` → outros) | Classe `RecebimentoFornecedor`. |
| Emitir relatório de vendas/serviços | UC-19 | `Sistema.emitirRelatorioOperacional`, `Sistema.emitirRelatorioFinanceiro` | — | Relatórios no `Sistema`. |
| Gerar balanço mensal | UC-20 | `Sistema.calcularBalancoMensal` | — | Balanço em `Sistema`. |
| Emitir conta/extrato de cliente | UC-15, UC-21 | `ContaAtendimento.fecharConta`, `Sistema.gerarExtratoServico`, `Cliente.registrarExtrato` | Estados `Calculada→Fechada` | Extratos em `Sistema`. |
| Registrar venda | UC-17 | `Sistema.registrarVenda`, `Venda.calcularTotal`, `ExtratoIO` | — | `Venda` e extrato. |
| Gerenciar fila secundária | UC-11 | `Sistema.adicionarAgendamentoSecundario`, `Deque` | — | Pilha em `Sistema`. |
| Autenticar usuário | UC-14 | `Sistema.assertAdmin/assertColaboradorOuAdmin`, `Usuario` | — | Permissão e papéis. |
| Atualizar caixa diário | UC-18 | `CaixaDiario.registrarEntrada/Saida`, `Sistema.obterCaixa` | — | Classe `CaixaDiario`. |
| Registrar despesa | UC-23 | `Sistema.registrarDespesa`, `Despesa` | — | Classe `Despesa`. |
| Persistir/Restaurar dados | UC-24 | `Sistema.saveAll/loadAll`, `JsonStorage`, `DataSnapshot`, `Servico.reidratarContadores` | — | Persistência. |

## 8. Apêndice — Notas para o Desenho em Ferramentas UML
- **Agrupamento visual:** utilizar pacotes diferentes para módulos (System, Model, Value, Enums, Persist, Util) com cores distintas (ex.: tons de azul para `model`, laranja para `system`).
- **Casos de uso:** aplicar estereótipos `<<include>>`/`<<extend>>` com setas tracejadas; representar a fronteira do sistema como retângulo com título “Sistema Barbearia (Desktop/JSON)”.
- **Cenários:** ao desenhar diagramas de atividade complementar, indicar verificação de permissão como decisão com guardas `[Admin]`, `[Colaborador]`.
- **Classes:** usar ícones/estereótipos `<<entity>>` para `model`, `<<valueObject>>` para valores, `<<service>>` para `Sistema`. Multiplicidades devem seguir tabela da Seção 5.3; destacar composições com losango preenchido (`Agendamento` ◼ `ItemDeServico`).
- **Estados:** alinhar estados iniciais com ponto sólido e finais com circunferência alvo; marcar transições com eventos (`cancelar`) e guardas (`[permissão válida]`).
- **Extratos e arquivos:** indicar dependência estereotipada `<<file>>` entre `Sistema` e `ExtratoIO`/`JsonStorage` para reforçar persistência em disco.
- **Fila secundária:** representar `Deque<Agendamento>` como nota anexada ao `Sistema` para lembrar que se trata de pilha LIFO.
- **Layout sugerido:** posicionar classes de `value` à esquerda, entidades `model` no centro, `system` à direita, `persist`/`util` abaixo como infraestrutura.
