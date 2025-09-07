package com.spaced_repetition_ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.core.context.SecurityContextHolder;

@EnableRetry
@SpringBootApplication
public class SpacedRepetitionAiApplication {
	public static void main(String[] args) {
		SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
		SpringApplication.run(SpacedRepetitionAiApplication.class, args);
	}

}
