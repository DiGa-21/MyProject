import type {
  AuthChangeEvent,
  Session,
  SupabaseClient,
  User,
} from '@supabase/supabase-js';

import { parseRecoveryUrl } from './recoveryUrl';
import type {
  AuthGateway,
  AuthSnapshot,
  ParentUser,
  RegistrationInput,
  SignInInput,
} from './types';

const emailConfirmationConfigurationError =
  'Отключи подтверждение почты в настройках Supabase';

export class SupabaseAuthGateway implements AuthGateway {
  constructor(private readonly client: SupabaseClient) {}

  async restoreSession(): Promise<ParentUser | null> {
    const { data, error } = await this.client.auth.getSession();
    if (error) {
      throw error;
    }
    return data.session ? this.mapSession(data.session) : null;
  }

  subscribe(listener: (snapshot: AuthSnapshot) => void): () => void {
    const { data } = this.client.auth.onAuthStateChange(
      (event: AuthChangeEvent, session: Session | null) => {
        if (!session) {
          listener({ status: 'unauthenticated', user: null });
          return;
        }

        void this.mapSession(session)
          .then((parent) => {
            listener({
              status: event === 'PASSWORD_RECOVERY' ? 'recovery' : 'authenticated',
              user: parent,
            });
          })
          .catch(() => {
            listener({
              status: event === 'PASSWORD_RECOVERY' ? 'recovery' : 'authenticated',
              user: this.mapUserMetadata(session.user),
            });
          });
      },
    );

    return () => data.subscription.unsubscribe();
  }

  async signUp(input: RegistrationInput): Promise<ParentUser> {
    const { data, error } = await this.client.auth.signUp({
      email: input.email,
      password: input.password,
      options: { data: { display_name: input.displayName } },
    });
    if (error) {
      throw error;
    }
    if (!data.session || !data.user) {
      throw new Error(emailConfirmationConfigurationError);
    }
    return this.mapSession(data.session);
  }

  async signIn(input: SignInInput): Promise<ParentUser> {
    const { data, error } = await this.client.auth.signInWithPassword({
      email: input.email,
      password: input.password,
    });
    if (error) {
      throw error;
    }
    return this.mapSession(data.session);
  }

  async signOut(): Promise<void> {
    const { error } = await this.client.auth.signOut();
    if (error) {
      throw error;
    }
  }

  async sendPasswordReset(email: string, redirectTo: string): Promise<void> {
    const { error } = await this.client.auth.resetPasswordForEmail(email, {
      redirectTo,
    });
    if (error) {
      throw error;
    }
  }

  async consumeRecoveryUrl(url: string): Promise<boolean> {
    const recovery = parseRecoveryUrl(url);
    if (!recovery) {
      return false;
    }

    const result =
      recovery.kind === 'tokens'
        ? await this.client.auth.setSession({
            access_token: recovery.accessToken,
            refresh_token: recovery.refreshToken,
          })
        : await this.client.auth.exchangeCodeForSession(recovery.code);

    if (result.error) {
      throw result.error;
    }
    return true;
  }

  async updatePassword(password: string): Promise<ParentUser> {
    const { data, error } = await this.client.auth.updateUser({ password });
    if (error) {
      throw error;
    }
    return this.mapUser(data.user);
  }

  private async mapSession(session: Session): Promise<ParentUser> {
    return this.mapUser(session.user);
  }

  private async mapUser(user: User): Promise<ParentUser> {
    const fallback = this.mapUserMetadata(user);
    const { data, error } = await this.client
      .from('profiles')
      .select('display_name')
      .eq('id', user.id)
      .maybeSingle();

    if (error || typeof data?.display_name !== 'string') {
      return fallback;
    }

    const displayName = data.display_name.trim();
    return displayName ? { ...fallback, displayName } : fallback;
  }

  private mapUserMetadata(user: User): ParentUser {
    const metadataName = user.user_metadata?.display_name;
    return {
      id: user.id,
      email: user.email ?? '',
      displayName:
        typeof metadataName === 'string' && metadataName.trim()
          ? metadataName.trim()
          : 'Родитель',
    };
  }
}
