package com.zonerental.consistency

import com.zonerental.testsupport.ProjectFiles
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Modrinth publish workflow reads the version from build.gradle.kts and extracts the
 * matching `## [x.y.z]` section from CHANGELOG.md as release notes, so these must agree.
 */
class VersionConsistencyTest {

    private val buildVersion by lazy {
        Regex("^version\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE).find(ProjectFiles.buildScript)!!.groupValues[1]
    }

    @Test
    fun `plugin yml version matches build version`() {
        val pluginVersion = Regex("^version:\\s*(\\S+)", RegexOption.MULTILINE).find(ProjectFiles.pluginYml)!!.groupValues[1]
        assertEquals(buildVersion, pluginVersion)
    }

    @Test
    fun `latest changelog entry matches build version`() {
        val latest = Regex("^## \\[(\\d+\\.\\d+\\.\\d+)]", RegexOption.MULTILINE).find(ProjectFiles.changelog)!!.groupValues[1]
        assertEquals(buildVersion, latest)
    }

    @Test
    fun `changelog section for build version ends with a separator`() {
        // Only the current release's section, up to the next release heading
        val section = ProjectFiles.changelog
            .substringAfter("## [$buildVersion]", "")
            .substringBefore("\n## [")
        assertTrue(section.isNotEmpty(), "No '## [$buildVersion]' section in CHANGELOG.md")
        assertTrue(Regex("^---\\s*$", RegexOption.MULTILINE).containsMatchIn(section),
            "publish-modrinth.yml extracts from '## [$buildVersion]' up to the next '---'; " +
            "the separator must be inside this release's section, on its own line")
    }
}
