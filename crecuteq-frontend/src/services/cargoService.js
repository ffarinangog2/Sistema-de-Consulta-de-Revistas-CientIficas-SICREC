import axios from "axios";

//const API = "http://localhost:8080/api/cargos";
const API = "/api/cargos";

export const listarCargos = () =>
    axios.get(API);

export const crearCargo = (cargo) =>
    axios.post(API, cargo);

export const actualizarCargo = (id, cargo) =>
    axios.put(`${API}/${id}`, cargo);

export const eliminarCargo = (id) =>
    axios.delete(`${API}/${id}`);

export const obtenerCargos = async () => {

    const response = await axios.get(API);

    return response.data;

};