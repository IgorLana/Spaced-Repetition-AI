package com.spaced_repetition_ai.payment.client;

import com.spaced_repetition_ai.payment.dto.CreatePreferenceRequestDTO;
import com.spaced_repetition_ai.payment.dto.CreatePreferenceResponseDTO;
import com.spaced_repetition_ai.payment.entity.Payer;
import com.spaced_repetition_ai.payment.entity.Payment;
import com.spaced_repetition_ai.payment.exceptions.PaymentGatewayException;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

    @Component
    @RequiredArgsConstructor
    @Slf4j
    public class MercadoPagoClient {


        @Value("${api.v1.mercadopago-access-token}")
        private String accessToken;

        @PostConstruct
        public void init() {
            MercadoPagoConfig.setAccessToken(accessToken);
            log.info("Mercado Pago SDK inicializado com sucesso.");
        }

        public CreatePreferenceResponseDTO createPreference(
                List<CreatePreferenceRequestDTO.ItemDTO> itemsInput,
                CreatePreferenceRequestDTO.PayerDTO payerInput,
                CreatePreferenceRequestDTO.BackUrlsDTO backUrlsInput,
                String notificationUrl,
                String externalReference) throws PaymentGatewayException {

            log.info("Criando preferência no Mercado Pago para a referência externa: {}", externalReference);
            try {
                PreferenceClient client = new PreferenceClient();

                List<PreferenceItemRequest> items = itemsInput.stream()
                        .map(item -> PreferenceItemRequest.builder()
                                .id(item.id())
                                .title(item.title())
                                .quantity(item.quantity())
                                .unitPrice(item.unitPrice())
                                .build())
                        .collect(Collectors.toList());

                PreferencePayerRequest payer = PreferencePayerRequest.builder()
                        .email(payerInput.email())
                        .name(payerInput.name())
                        .build();

                PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                        .success(backUrlsInput.success())
                        .failure(backUrlsInput.failure())
                        .pending(backUrlsInput.pending())
                        .build();

                PreferenceRequest request = PreferenceRequest.builder()
                        .items(items)
                        .payer(payer)
                        .backUrls(backUrls)
                        .notificationUrl(notificationUrl)
                        .autoReturn("approved") // <--- LINHA REMOVIDA
                        .externalReference(externalReference)
                        .build();

                Preference preference = client.create(request);

                log.info("Preferência do Mercado Pago criada. ID: {}, InitPoint: {}", preference.getId(), preference.getInitPoint());
                return new CreatePreferenceResponseDTO(preference.getId(), preference.getInitPoint());

            } catch (MPApiException e) {
                log.error("Erro de API do Mercado Pago ao criar preferência para {}: Status: {}, Resposta: {}",
                        externalReference, e.getStatusCode(), e.getApiResponse().getContent(), e);
                throw new PaymentGatewayException("Erro de API do Mercado Pago: " + e.getApiResponse().getContent(), e);
            } catch (MPException e) {
                log.error("Erro no SDK do Mercado Pago ao criar preferência para {}: {}", externalReference, e.getMessage(), e);
                throw new PaymentGatewayException("Erro no SDK do Mercado Pago: " + e.getMessage(), e);
            }
        }

        // O resto do arquivo continua igual...

        public Payment getPaymentDetails(String paymentId) throws PaymentGatewayException {
            log.info("Buscando detalhes do pagamento no Mercado Pago para o ID: {}", paymentId);
            try {
                PaymentClient client = new PaymentClient();
                com.mercadopago.resources.payment.Payment mpPayment = client.get(Long.parseLong(paymentId));

                if (mpPayment == null) {
                    log.warn("Pagamento não encontrado no Mercado Pago para o ID: {}", paymentId);
                    throw new PaymentGatewayException("Pagamento não encontrado com o ID: " + paymentId);
                }

                log.debug("Detalhes brutos do pagamento do MP: {}", mpPayment);

                Payment.PaymentStatus status = mapMercadoPagoStatus(Payment.PaymentStatus.valueOf(mpPayment.getStatus().toUpperCase()));
                Payment.PaymentMethod method = mapMercadoPagoMethod(mpPayment.getPaymentMethodId());

                Payer payer = null;
                if (mpPayment.getPayer() != null) {
                    Payer.Identification identification = null;
                    if (mpPayment.getPayer().getIdentification() != null) {
                        identification = Payer.Identification.builder()
                                .type(mpPayment.getPayer().getIdentification().getType())
                                .number(mpPayment.getPayer().getIdentification().getNumber())
                                .build();
                    }
                    payer = Payer.builder()
                            .email(mpPayment.getPayer().getEmail())
                            .firstName(mpPayment.getPayer().getFirstName())
                            .lastName(mpPayment.getPayer().getLastName())
                            .identification(identification)
                            .build();
                }

                return Payment.builder()
                        .id(mpPayment.getId().toString())
                        .orderId(mpPayment.getExternalReference())
                        .amount(mpPayment.getTransactionAmount())
                        .paymentMethod(method)
                        .status(status)
                        .statusDetail(mpPayment.getStatusDetail())
                        .payer(payer)
                        .build();

            } catch (NumberFormatException e) {
                log.error("Formato de ID de pagamento inválido: {}", paymentId, e);
                throw new PaymentGatewayException("Formato de ID de pagamento inválido.", e);
            } catch (MPApiException e) {
                log.error("Erro de API do Mercado Pago ao buscar detalhes do pagamento para o ID {}: Status: {}, Resposta: {}",
                        paymentId, e.getStatusCode(), e.getApiResponse().getContent(), e);
                if (e.getStatusCode() == 404) {
                    throw new PaymentGatewayException("Pagamento não encontrado com o ID: " + paymentId, e);
                }
                throw new PaymentGatewayException("Erro de API do Mercado Pago: " + e.getApiResponse().getContent(), e);
            } catch (MPException e) {
                log.error("Erro no SDK do Mercado Pago ao buscar detalhes do pagamento para o ID {}: {}", paymentId, e.getMessage(), e);
                throw new PaymentGatewayException("Erro no SDK do Mercado Pago: " + e.getMessage(), e);
            } catch (Exception e) {
                log.error("Erro inesperado ao buscar detalhes do pagamento no Mercado Pago para o ID {}: {}", paymentId, e.getMessage(), e);
                throw new PaymentGatewayException("Erro inesperado ao buscar detalhes do pagamento.", e);
            }
        }

        private Payment.PaymentStatus mapMercadoPagoStatus(Payment.PaymentStatus status) {
            if (status == null) return Payment.PaymentStatus.UNKNOWN;
            return switch (status) {
                case APPROVED -> Payment.PaymentStatus.APPROVED;
                case PENDING -> Payment.PaymentStatus.PENDING;
                case AUTHORIZED -> Payment.PaymentStatus.AUTHORIZED;
                case IN_PROCESS -> Payment.PaymentStatus.IN_PROCESS;
                case IN_MEDIATION -> Payment.PaymentStatus.IN_MEDIATION;
                case REJECTED -> Payment.PaymentStatus.REJECTED;
                case CANCELLED -> Payment.PaymentStatus.CANCELLED;
                case REFUNDED -> Payment.PaymentStatus.REFUNDED;
                case CHARGED_BACK -> Payment.PaymentStatus.CHARGED_BACK;
                default -> Payment.PaymentStatus.UNKNOWN;
            };
        }

        private Payment.PaymentMethod mapMercadoPagoMethod(String methodId) {
            if (methodId == null) return null;
            if (methodId.startsWith("pix")) {
                return Payment.PaymentMethod.PIX;
            } else if (methodId.matches("visa|master|amex|elo|hipercard|diners|discover|aura|jcb")) {
                return Payment.PaymentMethod.CREDIT_CARD;
            } else if (methodId.equals("bolbradesco") || methodId.equals("pec")) {
                return null;
            }
            return null;
        }
    }