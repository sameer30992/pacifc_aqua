package com.pacificaqua.customer;

import com.pacificaqua.customer.CustomerDtos.CustomerRequest;
import com.pacificaqua.customer.CustomerDtos.CustomerResponse;
import com.pacificaqua.customer.CustomerDtos.CustomerSyncMeta;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','HELPER')")
public class CustomerController {
  private final CustomerService service;

  public CustomerController(CustomerService service) {
    this.service = service;
  }

  @GetMapping("/sync-meta")
  public CustomerSyncMeta syncMeta() {
    return service.syncMeta();
  }

  /**
   * Full list when {@code page} is omitted (autosuggest / cache).
   * Paginated {@link PageResponse} when {@code page} is provided (Customers page).
   */
  @GetMapping
  public ResponseEntity<?> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String balance,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sortBy,
      @RequestParam(required = false) String sortDir) {
    if (page != null) {
      int sz = size == null ? 20 : size;
      return ResponseEntity.ok(service.listPage(q, balance, page, sz, sortBy, sortDir));
    }
    return ResponseEntity.ok(service.list(q, balance));
  }

  @GetMapping("/{id}")
  public CustomerResponse get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public CustomerResponse create(@Valid @RequestBody CustomerRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
    return service.update(id, request);
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public void delete(@PathVariable Long id) {
    service.remove(id);
  }
}
