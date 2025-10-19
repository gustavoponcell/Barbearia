package br.ufvjm.barbearia.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Representa uma quantidade com unidade de medida.
 */
public final class Quantidade {

    private static final int ESCALA_PADRAO = 3;

    private final BigDecimal valor;
    private final String unidade;

    private Quantidade(BigDecimal valor, String unidade) {
        this.valor = valor.setScale(ESCALA_PADRAO, RoundingMode.HALF_EVEN);
        this.unidade = unidade;
    }

    public static Quantidade of(BigDecimal valor, String unidade) {
        Objects.requireNonNull(valor, "Valor não pode ser nulo");
        Objects.requireNonNull(unidade, "Unidade não pode ser nula");
        if (valor.signum() < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        String unidadeNormalizada = unidade.trim();
        if (unidadeNormalizada.isEmpty()) {
            throw new IllegalArgumentException("Unidade não pode ser vazia");
        }
        return new Quantidade(valor, unidadeNormalizada);
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getUnidade() {
        return unidade;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quantidade)) {
            return false;
        }
        Quantidade that = (Quantidade) o;
        return valor.equals(that.valor) && unidade.equals(that.unidade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor, unidade);
    }

    @Override
    public String toString() {
        return valor.stripTrailingZeros().toPlainString() + " " + unidade;
    }
}
