import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import { obtenerPerfil } from "../services/perfilService";

function Perfil() {

    const navigate = useNavigate();

    const [perfil, setPerfil] = useState({

        nombreCompleto: "",
        correoInstitucional: "",
        rol: "",
        cargo: "",
        estado: false

    });

    const cargarPerfil = async () => {

        try {

            const usuario = JSON.parse(
                localStorage.getItem("usuario")
            );

            if (!usuario) {

                return;

            }

            const data = await obtenerPerfil(
                usuario.id
            );

            setPerfil(data);

        } catch (error) {

            console.error(error);

        }

    };

    const cerrarSesion = () => {

        localStorage.removeItem("usuario");

        window.location.href = "/login";

    };

    useEffect(() => {

        cargarPerfil();

    }, []);

    return (

        <MainLayout admin={true}>

            {/* INICIO - Mejora Perfil */}
            <div className="mb-4">
                <h2 className="mb-1">Mi Perfil</h2>
                <p className="text-muted">Información de la cuenta administrativa activa.</p>
            </div>

            <div className="row justify-content-center">
            <div className="col-12 col-xl-9">
            <div className="card shadow-sm border-0">

                <div className="card-body p-3 p-md-4">

                    <div className="text-center mb-4">

                        <img
                            src="https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
                            alt="Perfil"
                            width="120"
                        />

                    </div>

                    {/* INICIO - Responsividad */}
                    <div className="table-responsive">
                    <table className="table table-hover align-middle mb-0">

                        <tbody>

                        <tr>

                            <th>Nombre</th>

                            <td>{perfil.nombreCompleto}</td>

                        </tr>

                        <tr>

                            <th>Correo</th>

                            <td>{perfil.correoInstitucional}</td>

                        </tr>

                        <tr>

                            <th>Rol</th>

                            <td>{perfil.rol}</td>

                        </tr>

                        <tr>

                            <th>Cargo</th>

                            <td>{perfil.cargo}</td>

                        </tr>

                        <tr>

                            <th>Estado</th>

                            <td>

                                {perfil.estado
                                    ? <span className="badge bg-success">Activo</span>
                                    : <span className="badge bg-secondary">Inactivo</span>}

                            </td>

                        </tr>

                        </tbody>

                    </table>
                    </div>

                    <div className="d-flex flex-column flex-sm-row justify-content-end gap-2 mt-4">
                    <button
                        className="btn btn-danger"
                        onClick={cerrarSesion}
                    >
                        Cerrar sesión
                    </button>

                    <button
                        className="btn btn-warning"
                        onClick={() => navigate("/usuario/cambiar-password")}
                    >
                        Cambiar contraseña
                    </button>
                    </div>
                    {/* FIN - Responsividad */}

                </div>

            </div>
            </div>
            </div>
            {/* FIN - Mejora Perfil */}

        </MainLayout>

    );

}

export default Perfil;
