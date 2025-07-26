package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.ReviewEntity;
import com.spaced_repetition_ai.model.ReviewRating;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.ReviewRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class ReviewService {

    private final FlashCardRepository flashCardRepository;
    private final DeckRepository deckRepository;
    private final ReviewRepository reviewRepository;

    public ReviewService(FlashCardRepository flashCardRepository, DeckRepository deckRepository, ReviewRepository reviewRepository) {

        this.flashCardRepository = flashCardRepository;
        this.deckRepository = deckRepository;
        this.reviewRepository = reviewRepository;
    }

    public void reviewFlashCard(String id, ReviewRating rating){

        Optional<FlashCardEntity> optionalCard = flashCardRepository.findById(id);

        if (optionalCard.isPresent()) {
            FlashCardEntity card = optionalCard.get();
            int interval = card.getInterval();
            LocalDateTime nextReview = card.getNextReview();
            double easeFactor = card.getEaseFactor();

            switch (rating) {
                case DIFICIL:
                    easeFactor = easeFactor - 0.15;
                    interval = (int) Math.round(interval * easeFactor);
                    nextReview = LocalDateTime.now().plusDays(interval);
                    break;
                case BOM:
                    interval = (int) Math.round(interval * easeFactor);
                    nextReview = LocalDateTime.now().plusDays(interval);
                    break;
                case ERRADO:
                    easeFactor = 2;
                    interval = 1;
                    nextReview = LocalDateTime.now().plusMinutes(1);
                    break;
                case FACIL:
                    easeFactor = easeFactor + 0.15;
                    interval = (int) Math.round(interval * easeFactor);
                    nextReview = LocalDateTime.now().plusDays(interval);
                    break;
                default:
                    throw new IllegalArgumentException("Resposta inválida");
            }
                card.setEaseFactor(easeFactor);
                card.setInterval(interval);
                card.setNextReview(nextReview);
                card.setRating(rating);
                flashCardRepository.save(card);

                String deckId = card.getDeckId();
                String flashCardId = card.getId();
                LocalDateTime reviewDate = LocalDateTime.now();
                ReviewRating reviewRating = card.getRating();
                LocalDateTime nextReviewDate = card.getNextReview();
                int intervalReview = card.getInterval();
                double easeFactorReview = card.getEaseFactor();
                long timeSpent = reviewDate.until(nextReviewDate, java.time.temporal.ChronoUnit.MINUTES);

                ReviewEntity reviewEntity = new ReviewEntity(null, deckId, flashCardId, reviewRating, reviewDate, nextReviewDate, intervalReview, easeFactorReview, timeSpent);

                reviewRepository.save(reviewEntity);

                System.out.println("FlashCard revisado com sucesso!");

            } else {
            throw new EntityNotFoundException("FlashCard com ID " + id + " não encontrado.");
        }


    }

    public List<FlashCardEntity> listFlashCardsToReview(String deckId) {
        return flashCardRepository.findByDeckIdAndNextReviewBeforeOrderByNextReview(deckId, LocalDateTime.now());
    }


}
