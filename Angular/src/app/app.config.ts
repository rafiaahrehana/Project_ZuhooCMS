import { ApplicationConfig, DEFAULT_CURRENCY_CODE, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withXhr, withInterceptors } from '@angular/common/http';
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { errorInterceptor } from './core/interceptors/error.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withXhr(), withInterceptors([authInterceptor, errorInterceptor])),
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'BDT' },
    provideCharts(withDefaultRegisterables()),
  ],
};
