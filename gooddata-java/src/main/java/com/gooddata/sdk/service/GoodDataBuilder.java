/*
 * Copyright (C) 2007-2019, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.sdk.service;

import com.gooddata.gdc.Header;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class GoodDataBuilder {

    private static final String UNKNOWN_VERSION = "UNKNOWN";

    private final GoodDataEndpoint endpoint;
    private GoodDataSettings settings;
    private RestOperationsProvider provider;

    public GoodDataBuilder(String hostname) {
        this(new GoodDataEndpoint(hostname));
    }

    public GoodDataBuilder(String hostname, int port) {
        this(new GoodDataEndpoint(hostname, port));
    }

    public GoodDataBuilder(String hostname, int port, String protocol) {
        this(new GoodDataEndpoint(hostname, port, protocol));
    }

    public GoodDataBuilder(GoodDataEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public GoodDataBuilder withSettings(GoodDataSettings settings) {
        this.settings = settings;
        return this;
    }

    public GoodDataBuilder withRestOperationsProvider(RestOperationsProvider provider) {
        this.provider = provider;
        return this;
    }

    public GoodData2 build() {
        return build(GoodData::new);
    }

    public <T extends GoodData> T build(GoodDataProvider<T> provider) {
        return provider.createGoodData(this.provider, endpoint, settings);
    }


}
