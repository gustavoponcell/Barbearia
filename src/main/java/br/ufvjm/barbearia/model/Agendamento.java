package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.enums.StatusAtendimento;
import br.ufvjm.barbearia.system.Sistema;
import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Periodo;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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

        Sistema.incrementarTotalOS();
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
}
