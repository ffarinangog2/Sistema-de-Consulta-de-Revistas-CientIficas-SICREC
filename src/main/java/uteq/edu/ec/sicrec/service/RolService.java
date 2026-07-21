package uteq.edu.ec.sicrec.service;

import org.springframework.stereotype.Service;
import uteq.edu.ec.sicrec.entity.Rol;
import uteq.edu.ec.sicrec.repository.RolRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RolService {

    private final RolRepository rolRepository;
    private final AuditoriaService auditoriaService;

    public RolService(
            RolRepository rolRepository,
            AuditoriaService auditoriaService
    ) {

        this.rolRepository = rolRepository;
        this.auditoriaService = auditoriaService;
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    public Optional<Rol> buscarPorId(Long id) {
        return rolRepository.findById(id);
    }

    public Rol guardarRol(Rol rol) {

        // INICIO - Auditoría módulo Roles
        try {
            Rol rolGuardado = rolRepository.save(rol);

            auditoriaService.registrarExito(
                    null,
                    "ROLES",
                    "ROL_CREADO",
                    "Rol creado: " + rolGuardado.getNombreRol()
            );

            return rolGuardado;
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "ROLES",
                    "ROL_CREADO",
                    "No fue posible crear el rol"
            );
            throw e;
        }
        // FIN - Auditoría módulo Roles
    }

    public Rol actualizarRol(Long id, Rol rolActualizado) {

        // INICIO - Auditoría módulo Roles
        try {
            Rol rol = rolRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Rol no encontrado"));

            rol.setNombreRol(rolActualizado.getNombreRol());

            Rol rolGuardado = rolRepository.save(rol);

            auditoriaService.registrarExito(
                    null,
                    "ROLES",
                    "ROL_EDITADO",
                    "Rol editado: " + rolGuardado.getNombreRol()
            );

            return rolGuardado;
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "ROLES",
                    "ROL_EDITADO",
                    "No fue posible editar el rol con id " + id
            );
            throw e;
        }
        // FIN - Auditoría módulo Roles
    }

    public void eliminarRol(Long id) {

        // INICIO - Auditoría módulo Roles
        try {
            Rol rol = rolRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("Rol no encontrado"));

            rolRepository.delete(rol);

            auditoriaService.registrarExito(
                    null,
                    "ROLES",
                    "ROL_ELIMINADO",
                    "Rol eliminado: " + rol.getNombreRol()
            );
        } catch (RuntimeException e) {
            auditoriaService.registrarError(
                    null,
                    "ROLES",
                    "ROL_ELIMINADO",
                    "No fue posible eliminar el rol con id " + id
            );
            throw e;
        }
        // FIN - Auditoría módulo Roles
    }

}
