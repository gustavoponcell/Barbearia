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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PermissaoNegadaException[");
        builder.append("mensagem=").append(getMessage());
        if (getCause() != null) {
            builder.append(", causa=")
                    .append(getCause().getClass().getSimpleName())
                    .append(':')
                    .append(getCause().getMessage());
        }
        builder.append(']');
        return builder.toString();
    }
}
