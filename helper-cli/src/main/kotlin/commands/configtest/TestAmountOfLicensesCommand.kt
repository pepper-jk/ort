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

import org.ossreviewtoolkit.helper.utils.*
import org.ossreviewtoolkit.helper.utils.fetchScannedSources
import org.ossreviewtoolkit.helper.utils.getViolatedRulesByLicense
import org.ossreviewtoolkit.helper.utils.readOrtResult
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.PackageType
import org.ossreviewtoolkit.model.Provenance
import org.ossreviewtoolkit.model.TextLocation
import org.ossreviewtoolkit.model.licenses.LicenseView
import org.ossreviewtoolkit.model.utils.DirectoryPackageConfigurationProvider
import org.ossreviewtoolkit.model.utils.createLicenseInfoResolver
import org.ossreviewtoolkit.utils.common.expandTilde
import org.ossreviewtoolkit.utils.spdx.SpdxExpression

class TestAmountOfLicensesCommand : CliktCommand(
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

    private val applyLicenseFindingCurations by option(
        "--apply-license-finding-curations",
        help = "Apply the license finding curations contained in the ORT result."
    ).flag()

    private val decomposeLicenseExpressions by option(
        "--decompose-license-expressions",
        help = "Decompose SPDX license expressions into its single licenses components and list the findings for " +
                "each single license separately."
    ).flag()

    private val packageConfigurationDir by option(
        "--package-configuration-dir",
        help = "The directory containing the package configuration files to read as input. It is searched " +
                "recursively."
    ).convert { it.expandTilde() }
        .file(mustExist = true, canBeFile = false, canBeDir = true, mustBeWritable = false, mustBeReadable = true)

    // TODO: Add params for ort-config

    private fun getLicenses(ortFile: File): MutableMap<Provenance, Map<SpdxExpression, Set<TextLocation>>> {
        val ortResult = readOrtResult(ortFile)

        val packages = ortResult.getProjectsAndPackages()

        val findingsByProvenance = mutableMapOf<Provenance, Map<SpdxExpression, Set<TextLocation>>>()
        for (packageId in packages) {
            if (ortResult.getPackageOrProject(packageId) == null) {
                throw UsageError("Could not find the package for the given id '${packageId.toCoordinates()}'.")
            }

            val packageConfigurationProvider = DirectoryPackageConfigurationProvider(packageConfigurationDir)

            findingsByProvenance.putAll(
                ortResult.getLicenseFindingsById(
                    packageId,
                    packageConfigurationProvider,
                    applyLicenseFindingCurations,
                    decomposeLicenseExpressions
                )
            )
        }

        return findingsByProvenance
    }

    override fun run() {
        val findings = getLicenses(inputOrtFile)
        val contrastFindings = getLicenses(contrastOrtFile)

        if (findings != contrastFindings) {
            // FIXME: not really a usage error more a failed test, replace later
            throw UsageError(
                text = "Findings are not identical.",
                statusCode = 1
            )
        }
    }
}
