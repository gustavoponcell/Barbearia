package br.ufvjm.barbearia.system;

import br.ufvjm.barbearia.compare.AgendamentoPorInicio;
import br.ufvjm.barbearia.compare.ClientePorNome;
import br.ufvjm.barbearia.enums.FormaPagamento;
import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.exceptions.PermissaoNegadaException;
import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.CaixaDiario;
import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.model.ContaAtendimento;
import br.ufvjm.barbearia.model.Despesa;
import br.ufvjm.barbearia.model.Estacao;
import br.ufvjm.barbearia.model.ItemRecebimento;
import br.ufvjm.barbearia.model.Produto;
import br.ufvjm.barbearia.model.RecebimentoFornecedor;
import br.ufvjm.barbearia.model.Servico;
import br.ufvjm.barbearia.model.Usuario;
import br.ufvjm.barbearia.model.Venda;
import br.ufvjm.barbearia.persist.DataSnapshot;
import br.ufvjm.barbearia.persist.ExtratoIO;
import br.ufvjm.barbearia.persist.JsonStorage;
import br.ufvjm.barbearia.util.Log;
import br.ufvjm.barbearia.value.Dinheiro;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Núcleo orquestrador da aplicação de barbearia.
 * <p>
 * A classe centraliza as coleções in-memory e as principais operações de negócio
 * relacionadas a clientes, usuários, catálogo de serviços e produtos, agenda,
 * contas, vendas, despesas e recebimentos. Também encapsula regras que precisam
 * de consistência global, como a manutenção do contador de ordens de serviço,
 * a pilha (fila secundária) de agendamentos de espera e a geração de extratos.
 * </p>
 *
 * <p>
 * Principais regras de negócio gerenciadas aqui:
 * </p>
 * <ul>
 *     <li>Garantir unicidade lógica dos registros por {@link UUID} e validar
 *     transições (por exemplo, impedir edições de clientes com ID divergente).</li>
 *     <li>Gerenciar a fila secundária (estrutura {@link Deque}) usada para realocar
 *     atendimentos quando há cancelamentos.</li>
 *     <li>Reforçar invariantes financeiros, como nunca permitir {@code null} em
 *     totais, valores ou path de persistência.</li>
 *     <li>Delegar a persistência para {@link JsonStorage}, mantendo a classe como
 *     orquestradora, e não responsável pela serialização em si.</li>
 * </ul>
 *
 * <p>
 * Exemplo típico de uso em uma interface CLI ou teste automatizado:
 * </p>
 * <pre>{@code
 * Sistema sistema = new Sistema();
 * sistema.cadastrarCliente(cliente);
 * sistema.cadastrarServico(barba);
 * sistema.realizarAgendamento(agendamento);
 * sistema.saveAll(usuarioAdmin, Path.of("data/sistema.json"));
 *
 * sistema.loadAll(Path.of("data/sistema.json"));
 * List<Agendamento> doCliente = sistema.listarOrdensDeServicoDoCliente(cliente.getId());
 * }</pre>
 *
 * <p>
 * A classe foi pensada para ambientes desktop/offline, onde o snapshot completo
 * pode ser serializado para JSON sob demanda (fechamento de caixa, backup manual
 * etc.). Para integrações com UI, basta expor a instância única do sistema como
 * um serviço singleton.
 * </p>
 */
public class Sistema {

    // 🔹 Contadores
    private static int totalOrdensServico = 0;
    private static int totalServicos = 0;
    private static final BigDecimal RETENCAO_CANCELAMENTO = new BigDecimal("0.35");
    private static final DateTimeFormatter DATA_HORA_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ClientePorNome DEFAULT_CLIENTE_COMPARATOR = new ClientePorNome();
    private static final AgendamentoPorInicio DEFAULT_AGENDAMENTO_COMPARATOR = new AgendamentoPorInicio();
    private static final Path EXTRATOS_DIR = Path.of("data/extratos");

    /**
     * Implementação utilitária de busca linear usando {@link Iterator} e um {@link Comparator}.
     *
     * @param lista lista onde a busca será executada.
     * @param chave elemento de referência para comparar.
     * @param cmp   critério de comparação que define quando dois elementos são equivalentes.
     * @param <T>   tipo dos elementos manipulados pela lista.
     * @return índice do elemento correspondente ou {@code -1} caso não seja encontrado.
     */
    public static <T> int find(List<T> lista, T chave, Comparator<? super T> cmp) {
        Objects.requireNonNull(lista, "lista não pode ser nula");
        Objects.requireNonNull(chave, "chave não pode ser nula");
        Objects.requireNonNull(cmp, "comparador não pode ser nulo");

        int idx = 0;
        for (Iterator<T> it = lista.iterator(); it.hasNext(); idx++) {
            T atual = it.next();
            if (cmp.compare(atual, chave) == 0) {
                return idx;
            }
        }
        return -1;
    }

    public static synchronized void incrementarTotalOS() {
        totalOrdensServico++;
    }

    public static synchronized int getTotalOrdensServicoCriadas() {
        return totalOrdensServico;
    }

    /**
     * Estratégia encapsulada para o contador de serviços criados.
     * <p>
     * Centraliza o incremento em {@link Sistema} garantindo controle único e
     * diminuindo o risco de alterações indevidas em outros módulos.
     * </p>
     *
     * @return total de instâncias de {@link br.ufvjm.barbearia.model.Servico}
     *         criadas até o momento.
     */
    public static synchronized int getTotalServicos() {
        return totalServicos;
    }

