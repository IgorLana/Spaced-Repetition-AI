package com.spaced_repetition_ai.payment.controller;

import com.spaced_repetition_ai.payment.dto.CreatePreferenceResponseDTO;
import com.spaced_repetition_ai.payment.dto.PurchaseCreditsRequestDTO;
import com.spaced_repetition_ai.payment.service.CreatePaymentPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class CreatePaymentPreferenceController {
    private final CreatePaymentPreferenceService service;


    @PostMapping("/create-preference") // Endpoint mais específico
    public ResponseEntity<CreatePreferenceResponseDTO> createPreference(
            @Valid @RequestBody PurchaseCreditsRequestDTO requestDTO) {
        log.info("Recebida requisição para criar preferência de pagamento para o pacote: {}", requestDTO.packageId());

        try {
            CreatePreferenceResponseDTO serviceOutput = service.createPreference(requestDTO);

            // O service já retorna o DTO de resposta, então podemos retorná-lo diretamente.
            return ResponseEntity.ok(serviceOutput);

        } catch (IllegalArgumentException e) {
            log.warn("Dados inválidos para criar preferência: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erro ao criar preferência de pagamento para o pacote {}: {}", requestDTO.packageId(), e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
