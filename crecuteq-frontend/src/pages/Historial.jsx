import { useCallback, useEffect, useMemo, useState } from "react";
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
    const [busqueda, setBusqueda] = useState("");
    const [paginaActual, setPaginaActual] = useState(1);
    const registrosPorPagina = 10;

    // INICIO - Corrección botón Historial
    const usuario = JSON.parse(localStorage.getItem("usuario"));
    const usuarioId = usuario?.id;
    // FIN - Corrección botón Historial

    const navigate = useNavigate();
    // INICIO - Reutilización de la vista según el módulo activo
    const esAdministrador = window.location.pathname.startsWith("/admin/");
    const rutaRevistas = esAdministrador ? "/admin/revistas" : "/usuario/revistas";
    // FIN - Reutilización de la vista según el módulo activo

    const cargarHistorial = useCallback(async () => {

        try {

            const data = await obtenerHistorial(usuarioId);

            setHistorial(Array.isArray(data) ? data : []);

        } catch (error) {

            console.error(error);

        }

    }, [usuarioId]);

    const historialFiltrado = useMemo(() => {
        const consulta = busqueda.trim().toLocaleLowerCase("es");
        if (!consulta) return historial;
        return historial.filter((item) => [
            item.terminoBusqueda,
            item.fechaBusqueda?.replace("T", " "),
            item.cantidadResultados
        ].some((valor) => String(valor ?? "").toLocaleLowerCase("es").includes(consulta)));
    }, [busqueda, historial]);

    const totalPaginas = Math.ceil(historialFiltrado.length / registrosPorPagina);
    const paginaVisible = totalPaginas === 0 ? 1 : Math.min(paginaActual, totalPaginas);
    const indiceInicial = (paginaVisible - 1) * registrosPorPagina;
    const indiceFinal = Math.min(indiceInicial + registrosPorPagina, historialFiltrado.length);
    const historialPagina = historialFiltrado.slice(indiceInicial, indiceFinal);

    useEffect(() => {
        if (totalPaginas === 0 && paginaActual !== 1) setPaginaActual(1);
        else if (totalPaginas > 0 && paginaActual > totalPaginas) setPaginaActual(totalPaginas);
    }, [paginaActual, totalPaginas]);

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

        navigate(rutaRevistas, {
            state: {
                termino
            }
        });

    };

    useEffect(() => {

        cargarHistorial();

    }, [cargarHistorial]);

    return (

        <MainLayout admin={esAdministrador}>

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

                    <>
                    <div className="card shadow-sm border-0 mb-3">
                        <div className="card-body">
                            <label className="form-label" htmlFor="buscar-historial">Buscar en el historial</label>
                            <div className="input-group">
                                <span className="input-group-text" aria-hidden="true">⌕</span>
                                <input id="buscar-historial" type="search" className="form-control"
                                    placeholder="Buscar por término, fecha o cantidad de resultados"
                                    value={busqueda}
                                    onChange={(e) => {
                                        setBusqueda(e.target.value);
                                        setPaginaActual(1);
                                    }} />
                            </div>
                        </div>
                    </div>

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

                        {historialPagina.length === 0 ? (
                            <tr><td colSpan="5" className="text-center text-muted py-4">No se encontraron registros.</td></tr>
                        ) : historialPagina.map((h, index) => (

                            <tr key={h.id}>

                                <td>{indiceInicial + index + 1}</td>

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

                    <div className="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2 mt-3">
                        <span className="text-muted small">
                            Mostrando {historialFiltrado.length === 0 ? 0 : indiceInicial + 1}–{indiceFinal} de {historialFiltrado.length} registros
                        </span>
                        <div className="d-flex flex-wrap align-items-center gap-2">
                            <button className="btn btn-outline-primary btn-sm" disabled={paginaActual <= 1}
                                onClick={() => setPaginaActual((pagina) => Math.max(1, pagina - 1))}>Anterior</button>
                            <span>Página {totalPaginas === 0 ? 0 : paginaVisible} de {totalPaginas}</span>
                            <button className="btn btn-outline-primary btn-sm"
                                disabled={totalPaginas === 0 || paginaActual >= totalPaginas}
                                onClick={() => setPaginaActual((pagina) => Math.min(totalPaginas, pagina + 1))}>Siguiente</button>
                        </div>
                    </div>
                    </>

                )}

            </div>
            {/* FIN - Responsividad */}

        </MainLayout>

    );

}

export default Historial;
