package com.spaced_repetition_ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Permite CORS para todos os endpoints
                .allowedOrigins("http://localhost:4200") // Permite requisições do seu front-end Angular
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // Permite os métodos HTTP comuns
                .allowedHeaders("*") // Permite todos os cabeçalhos
                .allowCredentials(true); // Permite o envio de credenciais (se você usar sessões ou cookies)
    }
}