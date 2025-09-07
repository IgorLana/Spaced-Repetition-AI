package com.spaced_repetition_ai.controller;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.model.ImageStyle;
import com.spaced_repetition_ai.service.ImageGenerationService;
import com.spaced_repetition_ai.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/image")
public class ImageGenerationController {
    private final ImageGenerationService imageGenerationService;
    private final UserService userService;

    @Autowired
    public ImageGenerationController(ImageGenerationService imageGenerationService, UserService userService){
        this.imageGenerationService = imageGenerationService;
        this.userService = userService;
    }


    @PostMapping
    public ResponseEntity<Map<String, String>> generate(@RequestParam("prompt") String prompt,
                                                        @RequestParam(value = "images", required = false) List<MultipartFile> images,
                                                        @RequestParam(value = "imageStyle", required = false) ImageStyle imageStyle
    ) {
        UserEntity currentUser = userService.getAuthenticatedUser();
        Long userId = currentUser.getId();

        ImageGenerationService.GeneratedImageData imageData = this.imageGenerationService.generateImage(prompt, images, imageStyle, userId);

        String base64Image = Base64.getEncoder().encodeToString(imageData.imageBytes());

        Map<String, String> response = Map.of(
                "imageBase64", base64Image,
                "mimeType", imageData.mimeType()
        );
        return ResponseEntity.ok(response);
    }
}
