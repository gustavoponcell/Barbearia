package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Quantidade;
import java.util.Objects;

public class ItemVenda {

    private final Produto produto;
    private final Quantidade quantidade;
    private final Dinheiro precoUnitario;

    public ItemVenda(Produto produto, Quantidade quantidade, Dinheiro precoUnitario) {
        this.produto = Objects.requireNonNull(produto, "produto não pode ser nulo");
        this.quantidade = Objects.requireNonNull(quantidade, "quantidade não pode ser nula");
        this.precoUnitario = Objects.requireNonNull(precoUnitario, "precoUnitario não pode ser nulo");
    }

    public Produto getProduto() {
        return produto;
    }

    public Quantidade getQuantidade() {
        return quantidade;
    }

    public Dinheiro getPrecoUnitario() {
        return precoUnitario;
    }

    public Dinheiro subtotal() {
        return precoUnitario.multiplicar(quantidade.getValor());
    }

    @Override
    public String toString() {
        return "ItemVenda{"
                + "produto=" + produto
                + ", quantidade=" + quantidade
                + ", precoUnitario=" + precoUnitario
                + '}';
    }
}

