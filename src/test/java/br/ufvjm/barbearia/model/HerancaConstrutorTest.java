package br.ufvjm.barbearia.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.ufvjm.barbearia.enums.Papel;
import br.ufvjm.barbearia.value.CpfHash;
import br.ufvjm.barbearia.value.Email;
import br.ufvjm.barbearia.value.Endereco;
import br.ufvjm.barbearia.value.Telefone;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testes de fumaça para garantir que subclasses de {@link Pessoa} inicializam os
 * campos herdados por meio de chamadas ao construtor da superclasse.
 */
class HerancaConstrutorTest {

    private static Endereco criarEnderecoPadrao() {
        return Endereco.builder()
                .logradouro("Rua das Acácias")
                .numero("100")
                .bairro("Centro")
                .cidade("Diamantina")
                .estado("MG")
                .cep("39100000")
                .build();
    }

    @Test
    void clienteDeveInicializarCamposHerdados() {
        UUID id = UUID.randomUUID();
        Endereco endereco = criarEnderecoPadrao();
        Telefone telefone = Telefone.of("38 3531-0000");
        Email email = Email.of("cliente@teste.com");

        Cliente cliente = new Cliente(
                id,
                "Cliente Teste",
                endereco,
                telefone,
                email,
                CpfHash.fromMasked("123.456.789-09"),
                true
        );

        assertEquals(id, cliente.getId(), "ID herdado deve ser preservado");
        assertEquals("Cliente Teste", cliente.getNome(), "Nome herdado deve ser preservado");
        assertEquals(endereco, cliente.getEndereco(), "Endereço herdado deve ser preservado");
        assertEquals(telefone, cliente.getTelefone(), "Telefone herdado deve ser preservado");
        assertEquals(email, cliente.getEmail(), "E-mail herdado deve ser preservado");
        assertTrue(cliente.isAtivo(), "Flag ativo específica da subclasse deve ser mantida");
        assertNotNull(cliente.getCpf(), "CPF específico da subclasse deve ser inicializado");
    }

    @Test
    void usuarioDeveInicializarCamposHerdados() {
        UUID id = UUID.randomUUID();
        Endereco endereco = criarEnderecoPadrao();
        Telefone telefone = Telefone.of("38 99999-0000");
        Email email = Email.of("usuario@teste.com");

        Usuario usuario = new Usuario(
                id,
                "Usuário Teste",
                endereco,
                telefone,
                email,
                Papel.ADMIN,
                "usuario",
                "senhaHash",
                true
        );

        assertEquals(id, usuario.getId(), "ID herdado deve ser preservado");
        assertEquals("Usuário Teste", usuario.getNome(), "Nome herdado deve ser preservado");
        assertEquals(endereco, usuario.getEndereco(), "Endereço herdado deve ser preservado");
        assertEquals(telefone, usuario.getTelefone(), "Telefone herdado deve ser preservado");
        assertEquals(email, usuario.getEmail(), "E-mail herdado deve ser preservado");
        assertEquals(Papel.ADMIN, usuario.getPapel(), "Papel específico da subclasse deve ser mantido");
        assertEquals("usuario", usuario.getLogin(), "Login específico da subclasse deve ser mantido");
    }
}
