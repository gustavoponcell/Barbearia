package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final List<MovimentoCaixa> movimentos;

    public CaixaDiario(LocalDate data, Dinheiro saldoAbertura) {
        this.data = Objects.requireNonNull(data, "data não pode ser nula");
        this.saldoAbertura = Objects.requireNonNull(saldoAbertura, "saldoAbertura não pode ser nulo");
        this.entradas = valorZero();
        this.saidas = valorZero();
        this.vendas = new ArrayList<>();
        this.contas = new ArrayList<>();
        this.movimentos = new ArrayList<>();
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

    public List<MovimentoCaixa> getMovimentos() {
        return Collections.unmodifiableList(movimentos);
    }

    public void registrarEntrada(Dinheiro valor, String motivo) {
        validarValorNaoNegativo(valor);
        String motivoValidado = validarMotivo(motivo);
        entradas = entradas.somar(valor);
        registrarMovimento(MovimentoCaixa.entrada(valor, motivoValidado));
        saldoFechamento = null;
    }

    public void registrarSaida(Dinheiro valor, String motivo) {
        validarValorNaoNegativo(valor);
        String motivoValidado = validarMotivo(motivo);
        saidas = saidas.somar(valor);
        registrarMovimento(MovimentoCaixa.saida(valor, motivoValidado));
        saldoFechamento = null;
    }

    public void adicionarVenda(Venda venda) {
        vendas.add(Objects.requireNonNull(venda, "venda não pode ser nula"));
    }

    public void adicionarConta(ContaAtendimento conta) {
        contas.add(Objects.requireNonNull(conta, "conta não pode ser nula"));
    }

    public Dinheiro consolidar() {
        saldoFechamento = projetarBalanco();
        return saldoFechamento;
    }

    public Dinheiro projetarBalanco() {
        return saldoAbertura.somar(entradas).subtrair(saidas);
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
                + ", movimentos=" + movimentos
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

    private String validarMotivo(String motivo) {
        Objects.requireNonNull(motivo, "motivo não pode ser nulo");
        String normalizado = motivo.trim();
        if (normalizado.isEmpty()) {
            throw new IllegalArgumentException("motivo não pode ser vazio");
        }
        return normalizado;
    }

    private void registrarMovimento(MovimentoCaixa movimento) {
        movimentos.add(Objects.requireNonNull(movimento, "movimento não pode ser nulo"));
    }

    public static final class MovimentoCaixa {

        public enum Tipo {
            ENTRADA, SAIDA
        }

        private Tipo tipo;
        private Dinheiro valor;
        private String motivo;
        private LocalDateTime dataHora;

        private MovimentoCaixa() {
            // construtor padrão para serialização
        }

        private MovimentoCaixa(Tipo tipo, Dinheiro valor, String motivo, LocalDateTime dataHora) {
            this.tipo = Objects.requireNonNull(tipo, "tipo não pode ser nulo");
            this.valor = Objects.requireNonNull(valor, "valor não pode ser nulo");
            this.motivo = Objects.requireNonNull(motivo, "motivo não pode ser nulo");
            this.dataHora = Objects.requireNonNull(dataHora, "dataHora não pode ser nula");
        }

        public static MovimentoCaixa entrada(Dinheiro valor, String motivo) {
            return new MovimentoCaixa(Tipo.ENTRADA, valor, motivo, LocalDateTime.now());
        }

        public static MovimentoCaixa saida(Dinheiro valor, String motivo) {
            return new MovimentoCaixa(Tipo.SAIDA, valor, motivo, LocalDateTime.now());
        }

        public Tipo getTipo() {
            return tipo;
        }

        public Dinheiro getValor() {
            return valor;
        }

        public String getMotivo() {
            return motivo;
        }

        public LocalDateTime getDataHora() {
            return dataHora;
        }

        @Override
        public String toString() {
            return "MovimentoCaixa{"
                    + "tipo=" + tipo
                    + ", valor=" + valor
                    + ", motivo='" + motivo + '\''
                    + ", dataHora=" + dataHora
                    + '}';
        }
    }
}
