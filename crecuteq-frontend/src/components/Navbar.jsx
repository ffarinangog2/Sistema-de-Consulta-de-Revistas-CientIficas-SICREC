import { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import ThemeToggle from "./ThemeToggle";
import ConfirmDeleteModal from "./ConfirmDeleteModal";
import FotoPerfil from "./FotoPerfil";

const titulos = {
    dashboard: "Dashboard", revistas: "Buscar revistas", favoritos: "Mis favoritos",
    historial: "Historial", ayuda: "Centro de ayuda", perfil: "Mi perfil",
    usuarios: "Gestión de usuarios", roles: "Gestión de roles", cargos: "Gestión de cargos",
    reportes: "Reportes", auditoria: "Auditoría"
};

function Navbar({ onAbrirSidebar }) {
    const [confirmarSalida, setConfirmarSalida] = useState(false);
    const navigate = useNavigate();
    const location = useLocation();
    const usuario = JSON.parse(localStorage.getItem("usuario") || "null");
    const segmento = location.pathname.split("/").filter(Boolean).at(-1);
    const cerrarSesion = () => { localStorage.removeItem("usuario"); navigate("/login"); };

    return (
        <nav className="topbar">
            <div className="d-flex align-items-center gap-3 min-w-0">
                <button type="button" className="topbar__menu d-lg-none" aria-label="Abrir menú" onClick={onAbrirSidebar}>☰</button>
                <div className="topbar__breadcrumb"><span>CRECUTEQ</span><b>/</b><strong>{titulos[segmento] || "Sistema"}</strong></div>
            </div>
            <div className="topbar__actions">
                <FotoPerfil nombre={usuario?.nombreCompleto} />
                <ThemeToggle />
                <div className="topbar__identity d-none d-sm-block"><strong>{usuario?.nombreCompleto}</strong><small>{usuario?.rol}</small></div>
                <button className="topbar__logout" onClick={() => setConfirmarSalida(true)} title="Cerrar sesión" aria-label="Cerrar sesión">↪</button>
            </div>
            <ConfirmDeleteModal
                mostrar={confirmarSalida}
                titulo="Cerrar sesión"
                mensaje="¿Está seguro de que desea cerrar sesión?"
                onCancelar={() => setConfirmarSalida(false)}
                onConfirmar={cerrarSesion}
                textoConfirmar="Cerrar sesión"
            />
        </nav>
    );
}

export default Navbar;
