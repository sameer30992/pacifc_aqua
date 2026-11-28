package com.pacificaqua.repo;

import com.pacificaqua.domain.Product;
import com.pacificaqua.domain.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
  Optional<Product> findByCode(String code);
  List<Product> findByStatusOrderByNameAsc(EntityStatus status);
  List<Product> findAllByOrderByNameAsc();
  boolean existsByCode(String code);
}
