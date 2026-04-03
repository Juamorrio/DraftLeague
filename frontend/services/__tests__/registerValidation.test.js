import { validateRegister } from '../validation/registerValidation';

describe('validateRegister', () => {
  const validInput = {
    username: 'alice',
    displayName: 'Alice Smith',
    email: 'alice@example.com',
    password: 'secret123',
    confirmPassword: 'secret123',
  };

  test('input válido → objeto de errores vacío', () => {
    const errors = validateRegister(validInput);
    expect(Object.keys(errors)).toHaveLength(0);
  });

  test('username vacío → error en username', () => {
    const errors = validateRegister({ ...validInput, username: '' });
    expect(errors.username).toBeDefined();
  });

  test('username con menos de 3 caracteres → error en username', () => {
    const errors = validateRegister({ ...validInput, username: 'ab' });
    expect(errors.username).toBeDefined();
  });

  test('username con exactamente 3 caracteres → sin error en username', () => {
    const errors = validateRegister({ ...validInput, username: 'abc' });
    expect(errors.username).toBeUndefined();
  });

  test('displayName vacío → error en displayName', () => {
    const errors = validateRegister({ ...validInput, displayName: '' });
    expect(errors.displayName).toBeDefined();
  });

  test('displayName solo espacios → error en displayName', () => {
    const errors = validateRegister({ ...validInput, displayName: '   ' });
    expect(errors.displayName).toBeDefined();
  });

  test('email sin @ → error en email', () => {
    const errors = validateRegister({ ...validInput, email: 'notanemail' });
    expect(errors.email).toBeDefined();
  });

  test('email con formato válido → sin error en email', () => {
    const errors = validateRegister({ ...validInput, email: 'user@domain.com' });
    expect(errors.email).toBeUndefined();
  });

  test('password con menos de 8 caracteres → error en password', () => {
    const errors = validateRegister({ ...validInput, password: '1234567', confirmPassword: '1234567' });
    expect(errors.password).toBeDefined();
  });

  test('password con exactamente 8 caracteres → sin error en password', () => {
    const errors = validateRegister({ ...validInput, password: '12345678', confirmPassword: '12345678' });
    expect(errors.password).toBeUndefined();
  });

  test('confirmPassword distinto de password → error en confirmPassword', () => {
    const errors = validateRegister({ ...validInput, confirmPassword: 'otherpass' });
    expect(errors.confirmPassword).toBeDefined();
  });

  test('todos los campos inválidos → 5 claves de error presentes', () => {
    const errors = validateRegister({
      username: '',
      displayName: '',
      email: 'bademail',
      password: '123',
      confirmPassword: 'different',
    });
    expect(errors.username).toBeDefined();
    expect(errors.displayName).toBeDefined();
    expect(errors.email).toBeDefined();
    expect(errors.password).toBeDefined();
    expect(errors.confirmPassword).toBeDefined();
  });
});
