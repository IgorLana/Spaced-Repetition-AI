package com.spaced_repetition_ai.repository;


import com.spaced_repetition_ai.entity.ReviewEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ReviewRepository  extends MongoRepository<ReviewEntity, String> {
}
