/*
 * MIT License
 *
 * Copyright (c) 2022 Lark Technologies Pte. Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice, shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.lark.oapi.core.httpclient;

import com.lark.oapi.core.Constants;
import com.lark.oapi.core.request.FormData;
import com.lark.oapi.core.request.FormDataFile;
import com.lark.oapi.core.request.RawRequest;
import com.lark.oapi.core.response.RawResponse;
import com.lark.oapi.core.utils.IOs;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.core.utils.Strings;
import com.lark.oapi.okhttp.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class OkHttpTransport implements IHttpTransport {

    private OkHttpClient okHttpClient;

    public OkHttpTransport(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    private RequestBody buildReqBody(RawRequest request) {
        if (request == null || request.getBody() == null) {
            return null;
        }

        Object body = request.getBody();
        if (body instanceof FormData) {
            String contentType = "multipart/form-data;charset=" + StandardCharsets.UTF_8;
            MultipartBody.Builder builder = new MultipartBody.Builder()
                    .setType(MediaType.parse(contentType));

            for (Map.Entry<String, Object> entry : ((FormData) body).getParams().entrySet()) {
                builder.addFormDataPart(entry.getKey(), entry.getValue().toString());
            }

            for (FormDataFile file : ((FormData) body).getFiles()) {
                final File finalFile = file.getFile();
                builder.addFormDataPart(file.getFieldName(),
                        Strings.isEmpty(file.getFileName()) ? "unknown" : file.getFileName()
                        , RequestBody.create(MediaType.parse("application/octet-stream"), finalFile));
            }

            return builder.build();
        }

        if (request.isSupportLong2String()) {
            return RequestBody.create(MediaType.parse(Constants.JSON_CONTENT_TYPE)
                    , Jsons.LONG_TO_STR.toJson(body).getBytes(StandardCharsets.UTF_8));
        } else {
            return RequestBody.create(MediaType.parse(Constants.JSON_CONTENT_TYPE)
                    , Jsons.DEFAULT.toJson(body).getBytes(StandardCharsets.UTF_8));
        }

    }

    @Override
    public RawResponse execute(RawRequest request) throws Exception {
        // 转换为okhttp的request
        RequestBody body = buildReqBody(request);
        Request.Builder builder = new Request.Builder().url(request.getReqUrl())
                .method(request.getHttpMethod(), body);

        // 设置请求header
        for (Map.Entry<String, List<String>> entry : request.getHeaders().entrySet()) {
            for (String value : entry.getValue()) {
                builder.header(entry.getKey(), value);
            }
        }

        if (!(request.getBody() instanceof FormData)) {
            builder.header("content-type", "application/json; charset=utf-8");
        }

        // 执行请求
        Response response = okHttpClient.newCall(builder.build()).execute();

        // 转换结果为通用结果
        RawResponse rawResponse = new RawResponse();
        rawResponse.setStatusCode(response.code());
        rawResponse.setHeaders(response.headers().toMultimap());
        if (request.isSupportDownLoad()) {
            rawResponse.setBody(Objects.requireNonNull(IOs.readAll(response.body().byteStream())));
        } else {
            rawResponse.setBody(Objects.requireNonNull(response.body()).bytes());

        }
        return rawResponse;

    }
}
Client client=Client.newBuilder("appId","appSecret") // 默认配置为自建应用
    .marketplaceApp() // 设置应用类型为商店应用
    .openBaseUrl(BaseUrlEnum.FeiShu) // 设置域名，默认为飞书
    .helpDeskCredential("helpDeskId","helpDeskSecret") // 服务台应用才需要设置
    .requestTimeout(3,TimeUnit.SECONDS) // 设置httpclient 超时时间，默认永不超时
    .logReqAtDebug(true) // 在 debug 模式下会打印 http 请求和响应的 headers、body 等信息。
    .build();
public enum BaseUrlEnum {
  FeiShu("https://open.feishu.cn"),
  LarkSuite("https://open.larksuite.com"),
  ;
}
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.core.utils.Lists;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReq;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReqBody;
import com.lark.oapi.service.docx.v1.model.CreateDocumentResp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class DocxSample {
  public static void main(String arg[]) throws Exception {
    // 创建 API Client。你需在此传入你的应用的实际 App ID 和 App Secret
    Client client = Client.newBuilder("appId", "appSecret").build();
    // 设置自定义请求头
    Map<String, List<String>> headers = new HashMap<>();
    headers.put("key1", Lists.newArrayList("value1"));
    headers.put("key2", Lists.newArrayList("value2"));
    // 发起请求
    CreateDocumentResp resp = client.docx().document()
        .create(CreateDocumentReq.newBuilder()
                .createDocumentReqBody(CreateDocumentReqBody.newBuilder()
                    .title("title")   // 文档标题
                    .folderToken("")  // 文件夹 token，传空表示在根目录创建文档
                    .build())
                .build()
            , RequestOptions.newBuilder()
                .userAccessToken("u-2GxFH7ysh8E9lj9UJp8XAG0k0gh1h5KzM800khEw2G6e") // 传递用户token
                .headers(headers) // 传递自定义请求头
                .build());
    // 处理服务端错误
    if (!resp.success()) {
      System.out.println(String.format("code:%s,msg:%s,reqId:%s"
          , resp.getCode(), resp.getMsg(), resp.getRequestId()));
      return;
    }
    // 业务数据处理
    System.out.println(Jsons.DEFAULT.toJson(resp.getData()));
  }
}
