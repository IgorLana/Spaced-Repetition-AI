package com.spaced_repetition_ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FlashcardTextResponse {
    @JsonProperty("front")
    private String front;

    @JsonProperty("back")
    private String back;

    // Construtor padrão (necessário para Jackson)
    public FlashcardTextResponse() {
    }

    // Getters
    public String getFront() {
        return front;
    }

    public String getBack() {
        return back;
    }

    // Setters (opcionais, mas úteis se precisar modificar)
    public void setFront(String front) {
        this.front = front;
    }

    public void setBack(String back) {
        this.back = back;
    }
}
