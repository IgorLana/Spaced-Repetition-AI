package com.spaced_repetition_ai.repository;

import com.spaced_repetition_ai.entity.FlashCardEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FlashCardRepository
extends MongoRepository<FlashCardEntity, String> {
       List<FlashCardEntity> findByNextReviewBeforeOrderByNextReview(LocalDateTime now);
       List<FlashCardEntity> findByDeckIdAndNextReviewBeforeOrderByNextReview(String deckId, LocalDateTime now);
       List<FlashCardEntity> findByDeckId(String deckId);

}
