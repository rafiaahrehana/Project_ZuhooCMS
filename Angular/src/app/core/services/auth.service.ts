import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, throwError } from 'rxjs';
import { map, tap, catchError, switchMap } from 'rxjs/operators';
import { PermissionService } from './permission.service';
import {
  LoginRequest,
  LoginResponse,
  JwtResponse,
  RegisterRequest,
  VerifyEmailRequest,
  ResendVerificationRequest,
  ForgotPasswordRequest,
  VerifyResetCodeRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  User,
  TokenPayload,
  ImpersonationResponse,
  ImpersonationSession,
} from '../models/auth.model';
import { environment } from '../../../environments/environment';

// SaaS-provider staff roles - distinct from Company-scoped roles (COMPANY_OWNER, EMPLOYEE, CLIENT).
// Mirrors backend User.isPlatformUser() (com.businessos.auth.user.User).
const PLATFORM_ROLES = [
  'SUPER_ADMIN', 'SYSTEM_ADMIN', 'SUPPORT_AGENT', 'SUPPORT_MANAGER',
  'MARKETING_MANAGER', 'PLATFORM_ACCOUNTANT', 'SALES_MANAGER',
];

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';
  private readonly USER_KEY = 'user';

  private readonly ADMIN_TOKEN_KEY = 'admin_access_token';
  private readonly ADMIN_USER_KEY = 'admin_user';
  private readonly IMPERSONATION_KEY = 'impersonation_session';

  private currentUserSubject = new BehaviorSubject<User | null>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasValidToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private impersonationSubject = new BehaviorSubject<ImpersonationSession | null>(this.getImpersonationFromStorage());
  public impersonation$ = this.impersonationSubject.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
    private permissionService: PermissionService,
  ) {
    this.initializeAuthState();
  }

  isPlatformUser(): boolean {
    const user = this.currentUserSubject.value;
    return !!user && user.roles.some(r => PLATFORM_ROLES.includes(r));
  }

  isImpersonating(): boolean {
    return this.impersonationSubject.value !== null;
  }

  getImpersonationSession(): ImpersonationSession | null {
    return this.impersonationSubject.value;
  }

  startImpersonation(response: ImpersonationResponse): void {
    const adminUser = this.currentUserSubject.value;
    if (adminUser) {
      localStorage.setItem(this.ADMIN_USER_KEY, JSON.stringify(adminUser));
    }
    const adminToken = this.getAccessToken();
    if (adminToken) {
      localStorage.setItem(this.ADMIN_TOKEN_KEY, adminToken);
    }

    const impersonatedUser: User = {
      id: adminUser?.id ?? 0,
      username: adminUser?.username ?? '',
      email: adminUser?.email ?? '',
      fullName: adminUser?.fullName ?? '',
      roles: ['COMPANY_OWNER'],
      companyId: response.companyId,
    };

    localStorage.setItem(this.TOKEN_KEY, response.accessToken);
    this.setUserInStorage(impersonatedUser);
    this.currentUserSubject.next(impersonatedUser);

    const session: ImpersonationSession = {
      companyId: response.companyId,
      companyName: response.companyName,
      impersonationSessionId: response.impersonationSessionId,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
    };
    localStorage.setItem(this.IMPERSONATION_KEY, JSON.stringify(session));
    this.impersonationSubject.next(session);

    this.router.navigate(['/']);
  }


  endImpersonation(): Observable<any> {
    const session = this.impersonationSubject.value;
    const restore = () => {
      const adminUserRaw = localStorage.getItem(this.ADMIN_USER_KEY);
      const adminToken = localStorage.getItem(this.ADMIN_TOKEN_KEY);

      localStorage.removeItem(this.ADMIN_USER_KEY);
      localStorage.removeItem(this.ADMIN_TOKEN_KEY);
      localStorage.removeItem(this.IMPERSONATION_KEY);
      this.impersonationSubject.next(null);

      if (adminToken) {
        localStorage.setItem(this.TOKEN_KEY, adminToken);
      }
      if (adminUserRaw && adminUserRaw !== 'undefined') {
        const adminUser = JSON.parse(adminUserRaw) as User;
        this.setUserInStorage(adminUser);
        this.currentUserSubject.next(adminUser);
      }
      this.router.navigate(['/']);
    };

    if (!session) {
      restore();
      return this.http.post(`${environment.apiUrl}/platform-admin/impersonate/end`, {});
    }

    const request$ = this.http.post(
      `${environment.apiUrl}/platform-admin/impersonate/end`,
      { impersonationSessionId: session.impersonationSessionId },
    );
    request$.subscribe({ next: restore, error: restore });
    return request$;
  }

  private getImpersonationFromStorage(): ImpersonationSession | null {
    const raw = localStorage.getItem(this.IMPERSONATION_KEY);
    return (raw && raw !== 'undefined') ? JSON.parse(raw) : null;
  }
 
  login(credentials: LoginRequest): Observable<User> {
    let loggedInUser!: User;
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials).pipe(
      tap(response => {
        loggedInUser = {
          id: response.userId,
          username: response.email,
          email: response.email,
          fullName: response.firstName,
          roles: [response.role],
          companyId: response.companyId
        };
        this.setTokens(response.accessToken, response.refreshToken);
        this.setUserInStorage(loggedInUser);
        this.currentUserSubject.next(loggedInUser);
        this.isAuthenticatedSubject.next(true);
      }),
      // Load permissions before this observable completes so they're already cached
      // (route guards read them synchronously) by the time the caller navigates away
      // from the login page.
      switchMap(() => this.permissionService.load()),
      switchMap(() => this.permissionService.loadCatalog()),
      // Load profile so that we have the profile picture url from the start!
      switchMap(() => this.getProfile()),
      tap(profile => {
        loggedInUser.fullName = `${profile.firstName} ${profile.lastName}`;
        loggedInUser.profileImageUrl = this.resolveImageUrl(profile.image);
        this.setUserInStorage(loggedInUser);
        this.currentUserSubject.next(loggedInUser);
      }),
      map(() => loggedInUser),
      catchError(error => {
        console.error('Login failed', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Register a new company owner account + company (tenant).
   * On success the backend sends a verification email; the account is inactive
   * until /auth/verify-email is called, so we do NOT log the user in here.
   */
  register(request: RegisterRequest): Observable<any> {
    return this.http.post<any>(`${this.API_URL}/register`, request);
  }

  /**
   * Confirm the email-verification token sent to the user's inbox.
   */
  verifyEmail(request: VerifyEmailRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/verify-email`, request, { responseType: 'text' });
  }

  /**
   * Ask the backend to send a fresh verification email.
   * Backend always returns 200 (no user enumeration) regardless of whether the email exists.
   */
  resendVerification(request: ResendVerificationRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/resend-verification`, request, { responseType: 'text' });
  }

  /**
   * Request a password-reset code. Backend always returns 200 regardless of
   * whether the account exists, to avoid leaking which emails are registered.
   * Calling this again (e.g. "Resend") issues a fresh code and invalidates the old one.
   */
  forgotPassword(request: ForgotPasswordRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/forgot-password`, request, { responseType: 'text' });
  }

  /**
   * Checks a reset code before showing the new-password step. Doesn't consume
   * the code or touch the password - resetPassword() re-validates it for real.
   */
  verifyResetCode(request: VerifyResetCodeRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/verify-reset-code`, request, { responseType: 'text' });
  }

  /**
   * Complete a password reset with the 6-digit code from the reset-password email
   * (or, for a client-portal invite, the token from that emailed link - see
   * ResetPasswordRequest). All existing refresh tokens are revoked server-side, so
   * any other active session gets logged out next time it tries to refresh.
   */
  resetPassword(request: ResetPasswordRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/reset-password`, request, { responseType: 'text' });
  }

  /**
   * Change password while logged in (POST /api/auth/change-password).
   * Backend verifies the current password, then revokes ALL refresh tokens
   * (same as resetPassword) - so after a successful change the caller should
   * call logout() and send the user back to the login page.
   */
  changePassword(request: ChangePasswordRequest): Observable<any> {
    return this.http.post(`${this.API_URL}/change-password`, request, { responseType: 'text' });
  }

  /**
   * Logout user
   */
  /** Set while the session came from "See Demo" - the read-only banner keys off it. */
  private readonly DEMO_KEY = 'bos-demo';

  isDemoSession(): boolean {
    try { return localStorage.getItem(this.DEMO_KEY) === '1'; } catch { return false; }
  }

  /**
   * Start the public read-only demo: no credentials, a 45-minute token for the
   * seeded demo tenant. Mirrors login()'s pipe so permissions and profile are
   * cached before the caller navigates - the guards read them synchronously.
   */
  startDemo(): Observable<User> {
    let demoUser!: User;
    return this.http.post<LoginResponse>(`${environment.apiUrl}/public/demo/session`, {}).pipe(
      tap(response => {
        demoUser = {
          id: response.userId,
          username: response.email,
          email: response.email,
          fullName: response.firstName,
          roles: [response.role],
          companyId: response.companyId
        };
        // No refresh token by design: when the access token dies, the demo is over.
        localStorage.setItem(this.TOKEN_KEY, response.accessToken);
        localStorage.setItem(this.DEMO_KEY, '1');
        this.setUserInStorage(demoUser);
        this.currentUserSubject.next(demoUser);
        this.isAuthenticatedSubject.next(true);
      }),
      switchMap(() => this.permissionService.load()),
      switchMap(() => this.permissionService.loadCatalog()),
      map(() => demoUser),
    );
  }

  /** Leave the demo: wipe the session and land back on the marketing page. */
  exitDemo(): void {
    this.clearSession();
    this.router.navigate(['/home']);
  }

  logout(): void {
    const refreshToken = this.getRefreshToken();
    this.clearSession();
    this.router.navigate(['/auth/login']);

    if (refreshToken) {
      // Best-effort server-side revoke; ignore errors since we've already logged out locally.
      this.http.post(`${this.API_URL}/logout`, { refreshToken }, { responseType: 'text' }).subscribe({ error: () => {} });
    }
  }

  /**
   * Clear all local session state without navigating. Used by logout() and by callers
   * (e.g. AuthGuard) that discover the refresh token was rejected by the server and need
   * to purge it so it isn't retried on every subsequent navigation.
   */
  clearSession(): void {
    this.clearTokens();
    this.clearUserStorage();
    localStorage.removeItem(this.DEMO_KEY);
    localStorage.removeItem(this.ADMIN_TOKEN_KEY);
    localStorage.removeItem(this.ADMIN_USER_KEY);
    localStorage.removeItem(this.IMPERSONATION_KEY);
    this.impersonationSubject.next(null);
    this.currentUserSubject.next(null);
    this.isAuthenticatedSubject.next(false);
    this.permissionService.clear();
  }
 
  /**
   * Refresh access token.
   * IMPORTANT: /auth/refresh only returns { accessToken, refreshToken } - no user info -
   * so we must only update the stored tokens and keep the existing user object as-is.
   *
   * Deliberately does NOT log out here on failure - a failed refresh can mean the
   * refresh token is genuinely invalid, but it can just as easily mean the backend
   * was briefly unreachable (e.g. a dev restart). The caller (AuthInterceptor) decides
   * whether the failure warrants a logout based on the error's status code.
   */
  refreshToken(): Observable<JwtResponse> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<JwtResponse>(`${this.API_URL}/refresh`, {
      refreshToken
    }).pipe(
      tap(response => {
        this.setTokens(response.accessToken, response.refreshToken);
        this.isAuthenticatedSubject.next(true);
        const user = this.getUserFromStorage();
        if (user) {
          this.currentUserSubject.next(user);
        }
        this.permissionService.load().subscribe({ error: () => {} });
        this.permissionService.loadCatalog().subscribe({ error: () => {} });
      })
    );
  }

  /**
   * Get current access token
   */
  getAccessToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }
 
  /**
   * Get refresh token
   */
  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }
 
  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    return this.hasValidToken();
  }
 
  /**
   * Check if user has specific role
   */
  hasRole(role: string): boolean {
    const user = this.currentUserSubject.value;
    return user ? user.roles.includes(role) : false;
  }
 
  /**
   * Check if user has any of the roles
   */
  hasAnyRole(roles: string[]): boolean {
    const user = this.currentUserSubject.value;
    if (!user) return false;
    return roles.some(role => user.roles.includes(role));
  }

  /**
   * Check if user has any SaaS platform staff role
   */
  isPlatformStaff(): boolean {
    const user = this.currentUserSubject.value;
    if (!user) return false;
    return user.roles.some(role => PLATFORM_ROLES.includes(role));
  }
 
  /**
   * Get current user
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }
 
  /**
   * Get company ID
   */
  getCompanyId(): number | null {
    const user = this.currentUserSubject.value;
    return user?.companyId || null;
  }
 
  /**
   * Decode JWT token
   */
  private decodeToken(): TokenPayload | null {
    const token = this.getAccessToken();
    if (!token) return null;
 
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64).split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join('')
      );
      return JSON.parse(jsonPayload);
    } catch (error) {
      console.error('Error decoding token', error);
      return null;
    }
  }
 
  /**
   * Check if token is valid and not expired
   */
  private hasValidToken(): boolean {
    const token = this.getAccessToken();
    if (!token) return false;
 
    const payload = this.decodeToken();
    if (!payload) return false;
 
    const currentTime = Date.now() / 1000;
    return payload.exp > currentTime;
  }
 
  /**
   * Store tokens in localStorage
   */
  private setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(this.TOKEN_KEY, accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
  }
 
  /**
   * Clear tokens from localStorage
   */
  private clearTokens(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }
 
  /**
   * Get current user profile and location
   */
  getProfile(): Observable<any> {
    return this.http.get<any>(`${environment.apiUrl}/users/profile`);
  }

  /**
   * Update user profile and location
   */
  updateProfile(request: any): Observable<any> {
    return this.http.patch<any>(`${environment.apiUrl}/users/profile`, request).pipe(
      tap(profile => {
        const storedUser = this.getUserFromStorage();
        if (storedUser) {
          storedUser.fullName = `${profile.firstName} ${profile.lastName}`;
          storedUser.profileImageUrl = this.resolveImageUrl(profile.image);
          this.setUserInStorage(storedUser);
          this.currentUserSubject.next(storedUser);
        }
      })
    );
  }

  /**
   * Refreshes just the cached avatar (navbar, sidebar) after it's changed somewhere
   * other than the /users/profile flow above - e.g. the Employee self-update endpoint
   * (PATCH /api/employees/me) also syncs the User's image server-side, but that
   * response never passes through AuthService, so the cached currentUser would
   * otherwise keep showing the old picture until the next login/refresh.
   */
  updateCurrentUserImage(imageUrl: string | null | undefined): void {
    const storedUser = this.getUserFromStorage();
    if (!storedUser) return;
    storedUser.profileImageUrl = this.resolveImageUrl(imageUrl);
    this.setUserInStorage(storedUser);
    this.currentUserSubject.next(storedUser);
  }

  /**
   * Store user in localStorage
   */
  private setUserInStorage(user: User): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
  }
 
  /**
   * Get user from localStorage
   */
  private getUserFromStorage(): User | null {
    const user = localStorage.getItem(this.USER_KEY);
    return (user && user !== 'undefined') ? JSON.parse(user) : null;
  }
 
  /**
   * Clear user from localStorage
   */
  private clearUserStorage(): void {
    localStorage.removeItem(this.USER_KEY);
  }
 
  resolveImageUrl(url: string | undefined | null): string | undefined {
    if (!url) return undefined;
    if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
      return url;
    }
    const base = environment.apiUrl.replace(/\/api$/, '');
    const cleanUrl = url.startsWith('/') ? url : `/${url}`;
    return `${base}${cleanUrl}`;
  }

  /**
   * Initialize auth state on app startup
   */
  private initializeAuthState(): void {
    const user = this.getUserFromStorage();
    if (user) {
      this.currentUserSubject.next(user);
    }

    const isAuthenticated = this.hasValidToken();
    this.isAuthenticatedSubject.next(isAuthenticated);

    if (isAuthenticated) {
      // Deferred to a macrotask: authInterceptor calls inject(AuthService) on every
      // HTTP request, including these two. Firing them synchronously here - while
      // AuthService's own constructor (and thus its DI registration) is still on the
      // stack - makes Angular see a reentrant injection of a token that's still being
      // constructed and throw NG0200 (circular dependency). Deferring lets the
      // constructor return and AuthService finish registering before either call fires.
      setTimeout(() => {
        this.permissionService.load().subscribe({ error: () => {} });
        this.permissionService.loadCatalog().subscribe({ error: () => {} });
        // Fetch profile to populate full name and image url!
        this.getProfile().subscribe({
          next: (profile) => {
            const activeUser: User = user || {
              id: profile.id,
              username: profile.email,
              email: profile.email,
              fullName: `${profile.firstName} ${profile.lastName}`,
              roles: [profile.role],
              companyId: null
            };
            activeUser.fullName = `${profile.firstName} ${profile.lastName}`;
            activeUser.profileImageUrl = this.resolveImageUrl(profile.image);
            this.setUserInStorage(activeUser);
            this.currentUserSubject.next(activeUser);
          }
        });
      }, 0);
    }
  }
}