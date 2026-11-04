package com.slipcheck.backend.core.model;


/** Channels a slip submission can arrive through. The core never knows channel specifics beyond this enum. */
public enum Channel {
    TELEGRAM, LINE, WEB
}
