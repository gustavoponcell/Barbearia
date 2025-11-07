package br.ufvjm.barbearia.persist.adapters;

import br.ufvjm.barbearia.value.Dinheiro;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;

/**
 * Adapter do Gson para serializar e desserializar {@link Dinheiro} preservando precisão decimal.
 */
public final class DinheiroAdapter extends TypeAdapter<Dinheiro> {

    private static final String FIELD_VALOR = "valor";
    private static final String FIELD_MOEDA = "moeda";

    @Override
    public void write(JsonWriter out, Dinheiro value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }

        out.beginObject();
        out.name(FIELD_VALOR).value(value.getValor().toPlainString());
        out.name(FIELD_MOEDA).value(value.getMoeda().getCurrencyCode());
        out.endObject();
    }

    @Override
    public Dinheiro read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }

        BigDecimal valor = null;
        Currency moeda = null;

        in.beginObject();
        while (in.hasNext()) {
            String name = in.nextName();
            switch (name) {
                case FIELD_VALOR:
                    String rawValor = in.nextString();
                    try {
                        valor = new BigDecimal(rawValor);
                    } catch (NumberFormatException ex) {
                        throw new JsonParseException("Valor monetário inválido: " + rawValor, ex);
                    }
                    break;
                case FIELD_MOEDA:
                    String rawMoeda = in.nextString();
                    try {
                        moeda = Currency.getInstance(rawMoeda);
                    } catch (IllegalArgumentException ex) {
                        throw new JsonParseException("Código de moeda inválido: " + rawMoeda, ex);
                    }
                    break;
                default:
                    in.skipValue();
            }
        }
        in.endObject();

        if (valor == null || moeda == null) {
            throw new JsonParseException("Objeto Dinheiro inválido. Esperado campos 'valor' e 'moeda'.");
        }

        return Dinheiro.of(valor, moeda);
    }
}
