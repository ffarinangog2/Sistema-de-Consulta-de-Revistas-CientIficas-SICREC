import axios from "axios";

//const API_REVISTAS = "http://localhost:8080/api/revistas";
const API_REVISTAS = "/api/revistas";
//const API_FAVORITOS = "http://localhost:8080/api/favoritos";
const API_FAVORITOS = "/api/favoritos";
export const buscarRevistas = async (
    termino,
    cantidad = 25,
    usuarioId,
    facultad,
    campoEstudio
) => {

    const response = await axios.get(
        `${API_REVISTAS}/buscar`,
        {
            params: {
                termino,
                cantidad,
                usuarioId,
                ...(facultad ? { facultad } : {}),
                ...(facultad && campoEstudio ? { campoEstudio } : {})
            }
        }
    );

    return response.data;

};

export const guardarFavorito = async (favorito) => {

    const response = await axios.post(
        API_FAVORITOS,
        favorito
    );

    return response.data;

};

export const obtenerFavoritos = async (usuarioId) => {

    const response = await axios.get(
        `${API_FAVORITOS}/${usuarioId}`
    );

    return response.data;

};

export const eliminarFavorito = async (id) => {

    await axios.delete(
        `${API_FAVORITOS}/${id}`
    );

};

//const API_HISTORIAL = "http://localhost:8080/api/historial";
const API_HISTORIAL = "/api/historial";
export const obtenerHistorial = async (usuarioId) => {

    const response = await axios.get(
        `${API_HISTORIAL}/${usuarioId}`
    );

    return response.data;

};
export const eliminarHistorial = async (id) => {

    await axios.delete(
        `${API_HISTORIAL}/${id}`
    );

};

//const API_DASHBOARD = "http://localhost:8080/api/dashboard";
const API_DASHBOARD = "/api/dashboard";

export const obtenerDashboard = async () => {

    const response = await axios.get(API_DASHBOARD);

    return response.data;

};

export const buscarPaginaRevistasCampo = async (
    facultad,
    campoEstudio,
    usuarioId,
    pagina = 1,
    signal
) => {
    const count = 25;
    const response = await axios.get(`${API_REVISTAS}/buscar-pagina-campo`, {
        params: {
            facultad,
            campoEstudio,
            usuarioId,
            start: (pagina - 1) * count
        },
        signal
    });
    return response.data;
};

export const buscarTodasRevistasCampo = async (
    facultad,
    campoEstudio,
    usuarioId,
    signal
) => {
    const response = await axios.get(`${API_REVISTAS}/buscar-todas-campo`, {
        params: { facultad, campoEstudio, usuarioId },
        signal
    });
    return response.data;
};

export const exportarRevistasExcel = async (revistas) => {
    const response = await axios.post(
        `${API_REVISTAS}/exportar-excel`,
        revistas,
        { responseType: "blob" }
    );
    return response.data;
};

export const obtenerDetalleComplementarioRevista = async (
    revista,
    usuarioId,
    signal
) => {
    const response = await axios.get(`${API_REVISTAS}/detalle-complementario`, {
        params: {
            usuarioId,
            ...(revista?.sourceId ? { sourceId: revista.sourceId } : {}),
            ...(revista?.issn ? { issn: revista.issn } : {}),
            ...(revista?.eIssn ? { eIssn: revista.eIssn } : {}),
            ...(revista?.scopus?.publisher
                ? { editorial: revista.scopus.publisher }
                : {})
        },
        signal
    });
    return response.data;
};

export const obtenerCamposEstudioFacultad = async (facultad) => {
    if (!facultad) return [];
    const response = await axios.get(
        `${API_REVISTAS}/facultades/${encodeURIComponent(facultad)}/campos-estudio`
    );
    return response.data;
};

export const obtenerFacultades = async () => {
    const response = await axios.get(`${API_REVISTAS}/facultades`);
    return response.data;
};
