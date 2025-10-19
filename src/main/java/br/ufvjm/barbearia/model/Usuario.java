package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Telefone;
import java.util.Objects;
import java.util.UUID;

public class Usuario extends Pessoa {

    private Papel papel;
    private final String login;
    private String senhaHash;
    private boolean ativo;

    public Usuario(UUID id, String nome, Endereco endereco, Telefone telefone, Email email,
                   Papel papel, String login, String senhaHash, boolean ativo) {
        super(id, nome, endereco, telefone, email);
        this.papel = Objects.requireNonNull(papel, "papel não pode ser nulo");
        this.login = Objects.requireNonNull(login, "login não pode ser nulo");
        this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash não pode ser nulo");
        this.ativo = ativo;
    }

    public Papel getPapel() {
        return papel;
    }

    public void setPapel(Papel papel) {
        this.papel = Objects.requireNonNull(papel, "papel não pode ser nulo");
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void alterarSenha(String novaSenhaHash) {
        this.senhaHash = Objects.requireNonNull(novaSenhaHash, "nova senha não pode ser nula");
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    @Override
    public String toString() {
        return "Usuario{"
                + "papel=" + papel
                + ", login='" + login + '\''
                + ", ativo=" + ativo
                + ", pessoa=" + super.toString()
                + '}';
    }
}
