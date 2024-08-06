plugins {
    // Apply precompiled plugins.
    id("ort-library-conventions")
}

dependencies {
    api(projects.downloader)
    api(projects.model)
    api(projects.utils.commonUtils) {
        because("This is a CommandLineTool.")
    }

    api(libs.semver4j) {
        because("This is a CommandLineTool.")
    }

    implementation(projects.utils.ortUtils)

    testImplementation(libs.mockk)
}
