import { SupabaseAuthGateway } from '../../src/features/auth/SupabaseAuthGateway';

const user = {
  id: 'parent-1',
  email: 'diana@example.com',
  user_metadata: { display_name: 'Диана' },
};

const session = { user };

function makeFakeClient(profileName: string | null = null) {
  const unsubscribe = jest.fn();
  const profileQuery = {
    select: jest.fn(),
    eq: jest.fn(),
    maybeSingle: jest.fn().mockResolvedValue({
      data: profileName ? { display_name: profileName } : null,
      error: null,
    }),
  };
  profileQuery.select.mockReturnValue(profileQuery);
  profileQuery.eq.mockReturnValue(profileQuery);

  const auth = {
    getSession: jest.fn().mockResolvedValue({ data: { session }, error: null }),
    onAuthStateChange: jest.fn().mockReturnValue({
      data: { subscription: { unsubscribe } },
    }),
    signUp: jest.fn().mockResolvedValue({
      data: { user, session },
      error: null,
    }),
    signInWithPassword: jest.fn().mockResolvedValue({
      data: { user, session },
      error: null,
    }),
    signOut: jest.fn().mockResolvedValue({ error: null }),
    resetPasswordForEmail: jest.fn().mockResolvedValue({ error: null }),
    setSession: jest.fn().mockResolvedValue({ data: { user, session }, error: null }),
    exchangeCodeForSession: jest
      .fn()
      .mockResolvedValue({ data: { user, session }, error: null }),
    updateUser: jest.fn().mockResolvedValue({ data: { user }, error: null }),
  };

  return {
    client: {
      auth,
      from: jest.fn().mockReturnValue(profileQuery),
    },
    auth,
    profileQuery,
    unsubscribe,
  };
}

describe('SupabaseAuthGateway', () => {
  it('registers a parent with display_name metadata', async () => {
    const fake = makeFakeClient();
    const gateway = new SupabaseAuthGateway(fake.client as never);

    await expect(
      gateway.signUp({
        displayName: 'Диана',
        email: 'diana@example.com',
        password: '123456',
      }),
    ).resolves.toEqual({
      id: 'parent-1',
      email: 'diana@example.com',
      displayName: 'Диана',
    });
    expect(fake.auth.signUp).toHaveBeenCalledWith({
      email: 'diana@example.com',
      password: '123456',
      options: { data: { display_name: 'Диана' } },
    });
  });

  it('rejects registration without a session', async () => {
    const fake = makeFakeClient();
    fake.auth.signUp.mockResolvedValueOnce({
      data: { user, session: null },
      error: null,
    } as never);
    const gateway = new SupabaseAuthGateway(fake.client as never);

    await expect(
      gateway.signUp({
        displayName: 'Диана',
        email: 'diana@example.com',
        password: '123456',
      }),
    ).rejects.toThrow('Отключи подтверждение почты в настройках Supabase');
  });

  it('loads a profile name for a restored session', async () => {
    const fake = makeFakeClient('Диана из профиля');
    const gateway = new SupabaseAuthGateway(fake.client as never);

    await expect(gateway.restoreSession()).resolves.toEqual({
      id: 'parent-1',
      email: 'diana@example.com',
      displayName: 'Диана из профиля',
    });
    expect(fake.client.from).toHaveBeenCalledWith('profiles');
    expect(fake.profileQuery.select).toHaveBeenCalledWith('display_name');
    expect(fake.profileQuery.eq).toHaveBeenCalledWith('id', 'parent-1');
  });

  it('signs in, signs out, and sends the exact reset redirect', async () => {
    const fake = makeFakeClient();
    const gateway = new SupabaseAuthGateway(fake.client as never);

    await gateway.signIn({ email: 'diana@example.com', password: '123456' });
    await gateway.sendPasswordReset(
      'diana@example.com',
      'myway://reset-password',
    );
    await gateway.signOut();

    expect(fake.auth.signInWithPassword).toHaveBeenCalledWith({
      email: 'diana@example.com',
      password: '123456',
    });
    expect(fake.auth.resetPasswordForEmail).toHaveBeenCalledWith(
      'diana@example.com',
      { redirectTo: 'myway://reset-password' },
    );
    expect(fake.auth.signOut).toHaveBeenCalledTimes(1);
  });

  it('consumes token and PKCE recovery links', async () => {
    const fake = makeFakeClient();
    const gateway = new SupabaseAuthGateway(fake.client as never);

    await expect(
      gateway.consumeRecoveryUrl(
        'myway://reset-password#access_token=a&refresh_token=r&type=recovery',
      ),
    ).resolves.toBe(true);
    await expect(
      gateway.consumeRecoveryUrl(
        'myway://reset-password?code=abc&type=recovery',
      ),
    ).resolves.toBe(true);
    await expect(gateway.consumeRecoveryUrl('myway://parent')).resolves.toBe(
      false,
    );

    expect(fake.auth.setSession).toHaveBeenCalledWith({
      access_token: 'a',
      refresh_token: 'r',
    });
    expect(fake.auth.exchangeCodeForSession).toHaveBeenCalledWith('abc');
  });

  it('updates the password and unsubscribes auth listeners', async () => {
    const fake = makeFakeClient();
    const gateway = new SupabaseAuthGateway(fake.client as never);
    const stop = gateway.subscribe(jest.fn());

    await expect(gateway.updatePassword('654321')).resolves.toEqual({
      id: 'parent-1',
      email: 'diana@example.com',
      displayName: 'Диана',
    });
    stop();

    expect(fake.auth.updateUser).toHaveBeenCalledWith({ password: '654321' });
    expect(fake.unsubscribe).toHaveBeenCalledTimes(1);
  });
});
