import { router } from 'expo-router';

import { PlaceholderModeScreen } from '../screens/PlaceholderModeScreen';

export default function ParentRoute() {
  return (
    <PlaceholderModeScreen
      message="Вход и регистрация появятся на следующем этапе"
      onBack={() => router.back()}
      title="Режим родителя"
    />
  );
}
