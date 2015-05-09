package com.orhanobut.wasp;

import android.content.Context;
import android.text.TextUtils;

import com.orhanobut.wasp.parsers.GsonParser;
import com.orhanobut.wasp.parsers.Parser;
import com.orhanobut.wasp.utils.LogLevel;
import com.orhanobut.wasp.utils.NetworkMode;
import com.orhanobut.wasp.utils.RequestInterceptor;
import com.orhanobut.wasp.utils.SSLUtils;
import com.orhanobut.wasp.utils.WaspHttpStack;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * @author Orhan Obut
 */
public class Wasp {

    private Context context;
    private Parser parser;
    private static LogLevel logLevel;
    private static Parser defaultParser;
//    private static Context context;
//    private static LogLevel logLevel;
//    private static Parser parser;

    private final Builder builder;

    private Wasp(Builder builder) {
        this.builder = builder;

        logLevel = builder.logLevel;
        context = builder.context;
        parser = builder.parser;
        defaultParser = builder.defaultParser;
    }

    /**
     * It is used for the parse operations.
     */
    public static Parser getParser() {
        if (defaultParser == null) {
            throw new NullPointerException("Wasp.Builder must be called first");
        }
        return defaultParser;
    }

    static LogLevel getLogLevel() {
        return logLevel;
    }

    @SuppressWarnings("unchecked")
    public <T> T create(Class<T> service) {
        if (service == null) {
            throw new NullPointerException("service param may not be null");
        }
        if (!service.isInterface()) {
            throw new IllegalArgumentException("Only interface type is supported");
        }
        NetworkHandler handler = NetworkHandler.newInstance(service, builder);
        return (T) handler.getProxyClass();
    }

    /**
     * Initiate download and load image process
     */
    public static class Image {

        private static ImageHandler imageHandler;

        public static WaspImage.Builder from(Context context, String path) {
            if (TextUtils.isEmpty(path)) {
                throw new IllegalArgumentException("Path cannot be empty or null");
            }
            return new WaspImage.Builder()
                    .setImageHandler(getImageHandler(context.getApplicationContext()))
                    .from(path);
        }

        private static ImageHandler getImageHandler(Context context) {
            if (context == null) {
                throw new NullPointerException("Wasp.Builder should be instantiated first");
            }
            if (imageHandler == null) {
                imageHandler = new WaspImageHandler(
                        new BitmapWaspCache(), new VolleyImageNetworkHandler(context, new WaspOkHttpStack())
                );
            }
            return imageHandler;
        }

        public static void clearCache() {
            if (imageHandler == null) {
                return;
            }
            imageHandler.clearCache();
        }

    }

    /**
     * Initiate all required information for the wasp
     */
    @SuppressWarnings("unused")
    public static class Builder {

        private String endPointUrl;
        private LogLevel logLevel;
        private NetworkMode networkMode;
        private Context context;
        private Parser parser;
        private Parser defaultParser;
        private WaspHttpStack waspHttpStack;
        private RequestInterceptor requestInterceptor;
        private NetworkStack networkStack;
        private HostnameVerifier hostnameVerifier;
        private SSLSocketFactory sslSocketFactory;
        private CookieHandler cookieHandler;

        public Builder(Context context) {
            if (context == null) {
                throw new NullPointerException("Context may not be null");
            }
            this.context = context.getApplicationContext();
        }

        public Builder setEndpoint(String url) {
            if (url == null || url.trim().length() == 0) {
                throw new NullPointerException("End point url may not be null");
            }
            if (url.charAt(url.length() - 1) == '/') {
                throw new IllegalArgumentException("End point should not end with \"/\"");
            }
            this.endPointUrl = url;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder setWaspHttpStack(WaspHttpStack waspHttpStack) {
            if (waspHttpStack == null) {
                throw new NullPointerException("WaspHttpStack may not be null");
            }
            if (waspHttpStack.getHttpStack() == null) {
                throw new NullPointerException("WaspHttpStack.getHttpStack() may not return null");
            }
            this.waspHttpStack = waspHttpStack;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder trustCertificates() {
            if (sslSocketFactory != null) {
                throw new IllegalStateException("Only one type of trust certificate method can be used!");
            }
            this.sslSocketFactory = SSLUtils.getTrustAllCertSslSocketFactory();
            this.hostnameVerifier = SSLUtils.getEmptyHostnameVerifier();
            return this;
        }

        @SuppressWarnings("unused")
        public Builder trustCertificates(int keyStoreRawResId, String keyStorePassword) {
            if (sslSocketFactory != null) {
                throw new IllegalStateException("Only one type of trust certificate method can be used!");
            }
            this.sslSocketFactory = SSLUtils.getPinnedCertSslSocketFactory(
                    context, keyStoreRawResId, keyStorePassword
            );
            return this;
        }

        @SuppressWarnings("unused")
        public Builder enableCookies(CookiePolicy cookiePolicy) {
            return enableCookies(null, cookiePolicy);
        }

        public Builder enableCookies(CookieStore cookieStore, CookiePolicy cookiePolicy) {
            if (cookiePolicy == null) {
                throw new NullPointerException("CookiePolicy may not be null");
            }
            this.cookieHandler = new CookieManager(cookieStore, cookiePolicy);
            return this;
        }

        public Wasp build() {
            init();
            return new Wasp(this);
        }

        private void init() {
            if (endPointUrl == null) {
                throw new NullPointerException("Endpoint may not be null");
            }
            if (defaultParser == null) {
                defaultParser = new GsonParser();
            }
            if (parser == null) {
                parser = defaultParser;
            }
//            if (parser == null) {
//                parser = new GsonParser();
//            }
            if (logLevel == null) {
                logLevel = LogLevel.NONE;
            }
            if (networkMode == null) {
                networkMode = NetworkMode.LIVE;
            }
            if (waspHttpStack == null) {
                waspHttpStack = new WaspOkHttpStack();
            }
            waspHttpStack.setHostnameVerifier(hostnameVerifier);
            waspHttpStack.setSslSocketFactory(sslSocketFactory);
            waspHttpStack.setCookieHandler(cookieHandler);
            networkStack = VolleyNetworkStack.newInstance(context, waspHttpStack);
        }

        String getEndPointUrl() {
            return endPointUrl;
        }

        LogLevel getLogLevel() {
            return logLevel;
        }

        public Builder setLogLevel(LogLevel logLevel) {
            if (logLevel == null) {
                throw new NullPointerException("Log level should not be null");
            }
            this.logLevel = logLevel;
            return this;
        }

        NetworkMode getNetworkMode() {
            return networkMode;
        }

        @SuppressWarnings("unused")
        public Builder setNetworkMode(NetworkMode networkMode) {
            if (networkMode == null) {
                throw new NullPointerException("NetworkMode should not be null");
            }
            this.networkMode = networkMode;
            return this;
        }

        Context getContext() {
            return context;
        }

        Parser getParser() {
            return parser;
        }

        public Builder setParser(Parser parser) {
            if (parser == null) {
                throw new NullPointerException("Parser may not be null");
            }
            this.parser = parser;
            return this;
        }

        public Builder setDefaultParser(Parser parser) {
            if (parser == null) {
                throw new NullPointerException("Parser may not be null");
            }
            this.defaultParser = parser;
            return this;
        }

        RequestInterceptor getRequestInterceptor() {
            return requestInterceptor;
        }

        @SuppressWarnings("unused")
        public Builder setRequestInterceptor(RequestInterceptor interceptor) {
            this.requestInterceptor = interceptor;
            return this;
        }

        NetworkStack getNetworkStack() {
            return networkStack;
        }
    }
}
