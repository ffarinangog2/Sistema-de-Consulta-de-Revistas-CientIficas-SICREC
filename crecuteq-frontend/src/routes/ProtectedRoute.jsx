import { Navigate } from "react-router-dom";

function ProtectedRoute({ children }) {

    const usuario = JSON.parse(
        localStorage.getItem("usuario")
    );

    if (!usuario) {

        return <Navigate to="/login" />;

    }

    // INICIO - Contexto de navegación según el rol autenticado
    const esAdministrador = usuario.rol === "ADMIN";
    const esUsuario = usuario.rol === "USUARIO";

    if (!esAdministrador && !esUsuario) {
        localStorage.removeItem("usuario");
        return <Navigate to="/login" replace />;
    }
    const rutaCambioPassword = esAdministrador
        ? "/admin/cambiar-password"
        : "/usuario/cambiar-password";
    const rutaActual = window.location.pathname;

    if (
        usuario.debeCambiarPassword &&
        rutaActual !== rutaCambioPassword
    ) {

        return <Navigate to={rutaCambioPassword} />;

    }

    if (esAdministrador && rutaActual.startsWith("/usuario/")) {
        return <Navigate to="/admin/dashboard" />;
    }

    if (!esAdministrador && rutaActual.startsWith("/admin/")) {
        return <Navigate to="/usuario/dashboard" />;
    }
    // FIN - Contexto de navegación según el rol autenticado

    return children;

}

export default ProtectedRoute;
