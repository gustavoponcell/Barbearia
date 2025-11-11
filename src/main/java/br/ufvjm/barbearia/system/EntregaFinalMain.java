package br.ufvjm.barbearia.system;

import br.ufvjm.barbearia.compare.AgendamentoPorClienteNome;
import br.ufvjm.barbearia.compare.ClientePorEmail;
import br.ufvjm.barbearia.enums.CategoriaDespesa;
import br.ufvjm.barbearia.enums.FormaPagamento;
import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.exceptions.PermissaoNegadaException;
import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.model.ContaAtendimento;
import br.ufvjm.barbearia.model.Despesa;
import br.ufvjm.barbearia.model.Estacao;
import br.ufvjm.barbearia.model.ItemDeServico;
import br.ufvjm.barbearia.model.ItemVenda;
import br.ufvjm.barbearia.model.Produto;
import br.ufvjm.barbearia.model.Servico;
import br.ufvjm.barbearia.model.Usuario;
import br.ufvjm.barbearia.model.Venda;
import br.ufvjm.barbearia.value.CpfHash;
import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Quantidade;
import br.ufvjm.barbearia.value.Telefone;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Execução dirigida das evidências de implementação.
 */
public class EntregaFinalMain {

    private static final Currency BRL = Currency.getInstance("BRL");

    private EntregaFinalMain() {
    }

