package com.pacificaqua.product;

import com.pacificaqua.common.ApiException;
import com.pacificaqua.domain.Product;
import com.pacificaqua.domain.enums.EntityStatus;
import com.pacificaqua.product.ProductDtos.ProductRequest;
import com.pacificaqua.product.ProductDtos.ProductResponse;
import com.pacificaqua.repo.ProductRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
  private final ProductRepository products;

  public ProductService(ProductRepository products) {
    this.products = products;
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> list(boolean activeOnly) {
    List<Product> list = activeOnly
        ? products.findByStatusOrderByNameAsc(EntityStatus.ACTIVE)
        : products.findAllByOrderByNameAsc();
    return list.stream().map(this::toDto).toList();
  }

  @Transactional
  public ProductResponse create(ProductRequest req) {
    Product p = new Product();
    String code = (req.code() == null || req.code().isBlank())
        ? UUID.randomUUID().toString().substring(0, 8)
        : req.code().trim().toLowerCase(Locale.ROOT);
    if (products.existsByCode(code)) throw ApiException.conflict("Product code exists");
    p.setCode(code);
    apply(p, req);
    if (p.getStatus() == null) p.setStatus(EntityStatus.ACTIVE);
    return toDto(products.save(p));
  }

  @Transactional
  public ProductResponse update(Long id, ProductRequest req) {
    Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
    apply(p, req);
    return toDto(products.save(p));
  }

  @Transactional
  public ProductResponse setStock(Long id, int stock) {
    if (stock < 0) throw ApiException.badRequest("Stock cannot be negative");
    Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
    p.setStock(stock);
    return toDto(products.save(p));
  }

  @Transactional
  public void remove(Long id) {
    Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
    p.setStatus(EntityStatus.DEACTIVATED);
    products.save(p);
  }

  private void apply(Product p, ProductRequest req) {
    p.setName(req.name().trim());
    p.setCategory(req.category().trim());
    p.setUnit(req.unit().trim());
    p.setDefaultRate(req.defaultRate());
    if (req.stock() != null) p.setStock(req.stock());
    if (req.status() != null) p.setStatus(req.status());
    p.setImagePath(req.imagePath());
  }

  private ProductResponse toDto(Product p) {
    return new ProductResponse(
        p.getId(), p.getCode(), p.getName(), p.getCategory(), p.getUnit(),
        p.getDefaultRate(), p.getStock(), p.getStatus(), p.getImagePath());
  }
}
