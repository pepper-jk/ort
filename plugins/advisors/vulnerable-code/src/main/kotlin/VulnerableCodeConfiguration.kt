/*
 * Copyright (C) 2021 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.plugins.advisors.vulnerablecode

/**
 * The configuration for VulnerableCode as security vulnerability provider.
 */
data class VulnerableCodeConfiguration(
    /**
     * The base URL of the VulnerableCode REST API. By default, the public VulnerableCode instance is used.
     */
    val serverUrl: String? = null,

    /**
     * The optional API key to use.
     */
    val apiKey: String? = null,

    /**
     * The read timeout for the server connection in seconds. Defaults to whatever is the HTTP client's default value.
     */
    val readTimeout: Long? = null
)
