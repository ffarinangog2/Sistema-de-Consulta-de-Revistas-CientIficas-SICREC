import { Routes, Route, Navigate } from "react-router-dom";

import Login from "./pages/Login";
import DashboardAdmin from "./pages/DashboardAdmin";
import DashboardUsuario from "./pages/DashboardUsuario";
import Usuarios from "./pages/Usuarios";
import Roles from "./pages/Roles";
import Cargos from "./pages/Cargos";
import BuscarRevistas from "./pages/BuscarRevistas";
import Favoritos from "./pages/Favoritos";
import Historial from "./pages/Historial";
import CambiarPassword from "./pages/CambiarPassword";
import Perfil from "./pages/Perfil";
import Reportes from "./pages/Reportes";
import PerfilAdmin from "./pages/PerfilAdmin";
import CrearCuenta from "./pages/CrearCuenta";
// INICIO - Pantalla Ayuda
import Ayuda from "./pages/Ayuda";
// FIN - Pantalla Ayuda
// INICIO - Auditoría
import Auditoria from "./pages/Auditoria";
// FIN - Auditoría
// INICIO - Recuperación de contraseña
import RecuperarPassword from "./pages/RecuperarPassword";
// INICIO - Restablecer contraseña
import RestablecerPassword from "./pages/RestablecerPassword";
// FIN - Restablecer contraseña
// FIN - Recuperación de contraseña

import ProtectedRoute from "./routes/ProtectedRoute";

function App() {

    return (

        <Routes>

            <Route
                path="/"
                element={<Navigate to="/login" />}
            />

            <Route
                path="/login"
                element={<Login />}
            />

            <Route
                path="/crear-cuenta"
                element={<CrearCuenta />}
            />

            {/* INICIO - Ruta de recuperación de contraseña */}
            <Route
                path="/recuperar-password"
                element={<RecuperarPassword />}
            />

            {/* INICIO - Ruta para restablecer contraseña */}
            <Route
                path="/restablecer-password"
                element={<RestablecerPassword />}
            />
            {/* FIN - Ruta para restablecer contraseña */}
            {/* FIN - Ruta de recuperación de contraseña */}

            <Route
                path="/usuario/cambiar-password"
                element={<CambiarPassword />}
            />

            <Route
                path="/admin/dashboard"
                element={
                    <ProtectedRoute>
                        <DashboardAdmin />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/usuario/dashboard"
                element={
                    <ProtectedRoute>
                        <DashboardUsuario />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/admin/roles"
                element={
                    <ProtectedRoute>
                        <Roles />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/admin/usuarios"
                element={
                    <ProtectedRoute>
                        <Usuarios />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/admin/cargos"
                element={
                    <ProtectedRoute>
                        <Cargos />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/admin/reportes"
                element={
                    <ProtectedRoute>
                        <Reportes />
                    </ProtectedRoute>
                }
            />

            {/* INICIO - Auditoría */}
            <Route
                path="/admin/auditoria"
                element={
                    <ProtectedRoute>
                        <Auditoria />
                    </ProtectedRoute>
                }
            />
            {/* FIN - Auditoría */}

            <Route
                path="/admin/revistas"
                element={
                    <ProtectedRoute>
                        <BuscarRevistas />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/admin/perfil"
                element={
                    <ProtectedRoute>
                        <PerfilAdmin />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/usuario/revistas"
                element={
                    <ProtectedRoute>
                        <BuscarRevistas />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/usuario/favoritos"
                element={
                    <ProtectedRoute>
                        <Favoritos />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/usuario/historial"
                element={
                    <ProtectedRoute>
                        <Historial />
                    </ProtectedRoute>
                }
            />

            <Route
                path="/usuario/ayuda"
                element={
                    <ProtectedRoute>
                        {/* INICIO - Pantalla Ayuda */}
                        <Ayuda />
                        {/* FIN - Pantalla Ayuda */}
                    </ProtectedRoute>
                }
            />

            <Route
                path="/usuario/perfil"
                element={
                    <ProtectedRoute>
                        <Perfil />
                    </ProtectedRoute>
                }
            />

        </Routes>

    );

}

export default App;
