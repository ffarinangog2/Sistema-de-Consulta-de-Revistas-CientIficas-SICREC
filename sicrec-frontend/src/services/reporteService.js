import axios from "axios";

//const API_REPORTES = "http://localhost:8080/api/reportes";
const API_REPORTES = "/api/reportes";
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


export const exportarReporteHistorialExcel = async () => {

    const response = await axios.get(

        `${API_REPORTES}/historial/excel`,

        {

            responseType: "blob"

        }

    );

    return response.data;

};

export const exportarReporteHistorialPDF = async () => {

    const response = await axios.get(

        `${API_REPORTES}/historial/pdf`,

        {

            responseType: "blob"

        }

    );

    return response.data;

};