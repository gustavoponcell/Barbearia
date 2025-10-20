package br.ufvjm.barbearia.persist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utilitário responsável por persistir e recuperar snapshots do sistema em arquivos JSON.
 * <p>
 * Atua como camada de infraestrutura do {@link br.ufvjm.barbearia.system.Sistema},
 * garantindo que os dados sejam serializados usando {@link Gson} com formatação
 * legível. A classe não conhece detalhes das listas internas; ela trabalha com
 * {@link DataSnapshot} que funciona como DTO agregador.
 * </p>
 *
 * <p>
 * Regras e cuidados adotados:
 * </p>
 * <ul>
 *     <li>Cria diretórios automaticamente antes de salvar.</li>
 *     <li>Ao carregar, retorna snapshot vazio caso o arquivo não exista ou não
 *     possua conteúdo válido.</li>
 *     <li>Utiliza {@link StandardCharsets#UTF_8} para evitar problemas de
 *     acentuação.</li>
 * </ul>
 *
 * <p>
 * Exemplo integrado com o {@code Sistema}:
 * </p>
 * <pre>{@code
 * Path arquivo = Path.of("data/sistema.json");
 * sistema.saveAll(arquivo);      // delega para JsonStorage.save
 * sistema.loadAll(arquivo);      // delega para JsonStorage.load
 * }
 * </pre>
 */
public final class JsonStorage {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private JsonStorage() {
        // utilitário
    }

    public static void save(DataSnapshot data, Path file) throws IOException {
        Objects.requireNonNull(data, "data não pode ser nulo");
        Objects.requireNonNull(file, "file não pode ser nulo");

        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        }
    }

    public static DataSnapshot load(Path file) throws IOException {
        Objects.requireNonNull(file, "file não pode ser nulo");

        if (!Files.exists(file)) {
            return new DataSnapshot();
        }

        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            DataSnapshot snapshot = GSON.fromJson(reader, DataSnapshot.class);
            return snapshot != null ? snapshot : new DataSnapshot();
        }
    }
}
