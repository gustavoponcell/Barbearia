package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.ModoConsumoProduto;
import br.ufvjm.barbearia.value.Quantidade;
import java.util.Objects;

public class ConsumoDeProduto {

    private final Produto produto;
    private final Quantidade quantidade;
    private final ModoConsumoProduto modo;

    public ConsumoDeProduto(Produto produto, Quantidade quantidade, ModoConsumoProduto modo) {
        this.produto = Objects.requireNonNull(produto, "produto não pode ser nulo");
        this.quantidade = Objects.requireNonNull(quantidade, "quantidade não pode ser nula");
        this.modo = Objects.requireNonNull(modo, "modo não pode ser nulo");
    }

    public Produto getProduto() {
        return produto;
    }

    public Quantidade getQuantidade() {
        return quantidade;
    }

    public ModoConsumoProduto getModo() {
        return modo;
    }

    @Override
    public String toString() {
        return "ConsumoDeProduto{"
                + "produto=" + produto
                + ", quantidade=" + quantidade
                + ", modo=" + modo
                + '}';
    }
}
