import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export class LocationValidators {
  /**
   * Prevents only whitespaces. If the string contains only spaces, returns an error.
   */
  static noWhitespaceValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (control.value != null && typeof control.value === 'string') {
        const isWhitespace = (control.value || '').trim().length === 0;
        const isValid = !isWhitespace;
        return isValid ? null : { whitespace: true };
      }
      return null;
    };
  }
}
