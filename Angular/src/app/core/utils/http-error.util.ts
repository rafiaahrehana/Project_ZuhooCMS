import { HttpErrorResponse } from '@angular/common/http';

/**
 * Extracts the backend's error message from an HttpErrorResponse.
 *
 * Endpoints called through ApiService.postText/patchText/deleteText use
 * `responseType: 'text'` (their success body is a plain string, e.g.
 * "Company deactivated successfully") - but that setting also makes Angular
 * parse an *error* response body as a raw string instead of JSON, so
 * `error.error` ends up being the JSON text itself (e.g.
 * '{"message":"Cannot delete company with existing employees."}') rather
 * than an already-parsed object. Reading `error.error.message` in that case
 * silently returns undefined. This helper tries JSON.parse first so the real
 * backend message surfaces either way.
 */
export function extractErrorMessage(error: HttpErrorResponse, fallback = 'An error occurred'): string {
  if (error.error instanceof ErrorEvent) {
    return error.error.message || fallback;
  }

  if (typeof error.error === 'string' && error.error.trim().startsWith('{')) {
    try {
      const parsed = JSON.parse(error.error);
      return parsed?.message || parsed?.error || fallback;
    } catch {
      // Not actually JSON - fall through to the raw string below.
    }
  }

  if (typeof error.error === 'string' && error.error.trim()) {
    return error.error;
  }

  if (error.error && typeof error.error === 'object') {
    return error.error.message || error.error.error || fallback;
  }

  return error.message || fallback;
}
