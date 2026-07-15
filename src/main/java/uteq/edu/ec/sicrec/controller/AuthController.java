package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.dto.CambiarPasswordDTO;
import uteq.edu.ec.sicrec.dto.LoginRequestDTO;
import uteq.edu.ec.sicrec.dto.LoginResponseDTO;
import uteq.edu.ec.sicrec.dto.RecuperarPasswordDTO;
import uteq.edu.ec.sicrec.dto.RestablecerPasswordDTO;
import uteq.edu.ec.sicrec.service.UsuarioService;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
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
            @RequestBody CambiarPasswordDTO dto
    ) {

        usuarioService.cambiarPassword(dto);

        return "Contraseña actualizada correctamente";

    }

}
