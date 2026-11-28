package com.pacificaqua.repo;

import com.pacificaqua.domain.Customer;
import com.pacificaqua.domain.enums.EntityStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  @Query("""
      select max(c.updatedAt)
      from Customer c
      where c.status = com.pacificaqua.domain.enums.EntityStatus.ACTIVE
      """)
  Instant maxActiveUpdatedAt();

  @Query("""
      select count(c)
      from Customer c
      where c.status = com.pacificaqua.domain.enums.EntityStatus.ACTIVE
      """)
  long countActive();

  @Query("""
      select c from Customer c
      where c.status = com.pacificaqua.domain.enums.EntityStatus.ACTIVE
        and (:q is null or :q = '' or lower(c.name) like lower(concat('%', :q, '%'))
         or c.phone like concat('%', :q, '%')
         or lower(coalesce(c.cityArea, '')) like lower(concat('%', :q, '%'))
         or lower(coalesce(c.cardNo, '')) like lower(concat('%', :q, '%')))
      order by c.name
      """)
  List<Customer> searchActive(@Param("q") String q);

  @Query("""
      select c from Customer c
      where c.status = com.pacificaqua.domain.enums.EntityStatus.ACTIVE
        and (:q is null or :q = '' or lower(c.name) like lower(concat('%', :q, '%'))
         or c.phone like concat('%', :q, '%')
         or lower(coalesce(c.cityArea, '')) like lower(concat('%', :q, '%'))
         or lower(coalesce(c.cardNo, '')) like lower(concat('%', :q, '%')))
        and (
          :balance is null or :balance = '' or lower(:balance) = 'all'
          or (
            lower(:balance) = 'with'
            and coalesce((
              select sum(v.balance) from Visit v
              where v.customerId = c.id
                and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
            ), 0) > 0
          )
          or (
            lower(:balance) = 'none'
            and coalesce((
              select sum(v.balance) from Visit v
              where v.customerId = c.id
                and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
            ), 0) <= 0
          )
        )
      """)
  Page<Customer> searchActivePaged(
      @Param("q") String q,
      @Param("balance") String balance,
      Pageable pageable);

  @Query("""
      select c from Customer c
      where c.status = com.pacificaqua.domain.enums.EntityStatus.ACTIVE
        and (:q is null or :q = '' or lower(c.name) like lower(concat('%', :q, '%'))
         or c.phone like concat('%', :q, '%')
         or lower(coalesce(c.cityArea, '')) like lower(concat('%', :q, '%'))
         or lower(coalesce(c.cardNo, '')) like lower(concat('%', :q, '%')))
        and (
          :balance is null or :balance = '' or lower(:balance) = 'all'
          or (
            lower(:balance) = 'with'
            and coalesce((
              select sum(v.balance) from Visit v
              where v.customerId = c.id
                and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
            ), 0) > 0
          )
          or (
            lower(:balance) = 'none'
            and coalesce((
              select sum(v.balance) from Visit v
              where v.customerId = c.id
                and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
            ), 0) <= 0
          )
        )
      order by coalesce((
        select sum(v.balance) from Visit v
        where v.customerId = c.id
          and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
      ), 0) asc, c.name asc
      """)
  Page<Customer> searchActivePagedOutstandingAsc(
      @Param("q") String q,
      @Param("balance") String balance,
      Pageable pageable);

  @Query("""
      select c from Customer c
      where c.status = com.pacificaqua.domain.enums.EntityStatus.ACTIVE
        and (:q is null or :q = '' or lower(c.name) like lower(concat('%', :q, '%'))
         or c.phone like concat('%', :q, '%')
         or lower(coalesce(c.cityArea, '')) like lower(concat('%', :q, '%'))
         or lower(coalesce(c.cardNo, '')) like lower(concat('%', :q, '%')))
        and (
          :balance is null or :balance = '' or lower(:balance) = 'all'
          or (
            lower(:balance) = 'with'
            and coalesce((
              select sum(v.balance) from Visit v
              where v.customerId = c.id
                and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
            ), 0) > 0
          )
          or (
            lower(:balance) = 'none'
            and coalesce((
              select sum(v.balance) from Visit v
              where v.customerId = c.id
                and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
            ), 0) <= 0
          )
        )
      order by coalesce((
        select sum(v.balance) from Visit v
        where v.customerId = c.id
          and v.status = com.pacificaqua.domain.enums.VisitStatus.ACTIVE
      ), 0) desc, c.name asc
      """)
  Page<Customer> searchActivePagedOutstandingDesc(
      @Param("q") String q,
      @Param("balance") String balance,
      Pageable pageable);

  List<Customer> findByStatusOrderByNameAsc(EntityStatus status);
}
