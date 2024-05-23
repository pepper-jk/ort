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

import org.ossreviewtoolkit.model.config.RepositoryConfiguration
import org.ossreviewtoolkit.utils.ort.ORT_REPO_CONFIG_FILENAME

/**
 * A description of the source code repository that was used as input for ORT.
 */
data class AnalyzerInput(
    /**
     * Original VCS-related information from the working tree containing the analyzer root.
     */
    val provenance: Provenance,

    /**
     * A map of nested repositories, for example Git submodules or Git-Repo modules. The key is the path to the
     * nested repository relative to the root of the main repository.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    val nestedProvenances: NestedProvenance = NestedProvenance(provenance, emptyMap()),

    /**
     * The configuration of the repository, parsed from [ORT_REPO_CONFIG_FILENAME].
     */
    val config: RepositoryConfiguration = RepositoryConfiguration()
) {
    companion object {
        /**
         * A constant for a [AnalyzerInput] where all properties are empty strings.
         */
        @JvmField
        val EMPTY = AnalyzerInput(
            provenance = UnknownProvenance,
            nestedProvenances = NestedProvenance(UnknownProvenance, emptyMap()),
            config = RepositoryConfiguration()
        )
    }

    /**
     * Return the path of [otherProvenance] relative to [AnalyzerInput.provenance], or null if [otherProvenance] is
     * neither [AnalyzerInput.provenance] nor contained in [nestedProvenances].
     */
    fun getRelativePath(otherProvenance: Provenance): String? {
        fun Provenance.matches(other: Provenance) =
            (
                (this is UnknownProvenance && other is UnknownProvenance) || (
                    this is ArtifactProvenance && other is ArtifactProvenance &&
                    this.sourceArtifact == other.sourceArtifact) || (
                        this is RepositoryProvenance && other is RepositoryProvenance &&
                        this.vcsInfo == other.vcsInfo && this.resolvedRevision == other.resolvedRevision)
                )

        if (provenance.matches(otherProvenance)) return ""

        return nestedProvenances.subRepositories.entries.find { (_, nestedProvenance) -> nestedProvenance.matches(otherProvenance) }?.key
    }
}
