package com.platform.core.domain;

public enum WorkStatus {
    NEW, IN_PROGRESS, BLOCKED, COMPLETED;

    public boolean canMoveTo(WorkStatus next) {
        return switch (this) {
            case NEW -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == BLOCKED || next == COMPLETED;
            case BLOCKED -> next == IN_PROGRESS;
            case COMPLETED -> false;
        };
    }
}