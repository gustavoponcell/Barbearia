package br.ufvjm.barbearia.model;

import java.util.Objects;

/**
 * Representa uma estação física da barbearia.
 * <p>
 * O curso exige três posições fixas compartilhadas por todos os cenários e que
 * devem ser disponibilizadas via vetor estático. A estação de índice 0 precisa
 * obrigatoriamente oferecer lavagem para atender serviços que demandam o
 * recurso. As demais posições não possuem lavatório, permitindo composições
 * distintas conforme o fluxo de atendimento.
 * </p>
 */
public class Estacao {

    private final int numero;
    private final boolean possuiLavagem;

    /**
     * Conjunto imutável de estações disponíveis na barbearia.
     * <p>
     * A estrutura é inicializada uma única vez e compartilhada por todo o
     * sistema, garantindo que agendamentos sempre utilizem uma das três
     * posições conhecidas.
     * </p>
     */
    public static final Estacao[] ESTACOES = new Estacao[3];

    static {
        ESTACOES[0] = new Estacao(1, true);
        ESTACOES[1] = new Estacao(2, false);
        ESTACOES[2] = new Estacao(3, false);
    }

    /**
     * Cria uma estação identificada pelo número e pelo suporte a lavagem.
     *
     * @param numero         identificador legível da estação.
     * @param possuiLavagem  indica se há lavatório disponível.
     */
    public Estacao(int numero, boolean possuiLavagem) {
        this.numero = numero;
        this.possuiLavagem = possuiLavagem;
    }

    /**
     * Obtém o número sequencial da estação.
     *
     * @return identificador numérico usado em relatórios.
     */
    public int getNumero() {
        return numero;
    }

    /**
     * Informa se a estação possui lavatório, requisito para serviços que
     * incluem lavagem dos cabelos.
     *
     * @return {@code true} quando a estação oferece lavagem.
     */
    public boolean isPossuiLavagem() {
        return possuiLavagem;
    }

    @Override
    public String toString() {
        return "Estacao{"
                + "numero=" + numero
                + ", possuiLavagem=" + possuiLavagem
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Estacao estacao = (Estacao) o;
        return numero == estacao.numero && possuiLavagem == estacao.possuiLavagem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(numero, possuiLavagem);
    }
}