    public static void main(String[] args) {
        Sistema sistema = new Sistema();
        Endereco enderecoPadrao = Endereco.builder()
                .logradouro("Av. Central")
                .numero("1000")
                .bairro("Centro")
                .cidade("Teófilo Otoni")
                .estado("MG")
                .cep("39800000")
                .build();
        Telefone telefoneCliente = Telefone.of("(33) 98888-0000");
        Telefone telefoneAdmin = Telefone.of("(33) 97777-0001");
        Telefone telefoneColaborador = Telefone.of("(33) 98888-2222");
        Email emailCliente = Email.of("cliente@barbearia.com");
        Email emailAdmin = Email.of("admin@barbearia.com");
        Email emailColaborador = Email.of("colaborador@barbearia.com");

        Cliente clientePrincipal = new Cliente(UUID.randomUUID(), "Carlos Cliente", enderecoPadrao,
                telefoneCliente, emailCliente, CpfHash.fromMasked("123.456.789-09"), true);
        Usuario admin = new Usuario(UUID.randomUUID(), "Ana Admin", enderecoPadrao,
                telefoneAdmin, emailAdmin, Papel.ADMIN, "ana.admin", "hash-admin", true);
        Usuario colaborador = new Usuario(UUID.randomUUID(), "Caio Colaborador", enderecoPadrao,
                telefoneColaborador, emailColaborador, Papel.COLABORADOR, "caio.colab", "hash-colab", true);

        sistema.cadastrarCliente(clientePrincipal);

        Servico servicoCorte = new Servico(UUID.randomUUID(), "Corte Premium",
                Dinheiro.of(new BigDecimal("70.00"), BRL), 45, true);
        Servico servicoBarba = new Servico(UUID.randomUUID(), "Barba Express",
                Dinheiro.of(new BigDecimal("40.00"), BRL), 30, false);
        sistema.cadastrarServico(servicoCorte);
        sistema.cadastrarServico(servicoBarba);

        LocalDateTime inicioAtendimento = LocalDateTime.of(2025, Month.JANUARY, 15, 10, 0);
        LocalDateTime fimAtendimento = inicioAtendimento.plusMinutes(75);
        Agendamento agendamentoPrincipal = sistema.criarAgendamento(UUID.randomUUID(), clientePrincipal,
                Estacao.ESTACOES[0], inicioAtendimento, fimAtendimento,
                Dinheiro.of(new BigDecimal("30.00"), BRL));
        agendamentoPrincipal.adicionarItemServico(new ItemDeServico(servicoCorte, servicoCorte.getPreco(),
                servicoCorte.getDuracaoMin()));
        agendamentoPrincipal.adicionarItemServico(new ItemDeServico(servicoBarba, servicoBarba.getPreco(),
                servicoBarba.getDuracaoMin()));
        agendamentoPrincipal.associarBarbeiro(colaborador);

        ContaAtendimento contaPrincipal = sistema.criarContaAtendimento(agendamentoPrincipal);
        contaPrincipal.calcularTotal(agendamentoPrincipal.totalServicos());

        Despesa despesaPrincipal = new Despesa(UUID.randomUUID(), CategoriaDespesa.ALUGUEL,
                "Aluguel do espaço",
                Dinheiro.of(new BigDecimal("1500.00"), BRL), YearMonth.of(2025, Month.JANUARY));
        sistema.registrarDespesa(admin, despesaPrincipal);

        Produto produtoCera = new Produto(UUID.randomUUID(), "Cera Modeladora", "CER-001",
                Quantidade.of(new BigDecimal("10"), "UN"),
                Quantidade.of(new BigDecimal("2"), "UN"),
                Dinheiro.of(new BigDecimal("35.00"), BRL),
                Dinheiro.of(new BigDecimal("18.50"), BRL));
        sistema.cadastrarProduto(produtoCera);

        // Questao 1: Classes conforme diagrama (instanciar entidades principais, checar relações essenciais)
        executarQuestao(1, () -> {
            boolean relacionamentoOk = agendamentoPrincipal.getCliente().equals(clientePrincipal)
                    && contaPrincipal.getAgendamento().equals(agendamentoPrincipal)
                    && despesaPrincipal.getCategoria() == CategoriaDespesa.ALUGUEL;
            return "Relacionamentos principais: cliente->OS=" + agendamentoPrincipal.getId()
                    + ", conta->OS=" + contaPrincipal.getId()
                    + ", valido=" + relacionamentoOk;
        });

        // Questao 2: Papéis e acesso (ADMIN vs COLABORADOR) – tente ler relatório com colaborador => exceção; com admin => OK
        executarQuestao(2, () -> {
            YearMonth competencia = YearMonth.of(2025, Month.JANUARY);
            String bloqueioMensagem;
            try {
                sistema.emitirRelatorioFinanceiro(colaborador, competencia, BRL);
                bloqueioMensagem = "(falha) colaborador conseguiu emitir relatório";
            } catch (PermissaoNegadaException e) {
                bloqueioMensagem = "Colaborador bloqueado: " + e.getMessage();
            }
            String relatorioAdmin = sistema.emitirRelatorioFinanceiro(admin, competencia, BRL);
            String primeiraLinha = relatorioAdmin.lines().findFirst().orElse(relatorioAdmin);
            return bloqueioMensagem + " | Admin OK: " + primeiraLinha;
        });

        // Questao 3: toString() – imprimir toString() de 5 classes distintas (Cliente, Servico, Agendamento, ContaAtendimento, Despesa)
        executarQuestao(3, () -> String.join(System.lineSeparator(),
                "Cliente: " + clientePrincipal,
                "Servico1: " + servicoCorte,
                "Servico2: " + servicoBarba,
                "Agendamento: " + agendamentoPrincipal,
                "Conta: " + contaPrincipal,
                "Despesa: " + despesaPrincipal));

        // Questao 4: super(...) – instanciar Cliente/Usuario e validar campos herdados de Pessoa foram populados
        executarQuestao(4, () -> {
            boolean clienteCampos = clientePrincipal.getEndereco().equals(enderecoPadrao)
                    && clientePrincipal.getTelefone().equals(telefoneCliente)
                    && clientePrincipal.getEmail().equals(emailCliente);
            boolean usuarioCampos = admin.getNome().equals("Ana Admin")
                    && admin.getEndereco().equals(enderecoPadrao)
                    && admin.getTelefone().equals(telefoneAdmin);
            return "Cliente campos herdados OK=" + clienteCampos + ", Usuario campos herdados OK=" + usuarioCampos;
        });

        // Questao 5: Vetor estático das 3 estações – validar length==3 e ESTACOES[0].isLavagem()==true
        executarQuestao(5, () -> {
            Estacao[] estacoes = Estacao.ESTACOES;
            boolean condicao = estacoes.length == 3 && estacoes[0].isPossuiLavagem();
            return "Total estações=" + estacoes.length + ", primeira possui lavagem=" + condicao;
        });

        // Questao 6: CRUD colaboradores – cadastrar/editar/remover e validar quantidade e conteúdo
        executarQuestao(6, () -> {
            Usuario novoUsuario = new Usuario(UUID.randomUUID(), "Bruna Souza", enderecoPadrao,
                    Telefone.of("(33) 97777-3333"), Email.of("bruna@barbearia.com"),
                    Papel.COLABORADOR, "bruna.souza", "hash1", true);
            sistema.cadastrarUsuario(admin, novoUsuario);
            String relatorioAposCadastro = sistema.emitirRelatorioOperacional(admin);
            int totalAposCadastro = extrairContagem(relatorioAposCadastro, "Usuários cadastrados: ");

            Usuario usuarioEditado = new Usuario(novoUsuario.getId(), "Bruna Lima", enderecoPadrao,
                    Telefone.of("(33) 96666-4444"), Email.of("bruna.lima@barbearia.com"),
                    Papel.COLABORADOR, novoUsuario.getLogin(), "hash2", true);
            sistema.editarUsuario(admin, novoUsuario.getId(), usuarioEditado);
            String relatorioAposEdicao = sistema.emitirRelatorioOperacional(admin);
            int totalAposEdicao = extrairContagem(relatorioAposEdicao, "Usuários cadastrados: ");

            sistema.removerUsuario(admin, novoUsuario.getId());
            String relatorioAposRemocao = sistema.emitirRelatorioOperacional(admin);
            int totalAposRemocao = extrairContagem(relatorioAposRemocao, "Usuários cadastrados: ");

            return "Usuários cadastrados -> após cadastro=" + totalAposCadastro
                    + ", após edição=" + totalAposEdicao
                    + ", após remoção=" + totalAposRemocao;
        });

        // Questao 7: CRUD clientes – cadastrar/editar/remover e validar quantidade e conteúdo
        executarQuestao(7, () -> {
            Cliente clienteCrud = new Cliente(UUID.randomUUID(), "Beatriz", enderecoPadrao,
                    Telefone.of("(33) 98888-5555"), Email.of("beatriz@barbearia.com"),
                    CpfHash.fromMasked("987.654.321-00"), true);
            sistema.cadastrarCliente(clienteCrud);
            int totalAposCadastro = sistema.listarClientesOrdenados().size();

            Cliente clienteEditado = new Cliente(clienteCrud.getId(), "Beatriz Souza", enderecoPadrao,
                    Telefone.of("(33) 97777-6666"), Email.of("beatriz.souza@barbearia.com"),
                    CpfHash.fromMasked("987.654.321-00"), true);
            sistema.editarCliente(clienteCrud.getId(), clienteEditado);
            Cliente recuperado = sistema.listarClientesOrdenados().stream()
                    .filter(c -> c.getId().equals(clienteCrud.getId()))
                    .findFirst()
                    .orElseThrow();

            sistema.removerCliente(clienteCrud.getId());
            int totalAposRemocao = sistema.listarClientesOrdenados().size();

            return "Clientes -> após cadastro=" + totalAposCadastro
                    + ", nome atualizado=\"" + recuperado.getNome() + "\""
                    + ", após remoção=" + totalAposRemocao;
        });

        // Questao 8: Imprimir OS por cliente – criar OS, listar e imprimir
        executarQuestao(8, () -> {
            List<Agendamento> ordensCliente = sistema.listarOrdensDeServicoDoCliente(clientePrincipal.getId());
            sistema.imprimirOrdensDeServicoDoCliente(clientePrincipal.getId());
            String ids = ordensCliente.stream().map(Agendamento::getId).map(UUID::toString)
                    .collect(Collectors.joining(","));
            return "OS do cliente " + clientePrincipal.getNome() + ": " + ordensCliente.size()
                    + " registro(s) [" + ids + "]";
        });

        // Questao 9: Estruturas dinâmicas – adicionar elementos às listas/Deque e validar size()
        executarQuestao(9, () -> {
            Agendamento agendamentoFila = sistema.criarAgendamento(UUID.randomUUID(), clientePrincipal,
                    Estacao.ESTACOES[1], inicioAtendimento.plusDays(1),
                    inicioAtendimento.plusDays(1).plusMinutes(45),
                    Dinheiro.of(new BigDecimal("20.00"), BRL));
            agendamentoFila.adicionarItemServico(new ItemDeServico(servicoBarba, servicoBarba.getPreco(),
                    servicoBarba.getDuracaoMin()));
            int totalAgendamentos = sistema.listarAgendamentosOrdenados().size();

            sistema.adicionarAgendamentoSecundario(agendamentoFila);
            Optional<Agendamento> topoAntesPop = sistema.inspecionarFilaSecundaria();
            Agendamento recuperado = sistema.recuperarAgendamentoSecundario();
            Optional<Agendamento> topoDepoisPop = sistema.inspecionarFilaSecundaria();

            return "Agendamentos total=" + totalAgendamentos
                    + ", topo antes pop=" + topoAntesPop.map(Agendamento::getId).orElse(null)
                    + ", recuperado=" + recuperado.getId()
                    + ", fila vazia após pop=" + topoDepoisPop.isEmpty();
        });

        // Questao 10: Extratos automáticos – provocar fechamento de atendimento e registrar venda; checar se extratos foram gerados (arquivo existe)
        executarQuestao(10, () -> {
            Path extratosDir = Path.of("data", "extratos");
            limparDiretorio(extratosDir);

            ContaAtendimento contaFechada = sistema.fecharContaAtendimento(colaborador,
                    agendamentoPrincipal.getId(), FormaPagamento.PIX);
            String referenciaServico = contaFechada.getReferenciaExtratoServico();
            boolean extratoServicoExiste = referenciaServico != null && Files.exists(Path.of(referenciaServico));

            Venda venda = new Venda(UUID.randomUUID(), clientePrincipal,
                    LocalDateTime.of(2025, Month.JANUARY, 16, 14, 30), FormaPagamento.CARTAO_DEBITO);
            venda.adicionarItem(new ItemVenda(produtoCera,
                    Quantidade.of(new BigDecimal("1"), "UN"), produtoCera.getPrecoVenda()));
            venda.calcularTotal();
            sistema.registrarVenda(colaborador, venda);
            String referenciaVenda = venda.getReferenciaExtrato();
            boolean extratoVendaExiste = referenciaVenda != null && Files.exists(Path.of(referenciaVenda));

            Agendamento agendamentoCancelado = sistema.criarAgendamento(UUID.randomUUID(), clientePrincipal,
                    Estacao.ESTACOES[1],
                    inicioAtendimento.plusDays(2), inicioAtendimento.plusDays(2).plusMinutes(45),
                    Dinheiro.of(new BigDecimal("25.00"), BRL));
            agendamentoCancelado.adicionarItemServico(new ItemDeServico(servicoBarba, servicoBarba.getPreco(),
                    servicoBarba.getDuracaoMin()));
            sistema.cancelarAgendamento(colaborador, agendamentoCancelado.getId());
            String referenciaCancelamento = agendamentoCancelado.getReferenciaExtratoCancelamento();
            boolean extratoCancelamentoExiste = referenciaCancelamento != null
                    && Files.exists(Path.of(referenciaCancelamento));

            List<String> extratosGerados = new ArrayList<>();
            if (referenciaServico != null) {
                extratosGerados.add(referenciaServico);
            }
            if (referenciaVenda != null) {
                extratosGerados.add(referenciaVenda);
            }
            if (referenciaCancelamento != null) {
                extratosGerados.add(referenciaCancelamento);
            }
            return "Extratos gerados: " + extratosGerados
                    + " | serviço=" + extratoServicoExiste
                    + ", venda=" + extratoVendaExiste
                    + ", cancelamento=" + extratoCancelamentoExiste;
        });

        // Questao 11: Dois contadores de Servico – criar dois serviços e checar contadores (encapsulado e protegido)
        executarQuestao(11, () -> {
            int encapsulado = Sistema.getTotalServicos();
            int protegido = Cliente.getTotalServicosProtegido();
            return "Total Serviços (encapsulado)=" + encapsulado
                    + ", (protegido)=" + protegido;
        });

        // Questao 12: Total de OS – criar duas OS e checar contador único consistente
        executarQuestao(12, () -> {
            int totalOsContador = Sistema.getTotalOrdensServicoCriadas();
            int totalOsLista = sistema.listarAgendamentosOrdenados().size();
            return "Total OS contador=" + totalOsContador + ", lista=" + totalOsLista;
        });

        // Questao 13: Comparator – ordenar clientes e agendamentos com comparators existentes; validar ordem
        executarQuestao(13, () -> {
            Cliente clienteComparatorA = new Cliente(UUID.randomUUID(), "Amanda Cliente", enderecoPadrao,
                    Telefone.of("(33) 94444-1111"), Email.of("amanda@barbearia.com"),
                    CpfHash.fromMasked("321.654.987-00"), true);
            Cliente clienteComparatorB = new Cliente(UUID.randomUUID(), "Bruno Cliente", enderecoPadrao,
                    Telefone.of("(33) 95555-2222"), Email.of("bruno@barbearia.com"),
                    CpfHash.fromMasked("321.654.987-10"), true);
            sistema.cadastrarCliente(clienteComparatorA);
            sistema.cadastrarCliente(clienteComparatorB);

            Agendamento agendamentoA = sistema.criarAgendamento(UUID.randomUUID(), clienteComparatorA,
                    Estacao.ESTACOES[2], LocalDateTime.of(2025, Month.JANUARY, 20, 9, 0),
                    LocalDateTime.of(2025, Month.JANUARY, 20, 9, 45), Dinheiro.of(new BigDecimal("15.00"), BRL));
            agendamentoA.adicionarItemServico(new ItemDeServico(servicoBarba, servicoBarba.getPreco(),
                    servicoBarba.getDuracaoMin()));
            Agendamento agendamentoB = sistema.criarAgendamento(UUID.randomUUID(), clienteComparatorB,
                    Estacao.ESTACOES[1], LocalDateTime.of(2025, Month.JANUARY, 20, 10, 0),
                    LocalDateTime.of(2025, Month.JANUARY, 20, 10, 40), Dinheiro.of(new BigDecimal("25.00"), BRL));
            agendamentoB.adicionarItemServico(new ItemDeServico(servicoCorte, servicoCorte.getPreco(),
                    servicoCorte.getDuracaoMin()));

            List<Cliente> ordenadosClientes = sistema.listarClientesOrdenados(new ClientePorEmail(), 0, 10);
            List<String> emailsOrdenados = ordenadosClientes.stream()
                    .map(c -> c.getEmail().getValor())
                    .collect(Collectors.toList());

            List<Agendamento> ordenadosAgendamentos = sistema.listarAgendamentosOrdenados(
                    new AgendamentoPorClienteNome(), 0, 10);
            List<String> nomesAgendamentos = ordenadosAgendamentos.stream()
                    .map(a -> a.getCliente().getNome())
                    .collect(Collectors.toList());

            return "Clientes ordenados por e-mail=" + emailsOrdenados
                    + " | Agendamentos por cliente=" + nomesAgendamentos;
        });

        // Questao 14: Persistência JSON – saveAll() → loadAll() e comparar contagens antes/depois
        executarQuestao(14, () -> {
            Path snapshot = Path.of("target", "snapshot-entrega.json");
            Files.createDirectories(snapshot.getParent());
            sistema.saveAll(admin, snapshot);

            Sistema sistemaClonado = new Sistema();
            sistemaClonado.loadAll(snapshot);

            int clientesAntes = sistema.listarClientesOrdenados().size();
            int clientesDepois = sistemaClonado.listarClientesOrdenados().size();
            int agAntes = sistema.listarAgendamentosOrdenados().size();
            int agDepois = sistemaClonado.listarAgendamentosOrdenados().size();
            int contasAntes = sistema.listarContas().size();
            int contasDepois = sistemaClonado.listarContas().size();
            int vendasAntes = sistema.listarVendas(admin).size();
            int vendasDepois = sistemaClonado.listarVendas(admin).size();

            return "Persistência: clientes " + clientesAntes + "->" + clientesDepois
                    + ", agendamentos " + agAntes + "->" + agDepois
                    + ", contas " + contasAntes + "->" + contasDepois
                    + ", vendas " + vendasAntes + "->" + vendasDepois;
        });

        // Questao 15: Iterator – instanciar ArrayList<Cliente>, percorrer com Iterator e comparar com foreach
        executarQuestao(15, () -> {
            List<Cliente> clientesIterator = new ArrayList<>();
            clientesIterator.add(new Cliente(UUID.randomUUID(), "Alice Iteradora", enderecoPadrao,
                    Telefone.of("(33) 90000-0001"), Email.of("alice.iterator@barbearia.com"),
                    CpfHash.fromMasked("111.222.333-44"), true));
            clientesIterator.add(new Cliente(UUID.randomUUID(), "Bruno Cursor", enderecoPadrao,
                    Telefone.of("(33) 90000-0002"), Email.of("bruno.cursor@barbearia.com"),
                    CpfHash.fromMasked("555.666.777-88"), true));
            clientesIterator.add(new Cliente(UUID.randomUUID(), "Carla Foreach", enderecoPadrao,
                    Telefone.of("(33) 90000-0003"), Email.of("carla.foreach@barbearia.com"),
                    CpfHash.fromMasked("999.000.111-22"), true));

            StringBuilder demonstracao = new StringBuilder();
            demonstracao.append("Lista de demonstração contém ")
                    .append(clientesIterator.size())
                    .append(" cliente(s).")
                    .append(System.lineSeparator());

            Iterator<Cliente> iterator = clientesIterator.iterator();
            int indice = 0;
            while (iterator.hasNext()) {
                Cliente clienteAtual = iterator.next();
                demonstracao.append("[Iterator] cursor avança para índice ")
                        .append(indice)
                        .append(": ")
                        .append(clienteAtual.getNome())
                        .append(System.lineSeparator());
                indice++;
            }

            demonstracao.append("O Iterator mantém um cursor entre os elementos; hasNext() verifica se há próximo e next()")
                    .append(" desloca o cursor, retornando o registro atual. Alterar a lista sem usar o próprio Iterator durante a")
                    .append(" iteração quebra o contrato e pode lançar ConcurrentModificationException.")
                    .append(System.lineSeparator());

            demonstracao.append("O foreach compila para o mesmo mecanismo de Iterator, mas com sintaxe mais simples:")
                    .append(System.lineSeparator());
            for (Cliente cliente : clientesIterator) {
                demonstracao.append("[foreach] Encontrado: ")
                        .append(cliente.getNome())
                        .append(System.lineSeparator());
            }

            demonstracao.append("foreach usa internamente clientesIterator.iterator(), chamando hasNext()/next() automaticamente.");

            return demonstracao.toString();
        });
    }

    @FunctionalInterface
    private interface QuestaoExecutor {
        String executar() throws Exception;
    }

    private static void executarQuestao(int numero, QuestaoExecutor executor) {
        try {
            String evidencia = executor.executar();
            System.out.println("Questao " + numero + ": OK - " + evidencia);
        } catch (Exception e) {
            System.out.println("Questao " + numero + ": FAIL - " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static int extrairContagem(String relatorio, String prefixo) {
        return relatorio.lines()
                .filter(l -> l.startsWith(prefixo))
                .map(l -> l.substring(prefixo.length()).trim())
                .mapToInt(Integer::parseInt)
                .findFirst()
                .orElse(0);
    }

    private static void limparDiretorio(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> stream = Files.walk(dir)) {
                stream.filter(path -> !path.equals(dir))
                        .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException ignored) {
                                // ignora para manter execução
                            }
                        });
            }
        }
        Files.createDirectories(dir);
    }
}
