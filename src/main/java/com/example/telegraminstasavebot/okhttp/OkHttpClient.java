package com.example.telegraminstasavebot.okhttp;

import java.util.concurrent.TimeUnit;

public class OkHttpClient {

    private static okhttp3.OkHttpClient okhttp = null;

    public static okhttp3.OkHttpClient getInstance(){

        if (okhttp == null){
            okhttp = new okhttp3.OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .addInterceptor(new OkHttpInterceptor())
                    .build();
        }

        return okhttp;
    }
}
