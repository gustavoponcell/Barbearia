package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Telefone;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa os usuários internos do sistema (administrador, atendente ou barbeiro).
 * <p>
 * Amplia {@link Pessoa} com credenciais, papel e estado ativo, sendo a base para
 * o controle de permissões descrito nos casos de uso do projeto.
 * </p>
 *
 * <p>
 * Regras importantes:
 * </p>
 * <ul>
 *     <li>{@link Papel} define o conjunto de operações disponíveis na UI.</li>
 *     <li>O login é imutável após a criação, enquanto a senha pode ser alterada
 *     por meio de {@link #alterarSenha(String)} com valor já criptografado/hash.</li>
 *     <li>Usuários podem ser desativados temporariamente sem remoção de histórico
 *     de vendas/atendimentos.</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 * </p>
 * <pre>{@code
 * Usuario admin = new Usuario(UUID.randomUUID(), "Ana", endereco, telefone,
 *         email, Papel.ADMINISTRADOR, "ana", senhaHash, true);
 * }</pre>
 */
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
