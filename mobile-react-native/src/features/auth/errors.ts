const genericAuthError = 'Не удалось выполнить действие. Попробуй ещё раз';

export function mapAuthError(error: unknown): string {
  const source = error instanceof Error ? error.message.toLowerCase() : '';

  if (
    source.includes('invalid login credentials') ||
    source.includes('invalid credentials')
  ) {
    return 'Неверная почта или пароль';
  }

  if (source.includes('already registered') || source.includes('already exists')) {
    return 'Аккаунт с такой почтой уже существует';
  }

  if (source.includes('too many requests') || source.includes('rate limit')) {
    return 'Слишком много попыток. Попробуй немного позже';
  }

  if (
    source.includes('unable to resolve host') ||
    source.includes('network') ||
    source.includes('timeout') ||
    source.includes('failed to connect')
  ) {
    return 'Нет связи с интернетом. Попробуй ещё раз';
  }

  return genericAuthError;
}
