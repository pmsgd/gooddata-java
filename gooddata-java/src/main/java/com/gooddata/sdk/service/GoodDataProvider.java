/*
 * Copyright (C) 2007-2019, GoodData(R) Corporation. All rights reserved.
 * This source code is licensed under the BSD-style license found in the
 * LICENSE.txt file in the root directory of this source tree.
 */
package com.gooddata.sdk.service;

public interface GoodDataProvider<T extends GoodData> {

    T createGoodData(RestOperationsProvider provider, GoodDataEndpoint endpoint, GoodDataSettings settings);
}
