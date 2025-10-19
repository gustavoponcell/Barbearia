package br.ufvjm.barbearia.enums;

public enum StatusAtendimento {
    EM_ESPERA("Em espera"),
    EM_ATENDIMENTO("Em atendimento"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusAtendimento(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
