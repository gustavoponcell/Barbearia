package br.ufvjm.barbearia.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Representa um valor monetário com moeda.
 */
public final class Dinheiro {

    private static final int ESCALA_PADRAO = 2;
    private static final RoundingMode ARREDONDAMENTO_PADRAO = RoundingMode.HALF_EVEN;

    private final BigDecimal valor;
    private final Currency moeda;

    private Dinheiro(BigDecimal valor, Currency moeda) {
        this.valor = valor.setScale(ESCALA_PADRAO, ARREDONDAMENTO_PADRAO);
        this.moeda = moeda;
    }

    public static Dinheiro of(BigDecimal valor, Currency moeda) {
        Objects.requireNonNull(valor, "Valor não pode ser nulo");
        Objects.requireNonNull(moeda, "Moeda não pode ser nula");
        return new Dinheiro(valor, moeda);
    }

    public BigDecimal getValor() {
        return valor;
    }

    public Currency getMoeda() {
        return moeda;
    }

    public Dinheiro somar(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return new Dinheiro(this.valor.add(outro.valor), moeda);
    }

    public Dinheiro subtrair(Dinheiro outro) {
        validarMesmaMoeda(outro);
        return new Dinheiro(this.valor.subtract(outro.valor), moeda);
    }

    public Dinheiro multiplicar(BigDecimal multiplicador) {
        Objects.requireNonNull(multiplicador, "Multiplicador não pode ser nulo");
        BigDecimal resultado = this.valor.multiply(multiplicador);
        return new Dinheiro(resultado, moeda);
    }

    private void validarMesmaMoeda(Dinheiro outro) {
        Objects.requireNonNull(outro, "Dinheiro não pode ser nulo");
        if (!moeda.equals(outro.moeda)) {
            throw new IllegalArgumentException("As moedas devem ser iguais para a operação");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Dinheiro)) {
            return false;
        }
        Dinheiro dinheiro = (Dinheiro) o;
        return valor.equals(dinheiro.valor) && moeda.equals(dinheiro.moeda);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor, moeda);
    }

    @Override
    public String toString() {
        return moeda.getCurrencyCode() + " " + valor.toPlainString();
    }
}
