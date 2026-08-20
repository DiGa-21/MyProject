import type { AuthStatus } from './types';

export type ParentRoute =
  | '/parent/auth'
  | '/parent/cabinet'
  | '/parent/reset-password';

export function parentRouteFor(status: AuthStatus): ParentRoute | null {
  switch (status) {
    case 'initializing':
      return null;
    case 'unauthenticated':
      return '/parent/auth';
    case 'authenticated':
      return '/parent/cabinet';
    case 'recovery':
      return '/parent/reset-password';
  }
}
