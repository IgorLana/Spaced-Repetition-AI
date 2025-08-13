package com.spaced_repetition_ai.repository;


import com.spaced_repetition_ai.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ReviewRepository  extends JpaRepository<ReviewEntity, Long> {
}
