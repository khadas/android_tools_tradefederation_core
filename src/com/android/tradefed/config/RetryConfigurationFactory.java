/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tradefed.config;

import com.android.tradefed.log.LogUtil.CLog;
import com.android.tradefed.testtype.IRemoteTest;
import com.android.tradefed.testtype.suite.retry.RetryRescheduler;
import com.android.tradefed.util.keystore.IKeyStoreClient;

import com.google.common.annotations.VisibleForTesting;

import java.util.List;

/** Factory that handles retrying a command. */
public class RetryConfigurationFactory extends ConfigurationFactory {

    public static final String RETRY_CONFIG_NAME = "retry";

    private static RetryConfigurationFactory sInstance = null;

    /** Get the singleton {@link IConfigurationFactory} instance. */
    public static RetryConfigurationFactory getInstance() {
        if (sInstance == null) {
            sInstance = new RetryConfigurationFactory();
        }
        return sInstance;
    }

    @Override
    public IConfiguration createConfigurationFromArgs(
            String[] arrayArgs, List<String> unconsumedArgs, IKeyStoreClient keyStoreClient)
            throws ConfigurationException {
        // First create the retry configuration
        IConfiguration retryConfig = createConfiguration(arrayArgs, unconsumedArgs, keyStoreClient);
        if (retryConfig.getTests().size() != 1) {
            throw new ConfigurationException(
                    String.format("%s should only have one runner inside it.", RETRY_CONFIG_NAME));
        }
        IRemoteTest rerunner = retryConfig.getTests().get(0);
        if (!(rerunner instanceof RetryRescheduler)) {
            CLog.e("The runner inside the retry configuration is not a RetryRescheduler type.");
            return retryConfig;
        }
        // Then use the retry runner to generate the configuration to actually run.
        RetryRescheduler retryRunner = (RetryRescheduler) rerunner;
        retryRunner.setConfiguration(retryConfig);
        try {
            retryRunner.run(null);
            return retryRunner.getRetryConfiguration();
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    @VisibleForTesting
    IConfiguration createConfiguration(
            String[] arrayArgs, List<String> unconsumedArgs, IKeyStoreClient keyStoreClient)
            throws ConfigurationException {
        return super.createConfigurationFromArgs(arrayArgs, unconsumedArgs, keyStoreClient);
    }
}
