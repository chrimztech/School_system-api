package com.srms.api.modules.payment.repository;

import com.srms.api.modules.payment.entity.PaymentCallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentCallbackLogRepository extends JpaRepository<PaymentCallbackLog, String> {
}
