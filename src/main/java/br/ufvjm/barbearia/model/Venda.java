package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.FormaPagamento;
import br.ufvjm.barbearia.value.Dinheiro;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Venda {

    private final UUID id;
    private final Cliente cliente;
    private final LocalDateTime dataHora;
    private final List<ItemVenda> itens;
    private final FormaPagamento formaPagamento;
    private final Dinheiro desconto;
    private Dinheiro total;

    public Venda(UUID id, Cliente cliente, LocalDateTime dataHora, FormaPagamento formaPagamento) {
        this(id, cliente, dataHora, formaPagamento, null);
    }

    public Venda(UUID id, Cliente cliente, LocalDateTime dataHora, FormaPagamento formaPagamento, Dinheiro desconto) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.cliente = cliente;
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora não pode ser nula");
        this.formaPagamento = Objects.requireNonNull(formaPagamento, "formaPagamento não pode ser nula");
        if (desconto != null && desconto.getValor().signum() < 0) {
            throw new IllegalArgumentException("desconto não pode ser negativo");
        }
        this.desconto = desconto;
        this.itens = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public List<ItemVenda> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public FormaPagamento getFormaPagamento() {
        return formaPagamento;
    }

    public Dinheiro getDesconto() {
        return desconto;
    }

    public Dinheiro getTotal() {
        if (total == null) {
            throw new IllegalStateException("Total da venda ainda não foi calculado");
        }
        return total;
    }

    public void adicionarItem(ItemVenda itemVenda) {
        itens.add(Objects.requireNonNull(itemVenda, "itemVenda não pode ser nulo"));
        total = null;
    }

    public Dinheiro calcularTotal() {
        if (itens.isEmpty()) {
            throw new IllegalStateException("Venda não possui itens");
        }
        Dinheiro totalCalculado = itens.get(0).subtotal();
        for (int i = 1; i < itens.size(); i++) {
            totalCalculado = totalCalculado.somar(itens.get(i).subtotal());
        }
        if (desconto != null) {
            totalCalculado = totalCalculado.subtrair(desconto);
            if (totalCalculado.getValor().signum() < 0) {
                throw new IllegalStateException("Desconto maior que o total dos itens");
            }
        }
        total = totalCalculado;
        return totalCalculado;
    }

    @Override
    public String toString() {
        return "Venda{"
                + "id=" + id
                + ", cliente=" + cliente
                + ", dataHora=" + dataHora
                + ", itens=" + itens
                + ", formaPagamento=" + formaPagamento
                + ", desconto=" + desconto
                + ", total=" + total
                + '}';
    }
}

