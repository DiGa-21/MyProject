import { mapAuthError } from '../../src/features/auth/errors';

describe('mapAuthError', () => {
  it.each([
    ['Invalid login credentials', 'Неверная почта или пароль'],
    ['User already registered', 'Аккаунт с такой почтой уже существует'],
    ['Network request failed', 'Нет связи с интернетом. Попробуй ещё раз'],
    ['request timeout', 'Нет связи с интернетом. Попробуй ещё раз'],
    ['rate limit exceeded', 'Слишком много попыток. Попробуй немного позже'],
  ])('maps %s', (source, expected) => {
    expect(mapAuthError(new Error(source))).toBe(expected);
  });

  it('does not expose an unknown technical message', () => {
    expect(mapAuthError(new Error('internal detail'))).toBe(
      'Не удалось выполнить действие. Попробуй ещё раз',
    );
  });
});
