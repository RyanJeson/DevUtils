package dev.engine.http;

import dev.utils.JCLogUtils;

/**
 * detail: Http Engine 接口
 * @author Ttt
 */
public interface IHttpEngine {

    // 日志 TAG
    String TAG = IHttpEngine.class.getSimpleName();

    /**
     * detail: Request Object
     * @author Ttt
     */
    class Request {
    }

    /**
     * detail: Request Response
     * Created by Ttt
     */
    class Response {

        // Request
        protected Request request;
        // 发送请求时间
        protected long sentRequestAtMillis;
        // 请求响应时间
        protected long receivedResponseAtMillis;

        /**
         * 获取 Request Object
         * @param <T> 泛型
         * @return Request Object
         */
        public final <T extends Request> T getRequest() {
            try {
                return (T) request;
            } catch (Exception e) {
                JCLogUtils.eTag(TAG, e, "getRequest");
            }
            return null;
        }

        /**
         * 获取发送请求时间
         * @return 发送请求时间
         */
        public final long getSentRequestAtMillis() {
            return sentRequestAtMillis;
        }

        /**
         * 获取请求响应时间
         * @return 请求响应时间
         */
        public final long getReceivedResponseAtMillis() {
            return receivedResponseAtMillis;
        }
    }

    /**
     * detail: Request Call Operate
     * @author Ttt
     */
    interface Call {

        /**
         * 获取 Request Object
         * @param <T> 泛型
         * @return Request Object
         */
        <T extends Request> T getRequest();

        // ========
        // = 状态 =
        // ========

        /**
         * 是否取消请求
         * @return {@code true} yes, {@code false} no
         */
        boolean isCanceled();

        /**
         * 是否执行过请求
         * @return {@code true} yes, {@code false} no
         */
        boolean isExecuted();

        /**
         * 是否请求结束
         * @return {@code true} yes, {@code false} no
         */
        boolean isEnd();

        // ============
        // = 操作方法 =
        // ============

        /**
         * 取消请求
         */
        void cancel();

        /**
         * 开始请求方法 ( 同步 )
         * @return {@code true} operation successfully, {@code false} operation failed
         */
        boolean start();

        /**
         * 开始请求方法 ( 异步 )
         * @return {@code true} operation successfully, {@code false} operation failed
         */
        boolean startAync();
    }

    /**
     * detail: Request CallBack
     * Created by Ttt
     */
    abstract class RequestCallBack {

        /**
         * 开始请求
         * @param call {@link Call}
         */
        public void onStart(Call call){
        }

        /**
         * 请求取消
         * @param call {@link Call}
         */
        public void onCancel(Call call){
        }

        /**
         * 请求响应
         * @param call     {@link Call}
         * @param response {@link Response}
         */
        abstract void onResponse(Call call, Response response);

        /**
         * 请求失败
         * @param call      {@link Call}
         * @param throwable {@link Throwable}
         */
        abstract void onFailure(Call call, Throwable throwable);
    }

    // =============
    // = 获取 Call =
    // =============

    /**
     * 获取 Request Call Object
     * @param request  {@link Request}
     * @param callBack {@link RequestCallBack}
     * @return {@link Call}
     */
    Call newCall(Request request, RequestCallBack callBack);
}