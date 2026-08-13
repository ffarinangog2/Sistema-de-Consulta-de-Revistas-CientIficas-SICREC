

package uteq.edu.ec.crecuteq.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.crecuteq.security.CurrentUserAccess;
import uteq.edu.ec.crecuteq.dto.DashboardUsuarioDTO;
import uteq.edu.ec.crecuteq.service.DashboardUsuarioService;

@RestController
@RequestMapping("/api/dashboard-usuario")
public class DashboardUsuarioController {

    private final DashboardUsuarioService dashboardUsuarioService;
    private final CurrentUserAccess currentUserAccess;

    public DashboardUsuarioController(
            DashboardUsuarioService dashboardUsuarioService,
            CurrentUserAccess currentUserAccess
    ) {

        this.dashboardUsuarioService = dashboardUsuarioService;
        this.currentUserAccess = currentUserAccess;

    }

    @GetMapping("/{idUsuario}")
    public DashboardUsuarioDTO obtenerDashboardUsuario(
            @PathVariable Long idUsuario,
            Authentication authentication
    ) {

        currentUserAccess.requireSelfOrAdmin(authentication, idUsuario);

        return dashboardUsuarioService.obtenerDashboardUsuario(
                idUsuario
        );

    }

}
