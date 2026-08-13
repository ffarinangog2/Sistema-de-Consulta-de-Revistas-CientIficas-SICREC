import axios from "axios";

//const API = "http://localhost:8080/api/perfil";
const API = "/api/perfil";

export const obtenerPerfil = async (idUsuario) => {

    const response = await axios.get(
        `${API}/${idUsuario}`
    );

    return response.data;

};