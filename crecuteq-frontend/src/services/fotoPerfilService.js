import axios from "axios";

const API = "/api/foto-perfil";

const configuracion = () => {
    const usuario = JSON.parse(localStorage.getItem("usuario") || "null");
    return { headers: { Authorization: `Bearer ${usuario?.token || ""}` } };
};

export const obtenerFotoPerfil = async () => {
    const response = await axios.get(API, { ...configuracion(), responseType: "blob" });
    return response.data;
};

export const guardarFotoPerfil = async (archivo) => {
    const datos = new FormData();
    datos.append("archivo", archivo);
    await axios.put(API, datos, configuracion());
};

export const eliminarFotoPerfil = async () => axios.delete(API, configuracion());
