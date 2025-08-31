package com.spaced_repetition_ai.payment.repository;

import com.spaced_repetition_ai.payment.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {
    Optional<PaymentTransaction> findByMercadoPagoPaymentId(String mercadoPagoPaymentId);
}
