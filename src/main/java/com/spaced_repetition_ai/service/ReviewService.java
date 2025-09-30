package com.spaced_repetition_ai.service;

import com.spaced_repetition_ai.entity.DeckEntity;
import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.entity.ReviewEntity;
import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.ReviewRating;
import com.spaced_repetition_ai.repository.DeckRepository;
import com.spaced_repetition_ai.repository.FlashCardRepository;
import com.spaced_repetition_ai.repository.ReviewRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
public class ReviewService {

    private final FlashCardRepository flashCardRepository;
    private final DeckRepository deckRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(FlashCardRepository flashCardRepository, DeckRepository deckRepository, ReviewRepository reviewRepository, UserRepository userRepository) {

        this.flashCardRepository = flashCardRepository;
        this.deckRepository = deckRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public void reviewFlashCard(Long id, ReviewRating rating){

        UserEntity usuarioLogado = getUsuarioLogado();

        Optional<FlashCardEntity> optionalCard = flashCardRepository.findByIdAndDeckUserId(id, getUsuarioLogado().getId());

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


                // Salvando o review no banco de review
                Long deckId = card.getDeck().getId();
                Long flashCardId = card.getId();
                LocalDateTime reviewDate = LocalDateTime.now();
                ReviewRating reviewRating = card.getRating();
                LocalDateTime nextReviewDate = card.getNextReview();
                int intervalReview = card.getInterval();
                double easeFactorReview = card.getEaseFactor();
                long timeSpent = reviewDate.until(nextReviewDate, java.time.temporal.ChronoUnit.MINUTES);

                ReviewEntity reviewEntity = new ReviewEntity(null, deckId, flashCardId, reviewRating, reviewDate, nextReviewDate, intervalReview, easeFactorReview, timeSpent);

                reviewRepository.save(reviewEntity);


                // Salvando a nota geral do deck

                DeckEntity deck = setDeckScore(rating, card);
                deckRepository.save(deck);



                System.out.println("FlashCard revisado com sucesso!");

            } else {
            throw new EntityNotFoundException("FlashCard com ID " + id + " não encontrado.");
        }


    }

    private static DeckEntity setDeckScore(ReviewRating rating, FlashCardEntity card) {
        DeckEntity deck = card.getDeck();


        int totalReviewCount = deck.getTotalReviewCount();
        int totalReviewRate = deck.getTotalReviewRate();

        switch (rating) {
            case DIFICIL:
                totalReviewCount++;
                totalReviewRate += 4;
                break;
            case BOM:
                totalReviewCount++;
                totalReviewRate += 8;
                break;
            case ERRADO:
                totalReviewCount++;
                break;
            case FACIL:
                totalReviewCount++;
                totalReviewRate += 10;
                break;
            default:
                throw new IllegalArgumentException("Resposta inválida");
        }

        deck.setTotalReviewCount(totalReviewCount);
        deck.setTotalReviewRate(totalReviewRate);
        return deck;
    }

    public List<FlashCardEntity> listFlashCardsToReview(Long deckId) {

        UserEntity usuarioLogado = getUsuarioLogado();

        return flashCardRepository.findReviewableCardsByDeckAndUser(deckId, getUsuarioLogado().getId(), LocalDateTime.now());

    }

    private UserEntity getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email) // Assumindo que findByUsername agora é findByEmail
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }


}
