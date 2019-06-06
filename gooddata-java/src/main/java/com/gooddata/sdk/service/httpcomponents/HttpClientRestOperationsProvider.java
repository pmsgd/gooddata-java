/*
 * Copyright (C) 2007-2019, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.sdk.service.httpcomponents;

import com.gooddata.UriPrefixingClientHttpRequestFactory;
import com.gooddata.gdc.Header;
import com.gooddata.http.client.GoodDataHttpClient;
import com.gooddata.http.client.LoginSSTRetrievalStrategy;
import com.gooddata.http.client.SSTRetrievalStrategy;
import com.gooddata.sdk.service.*;
import com.gooddata.sdk.service.retry.GetServerErrorRetryStrategy;
import com.gooddata.sdk.service.retry.RetrySettings;
import com.gooddata.sdk.service.retry.RetryableRestTemplate;
import com.gooddata.sdk.service.util.ResponseErrorHandler;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.gooddata.util.Validate.notNull;
import static java.util.Arrays.asList;

public abstract class HttpClientRestOperationsProvider implements RestOperationsProvider {

    protected HttpClient httpClient;

    @Override
    public RestOperations createRestOperations(GoodDataEndpoint endpoint, GoodDataSettings settings) {
        return createRestOperations(endpoint, settings, createHttpClient(endpoint, createHttpClientBuilder(settings)));
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    protected RestOperations createRestOperations(GoodDataEndpoint endpoint, GoodDataSettings settings, HttpClient httpClient) {
        notNull(endpoint, "endpoint");
        notNull(settings, "settings");
        this.httpClient = notNull(httpClient, "httpClient");

        final UriPrefixingClientHttpRequestFactory factory = new UriPrefixingClientHttpRequestFactory(
                new HttpComponentsClientHttpRequestFactory(httpClient),
                endpoint.toUri()
        );

        final RestTemplate restTemplate;
        if (settings.getRetrySettings() == null) {
            restTemplate = new RestTemplate(factory);
        } else {
            restTemplate = createRetryRestTemplate(settings.getRetrySettings(), factory);
        }
        restTemplate.setInterceptors(asList(
                new HeaderSettingRequestInterceptor(settings.getPresetHeaders()),
                new DeprecationWarningRequestInterceptor()));

        restTemplate.setErrorHandler(new ResponseErrorHandler(restTemplate.getMessageConverters()));

        return restTemplate;
    }

    private RestTemplate createRetryRestTemplate(RetrySettings retrySettings, UriPrefixingClientHttpRequestFactory factory) {
        final RetryTemplate retryTemplate = new RetryTemplate();

        if (retrySettings.getRetryCount() != null) {
            retryTemplate.setRetryPolicy(new SimpleRetryPolicy(retrySettings.getRetryCount()));
        }

        if (retrySettings.getRetryInitialInterval() != null) {
            if (retrySettings.getRetryMultiplier() != null) {
                final ExponentialBackOffPolicy exponentialBackOffPolicy = new ExponentialBackOffPolicy();
                exponentialBackOffPolicy.setInitialInterval(retrySettings.getRetryInitialInterval());
                exponentialBackOffPolicy.setMultiplier(retrySettings.getRetryMultiplier());
                exponentialBackOffPolicy.setMaxInterval(retrySettings.getRetryMaxInterval());
                retryTemplate.setBackOffPolicy(exponentialBackOffPolicy);
            } else {
                final FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
                backOffPolicy.setBackOffPeriod(retrySettings.getRetryInitialInterval());
                retryTemplate.setBackOffPolicy(backOffPolicy);
            }
        }

        return new RetryableRestTemplate(factory, retryTemplate, new GetServerErrorRetryStrategy());
    }

    protected abstract HttpClient createHttpClient(final GoodDataEndpoint endpoint, final HttpClientBuilder builder);

    protected HttpClientBuilder createHttpClientBuilder(final GoodDataSettings settings) {
        final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(settings.getMaxConnections());
        connectionManager.setMaxTotal(settings.getMaxConnections());

        final SocketConfig.Builder socketConfig = SocketConfig.copy(SocketConfig.DEFAULT);
        socketConfig.setSoTimeout(settings.getSocketTimeout());
        connectionManager.setDefaultSocketConfig(socketConfig.build());

        final RequestConfig.Builder requestConfig = RequestConfig.copy(RequestConfig.DEFAULT);
        requestConfig.setConnectTimeout(settings.getConnectionTimeout());
        requestConfig.setConnectionRequestTimeout(settings.getConnectionRequestTimeout());
        requestConfig.setSocketTimeout(settings.getSocketTimeout());

        return HttpClientBuilder.create()
                .setUserAgent(settings.getUserAgent())
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig.build());
    }
}
