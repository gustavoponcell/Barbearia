# Entrega Final – Projeto Barbearia

## Como executar
- Build: `mvn -q -DskipTests clean package`
- Rodar: `mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.EntregaFinalMain`
- Saídas: extratos em `data/extratos`, snapshots em `data/`.

## Questões demonstradas no main
### Questão 1 a 14
Para cada bloco da `EntregaFinalMain`, o console registra as validações dos cadastros iniciais, configurações de serviços, agendamentos, atualizações de status, movimentação de estoque e geração de extratos. São exibidos prints confirmando criação de clientes, usuários e serviços, a alocação nas estações, entradas na pilha secundária, consumo de produtos e fechamento de contas, permitindo verificar o comportamento esperado para cada uma das primeiras quatorze questões.

### Questão 15 – Iterator vs foreach
- Trecho utilizado: no `main` de `EntregaFinalMain`, a lista de clientes é percorrida com um `Iterator` explícito antes de usar o laço enhanced `for`.
- Explicação: `Iterator` expõe um cursor manual (`hasNext`/`next`) que permite remoções seguras enquanto percorre; o `foreach` é açúcar sintático que o compilador traduz para o uso de um `Iterator` implícito.
- Exemplos: `while (iterator.hasNext()) { Cliente cliente = iterator.next(); ... }` e `for (Cliente cliente : clientes) { ... }`, mostrando ambas as abordagens operando sobre a mesma coleção.

### Questão 16 – Comparators e sort
- O main chama `Collections.sort(clientes, new ClientePorNome())` para ordenar alfabeticamente e, em seguida, `Collections.sort(clientes, new ClientePorEmail())` para reordenar pela chave de e-mail.
- Cada chamada usa um `Comparator` diferente, o que altera o critério de comparação e, portanto, a sequência resultante na lista.

### Questão 17 – find + binarySearch
- Antes da busca, o main garante que a lista está ordenada conforme o `Comparator` selecionado para o atributo usado na pesquisa.
- Em seguida, compara o índice retornado por `find(...)` com o resultado de `Collections.binarySearch(...)`, demonstrando que ambos convergem para o mesmo elemento quando a ordenação está alinhada com o comparador.

### Questão 18 – Fluxo ponta-a-ponta (10 clientes)
- Pipeline descrito na `EntregaFinalMain`: cadastro massivo de clientes → agendamento nas estações → gerenciamento da fila secundária → cancelamento com retenção → encerramento dos atendimentos → registro de vendas → geração automática de extratos → persistência em snapshot e extratos no disco.

## Observações finais
- Diagramas e cenários versionados permanecem em `docs/`.
- Para regenerar a JavaDoc: `mvn -q javadoc:javadoc`.
