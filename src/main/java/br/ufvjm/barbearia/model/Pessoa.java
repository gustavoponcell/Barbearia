package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Telefone;
import java.util.Objects;
import java.util.UUID;

/**
 * Classe base para entidades que representam uma pessoa dentro do domínio.
 */
public abstract class Pessoa {

    private final UUID id;
    private String nome;
    private Endereco endereco;
    private Telefone telefone;
    private Email email;

    protected Pessoa(UUID id, String nome, Endereco endereco, Telefone telefone, Email email) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.nome = Objects.requireNonNull(nome, "nome não pode ser nulo");
        this.endereco = Objects.requireNonNull(endereco, "endereco não pode ser nulo");
        this.telefone = Objects.requireNonNull(telefone, "telefone não pode ser nulo");
        this.email = Objects.requireNonNull(email, "email não pode ser nulo");
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    protected void setNome(String nome) {
        this.nome = Objects.requireNonNull(nome, "nome não pode ser nulo");
    }

    public Endereco getEndereco() {
        return endereco;
    }

    protected void setEndereco(Endereco endereco) {
        this.endereco = Objects.requireNonNull(endereco, "endereco não pode ser nulo");
    }

    public Telefone getTelefone() {
        return telefone;
    }

    protected void setTelefone(Telefone telefone) {
        this.telefone = Objects.requireNonNull(telefone, "telefone não pode ser nulo");
    }

    public Email getEmail() {
        return email;
    }

    protected void setEmail(Email email) {
        this.email = Objects.requireNonNull(email, "email não pode ser nulo");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pessoa pessoa = (Pessoa) o;
        return Objects.equals(id, pessoa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Pessoa{"
                + "id=" + id
                + ", nome='" + nome + '\''
                + ", endereco=" + endereco
                + ", telefone=" + telefone
                + ", email=" + email
                + '}';
    }
}
