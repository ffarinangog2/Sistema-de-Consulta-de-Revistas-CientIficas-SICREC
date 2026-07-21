import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
// INICIO - Notificaciones globales
import { useNotification } from "../hooks/useNotification";
// FIN - Notificaciones globales
import {
    obtenerReporteHistorial,
    obtenerReporteHistorialPorFechas,
    exportarReporteHistorialExcel,
    exportarReporteHistorialPDF
} from "../services/reporteService";

function Reportes() {

    // INICIO - Notificaciones globales
    const { notify } = useNotification();
    // FIN - Notificaciones globales

    const [reporte, setReporte] = useState([]);
    const [cargando, setCargando] = useState(false);

    const [fechaInicio, setFechaInicio] = useState("");
    const [fechaFin, setFechaFin] = useState("");

    const cargarReporte = async () => {

        setCargando(true);

        try {

            const data = await obtenerReporteHistorial();

            setReporte(data);

        } catch (error) {

            console.error(error);

        } finally {

            setCargando(false);

        }

    };

    const buscarPorFechas = async () => {

        if (!fechaInicio || !fechaFin) {

            cargarReporte();

            return;

        }

        setCargando(true);

        try {

            const data =
                await obtenerReporteHistorialPorFechas(
                    fechaInicio,
                    fechaFin
                );

            setReporte(data);

        } catch (error) {

            console.error(error);

        } finally {

            setCargando(false);

        }

    };

    const descargarExcel = async () => {

        try {

            const archivo =
                await exportarReporteHistorialExcel();

            const url = window.URL.createObjectURL(
                new Blob([archivo])
            );

            const link = document.createElement("a");

            link.href = url;

            link.setAttribute(
                "download",
                "Reporte_Historial.xlsx"
            );

            document.body.appendChild(link);

            link.click();

            link.remove();

            window.URL.revokeObjectURL(url);

        } catch (error) {

            console.error(error);

            notify("No se pudo descargar el reporte Excel.", "danger");

        }

    };

    const descargarPDF = async () => {

        try {

            const archivo =
                await exportarReporteHistorialPDF();

            const url = window.URL.createObjectURL(
                new Blob([archivo])
            );

            const link = document.createElement("a");

            link.href = url;

            link.setAttribute(
                "download",
                "Reporte_Historial.pdf"
            );

            document.body.appendChild(link);

            link.click();

            link.remove();

            window.URL.revokeObjectURL(url);

        } catch (error) {

            console.error(error);

            notify("No se pudo descargar el reporte PDF.", "danger");

        }

    };

    useEffect(() => {

        cargarReporte();

    }, []);

    return (

        <MainLayout admin={true}>

            {/* INICIO - Mejora Historial */}
            <div className="mb-4">
                <h2 className="mb-1">Reportes</h2>
                <p className="text-muted">Consulte y exporte el historial general de búsquedas.</p>
            </div>

            <div className="card shadow-sm border-0 mb-3">
            <div className="card-body">
            <div className="row g-3 mb-3">

                <div className="col-12 col-sm-6 col-lg-3">

                    <label className="form-label">
                        Desde
                    </label>

                    <input
                        type="date"
                        className="form-control"
                        value={fechaInicio}
                        onChange={(e) => setFechaInicio(e.target.value)}
                    />

                </div>

                <div className="col-12 col-sm-6 col-lg-3">

                    <label className="form-label">
                        Hasta
                    </label>

                    <input
                        type="date"
                        className="form-control"
                        value={fechaFin}
                        onChange={(e) => setFechaFin(e.target.value)}
                    />

                </div>

                <div className="col-12 col-lg-6 d-flex flex-wrap gap-2 align-items-end">

                    <button
                        className="btn btn-primary"
                        onClick={buscarPorFechas}
                    >
                        🔍 Buscar
                    </button>

                    <button
                        className="btn btn-success"
                        onClick={descargarExcel}
                    >
                        📊 Excel
                    </button>

                    <button
                        className="btn btn-danger"
                        onClick={descargarPDF}
                    >
                        📄 PDF
                    </button>

                </div>

            </div>
            </div>
            </div>

            <div className="card shadow-sm border-0">

                <div className="card-body">

                    <h5 className="mb-3">
                        Reporte de historial de búsquedas
                    </h5>

                    {cargando ? (

                        <div className="text-center py-4">
                            <div className="spinner-border text-primary" role="status" />
                            <p className="text-muted mt-2">Cargando historial...</p>
                        </div>

                    ) : (

                        <>
                        {/* INICIO - Responsividad */}
                        <div className="table-responsive">
                        <table className="table table-hover table-bordered mb-0">

                            <thead className="table-dark">

                            <tr>

                                <th>#</th>
                                <th>Fecha</th>
                                <th>Usuario</th>
                                <th>Término</th>
                                <th>Resultados</th>

                            </tr>

                            </thead>

                            <tbody>

                            {reporte.map((item, index) => (

                                <tr key={index}>

                                    <td>{index + 1}</td>

                                    <td>
                                        {item.fecha?.replace("T", " ")}
                                    </td>

                                    <td>{item.usuario}</td>

                                    <td>{item.termino}</td>

                                    <td>{item.resultados}</td>

                                </tr>

                            ))}

                            </tbody>

                        </table>
                        </div>
                        {/* FIN - Responsividad */}
                        </>

                    )}

                </div>

            </div>
            {/* FIN - Mejora Historial */}

        </MainLayout>

    );

}

export default Reportes;
