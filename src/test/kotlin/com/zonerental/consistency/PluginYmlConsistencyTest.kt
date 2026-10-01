package com.zonerental.consistency

import com.zonerental.testsupport.ProjectFiles
import com.zonerental.testsupport.YamlFiles
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PluginYmlConsistencyTest {

    companion object {
        /** Known issue: checked by RRCommand help pages but not declared in plugin.yml. */
        val KNOWN_UNDECLARED_PERMISSIONS = setOf("zonerental.admin")

        private val PERMISSION_LITERAL = Regex("\"(zonerental(?:\\.[a-z*]+)*)\"")
    }

    private val pluginYml by lazy { YamlFiles.load(ProjectFiles.pluginYml) }

    @Suppress("UNCHECKED_CAST")
    private val permissions by lazy { YamlFiles.section(pluginYml, "permissions") as Map<String, Map<String, Any?>?> }

    private val permissionsUsedInCode by lazy {
        ProjectFiles.mainSources.values
            .flatMap { PERMISSION_LITERAL.findAll(it).map { m -> m.groupValues[1] }.toList() }
            .toSet()
    }

    @Test
    fun `main class exists`() {
        val main = pluginYml["main"] as String
        ProjectFiles.file("src/main/java/" + main.replace('.', '/') + ".java")
    }

    @Test
    fun `only the base zr command is declared`() {
        assertEquals(setOf("zr"), YamlFiles.section(pluginYml, "commands").keys,
            "Subcommands are registered dynamically in ZoneRental.registerCommands(), not in plugin.yml")
    }

    @Test
    fun `scanner finds permissions in code`() {
        assertTrue(permissionsUsedInCode.size > 15, "Found only $permissionsUsedInCode")
    }

    @Test
    fun `every permission checked in code is declared except known issues`() {
        val undeclared = permissionsUsedInCode - permissions.keys
        assertEquals(KNOWN_UNDECLARED_PERMISSIONS, undeclared)
    }

    @Test
    fun `every child permission is declared`() {
        val missing = permissions.flatMap { (parent, def) ->
            @Suppress("UNCHECKED_CAST")
            val children = (def?.get("children") as? Map<String, Any?>)?.keys ?: emptySet()
            children.filter { it !in permissions }.map { "$parent -> $it" }
        }
        assertEquals(emptyList(), missing)
    }

    @Test
    fun `every admin permission is included in zonerental admin wildcard`() {
        @Suppress("UNCHECKED_CAST")
        val adminChildren = (permissions["zonerental.admin.*"]?.get("children") as Map<String, Any?>).keys
        val adminNodes = permissions.keys.filter { it.startsWith("zonerental.admin.") && it != "zonerental.admin.*" }.toSet()
        assertEquals(adminNodes, adminChildren)
    }

    @Test
    fun `every permission has a default`() {
        val noDefault = permissions.filter { (_, def) -> def?.get("default") == null }.keys
        assertEquals(emptySet(), noDefault)
    }
}
