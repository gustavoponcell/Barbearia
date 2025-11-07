package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.FormaPagamento;
import br.ufvjm.barbearia.value.Dinheiro;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa a conta de um atendimento, reunindo serviços e produtos faturados.
 * <p>
 * Nasce vinculada a um {@link Agendamento} concluído e acumula itens adicionais
 * (produtos vendidos/consumidos) antes de calcular o valor final para cobrança.
 * </p>
 *
 * <p>
 * Regras de negócio consideradas:
 * </p>
 * <ul>
 *     <li>Descontos não podem ser negativos nem superiores ao total devido.</li>
 *     <li>O total só pode ser consultado após o cálculo por
 *     {@link #calcularTotal(Dinheiro)}.</li>
 *     <li>A liquidação registra apenas a {@link FormaPagamento}, simulando a
 *     integração com gateways ou caixa físico.</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 * </p>
 * <pre>{@code
 * ContaAtendimento conta = new ContaAtendimento(UUID.randomUUID(), agendamento);
 * conta.adicionarProdutoFaturado(itemCera);
 * Dinheiro total = conta.calcularTotal(agendamento.totalServicos());
 * conta.liquidar(FormaPagamento.DINHEIRO);
 * }</pre>
 */
public class ContaAtendimento {

    private final UUID id;
    private final Agendamento agendamento;
    private final List<ItemContaProduto> produtosFaturados;
    private final List<ItemDeServico> servicosAdicionais;
    private final List<AjusteConta> ajustes;
    private Dinheiro desconto;
    private Dinheiro total;
    private FormaPagamento formaPagamento;
    private CancelamentoRegistro cancelamentoRegistro;
    private boolean fechada;
    private LocalDateTime extratoServicoGeradoEm;
    private String referenciaExtratoServico;

    public ContaAtendimento(UUID id, Agendamento agendamento) {
        this(id, agendamento, null);
    }

    public ContaAtendimento(UUID id, Agendamento agendamento, Dinheiro desconto) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.agendamento = Objects.requireNonNull(agendamento, "agendamento não pode ser nulo");
        if (desconto != null && desconto.getValor().signum() < 0) {
            throw new IllegalArgumentException("desconto não pode ser negativo");
        }
        this.desconto = desconto;
        this.produtosFaturados = new ArrayList<>();
        this.servicosAdicionais = new ArrayList<>();
        this.ajustes = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public Agendamento getAgendamento() {
        return agendamento;
    }

    public List<ItemContaProduto> getProdutosFaturados() {
        return Collections.unmodifiableList(produtosFaturados);
    }

    public Dinheiro getDesconto() {
        return desconto;
    }

    public void aplicarDesconto(Dinheiro desconto) {
        if (desconto != null && desconto.getValor().signum() < 0) {
            throw new IllegalArgumentException("desconto não pode ser negativo");
        }
        this.desconto = desconto;
        this.total = null;
    }

    public Dinheiro getTotal() {
        if (total == null) {
            throw new IllegalStateException("Total ainda não foi calculado");
        }
        return total;
    }

    public FormaPagamento getFormaPagamento() {
        if (formaPagamento == null) {
            throw new IllegalStateException("Conta ainda não foi liquidada");
        }
        return formaPagamento;
    }

    public void adicionarProdutoFaturado(ItemContaProduto item) {
        produtosFaturados.add(Objects.requireNonNull(item, "item não pode ser nulo"));
        total = null;
    }

    public void adicionarServicoFaturado(ItemDeServico item) {
        servicosAdicionais.add(Objects.requireNonNull(item, "item não pode ser nulo"));
        total = null;
    }

    public List<ItemDeServico> getServicosAdicionais() {
        return Collections.unmodifiableList(servicosAdicionais);
    }

    public List<AjusteConta> getAjustes() {
        return Collections.unmodifiableList(ajustes);
    }

    public CancelamentoRegistro getCancelamentoRegistro() {
        return cancelamentoRegistro;
    }

    public void registrarAjuste(AjusteConta ajuste) {
        ajustes.add(Objects.requireNonNull(ajuste, "ajuste não pode ser nulo"));
        total = null;
    }

    public void registrarRetencaoCancelamento(Agendamento.Cancelamento cancelamento) {
        Objects.requireNonNull(cancelamento, "cancelamento não pode ser nulo");
        this.cancelamentoRegistro = new CancelamentoRegistro(
                cancelamento.getPercentualRetencao(),
                cancelamento.getValorRetencao(),
                cancelamento.getValorReembolso(),
                cancelamento.getTotalServicos()
        );
        ajustes.add(AjusteConta.credito(
                String.format("Retenção %s%% sobre cancelamento", cancelamento.getPercentualRetencao()
                        .multiply(BigDecimal.valueOf(100))
                        .stripTrailingZeros()
                        .toPlainString()),
                cancelamento.getValorRetencao()
        ));
        total = null;
    }

    public Dinheiro calcularTotal(Dinheiro totalServicos) {
        Objects.requireNonNull(totalServicos, "totalServicos não pode ser nulo");
        Dinheiro acumulado = baseParaCalculo(totalServicos);
        for (ItemDeServico servico : servicosAdicionais) {
            acumulado = acumulado.somar(servico.subtotal());
        }
        for (ItemContaProduto item : produtosFaturados) {
            acumulado = acumulado.somar(item.subtotal());
        }
        for (AjusteConta ajuste : ajustes) {
            if (ajuste.getTipo() == AjusteConta.Tipo.CREDITO) {
                acumulado = acumulado.somar(ajuste.getValor());
            } else {
                acumulado = acumulado.subtrair(ajuste.getValor());
            }
        }
        if (desconto != null) {
            acumulado = acumulado.subtrair(desconto);
            if (acumulado.getValor().signum() < 0) {
                throw new IllegalStateException("Desconto maior que o total devido");
            }
        }
        total = acumulado;
        return acumulado;
    }

    public Dinheiro calcularTotal() {
        return calcularTotal(agendamento.totalServicos());
    }

    public void liquidar(FormaPagamento formaPagamento) {
        Objects.requireNonNull(formaPagamento, "formaPagamento não pode ser nula");
        if (total == null) {
            throw new IllegalStateException("Total deve ser calculado antes da liquidação");
        }
        // Simulação de pagamento: apenas registra a forma sem integrar com gateways externos.
        this.formaPagamento = formaPagamento;
    }

    public void fecharConta(FormaPagamento formaPagamento) {
        liquidar(formaPagamento);
        this.fechada = true;
    }

    public boolean isFechada() {
        return fechada;
    }

    public boolean isExtratoServicoGerado() {
        return extratoServicoGeradoEm != null;
    }

    public LocalDateTime getExtratoServicoGeradoEm() {
        return extratoServicoGeradoEm;
    }

    public String getReferenciaExtratoServico() {
        return referenciaExtratoServico;
    }

    public void marcarExtratoServicoGerado(LocalDateTime momento, String referencia) {
        if (isExtratoServicoGerado()) {
            return;
        }
        this.extratoServicoGeradoEm = Objects.requireNonNull(momento, "momento não pode ser nulo");
        this.referenciaExtratoServico = Objects.requireNonNull(referencia, "referencia não pode ser nula");
    }

    @Override
    public String toString() {
        return "ContaAtendimento{"
                + "id=" + id
                + ", agendamento=" + agendamento
                + ", produtosFaturados=" + produtosFaturados
                + ", servicosAdicionais=" + servicosAdicionais
                + ", ajustes=" + ajustes
                + ", desconto=" + desconto
                + ", total=" + total
                + ", formaPagamento=" + formaPagamento
                + ", cancelamentoRegistro=" + cancelamentoRegistro
                + ", fechada=" + fechada
                + '}';
    }

    private Dinheiro baseParaCalculo(Dinheiro totalServicos) {
        if (cancelamentoRegistro != null) {
            return Dinheiro.of(BigDecimal.ZERO, totalServicos.getMoeda());
        }
        return totalServicos;
    }

    /**
     * Representa ajustes manuais (créditos e débitos) aplicados à conta.
     */
    public static final class AjusteConta {

        public enum Tipo {
            CREDITO, DEBITO
        }

        private Tipo tipo;
        private String descricao;
        private Dinheiro valor;

        private AjusteConta() {
            // construtor para serialização
        }

        private AjusteConta(Tipo tipo, String descricao, Dinheiro valor) {
            this.tipo = Objects.requireNonNull(tipo, "tipo não pode ser nulo");
            this.descricao = validarDescricao(descricao);
            this.valor = Objects.requireNonNull(valor, "valor não pode ser nulo");
        }

        public static AjusteConta credito(String descricao, Dinheiro valor) {
            return new AjusteConta(Tipo.CREDITO, descricao, valor);
        }

        public static AjusteConta debito(String descricao, Dinheiro valor) {
            return new AjusteConta(Tipo.DEBITO, descricao, valor);
        }

        public Tipo getTipo() {
            return tipo;
        }

        public String getDescricao() {
            return descricao;
        }

        public Dinheiro getValor() {
            return valor;
        }

        private String validarDescricao(String descricao) {
            Objects.requireNonNull(descricao, "descricao não pode ser nula");
            String normalizado = descricao.trim();
            if (normalizado.isEmpty()) {
                throw new IllegalArgumentException("descricao não pode ser vazia");
            }
            return normalizado;
        }

        @Override
        public String toString() {
            return "AjusteConta{"
                    + "tipo=" + tipo
                    + ", descricao='" + descricao + '\''
                    + ", valor=" + valor
                    + '}';
        }
    }

    /**
     * Informações registradas quando há cancelamento com retenção.
     */
    public static final class CancelamentoRegistro {

        private BigDecimal percentualRetencao;
        private Dinheiro valorRetencao;
        private Dinheiro valorReembolso;
        private Dinheiro totalServicos;

        private CancelamentoRegistro() {
            // construtor para serialização
        }

        private CancelamentoRegistro(BigDecimal percentualRetencao, Dinheiro valorRetencao,
                                     Dinheiro valorReembolso, Dinheiro totalServicos) {
            this.percentualRetencao = Objects.requireNonNull(percentualRetencao, "percentualRetencao não pode ser nulo");
            this.valorRetencao = Objects.requireNonNull(valorRetencao, "valorRetencao não pode ser nulo");
            this.valorReembolso = Objects.requireNonNull(valorReembolso, "valorReembolso não pode ser nulo");
            this.totalServicos = Objects.requireNonNull(totalServicos, "totalServicos não pode ser nulo");
        }

        public BigDecimal getPercentualRetencao() {
            return percentualRetencao;
        }

        public Dinheiro getValorRetencao() {
            return valorRetencao;
        }

        public Dinheiro getValorReembolso() {
            return valorReembolso;
        }

        public Dinheiro getTotalServicos() {
            return totalServicos;
        }

        @Override
        public String toString() {
            return "CancelamentoRegistro{"
                    + "percentualRetencao=" + percentualRetencao
                    + ", valorRetencao=" + valorRetencao
                    + ", valorReembolso=" + valorReembolso
                    + ", totalServicos=" + totalServicos
                    + '}';
        }
    }
}
