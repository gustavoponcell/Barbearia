package br.ufvjm.barbearia.enums;

public enum CategoriaDespesa {
    LIMPEZA("Limpeza"),
    CAFE_FUNCIONARIOS("Café para funcionários"),
    MATERIAIS("Materiais"),
    ALUGUEL("Aluguel"),
    ENERGIA("Energia"),
    AGUA("Água"),
    OUTRAS("Outras");

    private final String descricao;

    CategoriaDespesa(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
