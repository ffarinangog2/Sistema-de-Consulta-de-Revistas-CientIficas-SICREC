package uteq.edu.ec.crecuteq.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uteq.edu.ec.crecuteq.dto.DashboardDTO;
import uteq.edu.ec.crecuteq.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {

        this.dashboardService = dashboardService;

    }

    @GetMapping
    public DashboardDTO obtenerResumen() {

        return dashboardService.obtenerResumen();

    }

}
