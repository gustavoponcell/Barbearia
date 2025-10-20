package br.ufvjm.barbearia.system;

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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class Sistema {

    // 🔹 Contadores
    private static int totalOrdensServico = 0;

    public static synchronized void incrementarTotalOS() {
        totalOrdensServico++;
    }

    public static synchronized int getTotalOrdensServicoCriadas() {
        return totalOrdensServico;
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
    public void cadastrarUsuario(Usuario u) {
        usuarios.add(Objects.requireNonNull(u, "usuario não pode ser nulo"));
    }

    public void editarUsuario(UUID id, Usuario novo) {
        Objects.requireNonNull(id, "id não pode ser nulo");
        Usuario usuarioAtualizado = Objects.requireNonNull(novo, "novo não pode ser nulo");
        if (!usuarioAtualizado.getId().equals(id)) {
            throw new IllegalArgumentException("ID do usuário não corresponde ao registro atualizado");
        }
        substituirUsuario(id, usuarioAtualizado);
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
}
