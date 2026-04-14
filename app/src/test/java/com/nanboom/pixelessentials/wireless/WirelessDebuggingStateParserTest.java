package com.nanboom.pixelessentials.wireless;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class WirelessDebuggingStateParserTest {
    @Test
    public void parseEnabled_returnsTrueForOne() {
        assertTrue(new WirelessDebuggingStateParser().parseEnabled("1\n"));
    }

    @Test
    public void parseEnabled_returnsFalseForZero() {
        assertFalse(new WirelessDebuggingStateParser().parseEnabled("0\n"));
    }
}
