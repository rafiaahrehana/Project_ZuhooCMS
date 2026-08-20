import { Pipe, PipeTransform } from '@angular/core';

/**
 * Angular's built-in `currency` pipe renders unrecognized ISO codes (e.g. 'BDT' in the
 * en-US locale) glued directly to the number - "BDT20,000.00" - because it treats the
 * code as a locale currency "symbol", and en-US's symbol placement pattern has no space.
 * This pipe formats the number directly and always inserts exactly one space after the code.
 *
 * Argument-compatible with the built-in `currency` pipe (currencyCode, display, digitsInfo)
 * so existing template call sites only need `currency` renamed to `bosCurrency` - the display
 * argument (e.g. 'symbol'/'symbol-narrow') is accepted but ignored since it's irrelevant here,
 * and digitsInfo's max-fraction-digits (e.g. '1.0-0' -> 0, '1.2-2' -> 2) is still honored.
 */
@Pipe({ name: 'bosCurrency', standalone: true })
export class BosCurrencyPipe implements PipeTransform {
  transform(
    value: number | string | null | undefined,
    currencyCode: string | null | undefined = 'BDT',
    displayOrDecimals?: string | number,
    digitsInfo?: string,
  ): string {
    const num = typeof value === 'string' ? parseFloat(value) : (value ?? 0);
    const code = (currencyCode || 'BDT').trim();

    let decimals = 2;
    if (typeof displayOrDecimals === 'number') {
      decimals = displayOrDecimals;
    } else if (typeof digitsInfo === 'string') {
      const match = digitsInfo.match(/\.\d+-(\d+)$/);
      if (match) decimals = parseInt(match[1], 10);
    }

    const formatted = new Intl.NumberFormat('en-US', {
      minimumFractionDigits: decimals,
      maximumFractionDigits: decimals,
    }).format(Number.isFinite(num) ? (num as number) : 0);
    return `${code} ${formatted}`;
  }
}
