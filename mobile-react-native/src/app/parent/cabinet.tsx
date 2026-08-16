import { Redirect } from 'expo-router';

import { useAuth } from '../../features/auth/AuthContext';
import { AuthLoadingScreen } from '../../screens/AuthLoadingScreen';
import { ParentCabinetScreen } from '../../screens/ParentCabinetScreen';

export default function ParentCabinetRoute() {
  const { status, user, signOut } = useAuth();

  if (status === 'initializing') {
    return <AuthLoadingScreen />;
  }
  if (status !== 'authenticated' || !user) {
    return <Redirect href="/parent" />;
  }

  return <ParentCabinetScreen onSignOut={() => void signOut()} user={user} />;
}
