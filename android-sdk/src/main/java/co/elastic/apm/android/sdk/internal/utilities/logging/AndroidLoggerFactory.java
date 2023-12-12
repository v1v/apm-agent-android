/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.apm.android.sdk.internal.utilities.logging;

import org.slf4j.Logger;

import co.elastic.apm.android.common.internal.logging.ELoggerFactory;
import co.elastic.apm.android.sdk.configuration.logging.LoggingPolicy;

public class AndroidLoggerFactory extends ELoggerFactory {
    private final LoggingPolicy policy;

    public AndroidLoggerFactory(LoggingPolicy policy) {
        this.policy = policy;
    }

    @Override
    public Logger getLogger(String name) {
        return new AndroidLogger(name, policy);
    }
}
