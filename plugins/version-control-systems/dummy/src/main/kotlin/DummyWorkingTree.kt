package org.ossreviewtoolkit.plugins.versioncontrolsystems.dummy

import java.io.File

import org.ossreviewtoolkit.downloader.WorkingTree
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType

internal open class DummyWorkingTree(workingDir: File, vcsType: VcsType) : WorkingTree(workingDir, vcsType) {

    override fun isValid(): Boolean = true

    override fun isShallow(): Boolean = true

    // import org.ossreviewtoolkit.model.Repository
    // private fun listSubmodulePaths(repo: Repository) = listOf<String>()

    override fun getNested(): Map<String, VcsInfo> = mapOf<String, VcsInfo>()

    override fun getRemoteUrl(): String = workingDir.toString()

    override fun getRevision(): String = "main"

    override fun getRootPath(): File = workingDir

    override fun listRemoteBranches(): List<String> = listOf<String>()

    override fun listRemoteTags(): List<String> = listOf<String>()
}
