import { Redirect, router } from 'expo-router';

import { useAuth } from '../../features/auth/AuthContext';
import { AuthLoadingScreen } from '../../screens/AuthLoadingScreen';
import { ParentAuthScreen } from '../../screens/ParentAuthScreen';

export default function ParentAuthRoute() {
  const { status } = useAuth();

  if (status === 'initializing') {
    return <AuthLoadingScreen />;
  }
  if (status !== 'unauthenticated') {
    return <Redirect href="/parent" />;
  }

  return <ParentAuthScreen onBack={() => router.replace('/')} />;
}
