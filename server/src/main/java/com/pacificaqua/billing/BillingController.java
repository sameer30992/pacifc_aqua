package com.pacificaqua.billing;

import com.pacificaqua.billing.BillingDtos.BillPreview;
import com.pacificaqua.billing.BillingDtos.CustomerLedger;
import com.pacificaqua.billing.BillingDtos.GenerateBillRequest;
import com.pacificaqua.customer.CustomerDtos.CustomerResponse;
import com.pacificaqua.customer.CustomerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billing")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class BillingController {
  private final BillingService billingService;
  private final CustomerService customerService;

  public BillingController(BillingService billingService, CustomerService customerService) {
    this.billingService = billingService;
    this.customerService = customerService;
  }

  @GetMapping("/customers/search")
  public List<CustomerResponse> search(@RequestParam(required = false) String q) {
    return customerService.list(q, null);
  }

  @GetMapping("/customers/{id}/ledger")
  public CustomerLedger ledger(
      @PathVariable Long id,
      @RequestParam int year,
      @RequestParam int month) {
    return billingService.ledger(id, year, month);
  }

  @PostMapping("/generate")
  public BillPreview generate(@Valid @RequestBody GenerateBillRequest request) {
    return billingService.generate(request);
  }
}
