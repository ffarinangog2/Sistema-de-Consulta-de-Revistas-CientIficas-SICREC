package uteq.edu.ec.crecuteq.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.criteria.JoinType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uteq.edu.ec.crecuteq.entity.Auditoria;
import uteq.edu.ec.crecuteq.entity.Usuario;
import uteq.edu.ec.crecuteq.dto.AuditoriaResponseDTO;
import uteq.edu.ec.crecuteq.dto.UsuarioAuditoriaDTO;
import uteq.edu.ec.crecuteq.repository.AuditoriaRepository;
import uteq.edu.ec.crecuteq.repository.UsuarioRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

// INICIO - Auditoría general
@Service
public class AuditoriaService {

    public static final String RESULTADO_EXITO = "ÉXITO";
    public static final String RESULTADO_ERROR = "ERROR";

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(
            AuditoriaRepository auditoriaRepository,
            UsuarioRepository usuarioRepository
    ) {

        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // INICIO - Registro de eventos
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarEvento(
            Usuario usuario,
            String modulo,
            String accion,
            String descripcion,
            String resultado
    ) {

        Auditoria auditoria = new Auditoria();

        auditoria.setUsuario(
                usuario != null ? usuario : obtenerUsuarioAutenticado()
        );
        auditoria.setModulo(modulo);
        auditoria.setAccion(accion);
        auditoria.setDescripcion(descripcion);
        auditoria.setIpOrigen(obtenerDireccionIp());
        auditoria.setResultado(resultado);

        auditoriaRepository.save(auditoria);
    }
    // FIN - Registro de eventos

    // INICIO - Registro de eventos
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarExito(
            Usuario usuario,
            String modulo,
            String accion,
            String descripcion
    ) {

        registrarEvento(
                usuario,
                modulo,
                accion,
                descripcion,
                RESULTADO_EXITO
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarError(
            Usuario usuario,
            String modulo,
            String accion,
            String descripcion
    ) {

        registrarEvento(
                usuario,
                modulo,
                accion,
                descripcion,
                RESULTADO_ERROR
        );
    }
    // FIN - Registro de eventos

    // INICIO - Filtros de auditoría
    @Transactional(readOnly = true)
    public List<AuditoriaResponseDTO> listarAuditoria(
            String usuario,
            String modulo,
            String accion,
            String resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String busqueda
    ) {

        return auditoriaRepository.findAll(
                        construirFiltros(
                                usuario,
                                modulo,
                                accion,
                                resultado,
                                fechaDesde,
                                fechaHasta,
                                busqueda
                        ),
                        Sort.by(
                                Sort.Direction.DESC,
                                "fechaAccion"
                        )
                )
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AuditoriaResponseDTO> listarAuditoria(
            String usuario,
            String modulo,
            String accion,
            String resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String busqueda,
            Pageable pageable
    ) {

        return auditoriaRepository.findAll(
                        construirFiltros(
                                usuario,
                                modulo,
                                accion,
                                resultado,
                                fechaDesde,
                                fechaHasta,
                                busqueda
                        ),
                        pageable
                )
                .map(this::convertirDTO);
    }

    private Specification<Auditoria> construirFiltros(
            String usuario,
            String modulo,
            String accion,
            String resultado,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String busqueda
    ) {

        Specification<Auditoria> filtros = (root, query, cb) ->
                cb.conjunction();

        if (tieneTexto(usuario)) {
            String valor = usuario.trim().toLowerCase(Locale.ROOT);
            filtros = filtros.and((root, query, cb) ->
                    cb.equal(
                            cb.lower(root.get("usuario").get("usuario")),
                            valor
                    ));
        }

        if (tieneTexto(modulo)) {
            String valor = modulo.trim().toLowerCase(Locale.ROOT);
            filtros = filtros.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("modulo")), valor));
        }

        if (tieneTexto(accion)) {
            String valor = accion.trim().toLowerCase(Locale.ROOT);
            filtros = filtros.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("accion")), valor));
        }

        if (tieneTexto(resultado)) {
            String valor = resultado.trim().toLowerCase(Locale.ROOT);
            filtros = filtros.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("resultado")), valor));
        }

        if (fechaDesde != null) {
            filtros = filtros.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(
                            root.get("fechaAccion"),
                            fechaDesde.atStartOfDay()
                    ));
        }

        if (fechaHasta != null) {
            filtros = filtros.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(
                            root.get("fechaAccion"),
                            fechaHasta.atTime(LocalTime.MAX)
                    ));
        }

        if (tieneTexto(busqueda)) {
            String valor = "%"
                    + busqueda.trim().toLowerCase(Locale.ROOT)
                    + "%";

            filtros = filtros.and((root, query, cb) -> {
                var usuarioJoin = root.join("usuario", JoinType.LEFT);

                return cb.or(
                        cb.like(cb.lower(root.get("modulo")), valor),
                        cb.like(cb.lower(root.get("accion")), valor),
                        cb.like(cb.lower(root.get("descripcion")), valor),
                        cb.like(cb.lower(root.get("ipOrigen")), valor),
                        cb.like(cb.lower(root.get("resultado")), valor),
                        cb.like(
                                cb.lower(usuarioJoin.get("usuario")),
                                valor
                        ),
                        cb.like(
                                cb.lower(usuarioJoin.get("nombreCompleto")),
                                valor
                        )
                );
            });
        }

        return filtros;
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private AuditoriaResponseDTO convertirDTO(Auditoria auditoria) {

        Usuario usuario = auditoria.getUsuario();

        UsuarioAuditoriaDTO usuarioDTO = usuario == null
                ? null
                : new UsuarioAuditoriaDTO(
                        usuario.getId(),
                        usuario.getUsuario(),
                        usuario.getNombreCompleto()
                );

        return new AuditoriaResponseDTO(
                auditoria.getId(),
                usuarioDTO,
                auditoria.getModulo(),
                auditoria.getAccion(),
                auditoria.getDescripcion(),
                auditoria.getFechaAccion(),
                auditoria.getIpOrigen(),
                auditoria.getResultado()
        );
    }
    // FIN - Filtros de auditoría

    private Usuario obtenerUsuarioAutenticado() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return null;
        }

        return usuarioRepository
                .findByUsuario(authentication.getName())
                .orElse(null);
    }

    private String obtenerDireccionIp() {

        if (!(RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes)) {

            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
// FIN - Auditoría general
