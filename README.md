# Barbearia

Sistema acadêmico para gestão de barbearia com foco em cadastros, agenda, estoque e finanças. O projeto utiliza Java 17 com Maven e persiste dados em JSON utilizando Gson.

## Estrutura do código
- `src/main/java/br/ufvjm/barbearia/model`: entidades de domínio (Cliente, Usuario, Agendamento, etc.).
- `src/main/java/br/ufvjm/barbearia/value`: objetos de valor (Dinheiro, Quantidade, Email, Telefone, Endereco, Periodo, CpfHash).
- `src/main/java/br/ufvjm/barbearia/system`: orquestração do núcleo (`Sistema`, `Main`, `EntregaFinalMain`).
- `src/main/java/br/ufvjm/barbearia/persist`: persistência JSON e geração de extratos.
- `src/main/java/br/ufvjm/barbearia/compare`: comparadores utilizados nas listagens.
- `docs/`: documentação geral, cenários, diagramas e relatórios de verificação.

## Como executar rapidamente
```bash
mvn -q -DskipTests clean package
mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.EntregaFinalMain
```

As evidências geradas pela `EntregaFinalMain` salvam extratos em `data/extratos/` e snapshots em `data/`.

Para executar a versão demonstrativa completa, rode:
```bash
mvn -q exec:java -Dexec.mainClass=br.ufvjm.barbearia.system.Main
```

## Documentação
Consulte `docs/EntregaFinal.md` para o roteiro da apresentação, `docs/README.md` para links adicionais e `docs/Verificacao_Final.md` para o relatório de auditoria.
