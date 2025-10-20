package br.ufvjm.barbearia.compare;

import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.Cliente;
import java.text.Collator;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * Comparator que ordena agendamentos pelo nome do cliente e desempata pela data/hora de início.
 */
public class AgendamentoPorClienteNome implements Comparator<Agendamento> {

    private static final Collator COLLATOR;

    static {
        COLLATOR = Collator.getInstance(new Locale("pt", "BR"));
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    @Override
    public int compare(Agendamento agendamento1, Agendamento agendamento2) {
        Objects.requireNonNull(agendamento1, "agendamento1 não pode ser nulo");
        Objects.requireNonNull(agendamento2, "agendamento2 não pode ser nulo");
        Cliente cliente1 = Objects.requireNonNull(agendamento1.getCliente(), "cliente do agendamento1 não pode ser nulo");
        Cliente cliente2 = Objects.requireNonNull(agendamento2.getCliente(), "cliente do agendamento2 não pode ser nulo");
        String nome1 = Objects.requireNonNull(cliente1.getNome(), "nome do cliente1 não pode ser nulo");
        String nome2 = Objects.requireNonNull(cliente2.getNome(), "nome do cliente2 não pode ser nulo");

        int comparacaoNome = COLLATOR.compare(nome1, nome2);
        if (comparacaoNome != 0) {
            return comparacaoNome;
        }

        LocalDateTime inicio1 = Objects.requireNonNull(agendamento1.getInicio(), "início do agendamento1 não pode ser nulo");
        LocalDateTime inicio2 = Objects.requireNonNull(agendamento2.getInicio(), "início do agendamento2 não pode ser nulo");
        return inicio1.compareTo(inicio2);
    }

    @Override
    public String toString() {
        return "AgendamentoPorClienteNome{critério='nome do cliente (A-Z) e, em caso de empate, data/hora de início'}";
    }
}
