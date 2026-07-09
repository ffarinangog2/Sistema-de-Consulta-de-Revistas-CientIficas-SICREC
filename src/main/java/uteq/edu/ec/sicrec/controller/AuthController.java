package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.dto.CambiarPasswordDTO;
import uteq.edu.ec.sicrec.dto.LoginRequestDTO;
import uteq.edu.ec.sicrec.dto.LoginResponseDTO;
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
    @PutMapping("/cambiar-password")
    public String cambiarPassword(
            @RequestBody CambiarPasswordDTO dto
    ) {

        usuarioService.cambiarPassword(dto);

        return "Contraseña actualizada correctamente";

    }

}