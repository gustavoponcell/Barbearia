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
