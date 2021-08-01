package org.coryjk.wikispider.core.message;

public record State<T>(T value, Status status) {

    public boolean isTerminal() {
        switch (status) {
            case WORKING:
                return false;
            case FOUND_RESULT:
            case MAX_ATTEMPTS_EXHAUSTED:
            case COLLISION:
            case ERROR:
            default:
                return true;
        }
    }
}
