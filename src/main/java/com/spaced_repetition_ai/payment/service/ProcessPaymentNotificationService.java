package com.spaced_repetition_ai.payment.service;

import com.spaced_repetition_ai.entity.UserEntity;
import com.spaced_repetition_ai.payment.client.MercadoPagoClient;
import com.spaced_repetition_ai.payment.entity.Payment;
import com.spaced_repetition_ai.payment.dto.ProcessPaymentNotificationRequestDTO;
import com.spaced_repetition_ai.payment.dto.ProcessPaymentNotificationResponseDTO;
import com.spaced_repetition_ai.payment.entity.PaymentTransaction;
import com.spaced_repetition_ai.payment.exceptions.PaymentGatewayException;
import com.spaced_repetition_ai.payment.repository.PaymentTransactionRepository;
import com.spaced_repetition_ai.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Slf4j
@Service
public class ProcessPaymentNotificationService {

    private final MercadoPagoClient mercadoPagoClient;
    private final UserRepository userRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;


    private static final Map<String, CreditPackage> CREDIT_PACKAGES = Map.of(
            "package_300", new CreditPackage("300 Créditos", 300, new BigDecimal("40.00")),
            "package_600", new CreditPackage("600 Créditos", 600, new BigDecimal("70.00")),
            "package_1000", new CreditPackage("1000 Créditos", 1000, new BigDecimal("100.00"))
    );

    @Transactional
    public ProcessPaymentNotificationResponseDTO processPaymentNotification(ProcessPaymentNotificationRequestDTO input) {
        log.info("Processando notificação de pagamento: {}", input.resourceId());

        if (input == null || input.resourceId() == null || !"payment".equalsIgnoreCase(input.resourceType())) {
            log.warn("Notificação inválida ou não é de pagamento. Ignorando.");
            return new ProcessPaymentNotificationResponseDTO(true, "INVALID_NOTIFICATION_IGNORED");
        }

        try {
            // 1. Buscar os detalhes do pagamento no Mercado Pago
            Payment paymentDetails = mercadoPagoClient.getPaymentDetails(input.resourceId());
            log.info("Detalhes do pagamento obtidos: ID {}, Status {}, ExternalReference {}",
                    paymentDetails.getId(), paymentDetails.getStatus(), paymentDetails.getOrderId());


            // 2. Processar apenas se o pagamento foi APROVADO
            if (paymentDetails.getStatus() == Payment.PaymentStatus.APPROVED) {
                // O orderId aqui é o nosso external_reference
                String externalReference = paymentDetails.getOrderId();

                // 3. Parse da nossa referência externa para obter userId e packageId
                long userId = parseUserIdFromReference(externalReference);
                String packageId = parsePackageIdFromReference(externalReference);
                CreditPackage creditPackage = CREDIT_PACKAGES.get(packageId);

                if (creditPackage == null) {
                    log.error("Pacote de créditos não encontrado na notificação: {}", packageId);
                    return new ProcessPaymentNotificationResponseDTO(false, "PACKAGE_NOT_FOUND");
                }

                // 4. Encontrar o usuário e adicionar os créditos
                Optional<UserEntity> userOptional = userRepository.findById(userId);
                if (userOptional.isPresent()) {
                    UserEntity user = userOptional.get();
                    int currentBalance = user.getBalance();
                    int creditsToAdd = creditPackage.credits();
                    user.setBalance(currentBalance + creditsToAdd);
                    userRepository.save(user); // Salva o usuário com o novo saldo
                    log.info("Sucesso! {} créditos adicionados ao usuário {}. Novo saldo: {}",
                            creditsToAdd, user.getUsername(), user.getBalance());

                    PaymentTransaction transaction = PaymentTransaction.builder()
                            .user(user)
                            .mercadoPagoPaymentId(paymentDetails.getId())
                            .packageId(packageId)
                            .creditsPurchased(creditPackage.credits())
                            .amount(paymentDetails.getAmount())
                            .status(paymentDetails.getStatus())
                            .statusDetail(paymentDetails.getStatusDetail())
                            .createdAt(LocalDateTime.now())
                            .build();
                            paymentTransactionRepository.save(transaction);

                } else {
                    log.error("Usuário com ID {} não encontrado para adicionar créditos.", userId);
                    // Importante: Mesmo com erro, retornamos sucesso para o MP não reenviar a notificação.
                    // O erro deve ser tratado internamente (ex: log, alerta para admin).
                }
            } else {
                log.info("Status do pagamento não é 'APROVADO'. Status: {}. Nenhuma ação necessária.", paymentDetails.getStatus());
            }
            return new ProcessPaymentNotificationResponseDTO(true, paymentDetails.getStatus().name());

        } catch (PaymentGatewayException e) {
            log.error("Erro de gateway ao processar notificação {}: {}", input.resourceId(), e.getMessage(), e);
            return new ProcessPaymentNotificationResponseDTO(false, "GATEWAY_ERROR");
        } catch (Exception e) {
            log.error("Erro inesperado ao processar notificação {}: {}", input.resourceId(), e.getMessage(), e);
            return new ProcessPaymentNotificationResponseDTO(false, "PROCESSING_ERROR");
        }
    }

    // Métodos auxiliares para parsear a external_reference
    private long parseUserIdFromReference(String reference) {
        // Exemplo de referência: "user:123;package:package_300"
        return Long.parseLong(reference.split(";")[0].split(":")[1]);
    }

    private String parsePackageIdFromReference(String reference) {
        return reference.split(";")[1].split(":")[1];
    }

    // Classe interna para representar um pacote de créditos
    private record CreditPackage(String title, int credits, BigDecimal unitPrice) {}
}