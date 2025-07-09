package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.service.ImageGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/image")
public class ImageGenerationController {
    private final ImageGenerationService imageGenerationService;

    @Autowired
    public ImageGenerationController(ImageGenerationService imageGenerationService){
        this.imageGenerationService = imageGenerationService;
    }


    @PostMapping
    public List<String> generate(@RequestParam("prompt") String prompt,
                                 @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        return this.imageGenerationService.generateImage(prompt, images);
    }


}
