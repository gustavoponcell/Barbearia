package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.FormaPagamento;
import br.ufvjm.barbearia.value.Dinheiro;
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
    private Dinheiro desconto;
    private Dinheiro total;
    private FormaPagamento formaPagamento;

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

    public Dinheiro calcularTotal(Dinheiro totalServicos) {
        Objects.requireNonNull(totalServicos, "totalServicos não pode ser nulo");
        Dinheiro acumulado = totalServicos;
        for (ItemContaProduto item : produtosFaturados) {
            acumulado = acumulado.somar(item.subtotal());
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

    public void liquidar(FormaPagamento formaPagamento) {
        Objects.requireNonNull(formaPagamento, "formaPagamento não pode ser nula");
        if (total == null) {
            throw new IllegalStateException("Total deve ser calculado antes da liquidação");
        }
        // Simulação de pagamento: apenas registra a forma sem integrar com gateways externos.
        this.formaPagamento = formaPagamento;
    }

    @Override
    public String toString() {
        return "ContaAtendimento{"
                + "id=" + id
                + ", agendamento=" + agendamento
                + ", produtosFaturados=" + produtosFaturados
                + ", desconto=" + desconto
                + ", total=" + total
                + ", formaPagamento=" + formaPagamento
                + '}';
    }
}
