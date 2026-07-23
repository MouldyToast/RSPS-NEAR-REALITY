package com.near_reality.game.world.info

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

/**
 * Visibility level for a world.
 */
enum class WorldVisibility {
    PUBLIC, BETA, DEVELOPER
}

/**
 * Represents a database connection profile parsed from worlds.yml.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class DatabaseProfile(
    val enabled: Boolean = false,
    val databaseUrl: String = "",
    val databasePort: Int = 5432,
    val databaseName: String = "",
    val databaseUser: String = "",
    val databasePassword: String = ""
)

/**
 * Represents the API settings block parsed from worlds.yml.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ApiProfile(
    val enabled: Boolean = false,
    val scheme: String = "http",
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val token: String = ""
)

/**
 * Represents a single world profile parsed from worlds.yml.
 * All fields use Jackson defaults so missing YAML keys don't crash.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class WorldProfile(
    val number: Int = 1,
    val host: String = "127.0.0.1",
    val port: Int = 43594,
    val activity: String = "",
    @JsonProperty("private")
    val private_: Boolean = false,
    val visibility: WorldVisibility = WorldVisibility.DEVELOPER,
    val verifyPasswords: Boolean = true,
    val location: String = "UNITED_STATES_OF_AMERICA",
    val logback: String = "logback-dev",
    val types: List<String> = emptyList(),
    val api: ApiProfile? = null,
    val mainDatabase: DatabaseProfile? = null,
    val logsDatabase: DatabaseProfile? = null,
    val discord: Map<String, Any>? = null,
    val useWhitelist: Boolean = false,
    val whitelistedUsernames: List<String> = emptyList(),
    val discordToken: String? = null
) {
    /** Kotlin property alias so Java callers can use `.private` without backticks. */
    @get:JvmName("isPrivate")
    val `private`: Boolean get() = private_

    fun isDevelopment(): Boolean = visibility == WorldVisibility.DEVELOPER
    fun isBeta(): Boolean = visibility == WorldVisibility.BETA
    fun isPublic(): Boolean = visibility == WorldVisibility.PUBLIC

    fun isMainDatabaseEnabled(): Boolean = mainDatabase?.enabled == true
    fun isLogsDatabaseEnabled(): Boolean = logsDatabase?.enabled == true
    fun isApiEnabled(): Boolean = api?.enabled == true
    fun isDiscordEnabled(): Boolean = discord != null
    fun verify2FA(): Boolean = isApiEnabled() && !isDevelopment()
}

/**
 * Top-level YAML structure: `worlds:` mapping of name → [WorldProfile].
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class WorldsRoot(
    val worlds: Map<String, WorldProfile> = emptyMap()
)

/**
 * Loads [WorldProfile] entries from a `worlds.yml` file.
 *
 * Usage (from Main.kt):
 * ```
 * val worldConfig = WorldConfig.fromYAML(File("worlds.yml"))
 * val profile = worldConfig["localhost"]
 * ```
 */
class WorldConfig private constructor(
    private val profiles: Map<String, WorldProfile>
) {
    operator fun get(key: String): WorldProfile? = profiles[key]

    companion object {
        private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

        @JvmStatic
        fun fromYAML(file: File): WorldConfig {
            val root = mapper.readValue(file, WorldsRoot::class.java)
            return WorldConfig(root.worlds)
        }
    }
}
