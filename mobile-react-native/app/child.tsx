import { router } from 'expo-router';

import { PlaceholderModeScreen } from '../src/screens/PlaceholderModeScreen';

export default function ChildRoute() {
  return (
    <PlaceholderModeScreen
      message="Подключение по коду появится на следующем этапе"
      onBack={() => router.back()}
      title="Режим ребёнка"
    />
  );
}
