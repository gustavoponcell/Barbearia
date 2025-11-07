package br.ufvjm.barbearia.persist;

import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.CaixaDiario;
import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.model.ContaAtendimento;
import br.ufvjm.barbearia.model.Despesa;
import br.ufvjm.barbearia.model.Produto;
import br.ufvjm.barbearia.model.RecebimentoFornecedor;
import br.ufvjm.barbearia.model.Servico;
import br.ufvjm.barbearia.model.Usuario;
import br.ufvjm.barbearia.model.Venda;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um snapshot completo do estado das entidades persistidas pelo sistema.
 */
public class DataSnapshot {

    private final List<Cliente> clientes;
    private final List<Usuario> usuarios;
    private final List<Servico> servicos;
    private final List<Produto> produtos;
    private final List<Agendamento> agendamentos;
    private final List<Venda> vendas;
    private final List<ContaAtendimento> contas;
    private final List<Despesa> despesas;
    private final List<RecebimentoFornecedor> recebimentos;
    private final List<CaixaDiario> caixas;

    public DataSnapshot() {
        this(null, null, null, null, null, null, null, null, null, null);
    }

    public DataSnapshot(
            List<Cliente> clientes,
            List<Usuario> usuarios,
            List<Servico> servicos,
            List<Produto> produtos,
            List<Agendamento> agendamentos,
            List<Venda> vendas,
            List<ContaAtendimento> contas,
            List<Despesa> despesas,
            List<RecebimentoFornecedor> recebimentos,
            List<CaixaDiario> caixas
    ) {
        this.clientes = copyList(clientes);
        this.usuarios = copyList(usuarios);
        this.servicos = copyList(servicos);
        this.produtos = copyList(produtos);
        this.agendamentos = copyList(agendamentos);
        this.vendas = copyList(vendas);
        this.contas = copyList(contas);
        this.despesas = copyList(despesas);
        this.recebimentos = copyList(recebimentos);
        this.caixas = copyList(caixas);
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<Cliente> getClientes() {
        return List.copyOf(clientes);
    }

    public List<Usuario> getUsuarios() {
        return List.copyOf(usuarios);
    }

    public List<Servico> getServicos() {
        return List.copyOf(servicos);
    }

    public List<Produto> getProdutos() {
        return List.copyOf(produtos);
    }

    public List<Agendamento> getAgendamentos() {
        return List.copyOf(agendamentos);
    }

    public List<Venda> getVendas() {
        return List.copyOf(vendas);
    }

    public List<ContaAtendimento> getContas() {
        return List.copyOf(contas);
    }

    public List<Despesa> getDespesas() {
        return List.copyOf(despesas);
    }

    public List<RecebimentoFornecedor> getRecebimentos() {
        return List.copyOf(recebimentos);
    }

    public List<CaixaDiario> getCaixas() {
        return List.copyOf(caixas);
    }

    @Override
    public String toString() {
        return String.format(
                "DataSnapshot[clientes=%d, usuarios=%d, servicos=%d, produtos=%d, agendamentos=%d, vendas=%d, contas=%d, despesas=%d, recebimentos=%d, caixas=%d]",
                sizeOf(clientes),
                sizeOf(usuarios),
                sizeOf(servicos),
                sizeOf(produtos),
                sizeOf(agendamentos),
                sizeOf(vendas),
                sizeOf(contas),
                sizeOf(despesas),
                sizeOf(recebimentos),
                sizeOf(caixas)
        );
    }

    private static <T> List<T> copyList(List<T> source) {
        return source != null ? new ArrayList<>(source) : new ArrayList<>();
    }

    private static int sizeOf(List<?> list) {
        return list.size();
    }

    public static final class Builder {
        private List<Cliente> clientes = List.of();
        private List<Usuario> usuarios = List.of();
        private List<Servico> servicos = List.of();
        private List<Produto> produtos = List.of();
        private List<Agendamento> agendamentos = List.of();
        private List<Venda> vendas = List.of();
        private List<ContaAtendimento> contas = List.of();
        private List<Despesa> despesas = List.of();
        private List<RecebimentoFornecedor> recebimentos = List.of();
        private List<CaixaDiario> caixas = List.of();

        private Builder() {
        }

        public Builder withClientes(List<Cliente> clientes) {
            this.clientes = clientes;
            return this;
        }

        public Builder withUsuarios(List<Usuario> usuarios) {
            this.usuarios = usuarios;
            return this;
        }

        public Builder withServicos(List<Servico> servicos) {
            this.servicos = servicos;
            return this;
        }

        public Builder withProdutos(List<Produto> produtos) {
            this.produtos = produtos;
            return this;
        }

        public Builder withAgendamentos(List<Agendamento> agendamentos) {
            this.agendamentos = agendamentos;
            return this;
        }

        public Builder withVendas(List<Venda> vendas) {
            this.vendas = vendas;
            return this;
        }

        public Builder withContas(List<ContaAtendimento> contas) {
            this.contas = contas;
            return this;
        }

        public Builder withDespesas(List<Despesa> despesas) {
            this.despesas = despesas;
            return this;
        }

        public Builder withRecebimentos(List<RecebimentoFornecedor> recebimentos) {
            this.recebimentos = recebimentos;
            return this;
        }

        public Builder withCaixas(List<CaixaDiario> caixas) {
            this.caixas = caixas;
            return this;
        }

        public DataSnapshot build() {
            return new DataSnapshot(
                    clientes,
                    usuarios,
                    servicos,
                    produtos,
                    agendamentos,
                    vendas,
                    contas,
                    despesas,
                    recebimentos,
                    caixas
            );
        }
    }
}
