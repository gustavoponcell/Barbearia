package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ItemDeServico {

    private final Servico servico;
    private Dinheiro preco;
    private int duracaoMin;
    private final List<ConsumoDeProduto> consumos;

    public ItemDeServico(Servico servico, Dinheiro preco, int duracaoMin) {
        this.servico = Objects.requireNonNull(servico, "serviço não pode ser nulo");
        this.preco = Objects.requireNonNull(preco, "preço não pode ser nulo");
        if (duracaoMin <= 0) {
            throw new IllegalArgumentException("duração deve ser positiva");
        }
        this.duracaoMin = duracaoMin;
        this.consumos = new ArrayList<>();
    }

    public Servico getServico() {
        return servico;
    }

    public Dinheiro getPreco() {
        return preco;
    }

    public void atualizarPreco(Dinheiro preco) {
        this.preco = Objects.requireNonNull(preco, "preço não pode ser nulo");
    }

    public int getDuracaoMin() {
        return duracaoMin;
    }

    public void atualizarDuracao(int duracaoMin) {
        if (duracaoMin <= 0) {
            throw new IllegalArgumentException("duração deve ser positiva");
        }
        this.duracaoMin = duracaoMin;
    }

    public List<ConsumoDeProduto> getConsumos() {
        return Collections.unmodifiableList(consumos);
    }

    public Dinheiro subtotal() {
        return preco;
    }

    public void registrarConsumo(ConsumoDeProduto consumo) {
        consumos.add(Objects.requireNonNull(consumo, "consumo não pode ser nulo"));
    }

    @Override
    public String toString() {
        return "ItemDeServico{"
                + "servico=" + servico
                + ", preco=" + preco
                + ", duracaoMin=" + duracaoMin
                + ", consumos=" + consumos
                + '}';
    }
}
