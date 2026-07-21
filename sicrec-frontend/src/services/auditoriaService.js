// INICIO - Auditoría
import axios from "axios";

const API_AUDITORIA = "/api/auditoria";

export const obtenerAuditoria = async () => {
    const response = await axios.get(API_AUDITORIA);
    return response.data;
};

export const exportarAuditoriaExcel = async (params) => {
    const response = await axios.get(`${API_AUDITORIA}/excel`, {
        params,
        responseType: "blob"
    });
    return response.data;
};

export const exportarAuditoriaPDF = async (params) => {
    const response = await axios.get(`${API_AUDITORIA}/pdf`, {
        params,
        responseType: "blob"
    });
    return response.data;
};
// FIN - Auditoría
