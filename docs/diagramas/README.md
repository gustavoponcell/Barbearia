# Diagramas do Projeto Barbearia

Os diagramas completos foram produzidos em ferramentas externas. Para fins de auditoria, registramos abaixo referências e descrições resumidas:

- **Diagrama de Classes (uml_barbearia_classes.png)**
  - Representa herança `Pessoa → Cliente/Usuario` e composições (`Agendamento → ItemDeServico → ConsumoDeProduto`, `Venda → ItemVenda`, `ContaAtendimento → ItemContaProduto`).
  - Destaca os contadores estáticos exigidos (Serviço/OS) e o vetor `Estacao.ESTACOES`.
- **Diagrama de Fluxo de Atendimento (uml_barbearia_fluxo.pdf)**
  - Mostra etapas: agendamento, fila secundária, atendimento, cancelamento com retenção e geração automática de extratos.

Os arquivos podem ser substituídos por versões atualizadas mantendo os mesmos nomes para preservar links em relatórios.
