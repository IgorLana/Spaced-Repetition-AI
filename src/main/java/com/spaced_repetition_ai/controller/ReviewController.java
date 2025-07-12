package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.entity.FlashCardEntity;
import com.spaced_repetition_ai.model.ReviewRating;
import com.spaced_repetition_ai.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public void generate(@RequestParam("id") String id,
                                 @RequestParam("review") ReviewRating rating) {
        reviewService.reviewFlashCard(id, rating);
    }

    @GetMapping("/review")
    public ResponseEntity<List<FlashCardEntity>> CardToReview(){
        List<FlashCardEntity> toReview = reviewService.listFlashCardsToReview();

        if(toReview.isEmpty()){
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toReview);
    }


}
