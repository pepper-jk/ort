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
import org.ossreviewtoolkit.helper.commands.groupByText
import org.ossreviewtoolkit.helper.utils.*
import org.ossreviewtoolkit.helper.utils.fetchScannedSources
import org.ossreviewtoolkit.helper.utils.getViolatedRulesByLicense
import org.ossreviewtoolkit.helper.utils.readOrtResult
import org.ossreviewtoolkit.model.Identifier
import org.ossreviewtoolkit.model.PackageType
import org.ossreviewtoolkit.model.Provenance
import org.ossreviewtoolkit.model.licenses.LicenseView
import org.ossreviewtoolkit.model.utils.DirectoryPackageConfigurationProvider
import org.ossreviewtoolkit.model.utils.createLicenseInfoResolver
import org.ossreviewtoolkit.utils.common.FileMatcher

import org.ossreviewtoolkit.utils.common.expandTilde

class TestAmountOfLicensesCommand : CliktCommand(
    help = "Converts the given ORT file to a different format, e.g. '.json' to '.yml'."
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

    // TODO: Add params for ort-config

    fun getLicenses(OrtFile: String) {
        val ortResult = readOrtResult(OrtFile)

        val licenseInfoResolver = ortResult.createLicenseInfoResolver()

        fun getDetectedLicenses(id: Identifier): List<String> =
            licenseInfoResolver.resolveLicenseInfo(id)
                .filter(LicenseView.ONLY_DETECTED)
                .map { it.license.toString() }

        val packagesWithOffendingRuleViolations = ortResult.getRuleViolations().filter {
            it.severity in offendingSeverities
        }.mapNotNullTo(mutableSetOf()) { it.pkg }

        val packages = ortResult.getProjectsAndPackages().filter { id ->
            (ortResult.isPackage(id) && PackageType.PACKAGE in type) || (ortResult.isProject(id) && PackageType.PROJECT in type)
        }.filter { id ->
            matchDetectedLicenses.isEmpty() || (matchDetectedLicenses - getDetectedLicenses(id)).isEmpty()
        }.filter { id ->
            !offendingOnly || id in packagesWithOffendingRuleViolations
        }.sortedBy { it }

        val findingsByProvenance = mutableMapOf<Provenance, MutableMap<SpdxExpression, MutableSet<TextLocation>>>()
        for (packageId in packages) {
            if (ortResult.getPackageOrProject(packageId) == null) {
                throw UsageError("Could not find the package for the given id '${packageId.toCoordinates()}'.")
            }

            val sourcesDir = sourceCodeDir ?: run {
                println("Downloading sources for package '${packageId.toCoordinates()}'...")
                ortResult.fetchScannedSources(packageId)
            }

            val packageConfigurationProvider = DirectoryPackageConfigurationProvider(packageConfigurationDir)

            fun isPathExcluded(provenance: Provenance, path: String): Boolean =
                if (ortResult.isProject(packageId)) {
                    ortResult.getExcludes().paths
                } else {
                    packageConfigurationProvider.getPackageConfigurations(packageId, provenance).flatMap { it.pathExcludes }
                }.any { it.matches(path) }

            val violatedRulesByLicense = ortResult.getViolatedRulesByLicense(packageId, offendingSeverities)

            findingsByProvenance.putAll( ortResult
                .getLicenseFindingsById(
                    packageId,
                    packageConfigurationProvider,
                    applyLicenseFindingCurations,
                    decomposeLicenseExpressions
                )
                .mapValues { (provenance, locationsByLicense) ->
                    locationsByLicense.filter { (license, _) ->
                        !offendingOnly || license.decompose().any { it in violatedRulesByLicense }
                    }.mapValues { (license, locations) ->
                        locations.filter { location ->
                            val isAllowedFile = fileAllowList.isEmpty() || FileMatcher.match(fileAllowList, location.path)

                            val isIncluded = !omitExcluded || !isPathExcluded(provenance, location.path) ||
                                    ignoreExcludedRuleIds.intersect(violatedRulesByLicense[license].orEmpty()).isNotEmpty()

                            isAllowedFile && isIncluded
                        }
                    }.mapValues { (_, locations) ->
                        locations.groupByText(sourcesDir)
                    }.filter { (_, locations) ->
                        locations.isNotEmpty()
                    }.filter { (license, _) ->
                        licenseAllowlist.isEmpty() || license.decompose().any { it.simpleLicense() in licenseAllowlist }
                    }
                }
            )
        }

        return findingsByProvenance
    }

    override fun run() {

        val findings = getLicenses(inputOrtFile)
        val contrastFindings = getLicenses(contrastOrtFile)

        if (findings != contrastFindings) {
            return 1;
        }
        return 0;
    }
}
