import { useEffect, useState } from "react";
import MainLayout from "../layouts/MainLayout";
import { obtenerDashboard } from "../services/dashboardService";
import TopBusquedasChart from "../components/TopBusquedasChart";
import UltimasBusquedasTable from "../components/UltimasBusquedasTable";

function DashboardAdmin() {

    const [dashboard, setDashboard] = useState({

        totalUsuarios: 0,
        totalFavoritos: 0,
        totalBusquedas: 0,
        totalRevistas: 0,
        topBusquedas: [],
        ultimasBusquedas: []

    });

    const cargarDashboard = async () => {

        try {

            const data = await obtenerDashboard();

            setDashboard(data);

        } catch (error) {

            console.error(error);

        }

    };

    useEffect(() => {

        cargarDashboard();

    }, []);

    return (

        <MainLayout admin={true}>

            {/* INICIO - Mejora Dashboard */}
            <div className="mb-4">
                <h2 className="mb-1">Panel de Administración</h2>
                <p className="text-muted">
                    Resumen general de la actividad y los recursos registrados en SICREC.
                </p>
            </div>

            <div className="row g-3">

                <div className="col-12 col-sm-6 col-xl-3">

                    <div className="card shadow-sm border-0 h-100">

                        <div className="card-body d-flex align-items-center gap-3">

                            <span className="fs-3" aria-hidden="true">👥</span>
                            <div>
                                <div className="text-muted small">Usuarios</div>
                                <h3 className="mb-0">{dashboard.totalUsuarios}</h3>
                            </div>

                        </div>

                    </div>

                </div>

                <div className="col-12 col-sm-6 col-xl-3">

                    <div className="card shadow-sm border-0 h-100">

                        <div className="card-body d-flex align-items-center gap-3">

                            <span className="fs-3" aria-hidden="true">⭐</span>
                            <div>
                                <div className="text-muted small">Favoritos</div>
                                <h3 className="mb-0">{dashboard.totalFavoritos}</h3>
                            </div>

                        </div>

                    </div>

                </div>

                <div className="col-12 col-sm-6 col-xl-3">

                    <div className="card shadow-sm border-0 h-100">

                        <div className="card-body d-flex align-items-center gap-3">

                            <span className="fs-3" aria-hidden="true">🔎</span>
                            <div>
                                <div className="text-muted small">Búsquedas</div>
                                <h3 className="mb-0">{dashboard.totalBusquedas}</h3>
                            </div>

                        </div>

                    </div>

                </div>

                <div className="col-12 col-sm-6 col-xl-3">

                    <div className="card shadow-sm border-0 h-100">

                        <div className="card-body d-flex align-items-center gap-3">

                            <span className="fs-3" aria-hidden="true">📚</span>
                            <div>
                                <div className="text-muted small">Revistas</div>
                                <h3 className="mb-0">{dashboard.totalRevistas}</h3>
                            </div>

                        </div>

                    </div>

                </div>

            </div>
            {/* FIN - Mejora Dashboard */}

            {/* =======================
                Top 10 búsquedas
            ======================== */}

            <TopBusquedasChart
                datos={dashboard.topBusquedas ?? []}
            />

            {/* =======================
                Últimas búsquedas
            ======================== */}

            <UltimasBusquedasTable
                datos={dashboard.ultimasBusquedas ?? []}
            />

        </MainLayout>

    );

}

export default DashboardAdmin;
