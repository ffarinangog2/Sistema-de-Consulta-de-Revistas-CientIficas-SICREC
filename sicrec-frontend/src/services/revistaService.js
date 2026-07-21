import axios from "axios";

//const API_REVISTAS = "http://localhost:8080/api/revistas";
const API_REVISTAS = "/api/revistas";
//const API_FAVORITOS = "http://localhost:8080/api/favoritos";
const API_FAVORITOS = "/api/favoritos";
export const buscarRevistas = async (
    termino,
    cantidad = 25,
    usuarioId
) => {

    const response = await axios.get(
        `${API_REVISTAS}/buscar`,
        {
            params: {
                termino,
                cantidad,
                usuarioId
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