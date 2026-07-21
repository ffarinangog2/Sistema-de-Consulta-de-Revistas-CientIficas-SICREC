import axios from "axios";

//const API = "http://localhost:8080/api/dashboard-usuario";
const API = "/api/dashboard-usuario";
export const obtenerDashboardUsuario = async (idUsuario) => {

    const response = await axios.get(
        `${API}/${idUsuario}`
    );

    return response.data;

};