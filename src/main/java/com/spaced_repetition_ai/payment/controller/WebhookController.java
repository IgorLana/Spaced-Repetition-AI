package com.spaced_repetition_ai.payment.controller;

import com.spaced_repetition_ai.payment.dto.MercadoPagoNotificationDTO;
import com.spaced_repetition_ai.payment.dto.ProcessPaymentNotificationRequestDTO;
import com.spaced_repetition_ai.payment.service.ProcessPaymentNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ProcessPaymentNotificationService service;

    @PostMapping("/mercadopago")
    public ResponseEntity<Void> handleMercadoPagoNotification(
            @RequestBody(required = false) MercadoPagoNotificationDTO inputData) {

        // TODO: For enhanced security, consider extracting the x-signature header and validating the hash using application secrets.

        if (inputData == null || inputData.getData() == null || inputData.getData().getId() == null || inputData.getType() == null) {
            log.warn("Received invalid Mercado Pago notification: {}", inputData);
            return ResponseEntity.ok().build(); // Always acknowledge
        }

        String resourceId = inputData.getData().getId(); // payment id
        String resourceType = inputData.getType(); // e.g., "payment"

        if (!"payment".equalsIgnoreCase(resourceType)) {
            log.info("Ignoring non-payment notification type: {}", resourceType);
            return ResponseEntity.ok().build(); // Acknowledge but skip processing
        }

        try {
            var request = new ProcessPaymentNotificationRequestDTO(resourceType, resourceId);
            var result = service.processPaymentNotification(request);

            if (result.success()) {
                log.info("Payment {} processed successfully. Status: {}", resourceId, result.updatedStatus());
            } else {
                log.warn("Payment {} processing returned failure. Status: {}", resourceId, result.updatedStatus());
            }

            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error processing payment notification {}: {}", inputData, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

