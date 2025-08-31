package com.spaced_repetition_ai.payment.entity;

import com.spaced_repetition_ai.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "mercadopago_payment_id", unique = true)
    private String mercadoPagoPaymentId; // ID do pagamento no Mercado Pago

    private String packageId; // Ex: "package_300"
    private int creditsPurchased;
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private Payment.PaymentStatus status;

    private String statusDetail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}
