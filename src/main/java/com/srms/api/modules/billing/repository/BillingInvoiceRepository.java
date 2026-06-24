package com.srms.api.modules.billing.repository;

import com.srms.api.modules.billing.entity.BillingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillingInvoiceRepository extends JpaRepository<BillingInvoice, String> {
    List<BillingInvoice> findBySchoolIdOrderByCreatedAtDesc(String schoolId);
}
