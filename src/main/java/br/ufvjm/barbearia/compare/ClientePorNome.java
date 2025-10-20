package br.ufvjm.barbearia.compare;

import br.ufvjm.barbearia.model.Cliente;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * Comparator que ordena clientes pelo nome em ordem alfabética ascendente.
 */
public class ClientePorNome implements Comparator<Cliente> {

    private static final Collator COLLATOR;

    static {
        COLLATOR = Collator.getInstance(new Locale("pt", "BR"));
        COLLATOR.setStrength(Collator.PRIMARY);
    }

    @Override
    public int compare(Cliente cliente1, Cliente cliente2) {
        Objects.requireNonNull(cliente1, "cliente1 não pode ser nulo");
        Objects.requireNonNull(cliente2, "cliente2 não pode ser nulo");
        String nome1 = Objects.requireNonNull(cliente1.getNome(), "nome do cliente1 não pode ser nulo");
        String nome2 = Objects.requireNonNull(cliente2.getNome(), "nome do cliente2 não pode ser nulo");
        return COLLATOR.compare(nome1, nome2);
    }

    @Override
    public String toString() {
        return "ClientePorNome{critério='nome do cliente em ordem alfabética ascendente'}";
    }
}
