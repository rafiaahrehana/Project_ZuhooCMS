package com.raf.zuhoo.ui.common;

// One-shot LiveData payload. Plain LiveData re-delivers its last value to every new observer, so
// a rotation would replay the last error toast; this makes the value readable exactly once.
public class Event<T> {

    private final T content;
    private boolean handled;

    public Event(T content) {
        this.content = content;
    }

    public T consume() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }
}
