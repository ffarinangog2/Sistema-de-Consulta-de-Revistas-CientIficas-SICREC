// INICIO - Botones con estado de carga
function LoadingButton({ loading, loadingText, children, disabled, ...props }) {

    return (
        <button disabled={loading || disabled} {...props}>
            {loading && (
                <span
                    className="spinner-border spinner-border-sm me-2"
                    role="status"
                    aria-hidden="true"
                />
            )}
            {loading ? loadingText : children}
        </button>
    );

}

export default LoadingButton;
// FIN - Botones con estado de carga
