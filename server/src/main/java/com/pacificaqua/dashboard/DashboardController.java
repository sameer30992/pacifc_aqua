package com.pacificaqua.dashboard;

import com.pacificaqua.dashboard.DashboardDtos.DashboardSummary;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping("/summary")
  public DashboardSummary summary(@RequestParam(required = false) String month) {
    return service.summary(month);
  }
}
