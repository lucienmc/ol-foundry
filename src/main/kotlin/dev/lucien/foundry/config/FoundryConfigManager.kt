package dev.lucien.foundry.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import dev.lucien.foundry.Foundry
import net.fabricmc.loader.api.FabricLoader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Loads and persists [FoundryConfig] as `config/foundry.json` using Gson (bundled with Minecraft,
 * so no extra dependency). The active config is read live by the block entity and lava tank, so
 * edits made through the in-game screen take effect on the next tick.
 */
object FoundryConfigManager {
    private val GSON: Gson = GsonBuilder().setPrettyPrinting().create()
    private val path: Path = FabricLoader.getInstance().configDir.resolve("${Foundry.MOD_ID}.json")

    @Volatile
    var config: FoundryConfig = FoundryConfig()
        private set

    fun load() {
        config = read()?.sanitized() ?: FoundryConfig().also(::write)
    }

    fun save(newConfig: FoundryConfig) {
        val sane = newConfig.sanitized()
        config = sane
        write(sane)
    }

    private fun read(): FoundryConfig? {
        if (!Files.exists(path)) return null
        return try {
            Files.newBufferedReader(path).use { GSON.fromJson(it, FoundryConfig::class.java) }
        } catch (e: IOException) {
            Foundry.LOGGER.warn("Couldn't read Foundry config, using defaults", e)
            null
        } catch (e: JsonSyntaxException) {
            Foundry.LOGGER.warn("Foundry config is malformed, using defaults", e)
            null
        }
    }

    private fun write(config: FoundryConfig) {
        try {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(path).use { GSON.toJson(config, it) }
        } catch (e: IOException) {
            Foundry.LOGGER.warn("Couldn't write Foundry config", e)
        }
    }
}
