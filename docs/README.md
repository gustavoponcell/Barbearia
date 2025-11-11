# Documentação do Sistema de Barbearia

Este diretório consolida materiais de apoio para a entrega final do projeto.

## Conteúdo disponível
- `EntregaFinal.md`: roteiro utilizado para demonstrar os requisitos Q1–Q18.
- `Verificacao_Final.md`: relatório de auditoria com o status de cada requisito.
- `cenarios.md`: descrição textual de cenários de uso relevantes (cadastro, agenda, cancelamento, vendas).
- `diagramas/`: imagens ou descrições estruturadas dos diagramas de classe e de fluxo.
- `build-offline.md`: instruções para montar um ambiente Maven offline caso o mirror oficial esteja indisponível.

## Executáveis principais
1. `br.ufvjm.barbearia.system.EntregaFinalMain` – script dirigido com evidências das questões avaliadas.
2. `br.ufvjm.barbearia.system.Main` – demonstração interativa mais longa com cadastros extras e relatórios.

## Como gerar relatórios
- **Build completo:** `mvn -q -DskipTests clean package`
- **Execução da entrega:** `mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.EntregaFinalMain`
- **JavaDoc:** `mvn -q javadoc:javadoc` (saída em `target/site/apidocs/index.html`).

## Próximos passos sugeridos
- Evoluir a camada de interface para uma aplicação web ou desktop.
- Adicionar testes automatizados para fluxos financeiros e cancelamentos.
- Integrar logs com ferramentas de observabilidade para auditoria contínua.
