package com.spaced_repetition_ai.model;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum Language {
    ARABE_EGIPCIO("Árabe (egípcio)", "ar-EG"),
    ALEMAO_ALEMANHA("Alemão (Alemanha)", "de-DE"),
    INGLES_EUA("Inglês (EUA)", "en-US"),
    ESPANHOL_EUA("Espanhol (EUA)", "es-US"),
    FRANCES_FRANCA("Francês (França)", "fr-FR"),
    HINDI_INDIA("Hindi (Índia)", "hi-IN"),
    INDONESIO_INDONESIA("Indonésio (Indonésia)", "id-ID"),
    ITALIANO_ITALIA("Italiano (Itália)", "it-IT"),
    JAPONES_JAPAO("Japonês (Japão)", "ja-JP"),
    COREANO_COREIA("Coreano (Coreia)", "ko-KR"),
    PORTUGUES_BRASIL("Português (Brasil)", "pt-BR"),
    RUSSO_RUSSIA("Russo (Rússia)", "ru-RU"),
    HOLANDES_PAIS_BAIXOS("Holandês (Países Baixos)", "nl-NL"),
    POLONES_POLONIA("Polonês (Polônia)", "pl-PL"),
    TAILANDES_TAILANDIA("Tailandês (Tailândia)", "th-TH"),
    TURCO_TURQUIA("Turco (Turquia)", "tr-TR"),
    VIETNAMITA_VIETNA("Vietnamita (Vietnã)", "vi-VN"),
    ROMENO_ROMENIA("Romeno (Romênia)", "ro-RO"),
    UCRANIANO_UCRANIA("Ucraniano (Ucrânia)", "uk-UA"),
    BENGALI_BANGLADESH("Bengali (Bangladesh)", "bn-BD"),
    INGLES_INDIA("Inglês (Índia)", "en-IN"),
    MARATI_INDIA("Marati (Índia)", "mr-IN"),
    TAMIL_INDIA("Tâmil (Índia)", "ta-IN"),
    TELUGO_INDIA("Telugo (Índia)", "te-IN");

    private final String nome;
    private final String localeCode;

}
