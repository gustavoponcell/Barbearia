package br.ufvjm.barbearia.model;

import java.util.Objects;

public class Estacao {
    private final int numero;
    private final boolean possuiLavagem;

    // Vetor com tamanho fixo para atender ao requisito de manter as estações predefinidas.
    public static final Estacao[] ESTACOES = new Estacao[3];

    static {
        ESTACOES[0] = new Estacao(1, true);
        ESTACOES[1] = new Estacao(2, false);
        ESTACOES[2] = new Estacao(3, false);
    }

    public Estacao(int numero, boolean possuiLavagem) {
        this.numero = numero;
        this.possuiLavagem = possuiLavagem;
    }

    public int getNumero() {
        return numero;
    }

    public boolean isPossuiLavagem() {
        return possuiLavagem;
    }

    @Override
    public String toString() {
        return "Estacao{" +
                "numero=" + numero +
                ", possuiLavagem=" + possuiLavagem +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Estacao estacao = (Estacao) o;
        return numero == estacao.numero && possuiLavagem == estacao.possuiLavagem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero, possuiLavagem);
    }
}
