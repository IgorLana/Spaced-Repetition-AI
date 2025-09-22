package com.spaced_repetition_ai.model;

import com.spaced_repetition_ai.util.PromptLoader;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;

@AllArgsConstructor
@Getter
public enum TextPromptStyle {
    MultipleFlashcards("templates/text_MultipleFlashcards_prompt.md"),
    GeneralFlashcards("templates/text_generalFlashcard_prompt.md"),
    Language("templates/text_languageFlashcards_Prompt.md");

    private final String templatePath;
    private String cachedTemplate;

    TextPromptStyle(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getTemplate() throws IOException {
        if (cachedTemplate == null) {
            // Se não estiver em cache, carrega usando nossa classe utilitária
            this.cachedTemplate = PromptLoader.loadTemplate(templatePath);
        }
        return cachedTemplate;
    }
}
