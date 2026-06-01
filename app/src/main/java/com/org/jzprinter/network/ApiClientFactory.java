package com.org.jzprinter.network;

import android.content.Context;

public final class ApiClientFactory {

    public static Api create(Context context) {
        return new ApiClient(context);
    }

    private ApiClientFactory() {}
}
