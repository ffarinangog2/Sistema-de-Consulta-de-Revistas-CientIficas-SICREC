import axios from "axios";

//const API_REPORTES = "http://localhost:8080/api/reportes";
const API_REPORTES = "/api/reportes";

export const obtenerAnaliticaReporte = async (fechaInicio, fechaFin, pagina = 0) => {
    const response = await axios.get(`${API_REPORTES}/historial/analitica`, {
        params: {
            ...(fechaInicio && fechaFin ? { fechaInicio, fechaFin } : {}),
            pagina
        }
    });
    return response.data;
};
export const obtenerReporteHistorial = async () => {

    const response = await axios.get(
        `${API_REPORTES}/historial`
    );

    return response.data;

};

export const obtenerReporteHistorialPorFechas = async (
    fechaInicio,
    fechaFin
) => {

    const response = await axios.get(
        `${API_REPORTES}/historial/fechas`,
        {
            params: {
                fechaInicio,
                fechaFin
            }
        }
    );

    return response.data;

};


export const exportarReporteHistorialExcel = async (fechaInicio, fechaFin) => {

    const response = await axios.get(

        `${API_REPORTES}/historial/excel`,

        {
            params: fechaInicio && fechaFin ? { fechaInicio, fechaFin } : {},
            responseType: "blob"

        }

    );

    return response.data;

};

export const exportarReporteHistorialPDF = async (fechaInicio, fechaFin) => {

    const response = await axios.get(

        `${API_REPORTES}/historial/pdf`,

        {
            params: fechaInicio && fechaFin ? { fechaInicio, fechaFin } : {},
            responseType: "blob"

        }

    );

    return response.data;

};
