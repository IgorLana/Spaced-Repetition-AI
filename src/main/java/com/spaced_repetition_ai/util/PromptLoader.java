package com.spaced_repetition_ai.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class PromptLoader {

    public static String loadTemplate(String path) throws IOException {
        InputStream inputStream = PromptLoader.class.getClassLoader().getResourceAsStream(path);

        if (inputStream == null) {
            throw new IOException("Arquivo de template não encontrado no caminho: " + path);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
