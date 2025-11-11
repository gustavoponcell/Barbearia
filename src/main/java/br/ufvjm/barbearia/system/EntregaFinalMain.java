package br.ufvjm.barbearia.system;

import br.ufvjm.barbearia.compare.AgendamentoPorClienteNome;
import br.ufvjm.barbearia.compare.AgendamentoPorInicio;
import br.ufvjm.barbearia.compare.ClientePorEmail;
import br.ufvjm.barbearia.compare.ClientePorNome;
import br.ufvjm.barbearia.enums.CategoriaDespesa;
import br.ufvjm.barbearia.enums.FormaPagamento;
import br.ufvjm.barbearia.enums.ModoConsumoProduto;
import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.enums.StatusAtendimento;
import br.ufvjm.barbearia.exceptions.PermissaoNegadaException;
import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.model.ContaAtendimento;
import br.ufvjm.barbearia.model.ConsumoDeProduto;
import br.ufvjm.barbearia.model.Despesa;
import br.ufvjm.barbearia.model.Estacao;
import br.ufvjm.barbearia.model.ItemDeServico;
import br.ufvjm.barbearia.model.ItemContaProduto;
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
import java.util.Collections;
import java.util.Currency;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Classe responsável por demonstrar, em sequência, as dezoito questões exigidas
 * na entrega final.
 * <p>
 * A execução apresenta um formato de relatório no console, indicando "Questao
 * N: OK" quando a verificação é bem-sucedida e fornecendo evidências textuais
 * (IDs, totais, caminhos de arquivos) que comprovam cada regra de negócio. Em
 * caso de erro, a pilha de execução é exibida imediatamente para facilitar o
 * diagnóstico.
 * </p>
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

        // Questao 16: Comparator – demonstrar ordenação manual de listas com diferentes comparators
        executarQuestao(16, () -> {
            Cliente clienteZelia = new Cliente(UUID.randomUUID(), "Zélia Ramos", enderecoPadrao,
                    Telefone.of("(33) 95555-1010"), Email.of("zelia.ramos@barbearia.com"),
                    CpfHash.fromMasked("123.321.456-00"), true);
            Cliente clienteMarcos = new Cliente(UUID.randomUUID(), "Marcos Oliveira", enderecoPadrao,
                    Telefone.of("(33) 92222-3030"), Email.of("marcos.oliveira@barbearia.com"),
                    CpfHash.fromMasked("789.987.654-11"), true);
            Cliente clienteAna = new Cliente(UUID.randomUUID(), "Ana Paula", enderecoPadrao,
                    Telefone.of("(33) 91111-2020"), Email.of("ana.paula@barbearia.com"),
                    CpfHash.fromMasked("456.654.123-22"), true);

            List<Cliente> clientesComparators = new ArrayList<>();
            clientesComparators.add(clienteZelia);
            clientesComparators.add(clienteMarcos);
            clientesComparators.add(clienteAna);

            System.out.println("Clientes - ordem original: " + clientesComparators.stream()
                    .map(Cliente::getNome)
                    .collect(Collectors.joining(", ")));
            Collections.sort(clientesComparators, new ClientePorNome());
            System.out.println("Ordenado por nome: " + clientesComparators.stream()
                    .map(Cliente::getNome)
                    .collect(Collectors.joining(", ")));
            Collections.sort(clientesComparators, new ClientePorEmail());
            System.out.println("Ordenado por email: " + clientesComparators.stream()
                    .map(c -> c.getEmail().getValor())
                    .collect(Collectors.joining(", ")));

            List<Agendamento> agendamentosComparators = new ArrayList<>();
            LocalDateTime baseInicio = LocalDateTime.of(2025, Month.FEBRUARY, 10, 8, 0);
            agendamentosComparators.add(new Agendamento(UUID.randomUUID(), clienteMarcos, Estacao.ESTACOES[0],
                    baseInicio.plusHours(2), baseInicio.plusHours(2).plusMinutes(45),
                    Dinheiro.of(new BigDecimal("60.00"), BRL)));
            agendamentosComparators.add(new Agendamento(UUID.randomUUID(), clienteAna, Estacao.ESTACOES[1],
                    baseInicio.plusHours(1), baseInicio.plusHours(1).plusMinutes(30),
                    Dinheiro.of(new BigDecimal("55.00"), BRL)));
            agendamentosComparators.add(new Agendamento(UUID.randomUUID(), clienteZelia, Estacao.ESTACOES[2],
                    baseInicio.plusHours(3), baseInicio.plusHours(3).plusMinutes(40),
                    Dinheiro.of(new BigDecimal("65.00"), BRL)));

            System.out.println("Agendamentos - ordem original: " + agendamentosComparators.stream()
                    .map(a -> a.getCliente().getNome() + " @ " + a.getInicio().toLocalTime())
                    .collect(Collectors.joining(", ")));
            Collections.sort(agendamentosComparators, new AgendamentoPorInicio());
            System.out.println("Agendamentos ordenados por início: " + agendamentosComparators.stream()
                    .map(a -> a.getCliente().getNome() + " @ " + a.getInicio().toLocalTime())
                    .collect(Collectors.joining(", ")));
            Collections.sort(agendamentosComparators, new AgendamentoPorClienteNome());
            System.out.println("Agendamentos ordenados por nome do cliente: " + agendamentosComparators.stream()
                    .map(a -> a.getCliente().getNome() + " @ " + a.getInicio().toLocalTime())
                    .collect(Collectors.joining(", ")));

            return "Comparators demonstrados explicitamente no main";
        });

        // Questao 17: find custom com Iterator vs Collections.binarySearch()
        executarQuestao(17, () -> {
            Cliente clienteHelena = new Cliente(UUID.randomUUID(), "Helena Pesquisa", enderecoPadrao,
                    Telefone.of("(33) 94444-5555"), Email.of("helena.pesquisa@barbearia.com"),
                    CpfHash.fromMasked("101.202.303-40"), true);
            Cliente clienteBruno = new Cliente(UUID.randomUUID(), "Bruno Pesquisa", enderecoPadrao,
                    Telefone.of("(33) 93333-4444"), Email.of("bruno.pesquisa@barbearia.com"),
                    CpfHash.fromMasked("202.303.404-50"), true);
            Cliente clienteAmanda = new Cliente(UUID.randomUUID(), "Amanda Pesquisa", enderecoPadrao,
                    Telefone.of("(33) 92222-3333"), Email.of("amanda.pesquisa@barbearia.com"),
                    CpfHash.fromMasked("303.404.505-60"), true);
            Cliente clienteRicardo = new Cliente(UUID.randomUUID(), "Ricardo Pesquisa", enderecoPadrao,
                    Telefone.of("(33) 91111-2222"), Email.of("ricardo.pesquisa@barbearia.com"),
                    CpfHash.fromMasked("404.505.606-70"), true);

            List<Cliente> clientesBusca = new ArrayList<>();
            clientesBusca.add(clienteHelena);
            clientesBusca.add(clienteBruno);
            clientesBusca.add(clienteAmanda);
            clientesBusca.add(clienteRicardo);

            Comparator<Cliente> comparadorPorNome = new ClientePorNome();
            clientesBusca.sort(comparadorPorNome);

            Cliente chaveExistente = new Cliente(UUID.randomUUID(), clienteHelena.getNome(), enderecoPadrao,
                    Telefone.of("(33) 98888-1111"), Email.of("chave.helena@barbearia.com"),
                    CpfHash.fromMasked("505.606.707-80"), true);
            Cliente chaveInexistente = new Cliente(UUID.randomUUID(), "Zilda Ausente", enderecoPadrao,
                    Telefone.of("(33) 97777-0000"), Email.of("zilda.ausente@barbearia.com"),
                    CpfHash.fromMasked("606.707.808-90"), true);

            int idxFindExistente = Sistema.find(clientesBusca, chaveExistente, comparadorPorNome);
            int idxBinExistente = Collections.binarySearch(clientesBusca, chaveExistente, comparadorPorNome);
            int idxFindInexistente = Sistema.find(clientesBusca, chaveInexistente, comparadorPorNome);
            int idxBinInexistente = Collections.binarySearch(clientesBusca, chaveInexistente, comparadorPorNome);

            System.out.printf("Questao 17 - chave existente -> find=%d, binarySearch=%d%n",
                    idxFindExistente, idxBinExistente);
            System.out.printf("Questao 17 - chave inexistente -> find=%d, binarySearch=%d%n",
                    idxFindInexistente, idxBinInexistente);
            System.out.println("find percorre a lista linearmente (O(n)). Já binarySearch exige a lista ordenada e executa em O(log n).");

            String nomesOrdenados = clientesBusca.stream()
                    .map(Cliente::getNome)
                    .collect(Collectors.joining(", "));

            return "Lista ordenada: [" + nomesOrdenados + "]";
        });

        // Questao 18: Pipeline completo de atendimento com fila secundária, cancelamentos e faturamento
        executarQuestao(18, () -> {
            System.out.println("\n=== Questao 18: Pipeline completo para 10 clientes ===");

            List<Cliente> clientesPipeline = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                String telefone = String.format("(33) 9%03d-%04d", 400 + i * 3, 7000 + i);
                String email = String.format("fluxo%d@barbearia.com", i);
                String cpf = String.format("200.100.%03d-%02d", 100 + i, 10 + i);
                Cliente clienteFluxo = new Cliente(UUID.randomUUID(), "Cliente Fluxo " + i, enderecoPadrao,
                        Telefone.of(telefone), Email.of(email),
                        CpfHash.fromMasked(cpf), true);
                sistema.cadastrarCliente(clienteFluxo);
                clientesPipeline.add(clienteFluxo);
                System.out.printf("Cliente cadastrado (%02d/10): %s%n", i, clienteFluxo.getNome());
            }

            Servico servicoColoracao = new Servico(UUID.randomUUID(), "Coloração Premium",
                    Dinheiro.of(new BigDecimal("150.00"), BRL), 90, true);
            Servico servicoRelaxamento = new Servico(UUID.randomUUID(), "Massagem Relaxante",
                    Dinheiro.of(new BigDecimal("80.00"), BRL), 40, false);
            Servico servicoHidratacao = new Servico(UUID.randomUUID(), "Hidratação Capilar",
                    Dinheiro.of(new BigDecimal("95.00"), BRL), 60, true);
            sistema.cadastrarServico(servicoColoracao);
            sistema.cadastrarServico(servicoRelaxamento);
            sistema.cadastrarServico(servicoHidratacao);

            List<Servico> servicosDisponiveis = new ArrayList<>();
            servicosDisponiveis.add(servicoCorte);
            servicosDisponiveis.add(servicoBarba);
            servicosDisponiveis.add(servicoColoracao);
            servicosDisponiveis.add(servicoRelaxamento);
            servicosDisponiveis.add(servicoHidratacao);

            Produto produtoTonico = new Produto(UUID.randomUUID(), "Tônico Refrescante", "TON-010",
                    Quantidade.of(new BigDecimal("25"), "UN"),
                    Quantidade.of(new BigDecimal("5"), "UN"),
                    Dinheiro.of(new BigDecimal("45.00"), BRL),
                    Dinheiro.of(new BigDecimal("22.00"), BRL));
            Produto produtoShampoo = new Produto(UUID.randomUUID(), "Shampoo Detox", "SHA-020",
                    Quantidade.of(new BigDecimal("30"), "UN"),
                    Quantidade.of(new BigDecimal("8"), "UN"),
                    Dinheiro.of(new BigDecimal("55.00"), BRL),
                    Dinheiro.of(new BigDecimal("28.00"), BRL));
            Produto produtoOleo = new Produto(UUID.randomUUID(), "Óleo para Barba", "OLE-030",
                    Quantidade.of(new BigDecimal("18"), "UN"),
                    Quantidade.of(new BigDecimal("4"), "UN"),
                    Dinheiro.of(new BigDecimal("60.00"), BRL),
                    Dinheiro.of(new BigDecimal("30.00"), BRL));
            sistema.cadastrarProduto(produtoTonico);
            sistema.cadastrarProduto(produtoShampoo);
            sistema.cadastrarProduto(produtoOleo);

            List<Produto> produtosDisponiveis = new ArrayList<>();
            produtosDisponiveis.add(produtoCera);
            produtosDisponiveis.add(produtoTonico);
            produtosDisponiveis.add(produtoShampoo);
            produtosDisponiveis.add(produtoOleo);

            List<Agendamento> agendamentosRegistrados = new ArrayList<>();
            List<Agendamento> filaSecundaria = new ArrayList<>();
            LocalDateTime baseFluxo = LocalDateTime.of(2025, Month.JANUARY, 20, 9, 0);

            for (int i = 0; i < clientesPipeline.size(); i++) {
                Cliente cliente = clientesPipeline.get(i);
                Servico servicoPrincipal = servicosDisponiveis.get(i % servicosDisponiveis.size());
                LocalDateTime inicio = baseFluxo.plusHours(i);
                LocalDateTime fim = inicio.plusMinutes(servicoPrincipal.getDuracaoMin());
                Dinheiro sinal = Dinheiro.of(BigDecimal.valueOf(20 + (i * 5L)), BRL);

                Agendamento agendamento = new Agendamento(UUID.randomUUID(), cliente,
                        Estacao.ESTACOES[i % Estacao.ESTACOES.length], inicio, fim, sinal);
                agendamento.associarBarbeiro(colaborador);

                ItemDeServico itemPrincipal = new ItemDeServico(servicoPrincipal, servicoPrincipal.getPreco(),
                        servicoPrincipal.getDuracaoMin());
                agendamento.adicionarItemServico(itemPrincipal);
                if (i % 3 == 0) {
                    Servico adicional = servicosDisponiveis.get((i + 1) % servicosDisponiveis.size());
                    agendamento.adicionarItemServico(new ItemDeServico(adicional, adicional.getPreco(),
                            adicional.getDuracaoMin()));
                }

                if (i >= 7) {
                    sistema.adicionarAgendamentoSecundario(agendamento);
                    filaSecundaria.add(agendamento);
                    System.out.printf("Agendamento %s enviado para fila secundária.%n", cliente.getNome());
                } else {
                    sistema.realizarAgendamento(agendamento);
                    agendamentosRegistrados.add(agendamento);
                    System.out.printf("Agendamento confirmado: %s @ %s%n",
                            cliente.getNome(), agendamento.getInicio());
                }
            }

            System.out.printf("Total inicial -> OS ativas: %d | em fila secundária: %d%n",
                    agendamentosRegistrados.size(), filaSecundaria.size());
            sistema.inspecionarFilaSecundaria();

            List<Agendamento> cancelados = new ArrayList<>();
            if (!agendamentosRegistrados.isEmpty()) {
                Agendamento cancelado1 = agendamentosRegistrados.get(1);
                Agendamento.Cancelamento cancelamento1 = sistema.cancelarAgendamento(colaborador, cancelado1.getId());
                cancelados.add(cancelado1);
                System.out.printf("Cancelamento aplicado para %s -> retenção: %s | reembolso: %s%n",
                        cancelado1.getCliente().getNome(), cancelamento1.getValorRetencao(),
                        cancelamento1.getValorReembolso());

                sistema.inspecionarFilaSecundaria();
                Agendamento promovido1 = sistema.recuperarAgendamentoSecundario();
                sistema.realizarAgendamento(promovido1);
                agendamentosRegistrados.add(promovido1);
                filaSecundaria.remove(promovido1);
                System.out.printf("Promovido da fila secundária: %s assumiu a vaga liberada.%n",
                        promovido1.getCliente().getNome());
            }

            Agendamento cancelarOutro = agendamentosRegistrados.stream()
                    .filter(ag -> ag.getStatus() != StatusAtendimento.CANCELADO)
                    .skip(2)
                    .findFirst()
                    .orElse(null);
            if (cancelarOutro != null) {
                Agendamento.Cancelamento cancelamento2 = sistema.cancelarAgendamento(colaborador, cancelarOutro.getId());
                cancelados.add(cancelarOutro);
                System.out.printf("Segundo cancelamento (%s) -> retenção: %s%n",
                        cancelarOutro.getCliente().getNome(), cancelamento2.getValorRetencao());

                sistema.inspecionarFilaSecundaria();
                Agendamento promovido2 = sistema.recuperarAgendamentoSecundario();
                sistema.realizarAgendamento(promovido2);
                agendamentosRegistrados.add(promovido2);
                filaSecundaria.remove(promovido2);
                System.out.printf("Novo atendimento promovido: %s agora na agenda principal.%n",
                        promovido2.getCliente().getNome());
            }

            if (!filaSecundaria.isEmpty()) {
                sistema.inspecionarFilaSecundaria();
                Agendamento promovidoExtra = sistema.recuperarAgendamentoSecundario();
                sistema.realizarAgendamento(promovidoExtra);
                agendamentosRegistrados.add(promovidoExtra);
                filaSecundaria.remove(promovidoExtra);
                System.out.printf("Fila secundária zerada com promoção extra para %s.%n",
                        promovidoExtra.getCliente().getNome());
            }

            List<Agendamento> agendamentosParaFechamento = agendamentosRegistrados.stream()
                    .filter(ag -> ag.getStatus() != StatusAtendimento.CANCELADO)
                    .collect(Collectors.toList());

            int indiceConta = 0;
            for (Agendamento agendamento : agendamentosParaFechamento) {
                agendamento.alterarStatus(StatusAtendimento.EM_ATENDIMENTO);
                agendamento.alterarStatus(StatusAtendimento.CONCLUIDO);

                ContaAtendimento conta = sistema.buscarContaPorAgendamento(agendamento.getId())
                        .orElseGet(() -> sistema.criarContaAtendimento(agendamento));

                Produto produtoConsumo = produtosDisponiveis.get(indiceConta % produtosDisponiveis.size());
                String unidadeProduto = produtoConsumo.getEstoqueAtual().getUnidade();
                Quantidade quantidadeConsumo = Quantidade.of(BigDecimal.ONE, unidadeProduto);
                produtoConsumo.movimentarSaida(quantidadeConsumo);
                agendamento.getItens().get(0).registrarConsumo(
                        new ConsumoDeProduto(produtoConsumo, quantidadeConsumo, ModoConsumoProduto.FATURADO));
                conta.adicionarProdutoFaturado(new ItemContaProduto(produtoConsumo, quantidadeConsumo,
                        produtoConsumo.getPrecoVenda()));

                if (indiceConta % 2 == 0) {
                    Servico upgrade = servicosDisponiveis.get((indiceConta + 2) % servicosDisponiveis.size());
                    conta.adicionarServicoFaturado(new ItemDeServico(upgrade, upgrade.getPreco(),
                            upgrade.getDuracaoMin()));
                }

                conta.calcularTotal(agendamento.totalServicos());

                FormaPagamento forma = switch (indiceConta % 3) {
                    case 0 -> FormaPagamento.CARTAO_CREDITO;
                    case 1 -> FormaPagamento.PIX;
                    default -> FormaPagamento.DINHEIRO;
                };
                sistema.fecharContaAtendimento(colaborador, agendamento.getId(), forma);
                System.out.printf("Conta fechada para %s (%s) com pagamento via %s%n",
                        agendamento.getCliente().getNome(), agendamento.getId(), forma);
                indiceConta++;
            }

            int vendasRegistradas = 0;
            for (int i = 0; i < clientesPipeline.size(); i += 3) {
                Cliente cliente = clientesPipeline.get(i);
                Produto produtoVenda = produtosDisponiveis.get((i + 1) % produtosDisponiveis.size());
                String unidade = produtoVenda.getEstoqueAtual().getUnidade();
                Quantidade quantidadeVenda = Quantidade.of(new BigDecimal("2"), unidade);
                produtoVenda.movimentarSaida(quantidadeVenda);

                Venda venda = new Venda(UUID.randomUUID(), cliente, baseFluxo.plusDays(1).plusHours(i),
                        FormaPagamento.CARTAO_DEBITO);
                venda.adicionarItem(new ItemVenda(produtoVenda, quantidadeVenda, produtoVenda.getPrecoVenda()));
                venda.calcularTotal();
                sistema.registrarVenda(colaborador, venda);
                vendasRegistradas++;
                System.out.printf("Venda adicional registrada: %s comprou %s (%s unidades).%n",
                        cliente.getNome(), produtoVenda.getNome(), quantidadeVenda.getValor());
            }

            int totalOsCriadas = Sistema.getTotalOrdensServicoCriadas();
            int totalServicosCriados = Sistema.getTotalServicos();
            System.out.printf("Totais globais -> OS: %d | Serviços catalogados: %d%n",
                    totalOsCriadas, totalServicosCriados);

            System.out.println("Estoque restante após atendimentos e vendas:");
            for (Produto produto : produtosDisponiveis) {
                System.out.printf("- %s (%s): %s%n", produto.getNome(), produto.getSku(), produto.getEstoqueAtual());
            }

            long extratosAntesLoad = 0;
            for (Cliente cliente : clientesPipeline) {
                List<String> extratosCliente = cliente.getExtratosGerados();
                extratosAntesLoad += extratosCliente.size();
                System.out.printf("Extratos gerados para %s (%d): %s%n", cliente.getNome(), extratosCliente.size(),
                        extratosCliente.isEmpty() ? "(nenhum)" : String.join(" | ", extratosCliente));
            }

            Path snapshotFinal = Path.of("data", "snapshot_final.json");
            sistema.saveAll(admin, snapshotFinal);
            System.out.printf("Snapshot salvo em: %s%n", snapshotFinal.toAbsolutePath());

            sistema.loadAll(snapshotFinal);
            List<Cliente> clientesReidratados = sistema.listarClientesOrdenados();
            long extratosPosLoad = clientesReidratados.stream()
                    .mapToLong(c -> c.getExtratosGerados().size())
                    .sum();
            System.out.printf("Reidratação concluída -> clientes=%d | OS=%d | Serviços=%d | Extratos=%d%n",
                    clientesReidratados.size(), Sistema.getTotalOrdensServicoCriadas(),
                    Sistema.getTotalServicos(), extratosPosLoad);

            return String.format("Pipeline finalizado com %d cancelamentos, %d vendas adicionais e %d extratos gerados",
                    cancelados.size(), vendasRegistradas, extratosPosLoad);
        });
    }

    /**
     * Bloco funcional utilizado para encapsular a lógica de cada questão.
     */
    @FunctionalInterface
    private interface QuestaoExecutor {
        String executar() throws Exception;
    }

    /**
     * Executa um bloco de demonstração, capturando exceções para sinalizar o
     * status da questão no console.
     *
     * @param numero   identificador da questão (1–18).
     * @param executor lógica a ser avaliada; deve retornar a evidência textual.
     */
    private static void executarQuestao(int numero, QuestaoExecutor executor) {
        try {
            String evidencia = executor.executar();
            System.out.println("Questao " + numero + ": OK - " + evidencia);
        } catch (Exception e) {
            System.out.println("Questao " + numero + ": FAIL - " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    /**
     * Extrai contagens numéricas impressas no relatório operacional.
     *
     * @param relatorio texto completo retornado pelo sistema.
     * @param prefixo   rótulo a ser localizado.
     * @return valor inteiro após o prefixo ou {@code 0} caso não esteja presente.
     */
    private static int extrairContagem(String relatorio, String prefixo) {
        return relatorio.lines()
                .filter(l -> l.startsWith(prefixo))
                .map(l -> l.substring(prefixo.length()).trim())
                .mapToInt(Integer::parseInt)
                .findFirst()
                .orElse(0);
    }

    /**
     * Remove arquivos existentes dentro de um diretório sem apagá-lo.
     * <p>
     * A limpeza garante que os extratos gerados nas questões 10 e 18 sejam
     * criados a partir de um estado conhecido, evitando falsos positivos.
     * </p>
     *
     * @param dir diretório alvo da limpeza.
     * @throws IOException caso ocorra falha ao excluir ou criar estruturas.
     */
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
