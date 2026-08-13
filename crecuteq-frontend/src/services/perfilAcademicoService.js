import axios from "axios";

// INICIO - Perfil Académico
const API = "/api/perfil-academico";

const obtenerConfiguracion = () => {
    const usuario = JSON.parse(localStorage.getItem("usuario") || "null");
    return {
        headers: {
            Authorization: `Bearer ${usuario?.token || ""}`
        }
    };
};

export const obtenerPerfilAcademico = async () => {
    const response = await axios.get(API, obtenerConfiguracion());
    return response.data;
};

export const crearPerfilAcademico = async (perfil) => {
    const response = await axios.post(API, perfil, obtenerConfiguracion());
    return response.data;
};

export const actualizarPerfilAcademico = async (perfil) => {
    const response = await axios.put(API, perfil, obtenerConfiguracion());
    return response.data;
};
// FIN - Perfil Académico
