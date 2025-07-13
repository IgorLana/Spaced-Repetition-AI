package com.spaced_repetition_ai.repository;

import com.spaced_repetition_ai.entity.DeckEntity;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface DeckRepository extends MongoRepository<DeckEntity, String> {

}