    /**
     * Atualiza o contador encapsulado de serviços criados.
     * <p>
     * Este método deve ser usado apenas durante a reidratação do snapshot, onde
     * o sistema precisa sincronizar os contadores com os dados persistidos.
     * </p>
     *
     * @param total valor recalculado a partir dos serviços carregados.
     */
    public static synchronized void setTotalServicos(int total) {
        totalServicos = Math.max(0, total);
    }

    private static synchronized void incrementarTotalServicos() {
        totalServicos++;
    }

    /**
     * Ajusta o contador encapsulado de ordens de serviço.
     * <p>
     * Mantido com visibilidade de pacote para permitir cenários de testes e
     * reidratação controlada dentro do módulo {@code system}.
     * </p>
     */
    static synchronized void redefinirTotalOrdensServico(int total) {
        totalOrdensServico = Math.max(0, total);
    }

    /**
     * Canal controlado para notificações de criação de serviços.
     * <p>
     * Mantido como classe aninhada pública para permitir chamadas externas sem
     * expor diretamente o método de incremento, preservando o encapsulamento.
     * </p>
     */
    public static final class ServicoTracker {

        private ServicoTracker() {
        }

        public static void registrarCriacaoServico() {
            incrementarTotalServicos();
        }
    }

    // 🔹 Estruturas principais
    private List<Cliente> clientes = new ArrayList<>();
    private List<Usuario> usuarios = new ArrayList<>();
    private List<Servico> servicos = new ArrayList<>();
    private List<Produto> produtos = new ArrayList<>();
    private List<Agendamento> agendamentos = new ArrayList<>();
    private List<Venda> vendas = new ArrayList<>();
    private List<ContaAtendimento> contas = new ArrayList<>();
    private List<Despesa> despesas = new ArrayList<>();
    private List<RecebimentoFornecedor> recebimentos = new ArrayList<>();
    private List<CaixaDiario> caixas = new ArrayList<>();

    // 🔹 Pilha de atendimentos secundários
    private Deque<Agendamento> filaSecundaria = new ArrayDeque<>();

    // 🔹 CRUD de Cliente
    public void cadastrarCliente(Cliente c) {
        clientes.add(Objects.requireNonNull(c, "cliente não pode ser nulo"));
    }

