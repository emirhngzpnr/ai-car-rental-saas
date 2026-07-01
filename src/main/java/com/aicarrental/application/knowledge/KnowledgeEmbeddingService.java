package com.aicarrental.application.knowledge;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class KnowledgeEmbeddingService {
    public static final int DIMENSIONS = 384;
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^a-z0-9]+");

    public String embedAsVectorLiteral(String text) {
        double[] vector = new double[DIMENSIONS];
        String normalized = normalize(text);
        for (String token : TOKEN_SPLIT.split(normalized)) {
            if (token.length() < 2) {
                continue;
            }
            int index = Math.floorMod(token.hashCode(), DIMENSIONS);
            vector[index] += 1.0d;
        }

        double magnitude = 0.0d;
        for (double value : vector) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        if (magnitude == 0.0d) {
            vector[0] = 1.0d;
            magnitude = 1.0d;
        }

        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < vector.length; index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.US, "%.6f", vector[index] / magnitude));
        }
        return builder.append(']').toString();
    }

    private String normalize(String value) {
        String lowered = value == null ? "" : value.toLowerCase(Locale.forLanguageTag("tr"));
        String ascii = Normalizer.normalize(lowered, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ascii
                .replace('\u0131', 'i')
                .replace('\u015f', 's')
                .replace('\u011f', 'g')
                .replace('\u00fc', 'u')
                .replace('\u00f6', 'o')
                .replace('\u00e7', 'c');
    }
}
