package br.ufvjm.barbearia.value;

import java.util.Objects;

/**
 * Representa um telefone brasileiro com DDD e número.
 */
public final class Telefone {

    private final String ddd;
    private final String numero;

    private Telefone(String ddd, String numero) {
        this.ddd = ddd;
        this.numero = numero;
    }

    public static Telefone of(String raw) {
        Objects.requireNonNull(raw, "Telefone não pode ser nulo");
        String digitos = raw.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new IllegalArgumentException("Telefone deve possuir 10 ou 11 dígitos numéricos");
        }
        String ddd = digitos.substring(0, 2);
        String numero = digitos.substring(2);
        if (!ddd.matches("[1-9][0-9]")) {
            throw new IllegalArgumentException("DDD inválido: " + ddd);
        }
        if (!numero.matches("[0-9]{8,9}")) {
            throw new IllegalArgumentException("Número de telefone inválido");
        }
        return new Telefone(ddd, numero);
    }

    public String getDdd() {
        return ddd;
    }

    public String getNumero() {
        return numero;
    }

    public String formatado() {
        String principal;
        if (numero.length() == 9) {
            principal = numero.substring(0, 5) + "-" + numero.substring(5);
        } else {
            principal = numero.substring(0, 4) + "-" + numero.substring(4);
        }
        return "(" + ddd + ") " + principal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Telefone)) {
            return false;
        }
        Telefone telefone = (Telefone) o;
        return ddd.equals(telefone.ddd) && numero.equals(telefone.numero);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ddd, numero);
    }

    @Override
    public String toString() {
        return formatado();
    }
}
