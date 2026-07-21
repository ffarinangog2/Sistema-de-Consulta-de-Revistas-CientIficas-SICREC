import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import { obtenerDashboardUsuario } from "../services/dashboardUsuarioService";

function DashboardUsuario() {

    const usuario = JSON.parse(
        localStorage.getItem("usuario")
    );

    const [dashboard, setDashboard] = useState({

        totalBusquedas: 0,

        totalFavoritos: 0,

        ultimasBusquedas: [],

        ultimosFavoritos: []

    });

    const cargarDashboard = async () => {

        try {

            const data = await obtenerDashboardUsuario(
                usuario.id
            );

            setDashboard(data);

        } catch (error) {

            console.error(error);

        }

    };

    useEffect(() => {

        cargarDashboard();

    }, []);

    return (

        <MainLayout admin={false}>

            {/* INICIO - Mejoras Inicio */}
            <header className="mb-4">
                <h2 className="mb-1">Bienvenido, {usuario?.nombreCompleto}</h2>
                <p className="text-muted">
                    Consulta, compara y organiza información de revistas científicas con datos de Scopus y SCImago.
                </p>
            </header>

            <div className="row g-3 mb-4">
                {[
                    ["📚", "Revistas científicas", "Consulta títulos e ISSN."],
                    ["🔵", "Scopus", "Revisa datos y métricas bibliométricas."],
                    ["📊", "SCImago", "Compara cuartiles, áreas e indicadores."],
                    ["⭐", "Favoritos", "Organiza las revistas de tu interés."],
                    ["🕒", "Historial", "Recupera tus búsquedas anteriores."]
                ].map(([icono, titulo, descripcion]) => (
                    <div className="col-12 col-sm-6 col-xl" key={titulo}>
                        <div className="card shadow-sm border-0 h-100">
                            <div className="card-body p-3">
                                <span className="d-block mb-2" aria-hidden="true">{icono}</span>
                                <h6 className="fw-bold mb-1">{titulo}</h6>
                                <p className="text-muted small">{descripcion}</p>
                            </div>
                        </div>
                    </div>
                ))}
            </div>
            {/* FIN - Mejoras Inicio */}

            {/* INICIO - Responsividad */}
            <div className="row g-3">

                <div className="col-12 col-md-6">

                    <div className="card shadow border-0">

                        <div className="card-body text-center">

                            <h5>🔎 Mis Búsquedas</h5>

                            <h2>

                                {dashboard.totalBusquedas}

                            </h2>

                        </div>

                    </div>

                </div>

                <div className="col-12 col-md-6">

                    <div className="card shadow border-0">

                        <div className="card-body text-center">

                            <h5>⭐ Mis Favoritos</h5>

                            <h2>

                                {dashboard.totalFavoritos}

                            </h2>

                        </div>

                    </div>

                </div>

            </div>
            {/* FIN - Responsividad */}

            <div className="row g-3 mt-1">

                <div className="col-12 col-xl-6">

                    <div className="card shadow border-0">

                        <div className="card-body">

                            <h5 className="mb-3">

                                🕒 Últimas búsquedas

                            </h5>

                            {/* INICIO - Responsividad */}
                            <div className="table-responsive">
                            <table className="table table-hover mb-0">

                                <thead>

                                <tr>

                                    <th>Término</th>

                                    <th>Fecha</th>

                                </tr>

                                </thead>

                                <tbody>

                                {dashboard.ultimasBusquedas.length > 0 ? (

                                    dashboard.ultimasBusquedas.map((item, index) => (

                                        <tr key={index}>

                                            <td>

                                                {item.termino}

                                            </td>

                                            <td>

                                                {item.fecha?.replace("T", " ")}

                                            </td>

                                        </tr>

                                    ))

                                ) : (

                                    <tr>

                                        <td
                                            colSpan="2"
                                            className="text-center"
                                        >

                                            No existen registros.

                                        </td>

                                    </tr>

                                )}

                                </tbody>

                            </table>
                            </div>
                            {/* FIN - Responsividad */}

                        </div>

                    </div>

                </div>

                <div className="col-12 col-xl-6">

                    <div className="card shadow border-0">

                        <div className="card-body">

                            <h5 className="mb-3">

                                ⭐ Últimos favoritos

                            </h5>

                            {/* INICIO - Responsividad */}
                            <div className="table-responsive">
                            <table className="table table-hover mb-0">

                                <thead>

                                <tr>

                                    <th>Revista</th>

                                    <th>Cuartil</th>

                                </tr>

                                </thead>

                                <tbody>

                                {dashboard.ultimosFavoritos.length > 0 ? (

                                    dashboard.ultimosFavoritos.map((item, index) => (

                                        <tr key={index}>

                                            <td>

                                                {item.revista}

                                            </td>

                                            <td>

                                                {item.cuartil}

                                            </td>

                                        </tr>

                                    ))

                                ) : (

                                    <tr>

                                        <td
                                            colSpan="2"
                                            className="text-center"
                                        >

                                            No existen registros.

                                        </td>

                                    </tr>

                                )}

                                </tbody>

                            </table>
                            </div>
                            {/* FIN - Responsividad */}

                        </div>

                    </div>

                </div>

            </div>

        </MainLayout>

    );

}

export default DashboardUsuario;
