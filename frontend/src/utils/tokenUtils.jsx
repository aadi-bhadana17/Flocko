export const isTokenExpired = (expiresAt) => {
    if (!expiresAt) return true;
    // expiresAt is a long (milliseconds) from Java
    return Date.now() > expiresAt;
};