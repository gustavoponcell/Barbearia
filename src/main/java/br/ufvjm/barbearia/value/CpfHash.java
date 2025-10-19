package br.ufvjm.barbearia.value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Representa um CPF armazenado de forma segura (hash + máscara).
 */
public final class CpfHash {

    private final String hash;
    private final String mascara;

    private CpfHash(String hash, String mascara) {
        this.hash = hash;
        this.mascara = mascara;
    }

    public static CpfHash fromMasked(String raw) {
        Objects.requireNonNull(raw, "CPF não pode ser nulo");
        String digitos = raw.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new IllegalArgumentException("CPF deve possuir 11 dígitos");
        }
        if (digitos.chars().allMatch(ch -> ch == digitos.charAt(0))) {
            throw new IllegalArgumentException("CPF inválido");
        }
        String hash = gerarHash(digitos);
        String mascara = "***.***." + digitos.substring(6, 9) + "-" + digitos.substring(9);
        return new CpfHash(hash, mascara);
    }

    private static String gerarHash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo de hash indisponível", e);
        }
    }

    public String getHash() {
        return hash;
    }

    public String getMascara() {
        return mascara;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CpfHash)) {
            return false;
        }
        CpfHash cpfHash = (CpfHash) o;
        return hash.equals(cpfHash.hash) && mascara.equals(cpfHash.mascara);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, mascara);
    }

    @Override
    public String toString() {
        return mascara;
    }
}
