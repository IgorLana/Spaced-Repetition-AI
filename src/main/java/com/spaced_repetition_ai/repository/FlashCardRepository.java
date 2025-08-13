package com.spaced_repetition_ai.repository;

import com.spaced_repetition_ai.entity.FlashCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FlashCardRepository
extends JpaRepository<FlashCardEntity, Long> {

       @Query("SELECT f FROM FlashCardEntity f WHERE f.deck.id = :deckId AND f.deck.user.id = :userId AND f.nextReview < :now ORDER BY f.nextReview")
       List<FlashCardEntity> findReviewableCardsByDeckAndUser(
               @Param("deckId") Long deckId,
               @Param("userId") Long userId,
               @Param("now") LocalDateTime now
       );

       List<FlashCardEntity> findByDeckId(Long deckId);
       @Query("SELECT f FROM FlashCardEntity f WHERE f.id = :deckId AND f.deck.user.id = :userId")
       List<FlashCardEntity> findByDeckIdAndDeckUserId(@Param("deckId") Long flashCardId, @Param("userId") Long userId);

       @Query("SELECT f FROM FlashCardEntity f WHERE f.id = :flashCardId AND f.deck.user.id = :userId")
       Optional<FlashCardEntity> findByIdAndDeckUserId(@Param("flashCardId") Long flashCardId, @Param("userId") Long userId);


}
