import { fireEvent, render, waitFor } from '@testing-library/react-native';

import { ResetPasswordScreen } from '../../src/screens/ResetPasswordScreen';

describe('ResetPasswordScreen', () => {
  it('validates six matching characters before submission', async () => {
    const onSubmit = jest.fn().mockResolvedValue(true);
    const screen = await render(
      <ResetPasswordScreen error={null} onSubmit={onSubmit} submitting={false} />,
    );

    await fireEvent.changeText(screen.getByLabelText('Новый пароль'), '12345');
    await fireEvent.changeText(screen.getByLabelText('Повтори новый пароль'), '54321');
    await fireEvent.press(
      screen.getByRole('button', { name: 'Сохранить новый пароль' }),
    );

    expect(screen.getByText('Минимум 6 символов')).toBeTruthy();
    expect(screen.getByText('Пароли не совпадают')).toBeTruthy();
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('submits a valid new password once', async () => {
    const onSubmit = jest.fn().mockResolvedValue(true);
    const screen = await render(
      <ResetPasswordScreen error={null} onSubmit={onSubmit} submitting={false} />,
    );

    await fireEvent.changeText(screen.getByLabelText('Новый пароль'), '123456');
    await fireEvent.changeText(
      screen.getByLabelText('Повтори новый пароль'),
      '123456',
    );
    await fireEvent.press(
      screen.getByRole('button', { name: 'Сохранить новый пароль' }),
    );

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith('123456'));
  });

  it('shows request errors and disables loading submission', async () => {
    const screen = await render(
      <ResetPasswordScreen
        error="Не удалось обновить пароль"
        onSubmit={jest.fn()}
        submitting
      />,
    );

    expect(screen.getByText('Не удалось обновить пароль')).toBeTruthy();
    expect(
      screen.getByRole('button', { name: 'Сохранить новый пароль' }).props
        .accessibilityState,
    ).toEqual({ disabled: true });
  });
});
