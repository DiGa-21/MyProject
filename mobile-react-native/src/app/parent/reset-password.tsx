import { Redirect } from 'expo-router';

import { useAuth } from '../../features/auth/AuthContext';
import { AuthLoadingScreen } from '../../screens/AuthLoadingScreen';
import { ResetPasswordScreen } from '../../screens/ResetPasswordScreen';

export default function ParentResetPasswordRoute() {
  const { status, submitting, error, updatePassword } = useAuth();

  if (status === 'initializing') {
    return <AuthLoadingScreen />;
  }
  if (status !== 'recovery') {
    return <Redirect href="/parent" />;
  }

  return (
    <ResetPasswordScreen
      error={error}
      onSubmit={updatePassword}
      submitting={submitting}
    />
  );
}
