package br.ufvjm.barbearia.persist;

import br.ufvjm.barbearia.model.Agendamento;
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

    public List<Cliente> clientes;
    public List<Usuario> usuarios;
    public List<Servico> servicos;
    public List<Produto> produtos;
    public List<Agendamento> agendamentos;
    public List<Venda> vendas;
    public List<ContaAtendimento> contas;
    public List<Despesa> despesas;
    public List<RecebimentoFornecedor> recebimentos;

    public DataSnapshot() {
        this.clientes = new ArrayList<>();
        this.usuarios = new ArrayList<>();
        this.servicos = new ArrayList<>();
        this.produtos = new ArrayList<>();
        this.agendamentos = new ArrayList<>();
        this.vendas = new ArrayList<>();
        this.contas = new ArrayList<>();
        this.despesas = new ArrayList<>();
        this.recebimentos = new ArrayList<>();
    }
}
