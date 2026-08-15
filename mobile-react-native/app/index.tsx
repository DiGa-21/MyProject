import { router } from 'expo-router';

import { HomeScreen } from '../src/screens/HomeScreen';

export default function HomeRoute() {
  return (
    <HomeScreen
      onOpenChild={() => router.push('/child')}
      onOpenParent={() => router.push('/parent')}
    />
  );
}
