package org.example.atuo_attend_backend.config;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 * 仅用于请求时使用指定代理的 RequestFactory（如大陆服务器通过代理访问 GitHub API）。
 */
public class ProxiedClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

    private final Proxy proxy;

    public ProxiedClientHttpRequestFactory(Proxy proxy) {
        this.proxy = proxy;
    }

    @Override
    protected java.net.HttpURLConnection openConnection(URL url, Proxy proxy) throws IOException {
        return (java.net.HttpURLConnection) url.openConnection(this.proxy);
    }
}
