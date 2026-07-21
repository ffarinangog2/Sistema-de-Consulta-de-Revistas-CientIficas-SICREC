import { Navigate } from "react-router-dom";

function ProtectedRoute({ children }) {

    const usuario = JSON.parse(
        localStorage.getItem("usuario")
    );

    if (!usuario) {

        return <Navigate to="/login" />;

    }

    if (
        usuario.debeCambiarPassword &&
        window.location.pathname !== "/usuario/cambiar-password"
    ) {

        return <Navigate to="/usuario/cambiar-password" />;

    }

    return children;

}

export default ProtectedRoute;