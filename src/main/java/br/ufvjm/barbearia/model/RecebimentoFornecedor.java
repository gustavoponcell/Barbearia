package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class RecebimentoFornecedor {

    private final UUID id;
    private final String fornecedor;
    private final LocalDateTime dataHora;
    private final String numeroNF;
    private final List<ItemRecebimento> itens;
    private Dinheiro total;

    public RecebimentoFornecedor(UUID id, String fornecedor, LocalDateTime dataHora, String numeroNF) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.fornecedor = validarTexto(fornecedor, "fornecedor");
        this.dataHora = Objects.requireNonNull(dataHora, "dataHora não pode ser nula");
        this.numeroNF = validarTexto(numeroNF, "numeroNF");
        this.itens = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public String getFornecedor() {
        return fornecedor;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getNumeroNF() {
        return numeroNF;
    }

    public List<ItemRecebimento> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public Dinheiro getTotal() {
        if (total == null) {
            throw new IllegalStateException("Total ainda não foi calculado");
        }
        return total;
    }

    public void adicionarItem(ItemRecebimento itemRecebimento) {
        itens.add(Objects.requireNonNull(itemRecebimento, "itemRecebimento não pode ser nulo"));
        total = null;
    }

    public Dinheiro calcularTotal() {
        if (itens.isEmpty()) {
            throw new IllegalStateException("Recebimento não possui itens");
        }
        Dinheiro acumulado = itens.get(0).subtotal();
        for (int i = 1; i < itens.size(); i++) {
            acumulado = acumulado.somar(itens.get(i).subtotal());
        }
        total = acumulado;
        return acumulado;
    }

    @Override
    public String toString() {
        return "RecebimentoFornecedor{"
                + "id=" + id
                + ", fornecedor='" + fornecedor + '\''
                + ", dataHora=" + dataHora
                + ", numeroNF='" + numeroNF + '\''
                + ", itens=" + itens
                + ", total=" + total
                + '}';
    }

    private String validarTexto(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " não pode ser nulo");
        String normalizado = valor.trim();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException(campo + " não pode ser vazio");
        }
        return normalizado;
    }
}
