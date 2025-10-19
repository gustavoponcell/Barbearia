package br.ufvjm.barbearia.value;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Representa um e-mail válido e imutável.
 */
public final class Email {

    private static final Pattern PADRAO_EMAIL = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );

    private final String valor;

    private Email(String valor) {
        this.valor = valor;
    }

    public static Email of(String raw) {
        Objects.requireNonNull(raw, "E-mail não pode ser nulo");
        String normalizado = raw.trim();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException("E-mail não pode ser vazio");
        }
        if (!PADRAO_EMAIL.matcher(normalizado).matches()) {
            throw new IllegalArgumentException("Formato de e-mail inválido: " + raw);
        }
        return new Email(normalizado);
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Email)) {
            return false;
        }
        Email email = (Email) o;
        return valor.equals(email.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
