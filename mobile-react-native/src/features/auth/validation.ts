import type {
  AuthFieldErrors,
  RegistrationInput,
  SignInInput,
  ValidationResult,
} from './types';

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function validateSignIn(
  email: string,
  password: string,
): ValidationResult<SignInInput> {
  const cleanEmail = email.trim().toLowerCase();
  const errors: AuthFieldErrors = {
    email: !cleanEmail
      ? 'Введите электронную почту'
      : !emailPattern.test(cleanEmail)
        ? 'Проверь адрес электронной почты'
        : undefined,
    password: !password
      ? 'Введите пароль'
      : password.length < 6
        ? 'Минимум 6 символов'
        : undefined,
  };

  if (errors.email || errors.password) {
    return { errors };
  }

  return {
    value: { email: cleanEmail, password },
    errors,
  };
}

export function validateRegistration(
  displayName: string,
  email: string,
  password: string,
  passwordRepeat: string,
): ValidationResult<RegistrationInput> {
  const signIn = validateSignIn(email, password);
  const cleanName = displayName.trim();
  const errors: AuthFieldErrors = {
    ...signIn.errors,
    displayName: cleanName ? undefined : 'Введите имя',
    passwordRepeat:
      password === passwordRepeat ? undefined : 'Пароли не совпадают',
  };

  if (
    errors.displayName ||
    errors.email ||
    errors.password ||
    errors.passwordRepeat
  ) {
    return { errors };
  }

  return {
    value: {
      displayName: cleanName,
      email: email.trim().toLowerCase(),
      password,
    },
    errors,
  };
}

export function validateNewPassword(
  password: string,
  passwordRepeat: string,
): ValidationResult<string> {
  const errors: AuthFieldErrors = {
    password: !password
      ? 'Введите новый пароль'
      : password.length < 6
        ? 'Минимум 6 символов'
        : undefined,
    passwordRepeat:
      password === passwordRepeat ? undefined : 'Пароли не совпадают',
  };

  if (errors.password || errors.passwordRepeat) {
    return { errors };
  }

  return { value: password, errors };
}
