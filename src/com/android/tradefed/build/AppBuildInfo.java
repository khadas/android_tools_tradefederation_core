/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.tradefed.build;

import com.android.tradefed.build.BuildInfoKey.BuildInfoFileKey;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link IBuildInfo} that represents an Android application and its test package(s).
 */
public class AppBuildInfo extends BuildInfo implements IAppBuildInfo {

    private static final long serialVersionUID = BuildSerializedVersion.VERSION;

    /**
     * Creates a {@link AppBuildInfo}.
     *
     * @param buildId the unique build id
     * @param buildName the build name
     */
    public AppBuildInfo(String buildId, String buildName) {
        super(buildId, buildName);
    }

    /**
     * @see BuildInfo#BuildInfo(BuildInfo)
     */
    public AppBuildInfo(BuildInfo buildToCopy) {
        super(buildToCopy);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<VersionedFile> getAppPackageFiles() {
        List<VersionedFile> origList = getVersionedFiles(BuildInfoFileKey.PACKAGE_FILES);
        List<VersionedFile> listCopy = new ArrayList<VersionedFile>();
        if (origList != null) {
            listCopy.addAll(origList);
        }
        return listCopy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAppPackageFile(File appPackageFile, String version) {
        setFile(BuildInfoFileKey.PACKAGE_FILES, appPackageFile, version);
    }
}
