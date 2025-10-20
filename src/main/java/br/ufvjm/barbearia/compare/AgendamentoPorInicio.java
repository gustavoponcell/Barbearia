package br.ufvjm.barbearia.compare;

import br.ufvjm.barbearia.model.Agendamento;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

/**
 * Comparator que ordena agendamentos pela data/hora de início.
 */
public class AgendamentoPorInicio implements Comparator<Agendamento> {

    @Override
    public int compare(Agendamento agendamento1, Agendamento agendamento2) {
        Objects.requireNonNull(agendamento1, "agendamento1 não pode ser nulo");
        Objects.requireNonNull(agendamento2, "agendamento2 não pode ser nulo");
        LocalDateTime inicio1 = Objects.requireNonNull(agendamento1.getInicio(), "início do agendamento1 não pode ser nulo");
        LocalDateTime inicio2 = Objects.requireNonNull(agendamento2.getInicio(), "início do agendamento2 não pode ser nulo");
        return inicio1.compareTo(inicio2);
    }

    @Override
    public String toString() {
        return "AgendamentoPorInicio{critério='data/hora de início do agendamento'}";
    }
}
