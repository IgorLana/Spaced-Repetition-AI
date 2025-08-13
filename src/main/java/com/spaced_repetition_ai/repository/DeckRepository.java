package com.spaced_repetition_ai.repository;

import com.spaced_repetition_ai.entity.DeckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface DeckRepository extends JpaRepository<DeckEntity, Long> {

    List<DeckEntity> findByUserId(Long userId);
    Optional<DeckEntity> findByUserIdAndId(Long userId, Long id);

}