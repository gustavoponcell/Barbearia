package br.ufvjm.barbearia.exceptions;

/**
 * Exceção lançada quando um usuário autenticado tenta executar uma operação sem ter o papel necessário.
 */
public class PermissaoNegadaException extends RuntimeException {

    public PermissaoNegadaException(String message) {
        super(message);
    }

    public PermissaoNegadaException(String message, Throwable cause) {
        super(message, cause);
    }
}
