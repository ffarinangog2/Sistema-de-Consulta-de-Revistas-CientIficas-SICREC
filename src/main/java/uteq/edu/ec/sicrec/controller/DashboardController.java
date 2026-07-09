package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uteq.edu.ec.sicrec.dto.DashboardDTO;
import uteq.edu.ec.sicrec.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
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