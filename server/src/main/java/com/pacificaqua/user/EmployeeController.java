package com.pacificaqua.user;

import com.pacificaqua.domain.enums.UserRole;
import com.pacificaqua.domain.enums.UserStatus;
import com.pacificaqua.user.EmployeeDtos.EmployeeRequest;
import com.pacificaqua.user.EmployeeDtos.EmployeeResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employees")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class EmployeeController {
  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @GetMapping
  public List<EmployeeResponse> list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UserRole role,
      @RequestParam(required = false) UserStatus status) {
    return service.list(q, role, status);
  }

  @PostMapping
  public EmployeeResponse create(@Valid @RequestBody EmployeeRequest request) {
    return service.create(request);
  }

  @PutMapping("/{id}")
  public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
    return service.update(id, request);
  }

  @PostMapping("/{id}/deactivate")
  public EmployeeResponse deactivate(@PathVariable Long id) {
    return service.setStatus(id, UserStatus.INACTIVE);
  }

  @PostMapping("/{id}/activate")
  public EmployeeResponse activate(@PathVariable Long id) {
    return service.setStatus(id, UserStatus.ACTIVE);
  }

  @GetMapping("/helpers")
  public List<EmployeeResponse> helpers() {
    return service.list(null, UserRole.HELPER, UserStatus.ACTIVE);
  }

  @PatchMapping("/{id}/status")
  public EmployeeResponse patchStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
    return service.setStatus(id, UserStatus.valueOf(body.get("status")));
  }
}
