import axios from "axios";

//const API_DASHBOARD = "http://localhost:8080/api/dashboard";
const API_DASHBOARD = "/api/dashboard";
export const obtenerDashboard = async () => {

    const response = await axios.get(API_DASHBOARD);

    return response.data;

};