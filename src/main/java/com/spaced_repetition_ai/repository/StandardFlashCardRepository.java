package com.spaced_repetition_ai.repository;


import com.spaced_repetition_ai.entity.StandardFlashCardEntity;
import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.model.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StandardFlashCardRepository extends JpaRepository<StandardFlashCardEntity, Long> {
    Optional<StandardFlashCardEntity> findByPromptAndImageStyleAndSourceLanguageAndTargetLanguage(String prompt, ImageStyle imageStyle, Language sourceLanguage, Language targetLanguage);
}
