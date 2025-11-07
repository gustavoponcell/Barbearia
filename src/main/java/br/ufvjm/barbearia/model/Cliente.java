package br.ufvjm.barbearia.model;

import br.ufvjm.barbearia.value.CpfHash;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Telefone;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidade de cliente da barbearia.
 * <p>
 * Estende {@link Pessoa} adicionando informações específicas, como o CPF com
 * armazenamento seguro ({@link CpfHash}) e o estado de atividade. Mantém ainda
 * os contadores estáticos {@link #totalVeiculosProtegido} e
 * {@link #totalServicosProtegido} para fins didáticos (exigência do projeto),
 * atualizados externamente na criação de veículos e serviços.
 * </p>
 *
 * <p>
 * Regras de negócio:
 * </p>
 * <ul>
 *     <li>O CPF é obrigatório e deve ser armazenado já em formato hash/mask.</li>
 *     <li>Clientes podem ser desativados sem perder histórico, permitindo bloqueio
 *     de agendamentos sem exclusão definitiva.</li>
 *     <li>Contato atualizado via {@link #atualizarContato(Endereco, Telefone, Email)}
 *     exige preenchimento de todos os campos.</li>
 * </ul>
 *
 * <p>
 * Exemplo de construção:
 * </p>
 * <pre>{@code
 * Cliente cliente = new Cliente(UUID.randomUUID(), "João", endereco,
 *         telefone, email, CpfHash.from("123.456.789-00"), true);
 * }</pre>
 */
public class Cliente extends Pessoa {

    protected static int totalVeiculosProtegido;
    /**
     * Estratégia protegida para contar serviços criados.
     * <p>
     * (+) Implementação simples; (–) expõe o contador a subclasses e classes do
     * mesmo pacote, aumentando o risco de acoplamento e modificações indevidas.
     * </p>
     */
    protected static int totalServicosProtegido;

    private final CpfHash cpf;
    private boolean ativo;

    public Cliente(UUID id, String nome, Endereco endereco, Telefone telefone, Email email,
                   CpfHash cpf, boolean ativo) {
        super(id, nome, endereco, telefone, email);
        this.cpf = Objects.requireNonNull(cpf, "cpf não pode ser nulo");
        this.ativo = ativo;
    }

    static void incrementarTotalServicosProtegido() {
        totalServicosProtegido++;
    }

    /**
     * Retorna o contador protegido de serviços.
     * <p>
     * (+) Encapsulado via getter para facilitar verificações; (–) continua
     * suscetível a alterações externas devido ao modificador {@code protected}.
     * </p>
     *
     * @return total de serviços registrados pela estratégia protegida.
     */
    public static int getTotalServicosProtegido() {
        return totalServicosProtegido;
    }

    public CpfHash getCpf() {
        return cpf;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void desativar() {
        this.ativo = false;
    }

    public void reativar() {
        this.ativo = true;
    }

    public void atualizarContato(Endereco endereco, Telefone telefone, Email email) {
        setEndereco(Objects.requireNonNull(endereco, "endereco não pode ser nulo"));
        setTelefone(Objects.requireNonNull(telefone, "telefone não pode ser nulo"));
        setEmail(Objects.requireNonNull(email, "email não pode ser nulo"));
    }

    @Override
    public String toString() {
        return "Cliente{"
                + "cpf=" + cpf
                + ", ativo=" + ativo
                + ", totalVeiculosProtegido=" + totalVeiculosProtegido
                + ", totalServicosProtegido=" + totalServicosProtegido
                + ", pessoa=" + super.toString()
                + '}';
    }
}
