package com.pacificaqua.repo;

import com.pacificaqua.domain.Visit;
import com.pacificaqua.domain.enums.PaymentStatus;
import com.pacificaqua.domain.enums.VisitStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long> {
  Optional<Visit> findByClientRequestId(String clientRequestId);

  @Query("""
      select v from Visit v
      where v.visitAt >= :from and v.visitAt < :to
        and (:status is null or v.paymentStatus = :status)
        and (:helperId is null or v.helperId = :helperId)
        and (:q is null or :q = '' or exists (
              select 1 from Customer c where c.id = v.customerId and (
                lower(c.name) like lower(concat('%', :q, '%'))
                or c.phone like concat('%', :q, '%')
                or lower(coalesce(c.cityArea, '')) like lower(concat('%', :q, '%'))
              ))
              or exists (select 1 from User u where u.id = v.helperId and lower(u.name) like lower(concat('%', :q, '%'))))
        and (:area is null or :area = '' or exists (
              select 1 from Customer c2 where c2.id = v.customerId
                and lower(coalesce(c2.cityArea, '')) like lower(concat('%', :area, '%'))))
      order by v.visitAt desc
      """)
  Page<Visit> search(
      @Param("from") Instant from,
      @Param("to") Instant to,
      @Param("q") String q,
      @Param("status") PaymentStatus status,
      @Param("area") String area,
      @Param("helperId") Long helperId,
      Pageable pageable);

  List<Visit> findByCustomerIdAndStatusAndVisitAtGreaterThanEqualAndVisitAtLessThanOrderByVisitAtAsc(
      Long customerId, VisitStatus status, Instant from, Instant to);

  @Query("""
      select v from Visit v
      where v.customerId = :customerId
        and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
        and v.balance > 0
      order by v.visitAt asc
      """)
  List<Visit> findDueByCustomerId(@Param("customerId") Long customerId);

  @Query("""
      select coalesce(sum(v.balance), 0) from Visit v
      where v.customerId = :customerId and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
      """)
  BigDecimal sumBalanceByCustomer(@Param("customerId") Long customerId);

  @Query("""
      select max(v.updatedAt)
      from Visit v
      where v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
      """)
  Instant maxActiveUpdatedAt();

  @Query("""
      select v.customerId, coalesce(sum(v.balance), 0)
      from Visit v
      where v.customerId in :customerIds
        and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
      group by v.customerId
      """)
  List<Object[]> sumBalanceGroupedByCustomer(@Param("customerIds") Collection<Long> customerIds);

  @Query("""
      select coalesce(sum(v.amountTotal), 0) from Visit v
      where v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
        and v.visitAt >= :from and v.visitAt < :to
      """)
  BigDecimal sumRevenue(@Param("from") Instant from, @Param("to") Instant to);

  @Query("""
      select coalesce(sum(v.amountPaid), 0) from Visit v
      where v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
        and v.visitAt >= :from and v.visitAt < :to
      """)
  BigDecimal sumCollected(@Param("from") Instant from, @Param("to") Instant to);

  @Query("""
      select coalesce(sum(v.balance), 0) from Visit v
      where v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
      """)
  BigDecimal sumAllPending();

  long countByStatusAndVisitAtGreaterThanEqualAndVisitAtLessThan(VisitStatus status, Instant from, Instant to);

  List<Visit> findTop8ByStatusOrderByVisitAtDesc(VisitStatus status);
}
