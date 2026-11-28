package com.pacificaqua.customer;

import com.pacificaqua.common.ApiException;
import com.pacificaqua.common.PageResponse;
import com.pacificaqua.customer.CustomerDtos.CustomerRequest;
import com.pacificaqua.customer.CustomerDtos.CustomerResponse;
import com.pacificaqua.customer.CustomerDtos.CustomerSyncMeta;
import com.pacificaqua.domain.Customer;
import com.pacificaqua.domain.enums.EntityStatus;
import com.pacificaqua.repo.CustomerRepository;
import com.pacificaqua.repo.VisitRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {
  private final CustomerRepository customers;
  private final VisitRepository visits;

  public CustomerService(CustomerRepository customers, VisitRepository visits) {
    this.customers = customers;
    this.visits = visits;
  }

  @Transactional(readOnly = true)
  public CustomerSyncMeta syncMeta() {
    Instant customersRevision = customers.maxActiveUpdatedAt();
    Instant balancesRevision = visits.maxActiveUpdatedAt();
    return new CustomerSyncMeta(
        customersRevision != null ? customersRevision : Instant.EPOCH,
        balancesRevision != null ? balancesRevision : Instant.EPOCH,
        customers.countActive());
  }

  @Transactional(readOnly = true)
  public List<CustomerResponse> list(String q, String balanceFilter) {
    List<Customer> list = customers.searchActive(blank(q));
    Map<Long, BigDecimal> dueByCustomer = sumDueByCustomer(list);
    return list.stream()
        .map(c -> toDto(c, dueByCustomer.getOrDefault(c.getId(), BigDecimal.ZERO)))
        .filter(c -> {
          if (balanceFilter == null || balanceFilter.isBlank() || "all".equalsIgnoreCase(balanceFilter)) return true;
          if ("with".equalsIgnoreCase(balanceFilter)) return c.outstanding().compareTo(BigDecimal.ZERO) > 0;
          if ("none".equalsIgnoreCase(balanceFilter)) return c.outstanding().compareTo(BigDecimal.ZERO) <= 0;
          return true;
        })
        .toList();
  }

  @Transactional(readOnly = true)
  public PageResponse<CustomerResponse> listPage(
      String q, String balanceFilter, int page, int size, String sortBy, String sortDir) {
    String bal = blank(balanceFilter);
    String query = blank(q);
    int pageNo = Math.max(page, 0);
    int pageSize = Math.min(Math.max(size, 1), 100);
    boolean outstanding = sortBy != null && sortBy.equalsIgnoreCase("outstanding");
    boolean desc = sortDir != null && sortDir.equalsIgnoreCase("desc");

    Page<Customer> result;
    if (outstanding) {
      PageRequest pr = PageRequest.of(pageNo, pageSize);
      result = desc
          ? customers.searchActivePagedOutstandingDesc(query, bal, pr)
          : customers.searchActivePagedOutstandingAsc(query, bal, pr);
    } else {
      result = customers.searchActivePaged(
          query,
          bal,
          PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "name")));
    }

    Map<Long, BigDecimal> dueByCustomer = sumDueByCustomer(result.getContent());
    Page<CustomerResponse> mapped = result.map(
        c -> toDto(c, dueByCustomer.getOrDefault(c.getId(), BigDecimal.ZERO)));
    return PageResponse.from(mapped);
  }

  @Transactional(readOnly = true)
  public CustomerResponse get(Long id) {
    Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
    return toDto(c, visits.sumBalanceByCustomer(c.getId()));
  }

  @Transactional
  public CustomerResponse create(CustomerRequest req) {
    Customer c = new Customer();
    apply(c, req);
    c.setStatus(EntityStatus.ACTIVE);
    Customer saved = customers.save(c);
    return toDto(saved, BigDecimal.ZERO);
  }

  @Transactional
  public CustomerResponse update(Long id, CustomerRequest req) {
    Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
    apply(c, req);
    Customer saved = customers.save(c);
    return toDto(saved, visits.sumBalanceByCustomer(saved.getId()));
  }

  @Transactional
  public void remove(Long id) {
    Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
    c.setStatus(EntityStatus.INACTIVE);
    customers.save(c);
  }

  private void apply(Customer c, CustomerRequest req) {
    c.setName(req.name().trim());
    c.setPhone(blank(req.phone()));
    c.setCityArea(blank(req.cityArea()));
    c.setType(req.type());
    c.setCardNo(blank(req.cardNo()));
    c.setNotes(blank(req.notes()));
    c.setRouteHelperId(req.routeHelperId());
    c.setAvatarPath(blank(req.avatarPath()));
  }

  private CustomerResponse toDto(Customer c, BigDecimal outstanding) {
    return new CustomerResponse(
        c.getId(), c.getName(), c.getPhone(), c.getCityArea(), c.getType(), c.getCardNo(),
        c.getNotes(), c.getRouteHelperId(), c.getAvatarPath(), c.getStatus(), outstanding);
  }

  private Map<Long, BigDecimal> sumDueByCustomer(List<Customer> list) {
    Map<Long, BigDecimal> dues = new HashMap<>();
    if (list == null || list.isEmpty()) {
      return dues;
    }
    List<Long> ids = list.stream().map(Customer::getId).collect(Collectors.toList());
    for (Object[] row : visits.sumBalanceGroupedByCustomer(ids)) {
      Long customerId = (Long) row[0];
      BigDecimal balance = row[1] instanceof BigDecimal
          ? (BigDecimal) row[1]
          : BigDecimal.valueOf(((Number) row[1]).doubleValue());
      dues.put(customerId, balance);
    }
    return dues;
  }

  private static String blank(String s) {
    return s == null || s.isBlank() ? null : s.trim();
  }
}
