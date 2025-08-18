package com.spaced_repetition_ai.repository;

import com.spaced_repetition_ai.dto.DeckRequestDTO;
import com.spaced_repetition_ai.dto.RegisterRequest;
import com.spaced_repetition_ai.entity.DeckEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class DeckRepositoryTest {

    private EntityManager entityManager;

    @Test
    void findByUserId() {

    }

    @Test
    void findByUserIdAndId() {
    }


}