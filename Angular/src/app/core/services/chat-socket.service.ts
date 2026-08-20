import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

/**
 * Live push, shared across every chat surface in the app (service-request comments,
 * support-ticket messages, ...). One persistent WebSocket connection for the whole
 * session; callers subscribe to whichever per-user queue they care about.
 *
 * The backend addresses messages per-user (convertAndSendToUser), never a public
 * /topic, so a socket only ever receives messages meant for the authenticated user -
 * see ServiceRequestServiceImpl.pushChatMessage / SupportMessageServiceImpl.pushChatMessage
 * on the backend.
 *
 * Plain WebSocket (no SockJS) - matches WebSocketConfig on the backend, and avoids
 * sockjs-client's Node-style `global` reference that Angular's esbuild/Vite bundler
 * doesn't polyfill.
 *
 * The WebSocket handshake can't carry a normal Authorization header (browsers don't
 * allow custom headers on the native WebSocket transport), so the JWT travels as a
 * ?token= query param instead - WebSocketAuthInterceptor on the backend validates it
 * at the handshake and refuses the upgrade if it's missing or invalid.
 */
@Injectable({ providedIn: 'root' })
export class ChatSocketService {
  private client: Client | null = null;
  private pending: { destination: string; handler: (payload: any) => void }[] = [];
  private activeSubs = new Map<string, StompSubscription>();
  private connectedFlag = false;

  constructor(private auth: AuthService) {}

  get connected(): boolean {
    return this.connectedFlag;
  }

  /** Subscribes to a per-user destination (e.g. /user/queue/service-requests/13/messages). Returns an unsubscribe fn. */
  subscribe(destination: string, handler: (payload: any) => void): () => void {
    const entry = { destination, handler };
    this.pending.push(entry);

    const client = this.ensureClient();
    if (client.connected) {
      this.doSubscribe(entry);
    }

    return () => {
      this.pending = this.pending.filter((p) => p !== entry);
      this.activeSubs.get(destination)?.unsubscribe();
      this.activeSubs.delete(destination);
    };
  }

  private ensureClient(): Client {
    if (this.client) return this.client;

    const token = this.auth.getAccessToken();
    // environment.apiUrl already ends in "/api" - the WS endpoint is a sibling of it.
    const wsBaseUrl = environment.apiUrl.replace(/\/api\/?$/, '').replace(/^http/, 'ws');

    this.client = new Client({
      brokerURL: `${wsBaseUrl}/ws?token=${encodeURIComponent(token || '')}`,
      reconnectDelay: 5000,
      onConnect: () => {
        this.connectedFlag = true;
        // (Re)subscribe everything - covers both the first connect and any
        // reconnect after a dropped connection.
        this.pending.forEach((entry) => this.doSubscribe(entry));
      },
      onWebSocketClose: () => {
        this.connectedFlag = false;
        this.activeSubs.clear();
      },
    });
    this.client.activate();
    return this.client;
  }

  private doSubscribe(entry: { destination: string; handler: (payload: any) => void }): void {
    if (!this.client?.connected || this.activeSubs.has(entry.destination)) return;
    const sub = this.client.subscribe(entry.destination, (frame: IMessage) => {
      try {
        entry.handler(JSON.parse(frame.body));
      } catch {
        // ignore malformed frame
      }
    });
    this.activeSubs.set(entry.destination, sub);
  }
}
