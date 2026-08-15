import { fireEvent, render } from '@testing-library/react-native';
import { PlaceholderModeScreen } from '../src/screens/PlaceholderModeScreen';

describe('PlaceholderModeScreen', () => {
  it('shows child copy and returns back', async () => {
    const onBack = jest.fn();
    const screen = await render(
      <PlaceholderModeScreen
        title="Режим ребёнка"
        message="Подключение по коду появится на следующем этапе"
        onBack={onBack}
      />,
    );

    screen.getByText('Режим ребёнка');
    screen.getByText('Подключение по коду появится на следующем этапе');
    fireEvent.press(screen.getByRole('button', { name: 'Назад' }));
    expect(onBack).toHaveBeenCalledTimes(1);
  });
});
