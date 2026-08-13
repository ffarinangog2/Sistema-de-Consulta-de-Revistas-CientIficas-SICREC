import { useEffect, useState } from "react";

const THEME_KEY = "crecuteq-theme";

const applyTheme = (theme) => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.setAttribute("data-bs-theme", theme);
};

function ThemeToggle({ floating = false }) {
    // INICIO - Selector persistente de tema
    const [theme, setTheme] = useState(() => localStorage.getItem(THEME_KEY) === "dark" ? "dark" : "light");

    useEffect(() => {
        applyTheme(theme);
        localStorage.setItem(THEME_KEY, theme);
        window.dispatchEvent(new CustomEvent("crecuteq-theme-change", { detail: theme }));
    }, [theme]);

    const nextTheme = theme === "light" ? "dark" : "light";

    return (
        <button
            type="button"
            className={`theme-toggle ${floating ? "theme-toggle--floating" : ""}`}
            onClick={() => setTheme(nextTheme)}
            aria-label={`Activar modo ${nextTheme === "dark" ? "oscuro" : "claro"}`}
            title={`Cambiar a modo ${nextTheme === "dark" ? "oscuro" : "claro"}`}
        >
            <span aria-hidden="true">{theme === "light" ? "🌙" : "☀️"}</span>
            <span className="theme-toggle__label d-none d-md-inline">{theme === "light" ? "Oscuro" : "Claro"}</span>
        </button>
    );
    // FIN - Selector persistente de tema
}

export default ThemeToggle;
