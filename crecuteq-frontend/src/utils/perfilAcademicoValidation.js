export const ORCID_PATTERN = /^https:\/\/orcid\.org\/\d{4}-\d{4}-\d{4}-\d{3}[\dX]\/?$/;
export const GOOGLE_SCHOLAR_PATTERN = /^https:\/\/scholar\.google\.com\/.+$/;

export const orcidValido = (valor) => !valor.trim() || ORCID_PATTERN.test(valor.trim());
export const googleScholarValido = (valor) => !valor.trim() || GOOGLE_SCHOLAR_PATTERN.test(valor.trim());
