package br.ufvjm.barbearia.system;

import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.enums.StatusAtendimento;
import br.ufvjm.barbearia.model.Agendamento;
import br.ufvjm.barbearia.model.Cliente;
import br.ufvjm.barbearia.model.Estacao;
import br.ufvjm.barbearia.model.ItemDeServico;
import br.ufvjm.barbearia.model.Produto;
import br.ufvjm.barbearia.model.Servico;
import br.ufvjm.barbearia.model.Usuario;
import br.ufvjm.barbearia.value.CpfHash;
import br.ufvjm.barbearia.value.Dinheiro;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Quantidade;
import br.ufvjm.barbearia.value.Telefone;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Currency;
import java.util.UUID;

/**
 * Classe utilitária com um cenário completo de uso do {@link Sistema}.
 */
public final class Main {

    private Main() {
        // evitar instanciação
    }

    public static void main(String[] args) {
        Sistema sistema = new Sistema();

        // Cadastros básicos
        Endereco enderecoBase = Endereco.builder()
                .logradouro("Rua das Flores")
                .numero("123")
                .bairro("Centro")
                .cidade("Diamantina")
                .estado("MG")
                .cep("39100000")
                .build();

        Telefone telefoneFixo = Telefone.of("38 3531-0000");

        Usuario adm = new Usuario(
                UUID.randomUUID(),
                "Carlos Admin",
                enderecoBase,
                telefoneFixo,
                Email.of("carlos.admin@barbearia.com"),
                Papel.ADMIN,
                "carlos",
                "123",
                true
        );

        Usuario barbeiro = new Usuario(
                UUID.randomUUID(),
                "João Barbeiro",
                enderecoBase,
                Telefone.of("38 99999-1234"),
                Email.of("joao.barbeiro@barbearia.com"),
                Papel.BARBEIRO,
                "joao",
                "321",
                true
        );

        sistema.cadastrarUsuario(adm, adm);
        sistema.cadastrarUsuario(adm, barbeiro);

        Cliente cli1 = new Cliente(
                UUID.randomUUID(),
                "Gustavo Poncell",
                enderecoBase,
                Telefone.of("38 98888-1122"),
                Email.of("gustavo.poncell@email.com"),
                CpfHash.fromMasked("123.456.789-09"),
                true
        );

        Cliente cli2 = new Cliente(
                UUID.randomUUID(),
                "Rafael Silva",
                enderecoBase,
                Telefone.of("38 97777-3344"),
                Email.of("rafael.silva@email.com"),
                CpfHash.fromMasked("987.654.321-00"),
                true
        );

        sistema.cadastrarCliente(cli1);
        sistema.cadastrarCliente(cli2);

        Currency brl = Currency.getInstance("BRL");

        Servico corte = new Servico(
                UUID.randomUUID(),
                "Corte de cabelo",
                Dinheiro.of(new BigDecimal("40"), brl),
                30,
                false
        );

        Produto pomada = new Produto(
                UUID.randomUUID(),
                "Pomada Modeladora",
                "POM01",
                Quantidade.of(new BigDecimal("20"), "un"),
                Quantidade.of(new BigDecimal("5"), "un"),
                Dinheiro.of(new BigDecimal("25"), brl),
                Dinheiro.of(new BigDecimal("15"), brl)
        );

        sistema.cadastrarServico(corte);
        sistema.cadastrarProduto(pomada);

        LocalDateTime inicio = LocalDateTime.now();
        LocalDateTime fim = inicio.plusMinutes(30);

        Agendamento agendamento = new Agendamento(
                UUID.randomUUID(),
                cli1,
                Estacao.ESTACOES[0],
                inicio,
                fim,
                Dinheiro.of(new BigDecimal("10"), brl)
        );

        agendamento.associarBarbeiro(barbeiro);
        agendamento.adicionarItemServico(new ItemDeServico(corte, corte.getPreco(), corte.getDuracaoMin()));
        agendamento.alterarStatus(StatusAtendimento.EM_ATENDIMENTO);
        agendamento.alterarStatus(StatusAtendimento.CONCLUIDO);

        sistema.realizarAgendamento(agendamento);
        sistema.gerarExtratoServico(agendamento);

        sistema.saveAll(Path.of("data/barbearia.json"));

        System.out.println(sistema);
        System.out.println("Total de OS criadas: " + Sistema.getTotalOrdensServicoCriadas());
    }
}
