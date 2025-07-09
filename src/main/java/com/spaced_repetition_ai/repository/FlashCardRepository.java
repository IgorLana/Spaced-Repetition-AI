package com.spaced_repetition_ai.repository;

import com.spaced_repetition_ai.entity.FlashCardEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FlashCardRepository
extends MongoRepository<FlashCardEntity, String> {
    FlashCardEntity findByFront(String front);
    FlashCardEntity findByBack(String back);
    FlashCardEntity findByImagePath(String imagePath);
    FlashCardEntity findByAudioPath(String audioPath);
    FlashCardEntity findByCreatedDate(String createdDate);
    FlashCardEntity findByLastReview(String lastReview);
    FlashCardEntity findByNextReview(String nextReview);
    FlashCardEntity findByInterval(double interval);
    void deleteByFront(String front);
}
