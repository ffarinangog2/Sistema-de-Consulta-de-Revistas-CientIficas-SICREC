import {
    Chart as ChartJS,
    CategoryScale,
    LinearScale,
    BarElement,
    Title,
    Tooltip,
    Legend
} from "chart.js";

import { Bar } from "react-chartjs-2";

ChartJS.register(
    CategoryScale,
    LinearScale,
    BarElement,
    Title,
    Tooltip,
    Legend
);

function TopBusquedasChart({ datos }) {

    const data = {

        labels: datos.map(item => item.termino),

        datasets: [

            {

                label: "Cantidad de búsquedas",

                data: datos.map(item => item.total),

                backgroundColor: "#0d6efd"

            }

        ]

    };

    const options = {

        responsive: true,

        plugins: {

            legend: {

                display: false

            },

            title: {

                display: true,

                text: "Top 10 términos más buscados"

            }

        }

    };

    return (

        // INICIO - Mejora Dashboard
        <div className="card shadow-sm border-0 mt-4">

            <div className="card-header bg-white border-0 pt-3 px-3 px-md-4">
                <h5 className="mb-1">Términos más buscados</h5>
                <p className="text-muted small">Distribución de las consultas más frecuentes.</p>
            </div>

            <div className="card-body overflow-hidden px-3 px-md-4">

                <Bar
                    data={data}
                    options={options}
                />

            </div>

        </div>
        // FIN - Mejora Dashboard

    );

}

export default TopBusquedasChart;
