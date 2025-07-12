package com.spaced_repetition_ai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private static final String STORAGE_PATH = "file:A:/DeJavan/spaced-repetition-ai/Storage/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry){
        registry.addResourceHandler("/storage/**")
                .addResourceLocations(STORAGE_PATH)
                .setCachePeriod(3600);
    }

}
