import { NavLink } from "react-router-dom";

const adminLinks = [
    ["/admin/dashboard", "⌂", "Dashboard"],
    ["/admin/revistas", "⌕", "Buscar revistas"],
    // INICIO - Accesos compartidos del administrador
    ["/admin/favoritos", "♡", "Mis favoritos"],
    ["/admin/historial", "◷", "Historial"],
    // FIN - Accesos compartidos del administrador
    ["/admin/usuarios", "◎", "Usuarios"],
    ["/admin/roles", "◇", "Roles"],
    ["/admin/cargos", "▣", "Cargos"],
    ["/admin/reportes", "▥", "Reportes"],
    ["/admin/auditoria", "≡", "Auditoría"],
    ["/admin/perfil", "○", "Mi perfil"]
];

const userLinks = [
    ["/usuario/dashboard", "⌂", "Inicio"],
    ["/usuario/revistas", "⌕", "Buscar revistas"],
    ["/usuario/favoritos", "♡", "Mis favoritos"],
    ["/usuario/historial", "◷", "Historial"],
    ["/usuario/ayuda", "?", "Ayuda"],
    ["/usuario/perfil", "○", "Mi perfil"]
];

function Sidebar({ admin, abierto = false, onCerrar }) {
    const usuario = JSON.parse(localStorage.getItem("usuario") || "null");
    const links = admin ? adminLinks : userLinks;
    const iniciales = usuario?.nombreCompleto?.split(" ").slice(0, 2).map((nombre) => nombre[0]).join("") || "SC";

    return (
        <>
            <aside className={`sidebar ${abierto ? "sidebar--abierto" : ""}`} onClick={(evento) => evento.target.closest("a") && onCerrar?.()}>
                <button type="button" className="btn-close btn-close-white sidebar__cerrar d-lg-none" aria-label="Cerrar menú" onClick={onCerrar} />
                <div className="sidebar__brand">
                    <span className="sidebar__logo" aria-hidden="true">S</span>
                    <div><strong>CRECUTEQ</strong><small>{admin ? "Administración" : "Consulta científica"}</small></div>
                </div>
                <div className="sidebar__label">Navegación</div>
                <nav className="sidebar__nav" aria-label="Navegación principal">
                    {links.map(([to, icon, label]) => (
                        <NavLink key={to} to={to} className={({ isActive }) => `sidebar__link ${isActive ? "active" : ""}`}>
                            <span className="sidebar__icon" aria-hidden="true">{icon}</span><span>{label}</span>
                        </NavLink>
                    ))}
                </nav>
                <div className="sidebar__user">
                    <span className="sidebar__avatar">{iniciales}</span>
                    <div><strong>{usuario?.nombreCompleto || "Usuario CRECUTEQ"}</strong><small>{usuario?.rol || (admin ? "Administrador" : "Usuario")}</small></div>
                </div>
            </aside>
            {abierto && <button type="button" className="sidebar__backdrop d-lg-none" aria-label="Cerrar menú" onClick={onCerrar} />}
        </>
    );
}

export default Sidebar;
