import {
  validateNewPassword,
  validateRegistration,
  validateSignIn,
} from '../../src/features/auth/validation';

describe('auth validation', () => {
  it('normalizes a valid sign-in', () => {
    expect(validateSignIn(' Diana@Example.COM ', '123456').value).toEqual({
      email: 'diana@example.com',
      password: '123456',
    });
  });

  it.each([
    ['', 'Введите электронную почту'],
    ['not-an-email', 'Проверь адрес электронной почты'],
  ])('validates email %p', (email, expected) => {
    expect(validateSignIn(email, '123456').errors.email).toBe(expected);
  });

  it.each([
    ['', 'Введите пароль'],
    ['12345', 'Минимум 6 символов'],
  ])('validates password %p', (password, expected) => {
    expect(validateSignIn('diana@example.com', password).errors.password).toBe(
      expected,
    );
  });

  it('validates registration name and matching passwords', () => {
    const invalid = validateRegistration(
      '  ',
      'diana@example.com',
      '123456',
      '654321',
    );
    expect(invalid.errors.displayName).toBe('Введите имя');
    expect(invalid.errors.passwordRepeat).toBe('Пароли не совпадают');

    expect(
      validateRegistration(
        '  Диана  ',
        ' Diana@Example.COM ',
        '123456',
        '123456',
      ).value,
    ).toEqual({
      displayName: 'Диана',
      email: 'diana@example.com',
      password: '123456',
    });
  });

  it('validates a new password and its repetition', () => {
    expect(validateNewPassword('', '').errors.password).toBe(
      'Введите новый пароль',
    );
    expect(validateNewPassword('12345', '12345').errors.password).toBe(
      'Минимум 6 символов',
    );
    expect(validateNewPassword('123456', '654321').errors.passwordRepeat).toBe(
      'Пароли не совпадают',
    );
    expect(validateNewPassword('123456', '123456').value).toBe('123456');
  });
});
