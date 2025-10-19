package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.CpfHash;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Telefone;
import java.util.Objects;
import java.util.UUID;

public class Cliente extends Pessoa {

    protected static int totalVeiculosProtegido;

    private final CpfHash cpf;
    private boolean ativo;

    public Cliente(UUID id, String nome, Endereco endereco, Telefone telefone, Email email,
                   CpfHash cpf, boolean ativo) {
        super(id, nome, endereco, telefone, email);
        this.cpf = Objects.requireNonNull(cpf, "cpf não pode ser nulo");
        this.ativo = ativo;
    }

    public CpfHash getCpf() {
        return cpf;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    public void atualizarContato(Endereco endereco, Telefone telefone, Email email) {
        setEndereco(Objects.requireNonNull(endereco, "endereco não pode ser nulo"));
        setTelefone(Objects.requireNonNull(telefone, "telefone não pode ser nulo"));
        setEmail(Objects.requireNonNull(email, "email não pode ser nulo"));
    }

    @Override
    public String toString() {
        return "Cliente{"
                + "cpf=" + cpf
                + ", ativo=" + ativo
                + ", totalVeiculosProtegido=" + totalVeiculosProtegido
                + ", pessoa=" + super.toString()
                + '}';
    }
}
