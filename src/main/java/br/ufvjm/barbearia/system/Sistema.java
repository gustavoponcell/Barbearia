package br.ufvjm.barbearia.system;

import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.exceptions.PermissaoNegadaException;
import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.model.ContaAtendimento;
import br.ufvjm.barbearia.model.Despesa;
import br.ufvjm.barbearia.model.Produto;
import br.ufvjm.barbearia.model.RecebimentoFornecedor;
import br.ufvjm.barbearia.model.Servico;
import br.ufvjm.barbearia.model.Usuario;
import br.ufvjm.barbearia.model.Venda;
import br.ufvjm.barbearia.persist.DataSnapshot;
import br.ufvjm.barbearia.persist.ExtratoIO;
import br.ufvjm.barbearia.persist.JsonStorage;
import br.ufvjm.barbearia.value.Dinheiro;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
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
 * sistema.saveAll(Path.of("data/sistema.json"));
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
    private static int totalServicosCriados = 0;

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
    public static synchronized int getTotalServicosCriados() {
        return totalServicosCriados;
    }

    private static synchronized void incrementarTotalServicos() {
        totalServicosCriados++;
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
        Objects.requireNonNull(solicitante, "usuario não pode ser nulo");
        return "Relatório Operacional\nClientes: " + clientes.size()
                + "\nUsuários: " + usuarios.size()
                + "\nAgendamentos: " + agendamentos.size();
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

    // 🔹 Agendamentos
    public void realizarAgendamento(Agendamento ag) {
        agendamentos.add(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
        incrementarTotalOS();
    }

    public void adicionarAgendamentoSecundario(Agendamento ag) {
        filaSecundaria.push(Objects.requireNonNull(ag, "agendamento não pode ser nulo"));
    }

    public Agendamento recuperarAgendamentoSecundario() {
        if (filaSecundaria.isEmpty()) {
            throw new NoSuchElementException("Não há agendamentos na fila secundária");
        }
        return filaSecundaria.pop();
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

    // 🔹 Extratos
    public void gerarExtratoServico(Agendamento ag) {
        Objects.requireNonNull(ag, "agendamento não pode ser nulo");
        String nomeBarbeiro = ag.getBarbeiro() != null ? ag.getBarbeiro().getNome() : "(sem barbeiro)";
        String extrato = "Extrato de Serviço\nCliente: " + ag.getCliente().getNome()
                + "\nBarbeiro: " + nomeBarbeiro
                + "\nTotal: " + ag.totalServicos();
        try {
            ExtratoIO.saveExtrato(ag.getCliente(), extrato, Path.of("data/extratos"));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar extrato de serviço", e);
        }
    }

    public void gerarExtratoVenda(Venda v) {
        Objects.requireNonNull(v, "venda não pode ser nula");
        Cliente cliente = Objects.requireNonNull(v.getCliente(), "venda deve estar associada a um cliente");
        String nomeCliente = cliente != null ? cliente.getNome() : "Consumidor";
        String extrato = "Extrato de Venda\nCliente: "
                + nomeCliente
                + "\nTotal: " + v.getTotal();
        try {
            ExtratoIO.saveExtrato(cliente, extrato, Path.of("data/extratos"));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar extrato de venda", e);
        }
    }

    // 🔹 Persistência
    public void saveAll(Path path) {
        Objects.requireNonNull(path, "path não pode ser nulo");
        DataSnapshot snap = new DataSnapshot();
        snap.clientes = new ArrayList<>(clientes);
        snap.usuarios = new ArrayList<>(usuarios);
        snap.servicos = new ArrayList<>(servicos);
        snap.produtos = new ArrayList<>(produtos);
        snap.agendamentos = new ArrayList<>(agendamentos);
        snap.vendas = new ArrayList<>(vendas);
        snap.contas = new ArrayList<>(contas);
        snap.despesas = new ArrayList<>(despesas);
        snap.recebimentos = new ArrayList<>(recebimentos);
        try {
            JsonStorage.save(snap, path);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao salvar dados do sistema", e);
        }
    }

    public void loadAll(Path path) {
        Objects.requireNonNull(path, "path não pode ser nulo");
        try {
            DataSnapshot snap = JsonStorage.load(path);
            this.clientes = new ArrayList<>(Objects.requireNonNullElse(snap.clientes, List.of()));
            this.usuarios = new ArrayList<>(Objects.requireNonNullElse(snap.usuarios, List.of()));
            this.servicos = new ArrayList<>(Objects.requireNonNullElse(snap.servicos, List.of()));
            this.produtos = new ArrayList<>(Objects.requireNonNullElse(snap.produtos, List.of()));
            this.agendamentos = new ArrayList<>(Objects.requireNonNullElse(snap.agendamentos, List.of()));
            this.vendas = new ArrayList<>(Objects.requireNonNullElse(snap.vendas, List.of()));
            this.contas = new ArrayList<>(Objects.requireNonNullElse(snap.contas, List.of()));
            this.despesas = new ArrayList<>(Objects.requireNonNullElse(snap.despesas, List.of()));
            this.recebimentos = new ArrayList<>(Objects.requireNonNullElse(snap.recebimentos, List.of()));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao carregar dados do sistema", e);
        }
    }

    @Override
    public String toString() {
        return String.format("\uD83D\uDCCA Sistema Barbearia: %d clientes, %d usuários, %d OS, %d vendas",
                clientes.size(), usuarios.size(), agendamentos.size(), vendas.size());
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
