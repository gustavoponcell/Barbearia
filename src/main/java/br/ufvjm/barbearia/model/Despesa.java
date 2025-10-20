package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.CategoriaDespesa;
import br.ufvjm.barbearia.value.Dinheiro;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

public class Despesa {

    private final UUID id;
    private final CategoriaDespesa categoria;
    private final String descricao;
    private final Dinheiro valor;
    private final YearMonth competencia;
    private LocalDate dataPagamento;

    public Despesa(UUID id, CategoriaDespesa categoria, String descricao, Dinheiro valor, YearMonth competencia) {
        this(id, categoria, descricao, valor, competencia, null);
    }

    public Despesa(UUID id, CategoriaDespesa categoria, String descricao, Dinheiro valor, YearMonth competencia, LocalDate dataPagamento) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.categoria = Objects.requireNonNull(categoria, "categoria não pode ser nula");
        this.descricao = validarDescricao(descricao);
        this.valor = Objects.requireNonNull(valor, "valor não pode ser nulo");
        this.competencia = Objects.requireNonNull(competencia, "competencia não pode ser nula");
        this.dataPagamento = dataPagamento;
    }

    public UUID getId() {
        return id;
    }

    public CategoriaDespesa getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public Dinheiro getValor() {
        return valor;
    }

    public YearMonth getCompetencia() {
        return competencia;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public boolean estaPaga() {
        return dataPagamento != null;
    }

    public void registrarPagamento(LocalDate dataPagamento) {
        this.dataPagamento = Objects.requireNonNull(dataPagamento, "dataPagamento não pode ser nula");
    }

    @Override
    public String toString() {
        return "Despesa{"
                + "id=" + id
                + ", categoria=" + categoria
                + ", descricao='" + descricao + '\''
                + ", valor=" + valor
                + ", competencia=" + competencia
                + ", dataPagamento=" + dataPagamento
                + '}';
    }

    private String validarDescricao(String descricao) {
        Objects.requireNonNull(descricao, "descricao não pode ser nula");
        String normalizada = descricao.trim();
        if (normalizada.isEmpty()) {
            throw new IllegalArgumentException("descricao não pode ser vazia");
        }
        return normalizada;
    }
}
