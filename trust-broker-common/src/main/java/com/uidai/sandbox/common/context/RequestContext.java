package com.uidai.sandbox.common.context;

import java.lang.ScopedValue;

public final class RequestContext {
    public static final ScopedValue<String> SYSTEM_ID = ScopedValue.newInstance();
    public static final ScopedValue<String> CORRELATION_ID = ScopedValue.newInstance();

    private RequestContext() {}
}
