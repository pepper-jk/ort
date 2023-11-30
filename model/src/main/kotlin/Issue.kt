/*
 * Copyright (C) 2017 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

import java.time.Instant

import org.apache.logging.log4j.kotlin.logger

import org.ossreviewtoolkit.utils.common.normalizeLineBreaks

/**
 * An issue that occurred while executing ORT.
 */
data class Issue(
    /**
     * The timestamp of the issue.
     */
    val timestamp: Instant = Instant.now(),

    /**
     * A description of the issue source, e.g. the tool that caused the issue.
     */
    val source: String,

    /**
     * The issue's message.
     */
    @JsonSerialize(using = NormalizeLineBreaksSerializer::class)
    val message: String,

    /**
     * The issue's severity.
     */
    val severity: Severity = Severity.ERROR,

    /**
     * The affected file or directory the issue is limited to, if any.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val affectedPath: String? = null
) {
    override fun toString(): String {
        val time = if (timestamp == Instant.EPOCH) "Unknown time" else timestamp.toString()
        return "$time [$severity]: $source - $message"
    }
}

class NormalizeLineBreaksSerializer : StdSerializer<String>(String::class.java) {
    override fun serialize(value: String, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.normalizeLineBreaks())
    }
}

/**
 * Create an [Issue] and log the message. The log level is aligned with the [severity].
 */
fun Any.createAndLogIssue(source: String, message: String, severity: Severity? = null): Issue {
    val issue = severity?.let { Issue(source = source, message = message, severity = it) }
        ?: Issue(source = source, message = message)
    logger.log(issue.severity.toLog4jLevel()) { message }
    return issue
}
