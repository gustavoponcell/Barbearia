package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Quantidade;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Modelo de produto comercializado ou consumido durante atendimentos.
 * <p>
 * Contém informações de estoque, preços e custo médio, permitindo validar
 * movimentações e monitorar se o estoque está abaixo do mínimo definido.
 * </p>
 *
 * <p>
 * Regras de negócio aplicadas:
 * </p>
 * <ul>
 *     <li>SKU e nome são obrigatórios e normalizados para evitar espaços extras.</li>
 *     <li>Movimentações de entrada/saída respeitam unidade de medida, impedindo
 *     estoque negativo.</li>
 *     <li>Preço de venda e custo médio devem ser fornecidos como {@link Dinheiro}
 *     para preservar arredondamentos consistentes.</li>
 * </ul>
 *
 * <p>
 * Exemplo:
 * </p>
 * <pre>{@code
 * Produto cera = new Produto(UUID.randomUUID(), "Cera modeladora", "CER-001",
 *         Quantidade.unidade(10), Quantidade.unidade(2),
 *         Dinheiro.of("35.00"), Dinheiro.of("18.50"));
 * }</pre>
 */
public class Produto {

    private final UUID id;
    private String nome;
    private final String sku;
    private Quantidade estoqueAtual;
    private final Quantidade estoqueMinimo;
    private Dinheiro precoVenda;
    private Dinheiro custoMedio;

    public Produto(UUID id, String nome, String sku, Quantidade estoqueAtual, Quantidade estoqueMinimo,
                   Dinheiro precoVenda, Dinheiro custoMedio) {
        this.id = Objects.requireNonNull(id, "id não pode ser nulo");
        this.nome = validarNome(nome);
        this.sku = validarSku(sku);
        this.estoqueAtual = Objects.requireNonNull(estoqueAtual, "estoqueAtual não pode ser nulo");
        this.estoqueMinimo = Objects.requireNonNull(estoqueMinimo, "estoqueMinimo não pode ser nulo");
        validarMesmaUnidade(estoqueAtual, estoqueMinimo);
        this.precoVenda = Objects.requireNonNull(precoVenda, "precoVenda não pode ser nulo");
        this.custoMedio = Objects.requireNonNull(custoMedio, "custoMedio não pode ser nulo");
    }

    private String validarNome(String nome) {
        String nomeNormalizado = Objects.requireNonNull(nome, "nome não pode ser nulo").trim();
        if (nomeNormalizado.isEmpty()) {
            throw new IllegalArgumentException("nome não pode ser vazio");
        }
        return nomeNormalizado;
    }

    private String validarSku(String sku) {
        String skuNormalizado = Objects.requireNonNull(sku, "sku não pode ser nulo").trim();
        if (skuNormalizado.isEmpty()) {
            throw new IllegalArgumentException("sku não pode ser vazio");
        }
        return skuNormalizado;
    }

    private void validarMesmaUnidade(Quantidade a, Quantidade b) {
        if (!a.getUnidade().equals(b.getUnidade())) {
            throw new IllegalArgumentException("quantidades devem possuir a mesma unidade");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSku() {
        return sku;
    }

    public Quantidade getEstoqueAtual() {
        return estoqueAtual;
    }

    public Quantidade getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public Dinheiro getPrecoVenda() {
        return precoVenda;
    }

    public Dinheiro getCustoMedio() {
        return custoMedio;
    }

    public void movimentarEntrada(Quantidade quantidade) {
        Objects.requireNonNull(quantidade, "quantidade não pode ser nula");
        validarMesmaUnidade(estoqueAtual, quantidade);
        BigDecimal novoValor = estoqueAtual.getValor().add(quantidade.getValor());
        estoqueAtual = Quantidade.of(novoValor, estoqueAtual.getUnidade());
    }

    public void movimentarSaida(Quantidade quantidade) {
        Objects.requireNonNull(quantidade, "quantidade não pode ser nula");
        validarMesmaUnidade(estoqueAtual, quantidade);
        BigDecimal novoValor = estoqueAtual.getValor().subtract(quantidade.getValor());
        if (novoValor.signum() < 0) {
            throw new IllegalArgumentException("estoque não pode ficar negativo");
        }
        estoqueAtual = Quantidade.of(novoValor, estoqueAtual.getUnidade());
    }

    public boolean abaixoDoMinimo() {
        return estoqueAtual.getValor().compareTo(estoqueMinimo.getValor()) < 0;
    }

    public void atualizarPrecoVenda(Dinheiro precoVenda) {
        this.precoVenda = Objects.requireNonNull(precoVenda, "precoVenda não pode ser nulo");
    }

    public void atualizarCustoMedio(Dinheiro custoMedio) {
        this.custoMedio = Objects.requireNonNull(custoMedio, "custoMedio não pode ser nulo");
    }

    public void atualizarNome(String nome) {
        this.nome = validarNome(nome);
    }

    @Override
    public String toString() {
        return "Produto{"
                + "id=" + id
                + ", nome='" + nome + '\''
                + ", sku='" + sku + '\''
                + ", estoqueAtual=" + estoqueAtual
                + ", estoqueMinimo=" + estoqueMinimo
                + ", precoVenda=" + precoVenda
                + ", custoMedio=" + custoMedio
                + '}';
    }
}
