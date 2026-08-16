import { fireEvent, render, waitFor } from '@testing-library/react-native';
import { Linking } from 'react-native';

import { AuthProvider } from '../../src/features/auth/AuthContext';
import type { AuthGateway, ParentUser } from '../../src/features/auth/types';
import { ParentAuthScreen } from '../../src/screens/ParentAuthScreen';

const parent: ParentUser = {
  id: 'parent-1',
  email: 'diana@example.com',
  displayName: 'Диана',
};

function makeGateway(): AuthGateway {
  return {
    restoreSession: jest.fn().mockResolvedValue(null),
    subscribe: jest.fn().mockReturnValue(jest.fn()),
    signUp: jest.fn().mockResolvedValue(parent),
    signIn: jest.fn().mockResolvedValue(parent),
    signOut: jest.fn().mockResolvedValue(undefined),
    sendPasswordReset: jest.fn().mockResolvedValue(undefined),
    consumeRecoveryUrl: jest.fn().mockResolvedValue(false),
    updatePassword: jest.fn().mockResolvedValue(parent),
  };
}

async function renderScreen(gateway = makeGateway(), onBack = jest.fn()) {
  const screen = await render(
    <AuthProvider gateway={gateway}>
      <ParentAuthScreen onBack={onBack} />
    </AuthProvider>,
  );
  await waitFor(() => expect(gateway.restoreSession).toHaveBeenCalledTimes(1));
  return { screen, gateway, onBack };
}

describe('ParentAuthScreen', () => {
  beforeEach(() => {
    jest.spyOn(Linking, 'getInitialURL').mockResolvedValue(null);
    jest.spyOn(Linking, 'addEventListener').mockReturnValue({
      remove: jest.fn(),
    } as never);
  });

  afterEach(() => jest.restoreAllMocks());

  it('shows the approved sign-in layout', async () => {
    const { screen } = await renderScreen();

    expect(screen.getByText('Кабинет родителя')).toBeTruthy();
    expect(screen.getByText('Войди, чтобы управлять делами семьи')).toBeTruthy();
    expect(screen.getByLabelText('Помощник родителя')).toBeTruthy();
    expect(screen.getByLabelText('Электронная почта')).toBeTruthy();
    expect(screen.getByLabelText('Пароль')).toBeTruthy();
  });

  it('switches to registration with all required fields', async () => {
    const { screen } = await renderScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Регистрация' }));

    expect(screen.getByLabelText('Имя')).toBeTruthy();
    expect(screen.getByLabelText('Электронная почта')).toBeTruthy();
    expect(screen.getByLabelText('Пароль')).toBeTruthy();
    expect(screen.getByLabelText('Повтори пароль')).toBeTruthy();
  });

  it('shows local errors without calling Supabase', async () => {
    const { screen, gateway } = await renderScreen();

    await fireEvent.press(screen.getByRole('button', { name: 'Войти' }));

    expect(screen.getByText('Введите электронную почту')).toBeTruthy();
    expect(screen.getByText('Введите пароль')).toBeTruthy();
    expect(gateway.signIn).not.toHaveBeenCalled();
  });

  it('toggles password visibility and submits valid sign-in', async () => {
    const { screen, gateway } = await renderScreen();
    const email = screen.getByLabelText('Электронная почта');
    const password = screen.getByLabelText('Пароль');

    expect(password.props.secureTextEntry).toBe(true);
    await fireEvent.press(
      screen.getByRole('button', { name: 'Показать пароль' }),
    );
    expect(screen.getByLabelText('Пароль').props.secureTextEntry).toBe(false);

    await fireEvent.changeText(email, ' Diana@Example.COM ');
    await fireEvent.changeText(password, '123456');
    await fireEvent.press(screen.getByRole('button', { name: 'Войти' }));

    await waitFor(() =>
      expect(gateway.signIn).toHaveBeenCalledWith({
        email: 'diana@example.com',
        password: '123456',
      }),
    );
  });

  it('keeps the submit button named while sign-in is loading', async () => {
    const gateway = makeGateway();
    const pending = new Promise<ParentUser>(() => undefined);
    (gateway.signIn as jest.Mock).mockReturnValueOnce(pending);
    const { screen } = await renderScreen(gateway);

    await fireEvent.changeText(
      screen.getByLabelText('Электронная почта'),
      'diana@example.com',
    );
    await fireEvent.changeText(screen.getByLabelText('Пароль'), '123456');
    await fireEvent.press(screen.getByRole('button', { name: 'Войти' }));

    expect(screen.getByRole('button', { name: 'Войти' }).props.accessibilityState)
      .toEqual({ disabled: true });
  });

  it('sends a neutral password reset response', async () => {
    const { screen, gateway } = await renderScreen();

    await fireEvent.press(
      screen.getByRole('button', { name: 'Забыли пароль?' }),
    );
    await fireEvent.changeText(
      screen.getByLabelText('Электронная почта'),
      'diana@example.com',
    );
    await fireEvent.press(
      screen.getByRole('button', { name: 'Отправить письмо' }),
    );

    await waitFor(() =>
      expect(screen.getByText(
        'Если аккаунт с такой почтой существует, мы отправили письмо',
      )).toBeTruthy(),
    );
    expect(gateway.sendPasswordReset).toHaveBeenCalledTimes(1);
  });

  it('calls the back action', async () => {
    const onBack = jest.fn();
    const { screen } = await renderScreen(makeGateway(), onBack);

    await fireEvent.press(screen.getByRole('button', { name: 'Назад' }));

    expect(onBack).toHaveBeenCalledTimes(1);
  });
});
