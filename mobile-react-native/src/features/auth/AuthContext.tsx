import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
} from 'react';
import { Linking } from 'react-native';

import { mapAuthError } from './errors';
import { readSupabasePublicConfig } from './config';
import { SupabaseAuthGateway } from './SupabaseAuthGateway';
import {
  createSupabaseClient,
  registerAuthAutoRefresh,
} from './supabaseClient';
import type {
  AuthGateway,
  AuthStatus,
  ParentUser,
  RegistrationInput,
  SignInInput,
} from './types';

const recoveryRedirect = 'myway://reset-password';

export type AuthContextValue = {
  status: AuthStatus;
  user: ParentUser | null;
  submitting: boolean;
  error: string | null;
  clearError(): void;
  signUp(input: RegistrationInput): Promise<boolean>;
  signIn(input: SignInInput): Promise<boolean>;
  signOut(): Promise<void>;
  sendPasswordReset(email: string): Promise<boolean>;
  updatePassword(password: string): Promise<boolean>;
};

type AuthState = Pick<
  AuthContextValue,
  'status' | 'user' | 'submitting' | 'error'
>;

type AuthAction =
  | { type: 'snapshot'; status: AuthStatus; user: ParentUser | null }
  | { type: 'submit' }
  | { type: 'failure'; error: string }
  | { type: 'clear-error' };

const initialState: AuthState = {
  status: 'initializing',
  user: null,
  submitting: false,
  error: null,
};

function authReducer(state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'snapshot':
      return {
        status: action.status,
        user: action.user,
        submitting: false,
        error: null,
      };
    case 'submit':
      return { ...state, submitting: true, error: null };
    case 'failure':
      return { ...state, submitting: false, error: action.error };
    case 'clear-error':
      return { ...state, error: null };
  }
}

function readableError(error: unknown): string {
  if (
    error instanceof Error &&
    error.message === 'Не настроено подключение к Supabase'
  ) {
    return error.message;
  }
  return mapAuthError(error);
}

type AuthDependency = {
  gateway: AuthGateway | null;
  cleanup: () => void;
  configurationError: string | null;
};

function createDefaultDependency(): AuthDependency {
  try {
    const client = createSupabaseClient(readSupabasePublicConfig());
    return {
      gateway: new SupabaseAuthGateway(client),
      cleanup: registerAuthAutoRefresh(client),
      configurationError: null,
    };
  } catch (error) {
    return {
      gateway: null,
      cleanup: () => undefined,
      configurationError: readableError(error),
    };
  }
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({
  children,
  gateway,
}: {
  children: ReactNode;
  gateway?: AuthGateway;
}) {
  const [state, dispatch] = useReducer(authReducer, initialState);
  const dependency = useMemo<AuthDependency>(
    () =>
      gateway
        ? { gateway, cleanup: () => undefined, configurationError: null }
        : createDefaultDependency(),
    [gateway],
  );

  useEffect(() => {
    let mounted = true;
    const activeGateway = dependency.gateway;

    if (!activeGateway) {
      dispatch({
        type: 'snapshot',
        status: 'unauthenticated',
        user: null,
      });
      if (dependency.configurationError) {
        dispatch({ type: 'failure', error: dependency.configurationError });
      }
      return dependency.cleanup;
    }

    let authEventVersion = 0;
    const restorationStartedAtVersion = authEventVersion;
    const unsubscribe = activeGateway.subscribe((snapshot) => {
      authEventVersion += 1;
      if (mounted) {
        dispatch({ type: 'snapshot', ...snapshot });
      }
    });

    const consumeLink = async (url: string) => {
      try {
        const consumed = await activeGateway.consumeRecoveryUrl(url);
        if (mounted && consumed) {
          dispatch({ type: 'snapshot', status: 'recovery', user: null });
        }
        return consumed;
      } catch (error) {
        if (mounted) {
          dispatch({ type: 'failure', error: readableError(error) });
        }
        return false;
      }
    };

    const linkSubscription = Linking.addEventListener('url', ({ url }) => {
      void consumeLink(url);
    });

    void (async () => {
      try {
        const restoredUser = await activeGateway.restoreSession();
        const initialUrl = await Linking.getInitialURL();
        const recovery = initialUrl ? await consumeLink(initialUrl) : false;
        if (
          mounted &&
          !recovery &&
          authEventVersion === restorationStartedAtVersion
        ) {
          dispatch({
            type: 'snapshot',
            status: restoredUser ? 'authenticated' : 'unauthenticated',
            user: restoredUser,
          });
        }
      } catch (error) {
        if (mounted) {
          dispatch({
            type: 'snapshot',
            status: 'unauthenticated',
            user: null,
          });
          dispatch({ type: 'failure', error: readableError(error) });
        }
      }
    })();

    return () => {
      mounted = false;
      unsubscribe();
      linkSubscription.remove();
      dependency.cleanup();
    };
  }, [dependency]);

  const runUserAction = useCallback(
    async (
      action: (activeGateway: AuthGateway) => Promise<ParentUser>,
    ): Promise<boolean> => {
      if (!dependency.gateway) {
        dispatch({
          type: 'failure',
          error:
            dependency.configurationError ??
            'Не настроено подключение к Supabase',
        });
        return false;
      }
      dispatch({ type: 'submit' });
      try {
        const user = await action(dependency.gateway);
        dispatch({ type: 'snapshot', status: 'authenticated', user });
        return true;
      } catch (error) {
        dispatch({ type: 'failure', error: readableError(error) });
        return false;
      }
    },
    [dependency],
  );

  const value = useMemo<AuthContextValue>(
    () => ({
      ...state,
      clearError: () => dispatch({ type: 'clear-error' }),
      signUp: (input) => runUserAction((active) => active.signUp(input)),
      signIn: (input) => runUserAction((active) => active.signIn(input)),
      signOut: async () => {
        if (!dependency.gateway) {
          return;
        }
        dispatch({ type: 'submit' });
        try {
          await dependency.gateway.signOut();
          dispatch({
            type: 'snapshot',
            status: 'unauthenticated',
            user: null,
          });
        } catch (error) {
          dispatch({ type: 'failure', error: readableError(error) });
        }
      },
      sendPasswordReset: async (email) => {
        if (!dependency.gateway) {
          dispatch({
            type: 'failure',
            error:
              dependency.configurationError ??
              'Не настроено подключение к Supabase',
          });
          return false;
        }
        dispatch({ type: 'submit' });
        try {
          await dependency.gateway.sendPasswordReset(email, recoveryRedirect);
          dispatch({ type: 'snapshot', status: state.status, user: state.user });
          return true;
        } catch (error) {
          dispatch({ type: 'failure', error: readableError(error) });
          return false;
        }
      },
      updatePassword: (password) =>
        runUserAction((active) => active.updatePassword(password)),
    }),
    [dependency, runUserAction, state],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error('useAuth должен использоваться внутри AuthProvider');
  }
  return value;
}
