package br.ufvjm.barbearia.persist.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Adapter do Gson para serializar e desserializar {@link YearMonth} no formato "yyyy-MM".
 */
public final class YearMonthAdapter extends TypeAdapter<YearMonth> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    @Override
    public void write(JsonWriter out, YearMonth value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        out.value(value.format(FORMATTER));
    }

    @Override
    public YearMonth read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String value = in.nextString();
        return YearMonth.parse(value, FORMATTER);
    }

    @Override
    public String toString() {
        return "YearMonthAdapter[formata YearMonth como yyyy-MM]";
    }
}
