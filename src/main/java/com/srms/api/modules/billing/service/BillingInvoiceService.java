package com.srms.api.modules.billing.service;

import com.srms.api.modules.billing.entity.BillingInvoice;
import com.srms.api.modules.billing.repository.BillingInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service @RequiredArgsConstructor @Transactional
public class BillingInvoiceService {
    private final BillingInvoiceRepository repo;

    public List<BillingInvoice> findAll(String schoolId) { return repo.findBySchoolIdOrderByCreatedAtDesc(schoolId); }

    public BillingInvoice create(String schoolId, BillingInvoice invoice) {
        invoice.setSchoolId(schoolId);
        if (invoice.getStatus() == null) invoice.setStatus("open");
        return repo.save(invoice);
    }

    public BillingInvoice markPaid(String schoolId, String id) {
        BillingInvoice inv = repo.findById(id)
            .filter(i -> i.getSchoolId().equals(schoolId))
            .orElseThrow();
        inv.setStatus("paid");
        inv.setPaidDate(java.time.LocalDate.now().toString());
        return repo.save(inv);
    }
}
