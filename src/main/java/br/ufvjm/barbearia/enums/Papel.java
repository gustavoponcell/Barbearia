package br.ufvjm.barbearia.enums;

public enum Papel {
    ADMIN("Administrador"),
    COLABORADOR("Colaborador"),
    BARBEIRO("Barbeiro");

    private final String descricao;

    Papel(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
