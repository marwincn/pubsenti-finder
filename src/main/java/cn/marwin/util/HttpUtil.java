package cn.marwin.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpUtil {

    public static String request(String url) throws IOException, HttpException {

        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(10, TimeUnit.SECONDS)
                //.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("", 8888)))
                .build();

        Request request = new Request.Builder()
                .url(url)
                //.header("cookie", "")
                .header("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.116 Safari/537.36")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new HttpException("HTTP REQUEST ERROR " + response.code());
        }

        return response.body().string();
    }
}
