package uteq.edu.ec.crecuteq.controller;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.crecuteq.dto.RegistroUsuarioDTO;
import uteq.edu.ec.crecuteq.dto.UsuarioResponseDTO;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponseDTO> listarUsuarios() {
        return usuarioService.listarUsuariosDTO();
    }

    @PostMapping
    public UsuarioResponseDTO crearUsuario(
            @Valid @RequestBody RegistroUsuarioDTO dto,
            Authentication authentication
    ) {

        boolean esAdministrador = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!esAdministrador) {
            dto.setRolId(null);
        }

        return usuarioService.convertirDTO(usuarioService.registrarUsuario(dto));

    }

    @GetMapping("/{id}")
    public UsuarioResponseDTO buscarUsuario(@PathVariable Long id) {
        return usuarioService.convertirDTO(usuarioService.buscarPorId(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado")));
    }

    @PutMapping("/{id}")
    public UsuarioResponseDTO actualizarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Usuario usuario) {

        return usuarioService.convertirDTO(
                usuarioService.actualizarUsuario(id, usuario, authorization));

    }

    @DeleteMapping("/{id}")
    public void eliminarUsuario(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {

        usuarioService.eliminarUsuario(id, authorization);

    }

}
