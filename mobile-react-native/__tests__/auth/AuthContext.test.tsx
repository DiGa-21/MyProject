import { act, fireEvent, render, waitFor } from '@testing-library/react-native';
import { Linking, Text, Pressable, View } from 'react-native';

import {
  AuthProvider,
  useAuth,
} from '../../src/features/auth/AuthContext';
import type {
  AuthGateway,
  AuthSnapshot,
  ParentUser,
} from '../../src/features/auth/types';

const parent: ParentUser = {
  id: 'parent-1',
  email: 'diana@example.com',
  displayName: 'Диана',
};

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

function makeGateway(restored: ParentUser | null = null) {
  let listener: ((snapshot: AuthSnapshot) => void) | undefined;
  const unsubscribe = jest.fn();
  const gateway: AuthGateway = {
    restoreSession: jest.fn().mockResolvedValue(restored),
    subscribe: jest.fn((next) => {
      listener = next;
      return unsubscribe;
    }),
    signUp: jest.fn().mockResolvedValue(parent),
    signIn: jest.fn().mockResolvedValue(parent),
    signOut: jest.fn().mockResolvedValue(undefined),
    sendPasswordReset: jest.fn().mockResolvedValue(undefined),
    consumeRecoveryUrl: jest.fn().mockResolvedValue(false),
    updatePassword: jest.fn().mockResolvedValue(parent),
  };
  return { gateway, unsubscribe, emit: (snapshot: AuthSnapshot) => listener?.(snapshot) };
}

function AuthProbe() {
  const auth = useAuth();
  return (
    <View>
      <Text>{auth.status}</Text>
      <Text>{auth.user?.displayName ?? 'no-user'}</Text>
      <Text>{auth.error ?? 'no-error'}</Text>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="sign-in"
        onPress={() =>
          void auth.signIn({ email: 'diana@example.com', password: '123456' })
        }>
        <Text>sign-in</Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="sign-up"
        onPress={() =>
          void auth.signUp({
            displayName: 'Диана',
            email: 'diana@example.com',
            password: '123456',
          })
        }>
        <Text>sign-up</Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="sign-out"
        onPress={() => void auth.signOut()}>
        <Text>sign-out</Text>
      </Pressable>
      <Pressable
        accessibilityRole="button"
        accessibilityLabel="reset"
        onPress={() => void auth.sendPasswordReset('diana@example.com')}>
        <Text>reset</Text>
      </Pressable>
    </View>
  );
}

describe('AuthProvider', () => {
  beforeEach(() => {
    jest.spyOn(Linking, 'getInitialURL').mockResolvedValue(null);
    jest.spyOn(Linking, 'addEventListener').mockReturnValue({
      remove: jest.fn(),
    } as never);
  });

  afterEach(() => jest.restoreAllMocks());

  it('stays initializing until session restoration finishes', async () => {
    const pending = deferred<ParentUser | null>();
    const fake = makeGateway();
    (fake.gateway.restoreSession as jest.Mock).mockReturnValueOnce(pending.promise);
    const screen = await render(
      <AuthProvider gateway={fake.gateway}>
        <AuthProbe />
      </AuthProvider>,
    );

    expect(screen.getByText('initializing')).toBeTruthy();
    pending.resolve(null);
    await waitFor(() => expect(screen.getByText('unauthenticated')).toBeTruthy());
  });

  it('restores an authenticated parent', async () => {
    const fake = makeGateway(parent);
    const screen = await render(
      <AuthProvider gateway={fake.gateway}>
        <AuthProbe />
      </AuthProvider>,
    );

    await waitFor(() => expect(screen.getByText('authenticated')).toBeTruthy());
    expect(screen.getByText('Диана')).toBeTruthy();
  });

  it('does not overwrite a newer auth event with stale restoration', async () => {
    const pending = deferred<ParentUser | null>();
    const fake = makeGateway();
    (fake.gateway.restoreSession as jest.Mock).mockReturnValueOnce(pending.promise);
    const screen = await render(
      <AuthProvider gateway={fake.gateway}>
        <AuthProbe />
      </AuthProvider>,
    );

    await act(async () => {
      fake.emit({ status: 'authenticated', user: parent });
    });
    expect(screen.getByText('authenticated')).toBeTruthy();

    pending.resolve(null);
    await act(async () => {
      await pending.promise;
    });

    expect(screen.getByText('authenticated')).toBeTruthy();
    expect(screen.getByText('Диана')).toBeTruthy();
  });

  it('supports sign-in, sign-up, and logout actions', async () => {
    const fake = makeGateway();
    const screen = await render(
      <AuthProvider gateway={fake.gateway}>
        <AuthProbe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByText('unauthenticated')).toBeTruthy());

    await fireEvent.press(screen.getByRole('button', { name: 'sign-in' }));
    await waitFor(() => expect(fake.gateway.signIn).toHaveBeenCalledTimes(1));
    expect(screen.getByText('authenticated')).toBeTruthy();

    await fireEvent.press(screen.getByRole('button', { name: 'sign-out' }));
    await waitFor(() => expect(fake.gateway.signOut).toHaveBeenCalledTimes(1));
    expect(screen.getByText('unauthenticated')).toBeTruthy();

    await fireEvent.press(screen.getByRole('button', { name: 'sign-up' }));
    await waitFor(() => expect(fake.gateway.signUp).toHaveBeenCalledTimes(1));
    expect(screen.getByText('authenticated')).toBeTruthy();
  });

  it('maps failed requests to a safe Russian message', async () => {
    const fake = makeGateway();
    (fake.gateway.signIn as jest.Mock).mockRejectedValueOnce(
      new Error('Invalid login credentials'),
    );
    const screen = await render(
      <AuthProvider gateway={fake.gateway}>
        <AuthProbe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByText('unauthenticated')).toBeTruthy());

    await fireEvent.press(screen.getByRole('button', { name: 'sign-in' }));
    await waitFor(() =>
      expect(screen.getByText('Неверная почта или пароль')).toBeTruthy(),
    );
  });

  it('uses the approved recovery redirect', async () => {
    const fake = makeGateway();
    const screen = await render(
      <AuthProvider gateway={fake.gateway}>
        <AuthProbe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByText('unauthenticated')).toBeTruthy());

    await fireEvent.press(screen.getByRole('button', { name: 'reset' }));
    await waitFor(() =>
      expect(fake.gateway.sendPasswordReset).toHaveBeenCalledWith(
        'diana@example.com',
        'myway://reset-password',
      ),
    );
  });

  it('consumes recovery link events and cleans subscriptions', async () => {
    let linkListener: ((event: { url: string }) => void) | undefined;
    const removeLinkListener = jest.fn();
    (Linking.addEventListener as jest.Mock).mockImplementationOnce(
      (_event, listener) => {
        linkListener = listener;
        return { remove: removeLinkListener };
      },
    );
    const fake = makeGateway();
    (fake.gateway.consumeRecoveryUrl as jest.Mock).mockResolvedValueOnce(true);
    const screen = await render(
      <AuthProvider gateway={fake.gateway}>
        <AuthProbe />
      </AuthProvider>,
    );
    await waitFor(() => expect(screen.getByText('unauthenticated')).toBeTruthy());

    await act(async () => {
      linkListener?.({ url: 'myway://reset-password?code=abc&type=recovery' });
    });
    await waitFor(() => expect(screen.getByText('recovery')).toBeTruthy());

    await screen.unmount();
    expect(fake.unsubscribe).toHaveBeenCalledTimes(1);
    expect(removeLinkListener).toHaveBeenCalledTimes(1);
  });
});
