import { fireEvent, render } from '@testing-library/react-native';
import { HomeScreen } from '../src/screens/HomeScreen';

describe('HomeScreen', () => {
  it('shows both modes and opens them', async () => {
    const onOpenChild = jest.fn();
    const onOpenParent = jest.fn();
    const screen = await render(
      <HomeScreen onOpenChild={onOpenChild} onOpenParent={onOpenParent} />,
    );

    screen.getByText('Мои домашние дела');
    screen.getByText('Режим ребёнка');
    screen.getByText('Режим родителя');

    await fireEvent.press(screen.getByRole('button', { name: 'Открыть режим ребёнка' }));
    await fireEvent.press(screen.getByRole('button', { name: 'Открыть режим родителя' }));

    expect(onOpenChild).toHaveBeenCalledTimes(1);
    expect(onOpenParent).toHaveBeenCalledTimes(1);
  });
});
