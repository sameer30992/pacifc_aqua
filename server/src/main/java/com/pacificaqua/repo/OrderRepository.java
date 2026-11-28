package com.pacificaqua.repo;

import com.pacificaqua.domain.Order;
import com.pacificaqua.domain.enums.OrderStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

  @Query("""
      select distinct o from Order o
      left join fetch o.lines
      where (:status is null or o.status = :status)
        and (:helperId is null or o.helperId = :helperId)
        and (:customerId is null or o.customerId = :customerId)
        and (:from is null or o.createdAt >= :from)
        and (:to is null or o.createdAt < :to)
      order by o.createdAt desc
      """)
  List<Order> search(
      @Param("status") OrderStatus status,
      @Param("helperId") Long helperId,
      @Param("customerId") Long customerId,
      @Param("from") Instant from,
      @Param("to") Instant to);

  @Query("""
      select distinct o from Order o
      left join fetch o.lines
      where o.helperId = :helperId
        and o.status in :statuses
      order by o.createdAt desc
      """)
  List<Order> findByHelperIdAndStatusInOrderByCreatedAtDesc(
      @Param("helperId") Long helperId,
      @Param("statuses") List<OrderStatus> statuses);
}
