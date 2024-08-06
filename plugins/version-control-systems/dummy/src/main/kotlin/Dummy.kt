package org.ossreviewtoolkit.plugins.versioncontrolsystems.dummy

import java.io.File

import kotlin.io.path.copyToRecursively

import org.ossreviewtoolkit.downloader.VersionControlSystem
import org.ossreviewtoolkit.downloader.WorkingTree
import org.ossreviewtoolkit.model.VcsInfo
import org.ossreviewtoolkit.model.VcsType
import org.ossreviewtoolkit.utils.common.CommandLineTool
import org.ossreviewtoolkit.utils.common.ProcessCapture

object DummyCommand : CommandLineTool {
    override fun command(workingDir: File?) = ""

    // we do not need this, but override it, so nothing gets executed by accident
    override fun transformVersion(output: String): String = ""

    override fun displayName(): String = "Dummy"

    override fun isInPath(): Boolean = true

    override fun run(vararg args: CharSequence, workingDir: File?, environment: Map<String, String>) = ProcessCapture()

    override fun getVersion(workingDir: File?): String = "1.0"

    override fun checkVersion(workingDir: File?) {
        return
    }
}

class Dummy : VersionControlSystem(DummyCommand) {
    override val type = VcsType.UNKNOWN.toString()
    override val priority = 0
    override val latestRevisionNames = listOf("HEAD", "@")

    override fun getVersion() = ""

    override fun getDefaultBranchName(url: String) = ""

    override fun getWorkingTree(vcsDirectory: File): WorkingTree = DummyWorkingTree(vcsDirectory, VcsType.forName(type))

    override fun isApplicableUrlInternal(vcsUrl: String): Boolean = true

    override fun initWorkingTree(targetDir: File, vcs: VcsInfo): WorkingTree {
        File(vcs.url).toPath().copyToRecursively(targetDir.toPath(), followLinks = false, overwrite = true)
        return getWorkingTree(targetDir)
    }

    override fun updateWorkingTree(
        workingTree: WorkingTree,
        revision: String,
        path: String,
        recursive: Boolean
    ): Result<String> = Result<String>.success(revision)
}
