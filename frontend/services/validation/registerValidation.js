export function validateRegister({ username, displayName, email, password, confirmPassword }) {
  const errors = {};
  if (!username || username.trim().length < 3) {
    errors.username = 'Usuario mínimo 3 caracteres';
  }
  if (!displayName || displayName.trim().length === 0) {
    errors.displayName = 'Nombre a mostrar requerido';
  }
  if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
    errors.email = 'Email inválido';
  }
  if (!password || password.length < 8) {
    errors.password = 'Contraseña mínimo 8 caracteres';
  }
  if (password !== confirmPassword) {
    errors.confirmPassword = 'Contraseñas no coinciden';
  }
  return errors;
}

export default { validateRegister };
