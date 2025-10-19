package br.ufvjm.barbearia.enums;

public enum ModoConsumoProduto {
    CONSUMO_INTERNO("Consumo interno"),
    FATURADO("Faturado");

    private final String descricao;

    ModoConsumoProduto(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
