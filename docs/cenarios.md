# Cenários de Uso – Barbearia

## Cadastro de colaboradores e clientes
1. Administrador autentica com papel `ADMIN`.
2. Executa `Sistema.cadastrarUsuario(...)` para registrar colaboradores e barbeiros.
3. Cadastra clientes via `Sistema.cadastrarCliente(...)` e valida contatos com objetos de valor (`Telefone`, `Email`).
4. Emite relatório operacional para confirmar totais atualizados.

## Agenda e fila secundária
1. Colaborador agenda clientes em `Estacao.ESTACOES`, sempre priorizando a estação 1 para serviços com lavagem.
2. Quando a agenda está cheia, chama `Sistema.adicionarAgendamentoSecundario(...)` para empilhar reservas adicionais.
3. Antes de promover um cliente, inspeciona o topo com `Sistema.inspecionarFilaSecundaria()` (opera como `peek`).
4. Ao liberar uma vaga (por cancelamento), chama `Sistema.recuperarAgendamentoSecundario()` e realiza o agendamento.

## Cancelamento com retenção de 35%
1. Cliente solicita cancelamento; colaborador aciona `Sistema.cancelarAgendamento(...)`.
2. O sistema aplica a retenção de 35% (`RETENCAO_CANCELAMENTO`) sobre `Agendamento.totalServicos()`.
3. `ContaAtendimento` registra o ajuste de crédito para a retenção, gerando movimentação no caixa.
4. `Sistema.gerarExtratoCancelamento(...)` grava extrato em `data/extratos/CLIENTE/` e vincula referência ao cliente.

## Atendimento e fechamento de conta
1. Após execução dos serviços, colaborador chama `Sistema.fecharContaAtendimento(...)` definindo a forma de pagamento.
2. A conta agrega serviços adicionais e produtos (via `ContaAtendimento.adicionarServicoFaturado`/`ItemContaProduto`).
3. O fechamento gera extrato automático (`Sistema.gerarExtratoServico`) e vincula o arquivo ao cliente.
4. `CaixaDiario` registra entrada correspondente para conciliação financeira.

## Vendas e estoque
1. Produtos são cadastrados com estoque inicial e custo médio.
2. Vendas criadas com `Sistema.registrarVenda(...)` geram extrato e atualizam estoque através dos itens de venda.
3. Recebimentos de fornecedor atualizam custo médio e quantidade (`Produto.movimentarEntrada`).

## Persistência e reidratação
1. Administrador chama `Sistema.saveAll(...)` para gravar snapshot JSON com Gson.
2. Em nova execução, `Sistema.loadAll(...)` repopula listas e reidrata contadores (serviços e OS).
3. Clientes mantêm referências aos extratos gerados para consulta histórica.
