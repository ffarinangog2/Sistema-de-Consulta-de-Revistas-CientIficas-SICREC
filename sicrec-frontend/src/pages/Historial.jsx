import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import MainLayout from "../layouts/MainLayout";
import {
    obtenerHistorial,
    eliminarHistorial
} from "../services/revistaService";
// INICIO - Notificaciones globales
import { useNotification } from "../hooks/useNotification";
// FIN - Notificaciones globales

function Historial() {

    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const [historial, setHistorial] = useState([]);

    // INICIO - Corrección botón Historial
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    const usuarioId = usuario?.id;
    // FIN - Corrección botón Historial

    const navigate = useNavigate();

    const cargarHistorial = async () => {

        try {

            const data = await obtenerHistorial(usuarioId);

            setHistorial(data);

        } catch (error) {

            console.error(error);

        }

    };

    const eliminar = async (id) => {

        if (!window.confirm("¿Eliminar esta búsqueda del historial?")) {
            return;
        }

        try {

            await eliminarHistorial(id);

            cargarHistorial();

        } catch (error) {

            console.error(error);

            notify("No se pudo eliminar.", "danger");

        }

    };

    const buscarNuevamente = (termino) => {

        navigate("/usuario/revistas", {
            state: {
                termino
            }
        });

    };

    useEffect(() => {

        cargarHistorial();

    }, []);

    return (

        <MainLayout admin={false}>

            {/* INICIO - Responsividad */}
            <div className="container-fluid px-0 mt-2 mt-md-4">

                <h2 className="mb-4">
                    Historial de búsquedas
                </h2>

                {historial.length === 0 ? (

                    <div className="alert alert-info">

                        No existen búsquedas registradas.

                    </div>

                ) : (

                    <div className="table-responsive">
                    <table className="table table-bordered table-hover align-middle mb-0">

                        <thead className="table-dark">

                        <tr>

                            <th>#</th>
                            <th>Término</th>
                            <th>Fecha</th>
                            <th>Resultados</th>
                            <th>Acciones</th>

                        </tr>

                        </thead>

                        <tbody>

                        {historial.map((h, index) => (

                            <tr key={h.id}>

                                <td>{index + 1}</td>

                                <td>{h.terminoBusqueda}</td>

                                <td>
                                    {h.fechaBusqueda?.replace("T", " ")}
                                </td>

                                <td>{h.cantidadResultados}</td>

                                <td className="text-nowrap">

                                    <div className="d-flex flex-wrap gap-2">
                                    <button
                                        className="btn btn-primary btn-sm"
                                        onClick={() => buscarNuevamente(h.terminoBusqueda)}
                                    >
                                        🔍 Buscar nuevamente
                                    </button>
                                    </div>

                                    <button
                                        className="btn btn-danger btn-sm"
                                        onClick={() => eliminar(h.id)}
                                    >
                                        🗑 Eliminar
                                    </button>

                                </td>

                            </tr>

                        ))}

                        </tbody>

                    </table>
                    </div>

                )}

            </div>
            {/* FIN - Responsividad */}

        </MainLayout>

    );

}

export default Historial;
