import axios from "axios";

//const API = "http://localhost:8080/api/roles";
const API = "/api/roles";
export const listarRoles = () =>
    axios.get(API);

export const crearRol = (rol) =>
    axios.post(API, rol);

export const actualizarRol = (id, rol) =>
    axios.put(`${API}/${id}`, rol);

export const eliminarRol = (id) =>
    axios.delete(`${API}/${id}`);