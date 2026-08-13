import { useState } from "react";
import Sidebar from "../components/Sidebar";
import Navbar from "../components/Navbar";

function MainLayout({ admin, children }) {

    // INICIO - Responsividad
    const [sidebarAbierto, setSidebarAbierto] = useState(false);
    // FIN - Responsividad

    return (

        // INICIO - Ajuste Responsive
        <div className={`main-layout ${admin ? "main-layout--admin" : "main-layout--user"}`}>

            <Sidebar admin={admin} abierto={sidebarAbierto} onCerrar={() => setSidebarAbierto(false)} />

            <div className="main-layout__content">

                <Navbar onAbrirSidebar={() => setSidebarAbierto(true)} />

                <main className="main-layout__page">

                    {children}

                </main>

            </div>

        </div>
        // FIN - Ajuste Responsive

    );

}

export default MainLayout;
