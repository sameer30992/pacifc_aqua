package com.pacificaqua.order;

import com.pacificaqua.auth.SecurityUtils;
import com.pacificaqua.common.ApiException;
import com.pacificaqua.domain.Customer;
import com.pacificaqua.domain.Order;
import com.pacificaqua.domain.OrderLine;
import com.pacificaqua.domain.Product;
import com.pacificaqua.domain.User;
import com.pacificaqua.domain.enums.OrderPriority;
import com.pacificaqua.domain.enums.OrderStatus;
import com.pacificaqua.order.OrderDtos.*;
import com.pacificaqua.repo.*;
import com.pacificaqua.visit.VisitDtos.CreateVisitRequest;
import com.pacificaqua.visit.VisitDtos.VisitResponse;
import com.pacificaqua.visit.VisitService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

  private final OrderRepository orders;
  private final CustomerRepository customers;
  private final UserRepository users;
  private final ProductRepository products;
  private final VisitRepository visits;
  private final VisitService visitService;
  private final SimpMessagingTemplate messaging;

  public OrderService(
      OrderRepository orders,
      CustomerRepository customers,
      UserRepository users,
      ProductRepository products,
      VisitRepository visits,
      VisitService visitService,
      SimpMessagingTemplate messaging) {
    this.orders = orders;
    this.customers = customers;
    this.users = users;
    this.products = products;
    this.visits = visits;
    this.visitService = visitService;
    this.messaging = messaging;
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> list(
      OrderStatus status,
      LocalDate date,
      LocalDate fromDate,
      LocalDate toDate,
      Long customerId,
      Long helperId) {
    Instant from = null;
    Instant to = null;
    if (fromDate != null || toDate != null) {
      LocalDate start = fromDate != null ? fromDate : toDate;
      LocalDate end = toDate != null ? toDate : fromDate;
      if (end.isBefore(start)) {
        LocalDate tmp = start;
        start = end;
        end = tmp;
      }
      from = start.atStartOfDay(ZONE).toInstant();
      to = end.plusDays(1).atStartOfDay(ZONE).toInstant();
    } else if (date != null) {
      from = date.atStartOfDay(ZONE).toInstant();
      to = date.plusDays(1).atStartOfDay(ZONE).toInstant();
    }
    return toDtoList(orders.search(status, helperId, customerId, from, to));
  }

  @Transactional(readOnly = true)
  public List<OrderResponse> mine(OrderStatus status) {
    Long helperId = SecurityUtils.requireUser().getId();
    List<OrderStatus> statuses = status == null
        ? List.of(OrderStatus.PENDING, OrderStatus.DELAYED)
        : List.of(status);
    return toDtoList(orders.findByHelperIdAndStatusInOrderByCreatedAtDesc(helperId, statuses));
  }

  @Transactional
  public OrderResponse create(CreateOrderRequest req) {
    customers.findById(req.customerId()).orElseThrow(() -> ApiException.notFound("Customer not found"));
    users.findById(req.helperId()).orElseThrow(() -> ApiException.notFound("Helper not found"));

    Order order = new Order();
    order.setCustomerId(req.customerId());
    order.setHelperId(req.helperId());
    order.setPriority(req.priority() == null ? OrderPriority.NORMAL : req.priority());
    order.setExpectedHours(req.expectedHours());
    order.setNotes(req.notes());
    order.setStatus(OrderStatus.PENDING);
    order.setCreatedBy(SecurityUtils.requireUser().getId());

    if (req.lines() != null) {
      for (OrderLineRequest lineReq : req.lines()) {
        products.findById(lineReq.productId())
            .orElseThrow(() -> ApiException.notFound("Product not found"));
        OrderLine line = new OrderLine();
        line.setProductId(lineReq.productId());
        line.setQty(lineReq.qty());
        order.addLine(line);
      }
    }

    Order saved = orders.save(order);
    OrderResponse dto = toDto(saved);
    notifyHelper(dto);
    return dto;
  }

  @Transactional
  public OrderResponse update(Long id, CreateOrderRequest req) {
    Order order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
    if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
      throw ApiException.badRequest("Cannot edit closed order");
    }
    order.setCustomerId(req.customerId());
    order.setHelperId(req.helperId());
    order.setPriority(req.priority() == null ? OrderPriority.NORMAL : req.priority());
    order.setExpectedHours(req.expectedHours());
    order.setNotes(req.notes());
    order.getLines().clear();
    if (req.lines() != null) {
      for (OrderLineRequest lineReq : req.lines()) {
        OrderLine line = new OrderLine();
        line.setProductId(lineReq.productId());
        line.setQty(lineReq.qty());
        order.addLine(line);
      }
    }
    return toDto(orders.save(order));
  }

  @Transactional
  public OrderResponse cancel(Long id) {
    Order order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
    order.setStatus(OrderStatus.CANCELLED);
    return toDto(orders.save(order));
  }

  @Transactional
  public VisitResponse deliver(Long id, DeliverOrderRequest req) {
    Order order = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
    if (order.getStatus() == OrderStatus.CANCELLED) {
      throw ApiException.badRequest("Order cancelled");
    }
    CreateVisitRequest visitReq = new CreateVisitRequest(
        order.getCustomerId(),
        order.getHelperId(),
        Instant.now(),
        req.lines(),
        req.amountPaid(),
        req.paymentMode(),
        req.remarks(),
        "order-" + id);
    VisitResponse visit = visitService.create(visitReq);
    order.setStatus(OrderStatus.DELIVERED);
    orders.save(order);
    return visit;
  }

  private void notifyHelper(OrderResponse dto) {
    OrderCreatedEvent event = new OrderCreatedEvent(
        "ORDER_CREATED",
        dto.id(),
        dto.customerId(),
        dto.customerName(),
        dto.priority(),
        dto.expectedHours(),
        dto.createdAt());
    messaging.convertAndSend("/topic/helpers/" + dto.helperId() + "/orders", event);
  }

  private List<OrderResponse> toDtoList(List<Order> list) {
    if (list == null || list.isEmpty()) {
      return List.of();
    }

    Set<Long> customerIds = new HashSet<>();
    Set<Long> helperIds = new HashSet<>();
    Set<Long> productIds = new HashSet<>();
    for (Order o : list) {
      if (o.getCustomerId() != null) customerIds.add(o.getCustomerId());
      if (o.getHelperId() != null) helperIds.add(o.getHelperId());
      for (OrderLine line : o.getLines()) {
        if (line.getProductId() != null) productIds.add(line.getProductId());
      }
    }

    Map<Long, Customer> customerById = indexById(customers.findAllById(customerIds), Customer::getId);
    Map<Long, User> helperById = indexById(users.findAllById(helperIds), User::getId);
    Map<Long, Product> productById = indexById(products.findAllById(productIds), Product::getId);
    Map<Long, BigDecimal> dueByCustomer = sumDueByCustomer(customerIds);

    return list.stream()
        .map(o -> toDto(o, customerById, helperById, productById, dueByCustomer))
        .toList();
  }

  private OrderResponse toDto(Order o) {
    return toDtoList(List.of(o)).get(0);
  }

  private OrderResponse toDto(
      Order o,
      Map<Long, Customer> customerById,
      Map<Long, User> helperById,
      Map<Long, Product> productById,
      Map<Long, BigDecimal> dueByCustomer) {
    Customer customer = customerById.get(o.getCustomerId());
    User helper = helperById.get(o.getHelperId());
    List<OrderLineResponse> lines = o.getLines().stream()
        .map(l -> {
          Product product = productById.get(l.getProductId());
          String name = product != null ? product.getName() : "?";
          return new OrderLineResponse(l.getProductId(), name, l.getQty());
        })
        .toList();
    return new OrderResponse(
        o.getId(),
        o.getCustomerId(),
        customer != null ? customer.getName() : "",
        customer != null ? customer.getPhone() : null,
        customer != null ? customer.getCityArea() : null,
        dueByCustomer.getOrDefault(o.getCustomerId(), BigDecimal.ZERO),
        o.getHelperId(),
        helper != null ? helper.getName() : null,
        o.getPriority(),
        o.getExpectedHours(),
        o.getNotes(),
        o.getStatus(),
        o.getCreatedAt(),
        lines);
  }

  private Map<Long, BigDecimal> sumDueByCustomer(Collection<Long> customerIds) {
    Map<Long, BigDecimal> dues = new HashMap<>();
    if (customerIds == null || customerIds.isEmpty()) {
      return dues;
    }
    for (Object[] row : visits.sumBalanceGroupedByCustomer(customerIds)) {
      Long customerId = (Long) row[0];
      BigDecimal balance = row[1] instanceof BigDecimal
          ? (BigDecimal) row[1]
          : BigDecimal.valueOf(((Number) row[1]).doubleValue());
      dues.put(customerId, balance);
    }
    return dues;
  }

  private static <T> Map<Long, T> indexById(Iterable<T> items, Function<T, Long> idFn) {
    Map<Long, T> map = new HashMap<>();
    for (T item : items) {
      map.put(idFn.apply(item), item);
    }
    return map;
  }
}
