/*
 * Copyright (C) 2022 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
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

package org.ossreviewtoolkit.helper.commands.configtest

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

import java.io.File

import org.ossreviewtoolkit.helper.utils.readOrtResult
import org.ossreviewtoolkit.model.RuleViolation
import org.ossreviewtoolkit.utils.common.expandTilde

class TestViolationsCommand : CliktCommand(
    help = "Takes two ORT files and compares their number of license findings."
) {
    private val inputOrtFile by option(
        "--input-ort-file", "-i",
        help = "The ORT file to test."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .convert { it.absoluteFile.normalize() }
        .required()

    private val contrastOrtFile by option(
        "--contrast-ort-file", "-c",
        help = "The ORT file to compare against."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = true, canBeDir = false, mustBeWritable = false, mustBeReadable = true)
        .convert { it.absoluteFile.normalize() }
        .required()

    private fun getViolations(ortFile: File): List<RuleViolation> {
        val ortResult = readOrtResult(ortFile)

        return ortResult.getRuleViolations(omitResolved = true)
    }

    override fun run() {
        val violations = getViolations(inputOrtFile)
        val contrastViolations = getViolations(contrastOrtFile)

        if (violations != contrastViolations) {
            // FIXME: not really a usage error more a failed test, replace later
            throw UsageError(
                text = "Findings are not identical.",
                statusCode = 1
            )
        }
    }
}
