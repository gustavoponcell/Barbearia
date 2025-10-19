package br.ufvjm.barbearia.enums;

public enum FormaPagamento {
    DINHEIRO("Dinheiro"),
    CARTAO_DEBITO("Cartão de débito"),
    CARTAO_CREDITO("Cartão de crédito"),
    PIX("Pix"),
    OUTRO("Outro");

    private final String descricao;

    FormaPagamento(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
