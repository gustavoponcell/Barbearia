package br.ufvjm.barbearia.value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Representa um período entre duas datas/horas.
 */
public final class Periodo {

    private static final DateTimeFormatter FORMATADOR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LocalDateTime inicio;
    private final LocalDateTime fim;

    private Periodo(LocalDateTime inicio, LocalDateTime fim) {
        this.inicio = inicio;
        this.fim = fim;
    }

    public static Periodo of(LocalDateTime inicio, LocalDateTime fim) {
        Objects.requireNonNull(inicio, "Início não pode ser nulo");
        Objects.requireNonNull(fim, "Fim não pode ser nulo");
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("Fim não pode ser anterior ao início");
        }
        return new Periodo(inicio, fim);
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public boolean contem(LocalDateTime momento) {
        Objects.requireNonNull(momento, "Momento não pode ser nulo");
        return (momento.isAfter(inicio) || momento.isEqual(inicio))
                && (momento.isBefore(fim) || momento.isEqual(fim));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Periodo)) {
            return false;
        }
        Periodo periodo = (Periodo) o;
        return inicio.equals(periodo.inicio) && fim.equals(periodo.fim);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inicio, fim);
    }

    @Override
    public String toString() {
        return FORMATADOR.format(inicio) + " - " + FORMATADOR.format(fim);
    }
}
