package com.example.telegraminstasavebot.okhttp;


import com.example.telegraminstasavebot.TelegramInstaSaveBotApplication;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class OkHttpInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        return chain.proceed(chain.request().newBuilder().addHeader(
                "Cookie",
                TelegramInstaSaveBotApplication.cookie
        ).addHeader(
                "User-Agent",
                TelegramInstaSaveBotApplication.userAgent
        ).build());
    }
}
