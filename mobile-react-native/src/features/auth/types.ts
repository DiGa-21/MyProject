export type ParentUser = {
  id: string;
  email: string;
  displayName: string;
};

export type AuthStatus =
  | 'initializing'
  | 'unauthenticated'
  | 'authenticated'
  | 'recovery';

export type AuthFieldErrors = {
  displayName?: string;
  email?: string;
  password?: string;
  passwordRepeat?: string;
  general?: string;
};

export type RegistrationInput = {
  displayName: string;
  email: string;
  password: string;
};

export type SignInInput = {
  email: string;
  password: string;
};

export type ValidationResult<T> = {
  value?: T;
  errors: AuthFieldErrors;
};

export type AuthSnapshot = {
  status: AuthStatus;
  user: ParentUser | null;
};

export type AuthUnsubscribe = () => void;

export interface AuthGateway {
  restoreSession(): Promise<ParentUser | null>;
  subscribe(listener: (snapshot: AuthSnapshot) => void): AuthUnsubscribe;
  signUp(input: RegistrationInput): Promise<ParentUser>;
  signIn(input: SignInInput): Promise<ParentUser>;
  signOut(): Promise<void>;
  sendPasswordReset(email: string, redirectTo: string): Promise<void>;
  consumeRecoveryUrl(url: string): Promise<boolean>;
  updatePassword(password: string): Promise<ParentUser>;
}
