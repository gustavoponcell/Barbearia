package br.ufvjm.barbearia.value;

import java.util.Objects;

/**
 * Representa um endereço imutável com campos básicos do Brasil.
 */
public final class Endereco {

    private final String logradouro;
    private final String numero;
    private final String complemento;
    private final String bairro;
    private final String cidade;
    private final String estado;
    private final String cep;

    private Endereco(Builder builder) {
        this.logradouro = builder.logradouro;
        this.numero = builder.numero;
        this.complemento = builder.complemento;
        this.bairro = builder.bairro;
        this.cidade = builder.cidade;
        this.estado = builder.estado;
        this.cep = builder.cep;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getComplemento() {
        return complemento;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getEstado() {
        return estado;
    }

    public String getCep() {
        return cep;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Endereco)) {
            return false;
        }
        Endereco endereco = (Endereco) o;
        return Objects.equals(logradouro, endereco.logradouro)
                && Objects.equals(numero, endereco.numero)
                && Objects.equals(complemento, endereco.complemento)
                && Objects.equals(bairro, endereco.bairro)
                && Objects.equals(cidade, endereco.cidade)
                && Objects.equals(estado, endereco.estado)
                && Objects.equals(cep, endereco.cep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(logradouro, numero, complemento, bairro, cidade, estado, cep);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(logradouro).append(", ").append(numero);
        if (complemento != null && !complemento.isEmpty()) {
            builder.append(" - ").append(complemento);
        }
        builder.append(" - ").append(bairro)
                .append(", ").append(cidade)
                .append("/").append(estado)
                .append(" - CEP: ").append(cep);
        return builder.toString();
    }

    public static final class Builder {
        private String logradouro;
        private String numero;
        private String complemento;
        private String bairro;
        private String cidade;
        private String estado;
        private String cep;

        private Builder() {
        }

        public Builder logradouro(String logradouro) {
            this.logradouro = normalizarObrigatorio(logradouro, "Logradouro");
            return this;
        }

        public Builder numero(String numero) {
            this.numero = normalizarObrigatorio(numero, "Número");
            return this;
        }

        public Builder complemento(String complemento) {
            this.complemento = normalizarOpcional(complemento);
            return this;
        }

        public Builder bairro(String bairro) {
            this.bairro = normalizarObrigatorio(bairro, "Bairro");
            return this;
        }

        public Builder cidade(String cidade) {
            this.cidade = normalizarObrigatorio(cidade, "Cidade");
            return this;
        }

        public Builder estado(String estado) {
            String valor = normalizarObrigatorio(estado, "Estado");
            if (valor.length() != 2) {
                throw new IllegalArgumentException("Estado deve ser a sigla de 2 letras");
            }
            this.estado = valor.toUpperCase();
            return this;
        }

        public Builder cep(String cep) {
            String valor = normalizarObrigatorio(cep, "CEP");
            String digitos = valor.replaceAll("\\D", "");
            if (digitos.length() != 8) {
                throw new IllegalArgumentException("CEP deve conter 8 dígitos");
            }
            this.cep = digitos.substring(0, 5) + "-" + digitos.substring(5);
            return this;
        }

        public Endereco build() {
            Objects.requireNonNull(logradouro, "Logradouro é obrigatório");
            Objects.requireNonNull(numero, "Número é obrigatório");
            Objects.requireNonNull(bairro, "Bairro é obrigatório");
            Objects.requireNonNull(cidade, "Cidade é obrigatória");
            Objects.requireNonNull(estado, "Estado é obrigatório");
            Objects.requireNonNull(cep, "CEP é obrigatório");
            return new Endereco(this);
        }

        private static String normalizarObrigatorio(String valor, String campo) {
            Objects.requireNonNull(valor, campo + " não pode ser nulo");
            String normalizado = valor.trim();
            if (normalizado.isEmpty()) {
                throw new IllegalArgumentException(campo + " não pode ser vazio");
            }
            return normalizado;
        }

        private static String normalizarOpcional(String valor) {
            if (valor == null) {
                return null;
            }
            String normalizado = valor.trim();
            return normalizado.isEmpty() ? null : normalizado;
        }
    }
}
