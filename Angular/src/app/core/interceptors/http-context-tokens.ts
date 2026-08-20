import { HttpContextToken } from '@angular/common/http';

// Set true (via { context: new HttpContext().set(SKIP_ERROR_TOAST, true) }) on a
// request whose caller already renders its own inline error message, so the user
// doesn't see the same failure reported twice (a global toast + an inline banner).
export const SKIP_ERROR_TOAST = new HttpContextToken<boolean>(() => false);
