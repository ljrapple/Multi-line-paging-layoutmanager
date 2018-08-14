// -*- Mode: java; tab-width: 4; indent-tabs-mode: nil; c-basic-offset: 4 -*-
//
// Copyright (C) 2014 Opera Software ASA.  All rights reserved.
//
// This file is an original work developed by Opera Software ASA

package com.ljr.com.multi.paging;

import java.util.Locale;

/**
 * Utility class for general assertions and checks.
 */
public final class Check {

    public static final boolean ON = true;

    private Check() {
        if (Check.ON) {
            Check.isTrue(false);
        }
    }

    public static void isNotNull(Object o) {
        isNotNull(o, "Reference should not be null");
    }

    public static void isNotNull(Object o, String message,
            Object... formatArgs) {
        isTrue(o != null, message, formatArgs);
    }

    public static void isTrue(boolean condition) {
        if (Check.ON && !condition) {
            throw new AssertionError();
        }
    }

    public static void isTrue(boolean condition, String message, Object... formatArgs) {
        if (message == null) {
            throw new NullPointerException();
        }
        if (Check.ON && !condition) {
            throw new AssertionError(String.format(Locale.US, message, formatArgs));
        }
    }

}
