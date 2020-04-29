package dev.other.okgo;

import android.app.Application;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheEntity;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.SPCookieStore;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.interceptor.HttpLoggingInterceptor;
import com.lzy.okgo.model.HttpHeaders;
import com.lzy.okgo.model.HttpParams;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import okhttp3.OkHttpClient;

/**
 * detail: OkGo 配置相关工具类
 * @author Ttt
 * <pre>
 *     用于初始化 Okhttp 等配置相关方法, 该类可不需要
 *     @see <a href="https://github.com/jeasonlzy/okhttp-OkGo"/>
 *     @see <a href="https://github.com/jeasonlzy/okhttp-OkGo/wiki"/>
 *     <p></p>
 *     OkGo 简写 OK 快捷输入关联调用
 * </pre>
 */
public final class OkUtils {

    private OkUtils() {
    }

    /**
     * 初始化 OkGo 配置
     * <pre>
     *     @see <a href="https://github.com/jeasonlzy/okhttp-OkGo/wiki/Init"/>
     *     tips: 最简单的配置直接调用 OkGo.getInstance().init(this); {@link OkGo}
     *     在 App {@link Application} 初始化
     * </pre>
     * @param application {@link Application}
     */
    public static void initOkGo(Application application) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        // ========================
        // = OkGo 内置 log 拦截器 =
        // ========================

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor("OkGo");
        // log 打印级别, 决定了 log 显示的详细程度
        loggingInterceptor.setPrintLevel(HttpLoggingInterceptor.Level.BODY);
        // log 颜色级别, 决定了 log 在控制台显示的颜色
        loggingInterceptor.setColorLevel(Level.INFO);
        builder.addInterceptor(loggingInterceptor);

        // ===============
        // = 配置超时时间 =
        // ===============

        // 全局的读取超时时间
        builder.readTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        // 全局的写入超时时间
        builder.writeTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);
        // 全局的连接超时时间
        builder.connectTimeout(OkGo.DEFAULT_MILLISECONDS, TimeUnit.MILLISECONDS);

        // =============================
        // = Cookie 持久化 ( Session ) =
        // =============================

        // 使用 sp 保持 cookie, 如果 cookie 不过期, 则一直有效
        builder.cookieJar(new CookieJarImpl(new SPCookieStore(application)));
//        // 使用数据库保持 cookie, 如果 cookie 不过期, 则一直有效
//        builder.cookieJar(new CookieJarImpl(new DBCookieStore(application)));
//        // 使用内存保持 cookie, app 退出后, cookie 消失
//        builder.cookieJar(new CookieJarImpl(new MemoryCookieStore()));

        // ==============
        // = Https 配置 =
        // ==============

//        // 方法一: 信任所有证书, 不安全有风险
//        HttpsUtils.SSLParams sslParams1 = HttpsUtils.getSslSocketFactory();
//        // 方法二: 自定义信任规则, 校验服务端证书
//        HttpsUtils.SSLParams sslParams2 = HttpsUtils.getSslSocketFactory(new SafeTrustManager());
//        // 方法三: 使用预埋证书, 校验服务端证书 ( 自签名证书 ) 
//        HttpsUtils.SSLParams sslParams3 = HttpsUtils.getSslSocketFactory(application.getAssets().open("srca.cer"));
//        // 方法四: 使用 bks 证书和密码管理客户端证书 ( 双向认证 ) , 使用预埋证书, 校验服务端证书 ( 自签名证书 ) 
//        HttpsUtils.SSLParams sslParams4 = HttpsUtils.getSslSocketFactory(application.getAssets().open("xxx.bks"),
//            "123456", application.getAssets().open("yyy.cer"));
//        builder.sslSocketFactory(sslParams1.sSLSocketFactory, sslParams1.trustManager);
//        // 配置 https 的域名匹配规则, 详细看 demo 的初始化介绍, 不需要就不要加入, 使用不当会导致 https 握手失败
//        builder.hostnameVerifier(new SafeHostnameVerifier());

        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();
        builder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        builder.hostnameVerifier(HttpsUtils.UnSafeHostnameVerifier);

        // ===============
        // = 全局公共数据 =
        // ===============

        // header 不支持中文, 不允许有特殊字符
        HttpHeaders headers = new HttpHeaders();
        headers.put("commonHeaderKey1", "commonHeaderValue1");
        headers.put("commonHeaderKey2", "commonHeaderValue2");

        // param 支持中文, 直接传, 不要自己编码
        HttpParams params = new HttpParams();
        params.put("commonParamsKey1", "commonParamsValue1");
        params.put("commonParamsKey2", "这里支持中文参数");

        // =======================
        // = 初始化 ( 应用 ) 配置 =
        // =======================

        OkGo.getInstance().init(application)
            // 建议设置 OkHttpClient, 不设置将使用默认的
            .setOkHttpClient(builder.build())
            // 全局统一缓存模式, 默认不使用缓存, 可以不传
            .setCacheMode(CacheMode.NO_CACHE)
            // 全局统一缓存时间, 默认永不过期, 可以不传
            .setCacheTime(CacheEntity.CACHE_NEVER_EXPIRE)
            // 全局统一超时重连次数, 默认为三次, 那么最差的情况会请求 4 次 ( 一次原始请求, 三次重连请求 ), 不需要可以设置为 0
            .setRetryCount(3)
            // 全局公共头
            .addCommonHeaders(headers)
            // 全局公共参数
            .addCommonParams(params);
    }
}