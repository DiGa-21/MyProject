import { fireEvent, render } from '@testing-library/react-native';

import { ParentCabinetScreen } from '../../src/screens/ParentCabinetScreen';

describe('ParentCabinetScreen', () => {
  it('shows the temporary protected cabinet and signs out', async () => {
    const onSignOut = jest.fn();
    const screen = await render(
      <ParentCabinetScreen
        onSignOut={onSignOut}
        user={{
          id: 'parent-1',
          displayName: 'Диана',
          email: 'diana@example.com',
        }}
      />,
    );

    expect(screen.getByText('Мой кабинет')).toBeTruthy();
    expect(screen.getByText('Диана')).toBeTruthy();
    expect(screen.getByText('diana@example.com')).toBeTruthy();
    expect(screen.getByText('Ты вошла в аккаунт')).toBeTruthy();

    await fireEvent.press(screen.getByRole('button', { name: 'Выйти' }));
    expect(onSignOut).toHaveBeenCalledTimes(1);
  });
});
