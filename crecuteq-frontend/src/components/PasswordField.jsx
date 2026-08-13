// INICIO - Botón mostrar contraseña
import { useState } from "react";

// INICIO - Validaciones visuales
const passwordRules = [
    { label: "Mínimo 8 caracteres", validate: (value) => value.length >= 8 },
    { label: "Al menos una mayúscula", validate: (value) => /[A-Z]/.test(value) },
    { label: "Al menos una minúscula", validate: (value) => /[a-z]/.test(value) },
    { label: "Al menos un número", validate: (value) => /\d/.test(value) }
];
// FIN - Validaciones visuales

function PasswordField({
    id,
    label,
    value,
    onChange,
    showValidation = true,
    className = "mb-3",
    ...inputProps
}) {

    const [visible, setVisible] = useState(false);

    return (
        <div className={className}>
            <label className="form-label" htmlFor={id}>{label}</label>

            <div className="input-group">
                <input
                    id={id}
                    type={visible ? "text" : "password"}
                    className="form-control"
                    value={value}
                    onChange={onChange}
                    {...inputProps}
                />
                <button
                    type="button"
                    className="btn btn-outline-secondary"
                    onClick={() => setVisible((current) => !current)}
                    aria-label={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
                    title={visible ? "Ocultar contraseña" : "Mostrar contraseña"}
                >
                    👁
                </button>
            </div>

            {/* INICIO - Validaciones visuales */}
            {showValidation && value && (
                <ul className="list-unstyled small mt-2 mb-0">
                    {passwordRules.map((rule) => {
                        const valid = rule.validate(value);
                        return (
                            <li key={rule.label} className={valid ? "text-success" : "text-danger"}>
                                {valid ? "✓" : "✗"} {rule.label}
                            </li>
                        );
                    })}
                </ul>
            )}
            {/* FIN - Validaciones visuales */}
        </div>
    );

}

export default PasswordField;
// FIN - Botón mostrar contraseña
