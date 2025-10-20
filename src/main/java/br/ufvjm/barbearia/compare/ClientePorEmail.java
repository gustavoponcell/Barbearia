package br.ufvjm.barbearia.compare;

import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.value.Email;
import java.util.Comparator;
import java.util.Objects;

/**
 * Comparator que ordena clientes pelo endereço de e-mail em ordem alfabética ascendente.
 */
public class ClientePorEmail implements Comparator<Cliente> {

    @Override
    public int compare(Cliente cliente1, Cliente cliente2) {
        Objects.requireNonNull(cliente1, "cliente1 não pode ser nulo");
        Objects.requireNonNull(cliente2, "cliente2 não pode ser nulo");
        Email email1 = Objects.requireNonNull(cliente1.getEmail(), "email do cliente1 não pode ser nulo");
        Email email2 = Objects.requireNonNull(cliente2.getEmail(), "email do cliente2 não pode ser nulo");
        return email1.getValor().compareToIgnoreCase(email2.getValor());
    }

    @Override
    public String toString() {
        return "ClientePorEmail{critério='e-mail do cliente em ordem alfabética ascendente'}";
    }
}
