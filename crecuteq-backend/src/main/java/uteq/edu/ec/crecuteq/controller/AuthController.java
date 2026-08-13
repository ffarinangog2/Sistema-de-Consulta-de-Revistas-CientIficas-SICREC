package uteq.edu.ec.crecuteq.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.crecuteq.security.CurrentUserAccess;
import uteq.edu.ec.crecuteq.dto.CambiarPasswordDTO;
import uteq.edu.ec.crecuteq.dto.LoginRequestDTO;
import uteq.edu.ec.crecuteq.dto.LoginResponseDTO;
import uteq.edu.ec.crecuteq.dto.RecuperarPasswordDTO;
import uteq.edu.ec.crecuteq.dto.RestablecerPasswordDTO;
import uteq.edu.ec.crecuteq.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final CurrentUserAccess currentUserAccess;

    public AuthController(UsuarioService usuarioService, CurrentUserAccess currentUserAccess) {
        this.usuarioService = usuarioService;
        this.currentUserAccess = currentUserAccess;
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) {
        return usuarioService.login(request);
    }

    @PostMapping("/recuperar-password")
    public String recuperarPassword(
            @RequestBody RecuperarPasswordDTO dto
    ) {

        usuarioService.solicitarRecuperacionPassword(dto.getCorreo());

        return "Si el correo está registrado, recibirá un enlace para restablecer su contraseña.";

    }

    @GetMapping("/validar-token")
    public String validarToken(
            @RequestParam String token
    ) {

        usuarioService.validarTokenRecuperacion(token);

        return "Token válido";

    }

    // Restablece la contraseña mediante un token de recuperación válido.
    @PostMapping("/restablecer-password")
    public String restablecerPassword(
            @RequestBody RestablecerPasswordDTO dto
    ) {

        usuarioService.restablecerPassword(
                dto.getToken(),
                dto.getNuevaPassword(),
                dto.getConfirmarPassword()
        );

        return "Contraseña restablecida correctamente.";

    }

    @PutMapping("/cambiar-password")
    public String cambiarPassword(
            @RequestBody CambiarPasswordDTO dto,
            Authentication authentication
    ) {

        usuarioService.cambiarPassword(
                currentUserAccess.requireUser(authentication).getId(), dto);

        return "Contraseña actualizada correctamente";

    }

}
