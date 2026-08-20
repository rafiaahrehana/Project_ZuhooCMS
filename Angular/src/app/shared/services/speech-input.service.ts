import { Injectable, NgZone } from '@angular/core';

/**
 * Thin wrapper around the browser's native SpeechRecognition API - no backend,
 * no new dependency. Transcription is always appended into the caller's own
 * text field for the employee to review and edit before sending; it never
 * sends anything itself, same "a human confirms before send" rule as
 * everywhere else in the AI module.
 */
@Injectable({ providedIn: 'root' })
export class SpeechInputService {
  private recognition: any = null;
  private listening = false;

  constructor(private zone: NgZone) {}

  get isSupported(): boolean {
    return typeof window !== 'undefined' && !!(window as any).webkitSpeechRecognition;
  }

  get isListening(): boolean {
    return this.listening;
  }

  /**
   * Starts listening. `onResult` fires once per final transcript chunk (append
   * it to the caller's own text, this service never owns the text itself).
   * `onEnd` fires when recognition stops, for any reason (silence, error, stop()).
   */
  start(onResult: (text: string) => void, onEnd: () => void, lang = 'en-US'): void {
    if (!this.isSupported || this.listening) return;

    const SpeechRecognition = (window as any).webkitSpeechRecognition;
    const recognition = new SpeechRecognition();
    recognition.lang = lang;
    recognition.continuous = false;
    recognition.interimResults = false;

    recognition.onresult = (event: any) => {
      const transcript = Array.from(event.results as any)
        .map((r: any) => r[0].transcript)
        .join(' ')
        .trim();
      if (transcript) {
        this.zone.run(() => onResult(transcript));
      }
    };
    recognition.onerror = () => {
      this.listening = false;
      this.zone.run(onEnd);
    };
    recognition.onend = () => {
      this.listening = false;
      this.zone.run(onEnd);
    };

    this.recognition = recognition;
    this.listening = true;
    recognition.start();
  }

  stop(): void {
    if (this.recognition && this.listening) {
      this.recognition.stop();
    }
  }
}
