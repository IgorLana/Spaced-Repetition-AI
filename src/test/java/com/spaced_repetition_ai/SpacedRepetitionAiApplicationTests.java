package com.spaced_repetition_ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@DataJpaTest
@ActiveProfiles("test")
class SpacedRepetitionAiApplicationTests {

	@Test
	void contextLoads() {
	}



}
