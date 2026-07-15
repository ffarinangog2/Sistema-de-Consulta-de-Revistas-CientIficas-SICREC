package uteq.edu.ec.sicrec.controller;

import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.sicrec.dto.RegistroUsuarioDTO;
import uteq.edu.ec.sicrec.dto.UsuarioResponseDTO;
import uteq.edu.ec.sicrec.entity.Usuario;
import uteq.edu.ec.sicrec.service.UsuarioService;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
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
    public Usuario crearUsuario(
            @RequestBody RegistroUsuarioDTO dto
    ) {

        return usuarioService.registrarUsuario(dto);

    }

    @GetMapping("/{id}")
    public Usuario buscarUsuario(@PathVariable Long id) {
        return usuarioService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @PutMapping("/{id}")
    public Usuario actualizarUsuario(
            @PathVariable Long id,
            @RequestBody Usuario usuario) {

        return usuarioService.actualizarUsuario(id, usuario);

    }

    @DeleteMapping("/{id}")
    public void eliminarUsuario(@PathVariable Long id) {

        usuarioService.eliminarUsuario(id);

    }

}