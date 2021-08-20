package com.huobi;

import com.alibaba.fastjson.JSON;
import com.huobi.constant.Constants;
import com.huobi.service.huobi.signature.ApiSignature;
import org.apache.commons.collections4.MapUtils;
import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class HbdmHttpClient {

    private final OkHttpClient httpClient;


    static final MediaType JSON_TYPE = MediaType.parse("application/json");

    private HbdmHttpClient() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(200, 10, TimeUnit.SECONDS)).connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS);
        httpClient = builder.build();


    }

    public static HbdmHttpClient getInstance() {
        return new HbdmHttpClient();
    }


    public String doGet(String url, Map<String, Object> params) {
        Request.Builder reqBuild = new Request.Builder();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (MapUtils.isNotEmpty(params)) {
            params.forEach((k, v) -> {
                urlBuilder.addQueryParameter(k, v.toString());
            });
        }
        reqBuild.url(urlBuilder.build());

        Response response = null;
        try {
            response = httpClient.newCall(reqBuild.build()).execute();
        } catch (IOException e) {
            throw new HttpRequestException("http执行异常，url=" + url, e);
        }
        if (response.isSuccessful()) {
            try {
                return response.body().string();
            } catch (IOException e) {
                throw new HttpRequestException("http结果解析异常", e);
            }
        } else {
            int statusCode = response.code();
            throw new HttpRequestException("响应码不为200，返回响应码：" + statusCode + "，url：" + urlBuilder.build());
        }
    }


    /**
     * 微信推送
     */
    public String doPost2WX(String uri, Map<String, Object> params) {
        try {
            RequestBody body = RequestBody.create(JSON_TYPE, JSON.toJSONString(params));
            Request.Builder builder = new Request.Builder().url(uri).post(body);
            Request request = builder.build();
            Response response = httpClient.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException("IOException 目标url：" + uri, e);
        }
    }


}
