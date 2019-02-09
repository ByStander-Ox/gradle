/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.kotlin.dsl

import org.gradle.plugin.management.internal.autoapply.AutoAppliedBuildScanPlugin
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec


/**
 * The `build-scan` plugin.
 *
 * Visit the [Build Scan Plugin User Manual](https://docs.gradle.com/build-scan-plugin/) for additional information.
 *
 * By default, the applied plugin version will be the same as the one used by the `--scan` command line option.
 *
 * You can also use e.g. `` `build-scan` version "1.8" `` to request a different version.
 */
val PluginDependenciesSpec.`build-scan`: PluginDependencySpec
    get() = AutoAppliedBuildScanPlugin.addBuildScanPluginDependencySpecTo(this)