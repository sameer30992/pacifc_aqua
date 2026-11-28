package com.pacificaqua.product;

import com.pacificaqua.product.ProductDtos.ProductRequest;
import com.pacificaqua.product.ProductDtos.ProductResponse;
import com.pacificaqua.product.ProductDtos.StockPatch;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@PreAuthorize("hasAnyRole('OWNER','ADMIN','HELPER')")
public class ProductController {
  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @GetMapping
  public List<ProductResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
    return service.list(activeOnly);
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public ProductResponse create(@Valid @RequestBody ProductRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return service.update(id, request);
  }

  @PatchMapping("/{id}/stock")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public ProductResponse stock(@PathVariable Long id, @Valid @RequestBody StockPatch patch) {
    return service.setStock(id, patch.stock());
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
  public void delete(@PathVariable Long id) {
    service.remove(id);
  }
}
