/*
 * Copyright (C) 2007-2019, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.sdk.service.httpcomponents;

import com.gooddata.http.client.GoodDataHttpClient;
import com.gooddata.http.client.LoginSSTRetrievalStrategy;
import com.gooddata.http.client.SSTRetrievalStrategy;
import com.gooddata.sdk.service.GoodDataEndpoint;
import com.gooddata.sdk.service.httpcomponents.HttpClientRestOperationsProvider;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import static com.gooddata.util.Validate.notNull;

public class LoginPasswordHttpClientRestOperationsProvider extends HttpClientRestOperationsProvider {
    private final String login;
    private final String password;

    public LoginPasswordHttpClientRestOperationsProvider(String login, String password) {
        this.login = login;
        this.password = password;
    }

    @Override
    protected HttpClient createHttpClient(GoodDataEndpoint endpoint, HttpClientBuilder builder) {
        notNull(endpoint, "endpoint");
        notNull(builder, "builder");

        final HttpClient httpClient = builder.build();
        final SSTRetrievalStrategy strategy = new LoginSSTRetrievalStrategy(login, password);
        final HttpHost httpHost = new HttpHost(endpoint.getHostname(), endpoint.getPort(), endpoint.getProtocol());
        return new GoodDataHttpClient(httpClient, httpHost, strategy);
    }
}
