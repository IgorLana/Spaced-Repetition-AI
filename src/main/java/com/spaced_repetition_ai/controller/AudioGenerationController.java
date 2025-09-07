package com.spaced_repetition_ai.controller;


import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.service.AudioGenerationService;
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
@RequestMapping("/api/audio")
public class AudioGenerationController {

    private final AudioGenerationService audioGenaiService;
    private final UserService userService;
    @Autowired
    public AudioGenerationController(AudioGenerationService audioGenaiService, UserService userService){
        this.audioGenaiService = audioGenaiService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> generateAudio(@RequestParam("prompt") String prompt,
                                                                   @RequestParam(value = "audios", required = false) List<MultipartFile> audios){

        UserEntity currentUser = userService.getAuthenticatedUser();
        Long userId = currentUser.getId();
        AudioGenerationService.GeneratedAudioData audioData = this.audioGenaiService.generateAudio(prompt, audios, userId);

        if (audioData == null) {
            return ResponseEntity.internalServerError().build();
        }

        String base64Audio = Base64.getEncoder().encodeToString(audioData.audioBytes());

        Map<String, String> response = Map.of(
                "audioBase64", base64Audio,
                "mimeType", audioData.mimeType()
        );

        return ResponseEntity.ok(response);



    }

}
