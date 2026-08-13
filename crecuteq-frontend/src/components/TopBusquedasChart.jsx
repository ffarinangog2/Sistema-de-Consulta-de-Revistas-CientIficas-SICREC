import { useEffect, useState } from "react";
import {
    Chart as ChartJS, CategoryScale, LinearScale, BarElement,
    Title, Tooltip, Legend
} from "chart.js";
import { Bar } from "react-chartjs-2";

ChartJS.register(CategoryScale, LinearScale, BarElement, Title, Tooltip, Legend);

function TopBusquedasChart({ datos }) {
    // INICIO - Adaptación visual del gráfico al tema activo
    const [theme, setTheme] = useState(() => document.documentElement.dataset.theme || "light");

    useEffect(() => {
        const actualizarTema = (evento) => setTheme(evento.detail);
        window.addEventListener("crecuteq-theme-change", actualizarTema);
        return () => window.removeEventListener("crecuteq-theme-change", actualizarTema);
    }, []);

    const darkMode = theme === "dark";
    const chartText = darkMode ? "#91a79a" : "#65766a";
    const chartGrid = darkMode ? "rgba(174,211,188,.10)" : "rgba(38,79,46,.10)";
    // FIN - Adaptación visual del gráfico al tema activo

    const data = {
        labels: datos.map((item) => item.termino),
        datasets: [{
            label: "Cantidad de búsquedas",
            data: datos.map((item) => item.total),
            backgroundColor: darkMode ? "#31a613" : "#1b7505",
            borderRadius: 6
        }]
    };

    const options = {
        responsive: true,
        plugins: {
            legend: { display: false },
            title: { display: true, text: "Top 10 términos más buscados", color: chartText }
        },
        scales: {
            x: { ticks: { color: chartText }, grid: { color: chartGrid } },
            y: { ticks: { color: chartText }, grid: { color: chartGrid } }
        }
    };

    return (
        <div className="card shadow-sm border-0 mt-4">
            <div className="card-header bg-white border-0 pt-3 px-3 px-md-4">
                <h5 className="mb-1">Términos más buscados</h5>
                <p className="text-muted small">Distribución de las consultas más frecuentes.</p>
            </div>
            <div className="card-body overflow-hidden px-3 px-md-4">
                <Bar data={data} options={options} />
            </div>
        </div>
    );
}

export default TopBusquedasChart;
