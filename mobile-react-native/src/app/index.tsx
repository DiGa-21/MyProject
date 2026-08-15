import { router } from 'expo-router';

import { HomeScreen } from '../screens/HomeScreen';

export default function HomeRoute() {
  return (
    <HomeScreen
      onOpenChild={() => router.push('/child')}
      onOpenParent={() => router.push('/parent')}
    />
  );
}
