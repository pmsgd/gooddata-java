/*
 * Copyright (C) 2007-2019, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.sdk.service;

import org.springframework.web.client.RestOperations;

import java.util.Map;

public interface RestOperationsProvider {

    RestOperations createRestOperations(GoodDataEndpoint endpoint, GoodDataSettings settings);
}
