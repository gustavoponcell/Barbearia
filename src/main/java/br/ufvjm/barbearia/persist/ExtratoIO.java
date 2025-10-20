package br.ufvjm.barbearia.persist;

import br.ufvjm.barbearia.model.Cliente;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Utilitário para gravação de extratos financeiros relacionados a um cliente.
 */
public final class ExtratoIO {

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private ExtratoIO() {
        // utilitário
    }

    public static void saveExtrato(Cliente cliente, String extrato, Path dir) throws IOException {
        Objects.requireNonNull(cliente, "cliente não pode ser nulo");
        Objects.requireNonNull(extrato, "extrato não pode ser nulo");
        Objects.requireNonNull(dir, "dir não pode ser nulo");

        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String fileName = String.format("extrato_%s_%s.txt", cliente.getId(), timestamp);
        Path file = dir.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(extrato);
        }
    }
}
