package com.spaced_repetition_ai.payment.service;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.payment.client.MercadoPagoClient;
import com.spaced_repetition_ai.payment.dto.CreatePreferenceRequestDTO;
import com.spaced_repetition_ai.payment.dto.CreatePreferenceResponseDTO;
import com.spaced_repetition_ai.payment.dto.PurchaseCreditsRequestDTO;
import com.spaced_repetition_ai.payment.exceptions.GenericBadRequestException;
import com.spaced_repetition_ai.payment.exceptions.PaymentGatewayException;
import com.spaced_repetition_ai.repository.UserRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class CreatePaymentPreferenceService {


    private final MercadoPagoClient mercadoPagoClient;
    private final UserRepository userRepository;

    private static final Map<String, CreditPackage> CREDIT_PACKAGES = Map.of(
            "package_300", new CreditPackage("300 Créditos", 300, new BigDecimal("40.00")),
            "package_600", new CreditPackage("600 Créditos", 600, new BigDecimal("70.00")),
            "package_1000", new CreditPackage("1000 Créditos", 1000, new BigDecimal("100.00"))
    );

    public CreatePreferenceResponseDTO createPreference(PurchaseCreditsRequestDTO input) {
        UserEntity currentUser = getUsuarioLogado();
        log.info("Executando CreatePaymentPreference para o usuário: {}", currentUser.getUsername());

        CreditPackage selectedPackage = CREDIT_PACKAGES.get(input.packageId());
        if (selectedPackage == null) {
            throw new GenericBadRequestException("Pacote de créditos inválido: " + input.packageId());
        }

        String externalReference = String.format("user:%d;package:%s", currentUser.getId(), input.packageId());


        var item = new CreatePreferenceRequestDTO.ItemDTO(
                externalReference,
                selectedPackage.title(),
                1,
                selectedPackage.unitPrice()
        );

        var payer = new CreatePreferenceRequestDTO.PayerDTO(
                currentUser.getEmail(),
                currentUser.getUsername()
        );

        var backUrls = new CreatePreferenceRequestDTO.BackUrlsDTO(
                "https://632e7b6263b0.ngrok-free.app",
                "https://632e7b6263b0.ngrok-free.app",
                "https://632e7b6263b0.ngrok-free.app"
        );


        String notificationUrl = "https://ce218d8d923a.ngrok-free.app/api/v1/webhooks/mercadopago";

        try {
            CreatePreferenceResponseDTO output = mercadoPagoClient.createPreference(
                    List.of(item),
                    payer,
                    backUrls,
                    notificationUrl,
                    externalReference
            );
            log.info("Preferência de pagamento criada com sucesso. PreferenceId: {}", output.preferenceId());
            return output;
        } catch (PaymentGatewayException e) {
            log.error("Erro ao criar preferência de pagamento para {}: {}", externalReference, e.getMessage(), e);
            throw e;
        }
    }

    private UserEntity getUsuarioLogado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + email));
    }

    private record CreditPackage(String title, int credits, BigDecimal unitPrice) {}
}