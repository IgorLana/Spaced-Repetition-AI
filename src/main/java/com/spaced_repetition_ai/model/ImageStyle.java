package com.spaced_repetition_ai.model;

import com.spaced_repetition_ai.util.PromptLoader;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.IOException;

@AllArgsConstructor
@Getter
public enum ImageStyle {

    CARTOON_STYLE("templates/image_cartoon_prompt.md"),
    PHOTOREALISM_STYLE("templates/image_photorealism_prompt.md"),
    ANIME_STYLE("templates/image_anime_prompt.md"),
    FILME_STYLE("templates/image_filme_prompt.md"),
    GENERAL_PHOTOREALISM_STYLE("templates/image_general_photorealism_prompt.md");


    private final String templatePath;
    private String cachedTemplate;

    ImageStyle(String templatePath) {
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
