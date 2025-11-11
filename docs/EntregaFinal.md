# Entrega Final – Projeto Barbearia

## Passo a passo de execução
1. Compile o projeto: `mvn -q -DskipTests clean package`.
2. Rode a rotina dirigida: `mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.EntregaFinalMain`.
3. Extratos serão salvos em `data/extratos/` (um subdiretório por cliente) e snapshots em `data/`.
4. Para resetar as evidências, limpe a pasta `data/` antes de executar novamente.

## Evidências exibidas por `EntregaFinalMain`
Cada bloco imprime `Questao N: OK - ...` quando validado com sucesso. Em caso de falha, o stack trace é exibido para depuração.

| Questão | Foco | Evidência impressa |
|--------|------|---------------------|
| 1 | Modelagem e relações | IDs cruzados entre cliente, OS, conta e despesa. |
| 2 | Papéis e autorização | Exceção para colaborador + primeira linha do relatório emitido pelo admin. |
| 3 | `toString()` | Impressão de objetos de domínio formatados. |
| 4 | Construtores com `super(...)` | Flags indicando campos herdados preenchidos. |
| 5 | Estações fixas | Quantidade de posições e lavagem disponível na primeira. |
| 6 | CRUD usuários | Totais do relatório operacional após cadastro/edição/remoção. |
| 7 | CRUD clientes | Totais e nome atualizado do cliente. |
| 8 | Listagem de OS por cliente | Lista de IDs vinculados ao cliente principal. |
| 9 | Estruturas dinâmicas | Operações `push`/`peek`/`pop` na fila secundária. |
| 10 | Extratos automáticos | Caminhos dos arquivos gerados para serviço, venda e cancelamento. |
| 11 | Contadores de serviços | Valores encapsulado vs. protegido com comentário de prós/contras. |
| 12 | Contador de OS | Total global comparado com o tamanho da lista. |
| 13 | Comparators em listagens | Sequências ordenadas por e-mail e nome de cliente. |
| 14 | Persistência JSON | Contagens antes/depois de `saveAll`/`loadAll`. |
| 15 | Iterator | Logs detalhados do cursor e explicação comparando com `foreach`. |
| 16 | `Collections.sort` com comparadores distintos | Impressão da ordem original e das duas ordenações. |
| 17 | `find` x `binarySearch` | Índices retornados e comentário sobre complexidade. |
| 18 | Pipeline ponta-a-ponta | Cadastros, fila secundária, cancelamentos com retenção de 35%, vendas extras, totais de OS/serviços/estoque, reidratação de extratos. |

## Artefatos gerados
- **Extratos:** `data/extratos/<cliente>/extrato-*.txt` (serviço, venda e cancelamento).
- **Snapshot final:** `data/snapshot_final.json` para reidratar o sistema com contadores consistentes.
- **JavaDoc:** `target/site/apidocs/index.html` (após `mvn -q javadoc:javadoc`).

## Integração com documentação
- `docs/cenarios.md` descreve a narrativa usada na Questão 18.
- `docs/diagramas/README.md` referencia os artefatos de classe e fluxo.
- `docs/Verificacao_Final.md` apresenta a auditoria realizada neste repositório.