    public void editarCliente(UUID id, Cliente novo) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Cliente clienteAtualizado = Objects.requireNonNull(novo, "novo não pode ser nulo");
        if (!clienteAtualizado.getId().equals(id)) {
            throw new IllegalArgumentException("ID do cliente não corresponde ao registro atualizado");
        }
        substituirCliente(id, clienteAtualizado);
    }

    public void removerCliente(UUID id) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        boolean removido = clientes.removeIf(c -> c.getId().equals(id));
        if (!removido) {
            throw new IllegalArgumentException("Cliente não encontrado: " + id);
        }
    }

    public List<Cliente> listarClientesOrdenados() {
        return listarClientesOrdenados(DEFAULT_CLIENTE_COMPARATOR, 0, clientes.size());
    }

    public List<Cliente> listarClientesOrdenados(int offset, int limit) {
        return listarClientesOrdenados(DEFAULT_CLIENTE_COMPARATOR, offset, limit);
    }

    public List<Cliente> listarClientesOrdenados(Comparator<Cliente> comparator, int offset, int limit) {
        Comparator<Cliente> criterio = comparator != null ? comparator : DEFAULT_CLIENTE_COMPARATOR;
        return ordenarERecortar(clientes, criterio, offset, limit);
    }

    // 🔹 CRUD de Colaboradores
    public void cadastrarUsuario(Usuario solicitante, Usuario novoUsuario) {
        assertAdmin(solicitante);
        usuarios.add(Objects.requireNonNull(novoUsuario, "usuario não pode ser nulo"));
    }

    public void editarUsuario(Usuario solicitante, UUID id, Usuario novo) {
        assertAdmin(solicitante);
        Objects.requireNonNull(id, "id não pode ser nulo");
        Usuario usuarioAtualizado = Objects.requireNonNull(novo, "novo não pode ser nulo");
        if (!usuarioAtualizado.getId().equals(id)) {
            throw new IllegalArgumentException("ID do usuário não corresponde ao registro atualizado");
        }
        substituirUsuario(id, usuarioAtualizado);
    }

    public void removerUsuario(Usuario solicitante, UUID id) {
        assertAdmin(solicitante);
        Objects.requireNonNull(id, "id não pode ser nulo");
        boolean removido = usuarios.removeIf(u -> u.getId().equals(id));
        if (!removido) {
            throw new IllegalArgumentException("Usuário não encontrado: " + id);
        }
    }

    // 🔹 Despesas e balanço
    public void registrarDespesa(Usuario solicitante, Despesa despesa) {
        assertAdmin(solicitante);
        despesas.add(Objects.requireNonNull(despesa, "despesa não pode ser nula"));
    }

    public List<Despesa> listarDespesas(Usuario solicitante) {
        assertAdmin(solicitante);
        return List.copyOf(despesas);
    }

    public void removerDespesa(Usuario solicitante, UUID id) {
        assertAdmin(solicitante);
        Objects.requireNonNull(id, "id não pode ser nulo");
        boolean removido = despesas.removeIf(d -> d.getId().equals(id));
        if (!removido) {
            throw new IllegalArgumentException("Despesa não encontrada: " + id);
        }
    }

    public Dinheiro calcularBalancoMensal(Usuario solicitante, YearMonth competencia, Currency moedaBase) {
        assertAdmin(solicitante);
        Objects.requireNonNull(competencia, "competencia não pode ser nula");
        Currency moeda = Objects.requireNonNull(moedaBase, "moedaBase não pode ser nula");

        Dinheiro totalReceitas = Dinheiro.of(BigDecimal.ZERO, moeda);
        for (Venda venda : vendas) {
            if (YearMonth.from(venda.getDataHora()).equals(competencia)) {
                Dinheiro totalVenda;
                try {
                    totalVenda = venda.getTotal();
                } catch (IllegalStateException e) {
                    totalVenda = venda.calcularTotal();
                }
                validarMoeda(totalVenda, moeda);
                totalReceitas = totalReceitas.somar(totalVenda);
            }
        }

        Dinheiro totalDespesas = Dinheiro.of(BigDecimal.ZERO, moeda);
        for (Despesa despesa : despesas) {
            if (despesa.getCompetencia().equals(competencia)) {
                validarMoeda(despesa.getValor(), moeda);
                totalDespesas = totalDespesas.somar(despesa.getValor());
            }
        }

        return totalReceitas.subtrair(totalDespesas);
    }

    // 🔹 Relatórios
    public String emitirRelatorioFinanceiro(Usuario solicitante, YearMonth competencia, Currency moedaBase) {
        Dinheiro balanco = calcularBalancoMensal(solicitante, competencia, moedaBase);
        return "Relatório Financeiro " + competencia + "\nBalanço: " + balanco;
    }

    public String emitirRelatorioOperacional(Usuario solicitante) {
        return emitirRelatorioOperacional(solicitante,
                DEFAULT_CLIENTE_COMPARATOR, 0, -1,
                DEFAULT_AGENDAMENTO_COMPARATOR, 0, -1);
    }

    public String emitirRelatorioOperacional(Usuario solicitante,
                                             Comparator<Cliente> clienteComparator, int clienteOffset, int clienteLimit,
                                             Comparator<Agendamento> agendamentoComparator, int agendamentoOffset, int agendamentoLimit) {
        Objects.requireNonNull(solicitante, "usuario não pode ser nulo");

        Comparator<Cliente> criterioClientes = clienteComparator != null ? clienteComparator : DEFAULT_CLIENTE_COMPARATOR;
        Comparator<Agendamento> criterioAgendamentos = agendamentoComparator != null ? agendamentoComparator : DEFAULT_AGENDAMENTO_COMPARATOR;

        int clienteOffsetNormalizado = normalizarOffset(clienteOffset);
        int clienteLimiteNormalizado = normalizarLimite(clienteLimit, clientes.size(), clienteOffsetNormalizado);
        int agendamentoOffsetNormalizado = normalizarOffset(agendamentoOffset);
        int agendamentoLimiteNormalizado = normalizarLimite(agendamentoLimit, agendamentos.size(), agendamentoOffsetNormalizado);

        List<Cliente> clientesOrdenados = listarClientesOrdenados(criterioClientes, clienteOffsetNormalizado, clienteLimiteNormalizado);
        List<Agendamento> agendamentosOrdenados = listarAgendamentosOrdenados(criterioAgendamentos, agendamentoOffsetNormalizado, agendamentoLimiteNormalizado);

        String clientesTexto = clientesOrdenados.isEmpty()
                ? "  (sem resultados no intervalo solicitado)"
                : clientesOrdenados.stream()
                .map(c -> String.format("  - %s <%s>", c.getNome(), formatarEmail(c)))
                .collect(Collectors.joining(System.lineSeparator()));

        String agendamentosTexto = agendamentosOrdenados.isEmpty()
                ? "  (sem resultados no intervalo solicitado)"
                : agendamentosOrdenados.stream()
                .map(a -> String.format("  - %s | %s | %s",
                        a.getInicio() != null ? a.getInicio().format(DATA_HORA_FORMATTER) : "(sem início)",
                        a.getCliente() != null ? a.getCliente().getNome() : "(sem cliente)",
                        a.getStatus()))
                .collect(Collectors.joining(System.lineSeparator()));

        return new StringBuilder()
                .append("Relatório Operacional").append(System.lineSeparator())
                .append("Clientes cadastrados: ").append(clientes.size()).append(System.lineSeparator())
                .append("Usuários cadastrados: ").append(usuarios.size()).append(System.lineSeparator())
                .append("Agendamentos registrados: ").append(agendamentos.size()).append(System.lineSeparator())
                .append("Clientes ordenados (offset ").append(clienteOffsetNormalizado)
                .append(", limite ").append(formatarLimite(clienteLimit, clienteLimiteNormalizado)).append(") - exibindo ")
                .append(clientesOrdenados.size()).append(" item(s):").append(System.lineSeparator())
                .append(clientesTexto).append(System.lineSeparator())
                .append("Agendamentos ordenados (offset ").append(agendamentoOffsetNormalizado)
                .append(", limite ").append(formatarLimite(agendamentoLimit, agendamentoLimiteNormalizado)).append(") - exibindo ")
                .append(agendamentosOrdenados.size()).append(" item(s):").append(System.lineSeparator())
                .append(agendamentosTexto)
                .toString();
    }

    // 🔹 Catálogo de Serviços
    public void cadastrarServico(Servico servico) {
        servicos.add(Objects.requireNonNull(servico, "servico não pode ser nulo"));
    }

    public List<Servico> listarServicos() {
        return List.copyOf(servicos);
    }

    // 🔹 Catálogo de Produtos
    public void cadastrarProduto(Produto produto) {
        produtos.add(Objects.requireNonNull(produto, "produto não pode ser nulo"));
    }

    public List<Produto> listarProdutos() {
        return List.copyOf(produtos);
    }

    // 🔹 Vendas
    public void registrarVenda(Usuario solicitante, Venda venda) {
        assertColaboradorOuAdmin(solicitante);
        Venda registro = Objects.requireNonNull(venda, "venda não pode ser nula");
        vendas.add(registro);
        gerarExtratoVenda(registro);
    }

    public List<Venda> listarVendas(Usuario solicitante) {
        assertAdmin(solicitante);
        return List.copyOf(vendas);
    }

    // 🔹 Contas de Atendimento
    public ContaAtendimento criarContaAtendimento(Agendamento agendamento) {
        ContaAtendimento conta = new ContaAtendimento(UUID.randomUUID(),
                Objects.requireNonNull(agendamento, "agendamento não pode ser nulo"));
        contas.add(conta);
        return conta;
    }

    public void registrarConta(ContaAtendimento conta) {
        contas.add(Objects.requireNonNull(conta, "conta não pode ser nula"));
    }

    public void atualizarConta(UUID id, ContaAtendimento contaAtualizada) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        ContaAtendimento atualizada = Objects.requireNonNull(contaAtualizada, "contaAtualizada não pode ser nula");
        if (!atualizada.getId().equals(id)) {
            throw new IllegalArgumentException("ID da conta não corresponde ao registro atualizado");
        }
        substituirConta(id, atualizada);
    }

    public void removerConta(UUID id) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        boolean removida = contas.removeIf(c -> c.getId().equals(id));
        if (!removida) {
            throw new IllegalArgumentException("Conta não encontrada: " + id);
        }
    }

    public List<ContaAtendimento> listarContas() {
        return List.copyOf(contas);
    }

    public Optional<ContaAtendimento> buscarContaPorAgendamento(UUID agendamentoId) {
        Objects.requireNonNull(agendamentoId, "agendamentoId não pode ser nulo");
        return contas.stream()
                .filter(c -> c.getAgendamento().getId().equals(agendamentoId))
                .findFirst();
    }

    public ContaAtendimento fecharContaAtendimento(Usuario solicitante, UUID agendamentoId,
                                                   FormaPagamento formaPagamento) {
        assertColaboradorOuAdmin(solicitante);
        Objects.requireNonNull(agendamentoId, "agendamentoId não pode ser nulo");
        Objects.requireNonNull(formaPagamento, "formaPagamento não pode ser nula");

        Agendamento agendamento = localizarAgendamento(agendamentoId);
        ContaAtendimento conta = buscarContaPorAgendamento(agendamentoId)
                .orElseGet(() -> criarContaAtendimento(agendamento));

        if (!conta.isFechada()) {
            boolean totalCalculado = true;
            try {
                conta.getTotal();
            } catch (IllegalStateException e) {
                totalCalculado = false;
            }
            if (!totalCalculado) {
                conta.calcularTotal(agendamento.totalServicos());
            }
            conta.fecharConta(formaPagamento);
            Log.info("Conta de atendimento %s fechada com %s", conta.getId(), formaPagamento);
        } else {
            Log.debug("Conta de atendimento %s já estava fechada", conta.getId());
        }

        gerarExtratoServico(conta);
        return conta;
    }

    // 🔹 Caixa Diário
    public CaixaDiario abrirCaixa(LocalDate data, Dinheiro saldoAbertura) {
        Objects.requireNonNull(data, "data não pode ser nula");
        Objects.requireNonNull(saldoAbertura, "saldoAbertura não pode ser nulo");
        if (localizarCaixaInterno(data).isPresent()) {
            throw new IllegalStateException("Já existe caixa para a data " + data);
        }
        CaixaDiario caixa = new CaixaDiario(data, saldoAbertura);
        caixas.add(caixa);
        return caixa;
    }

    public List<CaixaDiario> listarCaixas(Usuario solicitante) {
        assertAdmin(solicitante);
        return List.copyOf(caixas);
    }

    public Optional<CaixaDiario> localizarCaixa(Usuario solicitante, LocalDate data) {
        assertAdmin(solicitante);
        Objects.requireNonNull(data, "data não pode ser nula");
        return localizarCaixaInterno(data);
    }

    public CaixaDiario obterCaixa(Usuario solicitante, LocalDate data) {
        assertAdmin(solicitante);
        Objects.requireNonNull(data, "data não pode ser nula");
        return localizarCaixaInterno(data)
                .orElseThrow(() -> new IllegalArgumentException("Caixa não encontrado: " + data));
    }

    public void removerCaixa(LocalDate data) {
        Objects.requireNonNull(data, "data não pode ser nula");
        boolean removido = caixas.removeIf(c -> c.getData().equals(data));
        if (!removido) {
            throw new IllegalArgumentException("Caixa não encontrado: " + data);
        }
    }

    // 🔹 Agendamentos
    public Agendamento criarAgendamento(UUID id, Cliente cliente, Estacao estacao,
                                        LocalDateTime inicio, LocalDateTime fim, Dinheiro sinal) {
        Agendamento agendamento = new Agendamento(id, cliente, estacao, inicio, fim, sinal);
        realizarAgendamento(agendamento);
        return agendamento;
    }

    /**
     * Registra um agendamento já construído, respeitando os contadores globais.
     * <p>
     * O método é utilizado tanto pelo fluxo padrão de criação quanto pela
     * promoção de agendamentos vindos da fila secundária. O incremento do
     * contador de OS acontece em um único ponto para garantir consistência
     * estatística.
     * </p>
     *
     * @param ag agendamento validado a ser incluído na agenda.
     */
    public void realizarAgendamento(Agendamento ag) {
        registrarAgendamento(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
    }

    private void registrarAgendamento(Agendamento ag) {
        agendamentos.add(ag);
        incrementarTotalOS();
        String clienteNome = ag.getCliente() != null ? ag.getCliente().getNome() : "(sem cliente)";
        Log.info("Agendamento registrado: %s para %s", ag.getId(), clienteNome);
    }

    public List<Agendamento> listarAgendamentosOrdenados() {
        return listarAgendamentosOrdenados(DEFAULT_AGENDAMENTO_COMPARATOR, 0, agendamentos.size());
    }

    public List<Agendamento> listarAgendamentosOrdenados(int offset, int limit) {
        return listarAgendamentosOrdenados(DEFAULT_AGENDAMENTO_COMPARATOR, offset, limit);
    }

    public List<Agendamento> listarAgendamentosOrdenados(Comparator<Agendamento> comparator, int offset, int limit) {
        Comparator<Agendamento> criterio = comparator != null ? comparator : DEFAULT_AGENDAMENTO_COMPARATOR;
        return ordenarERecortar(agendamentos, criterio, offset, limit);
    }

    /**
     * Insere um agendamento na fila secundária (estrutura LIFO).
     *
     * @param ag agendamento aguardando vaga na agenda principal.
     */
    public void adicionarAgendamentoSecundario(Agendamento ag) {
        filaSecundaria.push(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
        String clienteNome = ag.getCliente() != null ? ag.getCliente().getNome() : "(sem cliente)";
        Log.info("Agendamento movido para fila secundária: %s (%s)", ag.getId(), clienteNome);
    }

    /**
     * Visualiza o topo da fila secundária sem removê-lo.
     * <p>
     * Implementa o comportamento de {@code peek()}, necessário para o roteiro
     * didático da questão 18. Logs informam se a fila está vazia ou qual cliente
     * aguarda promoção.
     * </p>
     *
     * @return {@link Optional} contendo o agendamento no topo ou vazio se a fila estiver vazia.
     */
    public Optional<Agendamento> inspecionarFilaSecundaria() {
        Agendamento topo = filaSecundaria.peek();
        if (topo == null) {
            Log.info("Inspeção fila secundária: vazia");
        } else {
            String clienteNome = topo.getCliente() != null ? topo.getCliente().getNome() : "(sem cliente)";
            Log.info("Inspeção fila secundária: próximo agendamento %s (%s)", topo.getId(), clienteNome);
        }
        return Optional.ofNullable(topo);
    }

    /**
     * Recupera o agendamento no topo da fila secundária, removendo-o.
     *
     * @return agendamento promovido para agenda principal.
     * @throws NoSuchElementException quando a fila está vazia.
     */
    public Agendamento recuperarAgendamentoSecundario() {
        if (filaSecundaria.isEmpty()) {
            throw new NoSuchElementException("Não há agendamentos na fila secundária");
        }
        return filaSecundaria.pop();
    }

    /**
     * Cancela uma ordem de serviço aplicando retenção de 35% sobre o total.
     * <p>
     * A retenção é definida por {@link #RETENCAO_CANCELAMENTO} e representa a
     * multa cobrada da entrada paga pelo cliente. O método garante que o valor
     * retido seja registrado em {@link ContaAtendimento}, movimenta o
     * {@link CaixaDiario} do dia e gera automaticamente o extrato textual do
     * cancelamento para fins de auditoria.
     * </p>
     *
     * @param solicitante   colaborador ou administrador responsável pela operação.
     * @param agendamentoId identificador da OS a ser cancelada.
     * @return objeto contendo percentual de retenção, total dos serviços e valores retido/reembolsado.
     */
    public Agendamento.Cancelamento cancelarAgendamento(Usuario solicitante, UUID agendamentoId) {
        assertColaboradorOuAdmin(solicitante);
        Objects.requireNonNull(agendamentoId, "agendamentoId não pode ser nulo");
        Agendamento agendamento = localizarAgendamento(agendamentoId);
        // Política financeira: retenção fixa de 35% sobre os serviços contratados.
        Agendamento.Cancelamento cancelamento = agendamento.cancelar(RETENCAO_CANCELAMENTO);
        ContaAtendimento conta = buscarContaPorAgendamento(agendamentoId)
                .orElseGet(() -> criarContaAtendimento(agendamento));
        conta.registrarRetencaoCancelamento(cancelamento);
        conta.calcularTotal(agendamento.totalServicos());

        Dinheiro valorRetencao = cancelamento.getValorRetencao();
        CaixaDiario caixa = obterOuCriarCaixa(LocalDate.now(),
                Dinheiro.of(BigDecimal.ZERO, valorRetencao.getMoeda()));
        caixa.registrarEntrada(valorRetencao, "Retenção cancelamento OS " + agendamento.getId());
        boolean contaAssociada = caixa.getContas().stream()
                .anyMatch(c -> c.getId().equals(conta.getId()));
        if (!contaAssociada) {
            caixa.adicionarConta(conta);
        }

        gerarExtratoCancelamento(agendamento, cancelamento);
        Log.info("Agendamento cancelado: %s (retenção %s)", agendamento.getId(), valorRetencao);
        return cancelamento;
    }

    public List<Agendamento> listarOrdensDeServicoDoCliente(UUID clienteId) {
        Objects.requireNonNull(clienteId, "clienteId não pode ser nulo");
        return agendamentos.stream()
                .filter(a -> a.getCliente().getId().equals(clienteId))
                .collect(Collectors.toList());
    }

    public void imprimirOrdensDeServicoDoCliente(UUID clienteId) {
        listarOrdensDeServicoDoCliente(Objects.requireNonNull(clienteId, "clienteId não pode ser nulo"))
                .forEach(a -> System.out.println(a.toString()));
    }

    // 🔹 Recebimentos de Fornecedor
    public void registrarRecebimentoFornecedor(Usuario solicitante, RecebimentoFornecedor recebimento) {
        registrarRecebimentoFornecedor(solicitante, recebimento, null, null);
    }

    public void registrarRecebimentoFornecedor(Usuario solicitante, RecebimentoFornecedor recebimento,
                                               Dinheiro pagamento, LocalDate dataPagamento) {
        assertAdmin(solicitante);
        RecebimentoFornecedor registro = Objects.requireNonNull(recebimento, "recebimento não pode ser nulo");
        registro.calcularTotal();
        for (ItemRecebimento item : registro.getItens()) {
            Produto produto = item.getProduto();
            produto.movimentarEntrada(item.getQuantidade());
            produto.atualizarCustoMedio(item.getCustoUnitario());
        }
        if (pagamento != null) {
            registro.registrarPagamento(pagamento);
            LocalDate dataMovimento = dataPagamento != null ? dataPagamento : LocalDate.now();
            CaixaDiario caixa = obterOuCriarCaixa(dataMovimento,
                    Dinheiro.of(BigDecimal.ZERO, pagamento.getMoeda()));
            caixa.registrarSaida(pagamento, "Pagamento fornecedor " + registro.getFornecedor());
        }
        recebimentos.add(registro);
    }

    public void atualizarRecebimentoFornecedor(Usuario solicitante, UUID id, RecebimentoFornecedor atualizado) {
        assertAdmin(solicitante);
        Objects.requireNonNull(id, "id não pode ser nulo");
        RecebimentoFornecedor novo = Objects.requireNonNull(atualizado, "atualizado não pode ser nulo");
        if (!novo.getId().equals(id)) {
            throw new IllegalArgumentException("ID do recebimento não corresponde ao registro atualizado");
        }
        substituirRecebimento(id, novo);
    }

    public void removerRecebimentoFornecedor(Usuario solicitante, UUID id) {
        assertAdmin(solicitante);
        Objects.requireNonNull(id, "id não pode ser nulo");
        boolean removido = recebimentos.removeIf(r -> r.getId().equals(id));
        if (!removido) {
            throw new IllegalArgumentException("Recebimento não encontrado: " + id);
        }
    }

    public List<RecebimentoFornecedor> listarRecebimentos(Usuario solicitante) {
        assertAdmin(solicitante);
        return List.copyOf(recebimentos);
    }

    // 🔹 Extratos
    /**
     * Gera o extrato textual do atendimento, associando-o ao cliente e à conta.
     *
     * @param conta conta de atendimento cujo extrato será materializado.
     */
    public void gerarExtratoServico(ContaAtendimento conta) {
        Objects.requireNonNull(conta, "conta não pode ser nula");
        if (conta.isExtratoServicoGerado()) {
            Log.debug("Extrato de serviço da conta %s já gerado em %s", conta.getId(),
                    conta.getExtratoServicoGeradoEm());
            return;
        }

        Agendamento ag = conta.getAgendamento();
        Cliente cliente = ag.getCliente();
        String nomeCliente = cliente != null ? cliente.getNome() : "(sem cliente)";
        String nomeBarbeiro = ag.getBarbeiro() != null ? ag.getBarbeiro().getNome() : "(sem barbeiro)";
        Dinheiro totalConta;
        try {
            totalConta = conta.getTotal();
        } catch (IllegalStateException e) {
            totalConta = conta.calcularTotal(ag.totalServicos());
        }
        String formaPagamentoTexto = conta.isFechada() ? conta.getFormaPagamento().name() : "(não informado)";

        String extrato = new StringBuilder()
                .append("Extrato de Serviço\n")
                .append("OS: ").append(ag.getId()).append('\n')
                .append("Cliente: ").append(nomeCliente).append('\n')
                .append("Barbeiro: ").append(nomeBarbeiro).append('\n')
                .append("Total serviços: ").append(ag.totalServicos()).append('\n')
                .append("Total conta: ").append(totalConta).append('\n')
                .append("Forma de pagamento: ").append(formaPagamentoTexto)
                .toString();

        try {
            Path arquivo = ExtratoIO.saveExtrato(cliente, extrato, EXTRATOS_DIR);
            conta.marcarExtratoServicoGerado(LocalDateTime.now(), arquivo.toString());
            if (cliente != null) {
                cliente.registrarExtrato(arquivo.toString());
            }
            Log.info("Extrato de serviço gerado em %s para %s", arquivo.toAbsolutePath(), nomeCliente);
        } catch (IOException e) {
            Log.error("Falha ao gerar extrato de serviço", e);
            throw new UncheckedIOException("Falha ao gerar extrato de serviço", e);
        }
    }

    /**
     * Gera o extrato de uma venda de produtos associada a um cliente.
     *
     * @param v venda que terá o comprovante gravado em disco.
     */
    public void gerarExtratoVenda(Venda v) {
        Objects.requireNonNull(v, "venda não pode ser nula");
        if (v.isExtratoGerado()) {
            Log.debug("Extrato da venda %s já gerado em %s", v.getId(), v.getExtratoGeradoEm());
            return;
        }
        Cliente cliente = v.getCliente();
        String nomeCliente = cliente != null ? cliente.getNome() : "Consumidor final";
        Dinheiro totalVenda;
        try {
            totalVenda = v.getTotal();
        } catch (IllegalStateException e) {
            totalVenda = v.calcularTotal();
        }
        String extrato = "Extrato de Venda\nCliente: "
                + nomeCliente
                + "\nTotal: " + totalVenda;
        try {
            Path arquivo = ExtratoIO.saveExtrato(cliente, extrato, EXTRATOS_DIR);
            if (cliente != null) {
                cliente.registrarExtrato(arquivo.toString());
            }
            v.marcarExtratoGerado(LocalDateTime.now(), arquivo.toString());
            Log.info("Extrato de venda gerado em %s para %s", arquivo.toAbsolutePath(), nomeCliente);
        } catch (IOException e) {
            Log.error("Falha ao gerar extrato de venda", e);
            throw new UncheckedIOException("Falha ao gerar extrato de venda", e);
        }
    }

    /**
     * Cria o extrato relativo ao cancelamento de um agendamento com retenção.
     *
     * @param agendamento agendamento cancelado.
     * @param cancelamento dados financeiros calculados durante o cancelamento.
     */
    public void gerarExtratoCancelamento(Agendamento agendamento, Agendamento.Cancelamento cancelamento) {
        Objects.requireNonNull(agendamento, "agendamento não pode ser nulo");
        Objects.requireNonNull(cancelamento, "cancelamento não pode ser nulo");
        if (agendamento.isExtratoCancelamentoGerado()) {
            Log.debug("Extrato de cancelamento da OS %s já gerado em %s",
                    agendamento.getId(), agendamento.getExtratoCancelamentoGeradoEm());
            return;
        }
        Cliente cliente = agendamento.getCliente();
        BigDecimal percentual = cancelamento.getPercentualRetencao().multiply(BigDecimal.valueOf(100));
        String extrato = "Extrato de Cancelamento\nCliente: " + cliente.getNome()
                + "\nOrdem de Serviço: " + agendamento.getId()
                + "\nTotal de Serviços: " + cancelamento.getTotalServicos()
                + "\nRetenção (" + percentual.stripTrailingZeros().toPlainString() + "%): " + cancelamento.getValorRetencao()
                + "\nValor a reembolsar: " + cancelamento.getValorReembolso();
        try {
            Path arquivo = ExtratoIO.saveExtrato(cliente, extrato, EXTRATOS_DIR);
            cliente.registrarExtrato(arquivo.toString());
            agendamento.marcarExtratoCancelamentoGerado(LocalDateTime.now(), arquivo.toString());
            Log.info("Extrato de cancelamento gerado em %s para %s", arquivo.toAbsolutePath(), cliente.getNome());
        } catch (IOException e) {
            Log.error("Falha ao gerar extrato de cancelamento", e);
            throw new UncheckedIOException("Falha ao gerar extrato de cancelamento", e);
        }
    }

    // 🔹 Persistência
    /**
     * Persiste um snapshot completo do sistema no caminho informado.
     *
     * @param solicitante usuário com papel {@code ADMIN} responsável pela operação.
     * @param path        destino do arquivo JSON.
     */
    public void saveAll(Usuario solicitante, Path path) {
        assertAdmin(solicitante);
        Objects.requireNonNull(path, "path não pode ser nulo");
        DataSnapshot snap = DataSnapshot.builder()
                .withClientes(clientes)
                .withUsuarios(usuarios)
                .withServicos(servicos)
                .withProdutos(produtos)
                .withAgendamentos(agendamentos)
                .withVendas(vendas)
                .withContas(contas)
                .withDespesas(despesas)
                .withRecebimentos(recebimentos)
                .withCaixas(caixas)
                .build();
        Log.info("Persistindo snapshot em %s via %s", path.toAbsolutePath(), JsonStorage.description());
        try {
            JsonStorage.save(snap, path);
        } catch (IOException e) {
            Log.error("Falha ao salvar dados do sistema", e);
            throw new UncheckedIOException("Falha ao salvar dados do sistema", e);
        }
    }

    /**
     * Carrega um snapshot previamente salvo, reidratando coleções e contadores.
     *
     * @param path caminho do arquivo JSON produzido por {@link #saveAll(Usuario, Path)}.
     */
    public void loadAll(Path path) {
        Objects.requireNonNull(path, "path não pode ser nulo");
        try {
            DataSnapshot snap = JsonStorage.load(path);
            Log.info("Snapshot carregado de %s usando %s", path.toAbsolutePath(), JsonStorage.description());
            this.clientes = new ArrayList<>(snap.getClientes());
            this.usuarios = new ArrayList<>(snap.getUsuarios());
            this.servicos = new ArrayList<>(snap.getServicos());
            this.produtos = new ArrayList<>(snap.getProdutos());
            this.agendamentos = new ArrayList<>(snap.getAgendamentos());
            this.vendas = new ArrayList<>(snap.getVendas());
            this.contas = new ArrayList<>(snap.getContas());
            this.despesas = new ArrayList<>(snap.getDespesas());
            this.recebimentos = new ArrayList<>(snap.getRecebimentos());
            this.caixas = new ArrayList<>(snap.getCaixas());

            Servico.reidratarContadores(this.servicos);
            redefinirTotalOrdensServico(contarElementos(this.agendamentos));
        } catch (IOException e) {
            Log.error("Falha ao carregar dados do sistema", e);
            throw new UncheckedIOException("Falha ao carregar dados do sistema", e);
        }
    }

    @Override
    public String toString() {
        return String.format("\uD83D\uDCCA Sistema Barbearia: %d clientes, %d usuários, %d OS, %d vendas, %d caixas",
                clientes.size(), usuarios.size(), agendamentos.size(), vendas.size(), caixas.size());
    }

    private CaixaDiario obterOuCriarCaixa(LocalDate data, Dinheiro saldoAberturaPadrao) {
        Objects.requireNonNull(data, "data não pode ser nula");
        Dinheiro saldo = Objects.requireNonNull(saldoAberturaPadrao, "saldoAberturaPadrao não pode ser nulo");
        return localizarCaixaInterno(data).orElseGet(() -> {
            CaixaDiario caixa = new CaixaDiario(data, saldo);
            caixas.add(caixa);
            return caixa;
        });
    }

    private Optional<CaixaDiario> localizarCaixaInterno(LocalDate data) {
        Objects.requireNonNull(data, "data não pode ser nula");
        return caixas.stream()
                .filter(c -> c.getData().equals(data))
                .findFirst();
    }

    private Agendamento localizarAgendamento(UUID id) {
        for (Agendamento agendamento : agendamentos) {
            if (agendamento.getId().equals(id)) {
                return agendamento;
            }
        }
        throw new IllegalArgumentException("Agendamento não encontrado: " + id);
    }

    private void substituirConta(UUID id, ContaAtendimento contaAtualizada) {
        for (ListIterator<ContaAtendimento> it = contas.listIterator(); it.hasNext(); ) {
            ContaAtendimento atual = it.next();
            if (atual.getId().equals(id)) {
                it.set(contaAtualizada);
                return;
            }
        }
        throw new IllegalArgumentException("Conta não encontrada: " + id);
    }

    private static int contarElementos(Iterable<?> elementos) {
        if (elementos == null) {
            return 0;
        }
        int total = 0;
        for (Object ignored : elementos) {
            total++;
        }
        return total;
    }

    private void substituirRecebimento(UUID id, RecebimentoFornecedor atualizado) {
        for (ListIterator<RecebimentoFornecedor> it = recebimentos.listIterator(); it.hasNext(); ) {
            RecebimentoFornecedor atual = it.next();
            if (atual.getId().equals(id)) {
                it.set(atualizado);
                return;
            }
        }
        throw new IllegalArgumentException("Recebimento não encontrado: " + id);
    }

    private static String formatarEmail(Cliente cliente) {
        return cliente.getEmail() != null ? cliente.getEmail().getValor() : "sem e-mail";
    }

    private static int normalizarOffset(int offset) {
        return Math.max(0, offset);
    }

    private static int normalizarLimite(int limit, int tamanho, int offsetNormalizado) {
        if (tamanho <= 0 || offsetNormalizado >= tamanho) {
            return 0;
        }
        int maxItensDisponiveis = tamanho - offsetNormalizado;
        if (limit <= 0) {
            return maxItensDisponiveis;
        }
        return Math.min(limit, maxItensDisponiveis);
    }

    private static String formatarLimite(int limiteOriginal, int limiteNormalizado) {
        return limiteOriginal <= 0 ? "todos" : Integer.toString(limiteNormalizado);
    }

    private static <T> List<T> ordenarERecortar(List<T> origem, Comparator<T> comparator, int offset, int limit) {
        List<T> ordenada = new ArrayList<>(origem);
        ordenada.sort(comparator);
        int safeOffset = normalizarOffset(offset);
        int safeLimit = normalizarLimite(limit, ordenada.size(), safeOffset);
        if (safeLimit <= 0) {
            return List.of();
        }
        return List.copyOf(ordenada.subList(safeOffset, safeOffset + safeLimit));
    }

    private void substituirCliente(UUID id, Cliente clienteAtualizado) {
        for (ListIterator<Cliente> it = clientes.listIterator(); it.hasNext(); ) {
            Cliente atual = it.next();
            if (atual.getId().equals(id)) {
                it.set(clienteAtualizado);
                return;
            }
        }
        throw new IllegalArgumentException("Cliente não encontrado: " + id);
    }

    private void substituirUsuario(UUID id, Usuario usuarioAtualizado) {
        for (ListIterator<Usuario> it = usuarios.listIterator(); it.hasNext(); ) {
            Usuario atual = it.next();
            if (atual.getId().equals(id)) {
                it.set(usuarioAtualizado);
                return;
            }
        }
        throw new IllegalArgumentException("Usuário não encontrado: " + id);
    }

    private void assertAdmin(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario solicitante não pode ser nulo");
        if (usuario.getPapel() != Papel.ADMIN) {
            throw new PermissaoNegadaException("Operação permitida apenas para administradores");
        }
    }

    private void assertColaboradorOuAdmin(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario solicitante não pode ser nulo");
        Papel papel = usuario.getPapel();
        if (papel != Papel.ADMIN && papel != Papel.COLABORADOR) {
            throw new PermissaoNegadaException("Operação permitida apenas para administradores ou colaboradores");
        }
    }

    private void validarMoeda(Dinheiro valor, Currency moedaEsperada) {
        if (!valor.getMoeda().equals(moedaEsperada)) {
            throw new IllegalArgumentException("Moeda divergente do balanço informado");
        }
    }
}
