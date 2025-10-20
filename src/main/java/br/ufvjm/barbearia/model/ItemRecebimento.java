package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Quantidade;
import java.util.Objects;

public class ItemRecebimento {

    private final Produto produto;
    private final Quantidade quantidade;
    private final Dinheiro custoUnitario;

    public ItemRecebimento(Produto produto, Quantidade quantidade, Dinheiro custoUnitario) {
        this.produto = Objects.requireNonNull(produto, "produto não pode ser nulo");
        this.quantidade = Objects.requireNonNull(quantidade, "quantidade não pode ser nula");
        this.custoUnitario = Objects.requireNonNull(custoUnitario, "custoUnitario não pode ser nulo");
    }

    public Produto getProduto() {
        return produto;
    }

    public Quantidade getQuantidade() {
        return quantidade;
    }

    public Dinheiro getCustoUnitario() {
        return custoUnitario;
    }

    public Dinheiro subtotal() {
        return custoUnitario.multiplicar(quantidade.getValor());
    }

    @Override
    public String toString() {
        return "ItemRecebimento{" 
                + "produto=" + produto
                + ", quantidade=" + quantidade
                + ", custoUnitario=" + custoUnitario
                + '}';
    }
}
