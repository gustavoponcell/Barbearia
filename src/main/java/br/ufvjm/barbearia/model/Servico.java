package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa um serviço disponível no catálogo da barbearia.
 * <p>
 * Armazena nome, preço, duração estimada em minutos e se requer estação com
 * lavagem, informação usada pelo {@link Agendamento} para reservar os recursos
 * corretos.
 * </p>
 *
 * <p>
 * Regras principais:
 * </p>
 * <ul>
 *     <li>Nome e preço são obrigatórios; a duração deve ser positiva.</li>
 *     <li>{@link #isRequerLavagem()} orienta a alocação de {@link Estacao} com
 *     lavatório quando necessário.</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 * </p>
 * <pre>{@code
 * Servico corteComLavagem = new Servico(UUID.randomUUID(), "Corte + Lavagem",
 *         Dinheiro.of("70.00"), 45, true);
 * }</pre>
 */
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
