package com.zonerental.testsupport

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import java.io.StringReader

object YamlFiles {

    /** Loads YAML into nested maps. Duplicate keys are allowed here (last wins, like Bukkit). */
    @Suppress("UNCHECKED_CAST")
    fun load(text: String): Map<String, Any?> = Yaml().load<Map<String, Any?>>(text) ?: emptyMap()

    /** Returns dotted paths of every mapping key that appears more than once in its mapping. */
    fun duplicateKeyPaths(text: String): List<String> {
        val root = Yaml(LoaderOptions()).compose(StringReader(text)) ?: return emptyList()
        val duplicates = mutableListOf<String>()
        fun walk(node: Node, path: String) {
            if (node !is MappingNode) return
            val seen = mutableSetOf<String>()
            for (tuple in node.value) {
                val key = (tuple.keyNode as? ScalarNode)?.value ?: continue
                val childPath = if (path.isEmpty()) key else "$path.$key"
                if (!seen.add(key)) duplicates += childPath
                walk(tuple.valueNode, childPath)
            }
        }
        walk(root, "")
        return duplicates
    }

    @Suppress("UNCHECKED_CAST")
    fun section(map: Map<String, Any?>, key: String): Map<String, Any?> =
        (map[key] as? Map<String, Any?>) ?: emptyMap()
}
