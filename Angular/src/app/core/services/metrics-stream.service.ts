import { Injectable, NgZone } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class MetricsStreamService {

  constructor(private ngZone: NgZone) {}

  /**
   * Listens to the Server-Sent Events (SSE) stream from the backend.
   * By using NgZone.runOutsideAngular, we prevent this rapid stream (every 500ms)
   * from triggering Angular's change detection, which would destroy UI performance.
   */
  public getTrafficStream(): Observable<number> {
    return new Observable(observer => {
      // Create connection to the SSE endpoint
      const eventSource = new EventSource(`${environment.apiUrl}/v1/metrics/stream`);

      eventSource.onmessage = (event) => {
        // MUST run outside Angular to avoid change detection thrashing
        this.ngZone.runOutsideAngular(() => {
          const trafficValue = parseInt(event.data, 10);
          observer.next(trafficValue);
        });
      };

      eventSource.onerror = (error) => {
        console.error('SSE connection error, closing stream', error);
        this.ngZone.run(() => observer.error(error));
        eventSource.close();
      };

      // Cleanup when component un-subscribes
      return () => {
        eventSource.close();
      };
    });
  }
}
