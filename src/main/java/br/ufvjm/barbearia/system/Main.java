package br.ufvjm.barbearia.system;

import br.ufvjm.barbearia.enums.CategoriaDespesa;
import br.ufvjm.barbearia.enums.FormaPagamento;
import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.enums.StatusAtendimento;
import br.ufvjm.barbearia.exceptions.PermissaoNegadaException;
import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.CaixaDiario;
import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.model.ContaAtendimento;
import br.ufvjm.barbearia.model.Estacao;
import br.ufvjm.barbearia.model.ItemDeServico;
import br.ufvjm.barbearia.model.ItemVenda;
import br.ufvjm.barbearia.model.Despesa;
import br.ufvjm.barbearia.model.Produto;
import br.ufvjm.barbearia.model.Servico;
import br.ufvjm.barbearia.model.Venda;
import br.ufvjm.barbearia.model.Usuario;
import br.ufvjm.barbearia.value.CpfHash;
import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Quantidade;
import br.ufvjm.barbearia.value.Telefone;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Classe utilitária com um cenário completo de uso do {@link Sistema}.
 */
public final class Main {

    private Main() {
        // evitar instanciação
    }

    public static void main(String[] args) {
        Sistema sistema = new Sistema();

        Path extratosDir = Path.of("data/extratos");
        Path snapshotPath = Path.of("data/snapshots/barbearia_snapshot.json");
        Currency brl = Currency.getInstance("BRL");
        Set<Path> extratosAntes = listarArquivos(extratosDir);

        CaixaDiario caixaHoje = sistema.abrirCaixa(LocalDate.now(), Dinheiro.of(BigDecimal.ZERO, brl));
        System.out.printf("Caixa aberto para %s com saldo inicial %s%n", caixaHoje.getData(), caixaHoje.getSaldoAbertura());

        System.out.println("=== Demonstração completa do Sistema da Barbearia ===");

        // Cadastros básicos
        Endereco enderecoBase = Endereco.builder()
                .logradouro("Rua das Flores")
                .numero("123")
                .bairro("Centro")
                .cidade("Diamantina")
                .estado("MG")
                .cep("39100000")
                .build();

        Telefone telefoneFixo = Telefone.of("38 3531-0000");

        Usuario administrador = new Usuario(
                UUID.randomUUID(),
                "Carlos Admin",
                enderecoBase,
                telefoneFixo,
                Email.of("carlos.admin@barbearia.com"),
                Papel.ADMIN,
                "carlos",
                "123",
                true
        );

        Usuario colaborador = new Usuario(
                UUID.randomUUID(),
                "Marina Colaboradora",
                enderecoBase,
                Telefone.of("38 99999-0001"),
                Email.of("marina.colaboradora@barbearia.com"),
                Papel.COLABORADOR,
                "marina",
                "abc",
                true
        );

        Usuario barbeiro = new Usuario(
                UUID.randomUUID(),
                "João Barbeiro",
                enderecoBase,
                Telefone.of("38 99999-1234"),
                Email.of("joao.barbeiro@barbearia.com"),
                Papel.BARBEIRO,
                "joao",
                "321",
                true
        );

        sistema.cadastrarUsuario(administrador, administrador);
        sistema.cadastrarUsuario(administrador, colaborador);
        sistema.cadastrarUsuario(administrador, barbeiro);
        System.out.printf("Administrador autenticado: %s (%s)%n", administrador.getNome(), administrador.getPapel());

        Cliente cliente = new Cliente(
                UUID.randomUUID(),
                "Gustavo Poncell",
                enderecoBase,
                Telefone.of("38 98888-1122"),
                Email.of("gustavo.poncell@email.com"),
                CpfHash.fromMasked("123.456.789-09"),
                true
        );
        sistema.cadastrarCliente(cliente);
        System.out.printf("Cliente cadastrado: %s%n", cliente.getNome());

        Servico corte = new Servico(
                UUID.randomUUID(),
                "Corte de cabelo",
                Dinheiro.of(new BigDecimal("40"), brl),
                30,
                false
        );
        Servico barba = new Servico(
                UUID.randomUUID(),
                "Barba premium",
                Dinheiro.of(new BigDecimal("28"), brl),
                25,
                false
        );

        Produto pomada = new Produto(
                UUID.randomUUID(),
                "Pomada Modeladora",
                "POM01",
                Quantidade.of(new BigDecimal("20"), "un"),
                Quantidade.of(new BigDecimal("5"), "un"),
                Dinheiro.of(new BigDecimal("25"), brl),
                Dinheiro.of(new BigDecimal("15"), brl)
        );
        Produto balm = new Produto(
                UUID.randomUUID(),
                "Balm Refrescante",
                "BALM01",
                Quantidade.of(new BigDecimal("12"), "un"),
                Quantidade.of(new BigDecimal("4"), "un"),
                Dinheiro.of(new BigDecimal("32"), brl),
                Dinheiro.of(new BigDecimal("18"), brl)
        );

        sistema.cadastrarServico(corte);
        sistema.cadastrarServico(barba);
        sistema.cadastrarProduto(pomada);
        sistema.cadastrarProduto(balm);
        System.out.printf("Catálogo pronto: %d serviços, %d produtos%n",
                sistema.listarServicos().size(), sistema.listarProdutos().size());

        LocalDateTime agora = LocalDateTime.now().withSecond(0).withNano(0);
        Agendamento agendamentoPrincipal = sistema.criarAgendamento(
                UUID.randomUUID(),
                cliente,
                Estacao.ESTACOES[0],
                agora.plusHours(1),
                agora.plusHours(1).plusMinutes(55),
                Dinheiro.of(new BigDecimal("20"), brl)
        );
        agendamentoPrincipal.associarBarbeiro(barbeiro);
        agendamentoPrincipal.adicionarItemServico(new ItemDeServico(corte, corte.getPreco(), corte.getDuracaoMin()));
        agendamentoPrincipal.adicionarItemServico(new ItemDeServico(barba, barba.getPreco(), barba.getDuracaoMin()));
        System.out.printf("Agendamento principal criado: %s%n", agendamentoPrincipal.getId());

        ContaAtendimento contaPrincipal = sistema.criarContaAtendimento(agendamentoPrincipal);
        System.out.printf("Conta vinculada aberta: %s%n", contaPrincipal.getId());

        Agendamento agendamentoFila = new Agendamento(
                UUID.randomUUID(),
                cliente,
                Estacao.ESTACOES[2],
                agora.plusHours(2),
                agora.plusHours(3),
                Dinheiro.of(new BigDecimal("15"), brl)
        );
        agendamentoFila.adicionarItemServico(new ItemDeServico(corte, corte.getPreco(), corte.getDuracaoMin()));
        sistema.adicionarAgendamentoSecundario(agendamentoFila);
        System.out.printf("Agendamento aguardando na pilha secundária: %s%n", agendamentoFila.getId());

        Agendamento.Cancelamento cancelamento = sistema.cancelarAgendamento(colaborador, agendamentoPrincipal.getId());
        BigDecimal percentualRetencao = cancelamento.getPercentualRetencao().multiply(BigDecimal.valueOf(100));
        System.out.printf("Cancelamento aplicado (retenção %s%%): total=%s | retenção=%s | reembolso=%s%n",
                percentualRetencao.stripTrailingZeros().toPlainString(),
                cancelamento.getTotalServicos(),
                cancelamento.getValorRetencao(),
                cancelamento.getValorReembolso());

        ContaAtendimento contaCancelamento = sistema.buscarContaPorAgendamento(agendamentoPrincipal.getId())
                .orElseThrow();
        System.out.printf("Conta atualizada para cancelamento: total líquido=%s%n", contaCancelamento.getTotal());

        try {
            sistema.obterCaixa(colaborador, LocalDate.now());
        } catch (PermissaoNegadaException e) {
            System.out.printf("Acesso negado ao colaborador para consulta de caixa: %s%n", e.getMessage());
        }

        CaixaDiario caixaAtualizado = sistema.obterCaixa(administrador, LocalDate.now());
        System.out.printf("Caixa após retenção: entradas=%s | projeção do dia=%s%n",
                caixaAtualizado.getEntradasAcumuladas(), caixaAtualizado.projetarBalanco());

        Agendamento recuperado = sistema.recuperarAgendamentoSecundario();
        sistema.realizarAgendamento(recuperado);
        recuperado.associarBarbeiro(barbeiro);
        recuperado.alterarStatus(StatusAtendimento.EM_ATENDIMENTO);
        recuperado.alterarStatus(StatusAtendimento.CONCLUIDO);
        System.out.printf("Agendamento recuperado e concluído: %s%n", recuperado.getId());

        sistema.gerarExtratoServico(recuperado);

        Venda venda = new Venda(
                UUID.randomUUID(),
                cliente,
                LocalDateTime.now(),
                FormaPagamento.PIX
        );
        venda.adicionarItem(new ItemVenda(pomada, Quantidade.of(BigDecimal.ONE, "un"), pomada.getPrecoVenda()));
        venda.adicionarItem(new ItemVenda(balm, Quantidade.of(new BigDecimal("2"), "un"), balm.getPrecoVenda()));
        venda.calcularTotal();
        sistema.registrarVenda(colaborador, venda);
        sistema.gerarExtratoVenda(venda);

        Set<Path> extratosDepois = listarArquivos(extratosDir);
        extratosDepois.removeAll(extratosAntes);
        List<Path> extratosGerados = new ArrayList<>(extratosDepois);
        extratosGerados.sort(Comparator.comparing(Path::toString));
        if (extratosGerados.isEmpty()) {
            System.out.printf("Nenhum novo extrato encontrado em %s%n", extratosDir.toAbsolutePath());
        } else {
            extratosGerados.forEach(path ->
                    System.out.printf("Extrato salvo em: %s%n", path.toAbsolutePath()));
        }

        Despesa despesa = new Despesa(
                UUID.randomUUID(),
                CategoriaDespesa.LIMPEZA,
                "Produtos de limpeza",
                Dinheiro.of(new BigDecimal("80"), brl),
                YearMonth.from(LocalDate.now())
        );
        sistema.registrarDespesa(administrador, despesa);
        System.out.printf("Despesa registrada por %s: %s - %s%n",
                administrador.getNome(), despesa.getCategoria(), despesa.getValor());

        YearMonth competenciaAtual = YearMonth.from(LocalDate.now());

        try {
            sistema.emitirRelatorioFinanceiro(colaborador, competenciaAtual, brl);
        } catch (PermissaoNegadaException e) {
            System.out.printf("Colaborador não pode emitir relatório financeiro: %s%n", e.getMessage());
        }

        String relatorioFinanceiro = sistema.emitirRelatorioFinanceiro(administrador, competenciaAtual, brl);
        System.out.println(relatorioFinanceiro);

        sistema.saveAll(administrador, snapshotPath);
        System.out.printf("Snapshot JSON salvo em: %s%n", snapshotPath.toAbsolutePath());

        System.out.println();
        System.out.println(sistema);
        int totalOsAntes = Sistema.getTotalOrdensServicoCriadas();
        int totalServicosEncapsuladoAntes = Sistema.getTotalServicos();
        int totalServicosProtegidoAntes = Cliente.getTotalServicosProtegido();
        System.out.printf("Total de OS criadas: %d%n", totalOsAntes);
        System.out.printf("Total de serviços (encapsulado): %d%n", totalServicosEncapsuladoAntes);
        System.out.printf("Total de serviços (protegido): %d%n", totalServicosProtegidoAntes);
        System.out.printf("Validação runtime -> serviços=%s | OS=%s%n",
                totalServicosEncapsuladoAntes == 2 && totalServicosProtegidoAntes == 2 ? "OK" : "ERRO",
                totalOsAntes == 2 ? "OK" : "ERRO");

        LocalDate hoje = LocalDate.now();
        List<Venda> vendasDoDia = sistema.listarVendas(administrador).stream()
                .filter(v -> v.getDataHora().toLocalDate().equals(hoje))
                .collect(Collectors.toCollection(ArrayList::new));
        Dinheiro totalVendasDoDia = vendasDoDia.stream()
                .map(Venda::getTotal)
                .reduce(Dinheiro.of(BigDecimal.ZERO, brl), Dinheiro::somar);
        System.out.printf("Relatório de vendas (%s): %d vendas totalizando %s%n",
                hoje, vendasDoDia.size(), totalVendasDoDia);

        Sistema sistemaReidratado = new Sistema();
        // Simula reinício: zera contadores antes do carregamento do snapshot.
        Servico.reidratarContadores(List.of());
        Sistema.redefinirTotalOrdensServico(0);
        sistemaReidratado.loadAll(snapshotPath);
        int totalOsAposLoad = Sistema.getTotalOrdensServicoCriadas();
        int totalServicosEncapsuladoAposLoad = Sistema.getTotalServicos();
        int totalServicosProtegidoAposLoad = Cliente.getTotalServicosProtegido();
        System.out.printf("Após load -> serviços(encapsulado)=%d | serviços(protegido)=%d | OS=%d%n",
                totalServicosEncapsuladoAposLoad, totalServicosProtegidoAposLoad, totalOsAposLoad);
        System.out.printf("Validação pós-load -> serviços=%s | OS=%s%n",
                totalServicosEncapsuladoAposLoad == 2 && totalServicosProtegidoAposLoad == 2 ? "OK" : "ERRO",
                totalOsAposLoad == 2 ? "OK" : "ERRO");

        System.out.println("\nFluxo finalizado sem exceções.");
    }

    @Override
    public String toString() {
        return "Main[roteiro demonstrativo do Sistema da barbearia]";
    }

    private static Set<Path> listarArquivos(Path dir) {
        if (!Files.exists(dir)) {
            return new HashSet<>();
        }
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toCollection(HashSet::new));
        } catch (java.io.IOException e) {
            throw new UncheckedIOException("Falha ao listar arquivos em " + dir, e);
        }
    }
}
