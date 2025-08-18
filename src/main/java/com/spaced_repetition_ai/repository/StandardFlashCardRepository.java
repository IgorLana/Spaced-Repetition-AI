package com.spaced_repetition_ai.repository;


import com.spaced_repetition_ai.entity.StandardFlashCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StandardFlashCardRepository extends JpaRepository<StandardFlashCardEntity, Long> {
    Optional<StandardFlashCardEntity> findByPrompt(String prompt);
}
