package dev.lucien.foundry.jei

import mezz.jei.api.registration.IRecipeRegistration

/**
 * JEI recipe registration handler.
 * JEI will automatically discover recipes from the recipe manager.
 */
object FoundryJeiEvents {

    fun registerJeiRuntime(registration: IRecipeRegistration) {
        // JEI will automatically discover recipes of our registered type
        // from the recipe manager. We don't need to manually populate them here.
        // The recipes are defined in src/main/resources/data/foundry/recipes/
        // and will be loaded by Minecraft's recipe manager.
    }
}
