package uteq.edu.ec.crecuteq.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.crecuteq.entity.Rol;
import uteq.edu.ec.crecuteq.repository.RolRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RolService {

    private static final List<String> ROLES_PERMITIDOS = List.of("ADMIN", "USUARIO");
    private final RolRepository rolRepository;
    private final AuditoriaService auditoriaService;

    public RolService(RolRepository rolRepository, AuditoriaService auditoriaService) {
        this.rolRepository = rolRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Rol> listarRoles() {
        return rolRepository.findByNombreRolInOrderById(ROLES_PERMITIDOS);
    }

    public Optional<Rol> buscarPorId(Long id) {
        return rolRepository.findById(id)
                .filter(rol -> ROLES_PERMITIDOS.contains(rol.getNombreRol()));
    }

    public Rol guardarRol(Rol rol) {
        validarNombre(rol.getNombreRol());
        return rolRepository.findByNombreRol(rol.getNombreRol())
                .orElseGet(() -> {
                    Rol guardado = rolRepository.save(rol);
                    auditoriaService.registrarExito(null, "ROLES", "ROL_CREADO",
                            "Rol creado: " + guardado.getNombreRol());
                    return guardado;
                });
    }

    public Rol actualizarRol(Long id, Rol rolActualizado) {
        throw new IllegalStateException("Los roles ADMIN y USUARIO son fijos");
    }

    public void eliminarRol(Long id) {
        throw new IllegalStateException("Los roles ADMIN y USUARIO no se pueden eliminar");
    }

    private void validarNombre(String nombre) {
        if (!ROLES_PERMITIDOS.contains(nombre)) {
            throw new IllegalArgumentException("Solo se permiten los roles ADMIN y USUARIO");
        }
    }
}
