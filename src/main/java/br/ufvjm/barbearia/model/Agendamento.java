package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.StatusAtendimento;
import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Periodo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa uma Ordem de Serviço (OS) na agenda da barbearia.
 * <p>
 * A entidade vincula cliente, barbeiro (opcional na criação), estação, período e
 * itens de serviço que serão executados, além de controlar o status do
 * atendimento e o sinal recebido na reserva.
 * </p>
 *
 * <p>
 * Regras centrais:
 * </p>
 * <ul>
 *     <li>O contador global de OS é mantido pelo serviço de agendamento do
 *     sistema, garantindo um único ponto de criação.</li>
 *     <li>O período {@link #inicio}/{@link #fim} deve ser válido e com fim posterior
 *     ao início.</li>
 *     <li>As transições de {@link StatusAtendimento} respeitam a sequência
 *     {@code EM_ESPERA → EM_ATENDIMENTO → CONCLUIDO} com cancelamento permitido a
 *     qualquer momento.</li>
 *     <li>O total de serviços é calculado pela soma dos itens vinculados, e a
 *     ausência de itens impede a faturação.</li>
 * </ul>
 *
 * <p>
 * Exemplo de uso:
 * </p>
 * <pre>{@code
 * Agendamento ag = new Agendamento(UUID.randomUUID(), cliente, estacao,
 *         inicio, fim, Dinheiro.of("50.00"));
 * ag.adicionarItemServico(new ItemDeServico(servicoCorte, 1));
 * ag.associarBarbeiro(usuarioBarbeiro);
 * ag.alterarStatus(StatusAtendimento.EM_ATENDIMENTO);
 * }</pre>
 */
public class Agendamento {

    private final UUID id;
    private final Cliente cliente;
    private Usuario barbeiro;
    private final Estacao estacao;
    private final LocalDateTime inicio;
    private final LocalDateTime fim;
    private final List<ItemDeServico> itens;
    private StatusAtendimento status;
    private final Dinheiro sinal;
    private LocalDateTime extratoCancelamentoGeradoEm;
    private String referenciaExtratoCancelamento;

    public Agendamento(UUID id, Cliente cliente, Estacao estacao,
                       LocalDateTime inicio, LocalDateTime fim, Dinheiro sinal) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.cliente = Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        this.estacao = Objects.requireNonNull(estacao, "estacao não pode ser nula");
        this.inicio = Objects.requireNonNull(inicio, "inicio não pode ser nulo");
        this.fim = Objects.requireNonNull(fim, "fim não pode ser nulo");
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("fim não pode ser anterior ao início");
        }
        this.sinal = Objects.requireNonNull(sinal, "sinal não pode ser nulo");
        this.itens = new ArrayList<>();
        this.status = StatusAtendimento.EM_ESPERA;
    }

    public UUID getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Usuario getBarbeiro() {
        return barbeiro;
    }

    public Estacao getEstacao() {
        return estacao;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public List<ItemDeServico> getItens() {
        return Collections.unmodifiableList(itens);
    }

    public StatusAtendimento getStatus() {
        return status;
    }

    public Dinheiro getSinal() {
        return sinal;
    }

    public void adicionarItemServico(ItemDeServico itemDeServico) {
        itens.add(Objects.requireNonNull(itemDeServico, "itemDeServico não pode ser nulo"));
    }

    public void associarBarbeiro(Usuario barbeiro) {
        this.barbeiro = Objects.requireNonNull(barbeiro, "barbeiro não pode ser nulo");
    }

    public void alterarStatus(StatusAtendimento novoStatus) {
        Objects.requireNonNull(novoStatus, "novoStatus não pode ser nulo");
        if (!transicaoValida(this.status, novoStatus)) {
            throw new IllegalStateException("Transição de status inválida: " + status + " -> " + novoStatus);
        }
        this.status = novoStatus;
    }

    public Cancelamento cancelar(BigDecimal percentualRetencao) {
        Objects.requireNonNull(percentualRetencao, "percentualRetencao não pode ser nulo");
        if (percentualRetencao.compareTo(BigDecimal.ZERO) < 0
                || percentualRetencao.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("percentual de retenção deve estar entre 0 e 1");
        }
        if (status == StatusAtendimento.CANCELADO) {
            throw new IllegalStateException("Agendamento já está cancelado");
        }
        Dinheiro totalServicos = totalServicos();
        Dinheiro valorRetencao = totalServicos.multiplicar(percentualRetencao);
        Dinheiro valorReembolso = totalServicos.subtrair(valorRetencao);
        alterarStatus(StatusAtendimento.CANCELADO);
        return new Cancelamento(percentualRetencao, totalServicos, valorRetencao, valorReembolso);
    }

    private boolean transicaoValida(StatusAtendimento atual, StatusAtendimento novo) {
        if (novo == StatusAtendimento.CANCELADO) {
            return true;
        }
        if (atual == StatusAtendimento.EM_ESPERA && novo == StatusAtendimento.EM_ATENDIMENTO) {
            return true;
        }
        return atual == StatusAtendimento.EM_ATENDIMENTO && novo == StatusAtendimento.CONCLUIDO;
    }

    public Dinheiro totalServicos() {
        if (itens.isEmpty()) {
            throw new IllegalStateException("Agendamento não possui itens de serviço");
        }
        Dinheiro total = itens.get(0).subtotal();
        for (int i = 1; i < itens.size(); i++) {
            total = total.somar(itens.get(i).subtotal());
        }
        return total;
    }

    public Periodo periodo() {
        return Periodo.of(inicio, fim);
    }

    public boolean requerLavagem() {
        return itens.stream()
                .map(ItemDeServico::getServico)
                .anyMatch(Servico::isRequerLavagem);
    }

    public boolean isExtratoCancelamentoGerado() {
        return extratoCancelamentoGeradoEm != null;
    }

    public LocalDateTime getExtratoCancelamentoGeradoEm() {
        return extratoCancelamentoGeradoEm;
    }

    public String getReferenciaExtratoCancelamento() {
        return referenciaExtratoCancelamento;
    }

    public void marcarExtratoCancelamentoGerado(LocalDateTime momento, String referencia) {
        if (isExtratoCancelamentoGerado()) {
            return;
        }
        this.extratoCancelamentoGeradoEm = Objects.requireNonNull(momento, "momento não pode ser nulo");
        this.referenciaExtratoCancelamento = Objects.requireNonNull(referencia, "referencia não pode ser nula");
    }

    @Override
    public String toString() {
        return "Agendamento{"
                + "id=" + id
                + ", cliente=" + cliente
                + ", barbeiro=" + barbeiro
                + ", estacao=" + estacao
                + ", inicio=" + inicio
                + ", fim=" + fim
                + ", itens=" + itens
                + ", status=" + status
                + ", sinal=" + sinal
                + '}';
    }

    public static final class Cancelamento {

        private final BigDecimal percentualRetencao;
        private final Dinheiro totalServicos;
        private final Dinheiro valorRetencao;
        private final Dinheiro valorReembolso;

        private Cancelamento(BigDecimal percentualRetencao, Dinheiro totalServicos,
                              Dinheiro valorRetencao, Dinheiro valorReembolso) {
            this.percentualRetencao = percentualRetencao;
            this.totalServicos = totalServicos;
            this.valorRetencao = valorRetencao;
            this.valorReembolso = valorReembolso;
        }

        public BigDecimal getPercentualRetencao() {
            return percentualRetencao;
        }

        public Dinheiro getTotalServicos() {
            return totalServicos;
        }

        public Dinheiro getValorRetencao() {
            return valorRetencao;
        }

        public Dinheiro getValorReembolso() {
            return valorReembolso;
        }

        @Override
        public String toString() {
            return "Cancelamento{"
                    + "percentualRetencao=" + percentualRetencao
                    + ", totalServicos=" + totalServicos
                    + ", valorRetencao=" + valorRetencao
                    + ", valorReembolso=" + valorReembolso
                    + '}';
        }
    }
}
