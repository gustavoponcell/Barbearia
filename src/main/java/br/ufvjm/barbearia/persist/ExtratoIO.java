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
    private static final ExtratoIO DEBUG_VIEW = new ExtratoIO();

    private ExtratoIO() {
        // utilitário
    }

    /**
     * Persiste o extrato em disco e retorna o {@link Path} gerado.
     *
     * @param cliente cliente associado ao extrato ou {@code null} para consumidor final.
     * @param extrato conteúdo textual do extrato.
     * @param dir diretório base onde o arquivo será criado.
     * @return caminho do arquivo criado.
     * @throws IOException se ocorrer erro de escrita.
     */
    public static Path saveExtrato(Cliente cliente, String extrato, Path dir) throws IOException {
        Objects.requireNonNull(extrato, "extrato não pode ser nulo");
        Objects.requireNonNull(dir, "dir não pode ser nulo");

        Files.createDirectories(dir);

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String identificador = cliente != null ? cliente.getId().toString() : "consumidor_final";
        String fileName = String.format("extrato_%s_%s.txt", identificador, timestamp);
        Path file = dir.resolve(fileName);

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(extrato);
        }
        return file;
    }

    public static String description() {
        return DEBUG_VIEW.toString();
    }

    @Override
    public String toString() {
        return "ExtratoIO[utilitário para salvar extratos com timestamp yyyyMMddHHmmss]";
    }
}
