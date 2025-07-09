package com.spaced_repetition_ai.controller;


import com.spaced_repetition_ai.service.AudioGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/audio")
public class AudioGenerationController {

    private final AudioGenerationService audioGenaiService;
    @Autowired
    public AudioGenerationController(AudioGenerationService audioGenaiService){
        this.audioGenaiService = audioGenaiService;
    }

    @PostMapping
    public List<String> generateAudio(@RequestParam("prompt") String prompt,
                                      @RequestParam(value = "audios", required = false) List<MultipartFile> audios){
        return this.audioGenaiService.generateAudio(prompt, audios);
    }

}
