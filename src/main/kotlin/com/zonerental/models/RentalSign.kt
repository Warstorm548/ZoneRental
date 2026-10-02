package com.zonerental.models

import org.bukkit.Location
import org.bukkit.Server

/**
 * One rental sign as stored in signs.yml under `signs.<regionWorld>:<regionName>.<id>`.
 *
 * The region and the sign can be in different worlds: [regionWorld] is the world the
 * WorldGuard region lives in (the key), [signWorld] is the world the sign block is in
 * (the sign's `world` field).
 */
data class RentalSign(
    val regionWorld: String,
    val regionName: String,
    val id: Int,
    val signWorld: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val support: SupportBlock?
) {
    /** Support block a sign is attached to, with the block it was before the sign was registered. */
    data class SupportBlock(
        val x: Int,
        val y: Int,
        val z: Int,
        val originalType: String,
        val originalData: String
    )

    /** Composite key "world:region" of the region this sign belongs to. */
    val regionKey: String
        get() = "$regionWorld:$regionName"

    /** Sign location, or null if the sign's world is not loaded. */
    fun location(server: Server): Location? {
        val world = server.getWorld(signWorld) ?: return null
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }

    /** Support block location, or null if there is none or the sign's world is not loaded. */
    fun supportLocation(server: Server): Location? {
        val block = support ?: return null
        val world = server.getWorld(signWorld) ?: return null
        return Location(world, block.x.toDouble(), block.y.toDouble(), block.z.toDouble())
    }

    fun describe(): String = "$regionKey #$id at $signWorld $x,$y,$z"
}
