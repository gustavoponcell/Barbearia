package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CaixaDiario {

    private final LocalDate data;
    private final Dinheiro saldoAbertura;
    private Dinheiro entradas;
    private Dinheiro saidas;
    private Dinheiro saldoFechamento;
    private final List<Venda> vendas;
    private final List<ContaAtendimento> contas;

    public CaixaDiario(LocalDate data, Dinheiro saldoAbertura) {
        this.data = Objects.requireNonNull(data, "data não pode ser nula");
        this.saldoAbertura = Objects.requireNonNull(saldoAbertura, "saldoAbertura não pode ser nulo");
        this.entradas = valorZero();
        this.saidas = valorZero();
        this.vendas = new ArrayList<>();
        this.contas = new ArrayList<>();
    }

    public LocalDate getData() {
        return data;
    }

    public Dinheiro getSaldoAbertura() {
        return saldoAbertura;
    }

    public Dinheiro getEntradasAcumuladas() {
        return entradas;
    }

    public Dinheiro getSaidasAcumuladas() {
        return saidas;
    }

    public Dinheiro getSaldoFechamento() {
        if (saldoFechamento == null) {
            throw new IllegalStateException("Caixa ainda não foi consolidado");
        }
        return saldoFechamento;
    }

    public List<Venda> getVendas() {
        return Collections.unmodifiableList(vendas);
    }

    public List<ContaAtendimento> getContas() {
        return Collections.unmodifiableList(contas);
    }

    public void registrarEntrada(Dinheiro valor) {
        validarValorNaoNegativo(valor);
        entradas = entradas.somar(valor);
        saldoFechamento = null;
    }

    public void registrarSaida(Dinheiro valor) {
        validarValorNaoNegativo(valor);
        saidas = saidas.somar(valor);
        saldoFechamento = null;
    }

    public void adicionarVenda(Venda venda) {
        vendas.add(Objects.requireNonNull(venda, "venda não pode ser nula"));
    }

    public void adicionarConta(ContaAtendimento conta) {
        contas.add(Objects.requireNonNull(conta, "conta não pode ser nula"));
    }

    public Dinheiro consolidar() {
        saldoFechamento = saldoAbertura.somar(entradas).subtrair(saidas);
        return saldoFechamento;
    }

    @Override
    public String toString() {
        return "CaixaDiario{"
                + "data=" + data
                + ", saldoAbertura=" + saldoAbertura
                + ", entradas=" + entradas
                + ", saidas=" + saidas
                + ", saldoFechamento=" + saldoFechamento
                + ", vendas=" + vendas
                + ", contas=" + contas
                + '}';
    }

    private Dinheiro valorZero() {
        return Dinheiro.of(BigDecimal.ZERO, saldoAbertura.getMoeda());
    }

    private void validarValorNaoNegativo(Dinheiro valor) {
        Objects.requireNonNull(valor, "valor não pode ser nulo");
        if (valor.getValor().signum() < 0) {
            throw new IllegalArgumentException("valor não pode ser negativo");
        }
    }
}
