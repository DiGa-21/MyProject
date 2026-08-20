import { Redirect } from 'expo-router';

import { useAuth } from '../../features/auth/AuthContext';
import { parentRouteFor } from '../../features/auth/parentRouteDecision';
import { AuthLoadingScreen } from '../../screens/AuthLoadingScreen';

export default function ParentIndexRoute() {
  const { status } = useAuth();
  const target = parentRouteFor(status);

  if (!target) {
    return <AuthLoadingScreen />;
  }

  return <Redirect href={target} />;
}
