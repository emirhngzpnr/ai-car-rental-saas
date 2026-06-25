package com.aicarrental.common.validation;

public final class ValidationPatterns {

    public static final String EMAIL =
            "^[A-Za-z0-9._%+-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$";

    private ValidationPatterns() {
    }
}
