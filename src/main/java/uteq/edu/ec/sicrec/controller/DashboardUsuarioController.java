

package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.dto.DashboardUsuarioDTO;
import uteq.edu.ec.sicrec.service.DashboardUsuarioService;

@RestController
@RequestMapping("/api/dashboard-usuario")
@CrossOrigin(origins = "*")
public class DashboardUsuarioController {

    private final DashboardUsuarioService dashboardUsuarioService;

    public DashboardUsuarioController(
            DashboardUsuarioService dashboardUsuarioService
    ) {

        this.dashboardUsuarioService = dashboardUsuarioService;

    }

    @GetMapping("/{idUsuario}")
    public DashboardUsuarioDTO obtenerDashboardUsuario(
            @PathVariable Long idUsuario
    ) {

        return dashboardUsuarioService.obtenerDashboardUsuario(
                idUsuario
        );

    }

}