package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import java.util.Objects;
import java.util.UUID;

public class Servico {

    private final UUID id;
    private final String nome;
    private final Dinheiro preco;
    private final int duracaoMin;
    private final boolean requerLavagem;

    public Servico(UUID id, String nome, Dinheiro preco, int duracaoMin, boolean requerLavagem) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.nome = validarNome(nome);
        this.preco = Objects.requireNonNull(preco, "preço não pode ser nulo");
        if (duracaoMin <= 0) {
            throw new IllegalArgumentException("duração deve ser positiva");
        }
        this.duracaoMin = duracaoMin;
        this.requerLavagem = requerLavagem;
    }

    private String validarNome(String nome) {
        String nomeNormalizado = Objects.requireNonNull(nome, "nome não pode ser nulo").trim();
        if (nomeNormalizado.isEmpty()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        return nomeNormalizado;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Dinheiro getPreco() {
        return preco;
    }

    public int getDuracaoMin() {
        return duracaoMin;
    }

    public boolean isRequerLavagem() {
        return requerLavagem;
    }

    @Override
    public String toString() {
        return "Servico{"
                + "id=" + id
                + ", nome='" + nome + '\''
                + ", preco=" + preco
                + ", duracaoMin=" + duracaoMin
                + ", requerLavagem=" + requerLavagem
                + '}';
    }
}
