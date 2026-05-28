package dev.lucien.foundry.screen

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.menu.FoundryMenu
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory

class FoundryScreen(
    menu: FoundryMenu, inventory: Inventory, title: Component
) : AbstractContainerScreen<FoundryMenu>(menu, inventory, title) {

    companion object {
        private val CONTAINER_TEXTURE: Identifier = Identifier.fromNamespaceAndPath(
            Foundry.MOD_ID, "textures/gui/container/foundry.png"
        )

        // Vanilla blast-furnace animation sprites (separate PNGs since 1.21.x)
        private val LIT_PROGRESS_SPRITE: Identifier =
            Identifier.withDefaultNamespace("container/blast_furnace/lit_progress")
        private val BURN_PROGRESS_SPRITE: Identifier =
            Identifier.withDefaultNamespace("container/blast_furnace/burn_progress")

        // Custom lava-bar sprites (1×1 solid-colour pixels, stretched at render time)
        private val LAVA_BAR_BG: Identifier =
            Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "container/foundry/lava_bar_bg")
        private val LAVA_BAR_FILL: Identifier =
            Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "container/foundry/lava_bar_fill")

        // Lava bar layout (relative to GUI origin)
        private const val BAR_X = 152
        private const val BAR_Y = 15
        private const val BAR_W = 12
        private const val BAR_H = 42   // total height including 1-px border
        private const val FILL_W = 10
        private const val FILL_H = 40  // usable interior height
    }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2
    }

    /**
     * Draws background texture plus all animated elements.
     * Following AbstractFurnaceScreen: everything goes here so slot items render on top.
     */
    override fun extractBackground(
        graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float
    ) {
        val xo = leftPos
        val yo = topPos

        // ── Static background ─────────────────────────────────────────────────
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            CONTAINER_TEXTURE,
            xo,
            yo,
            0.0f,
            0.0f,
            imageWidth,
            imageHeight,
            BACKGROUND_TEXTURE_WIDTH,
            BACKGROUND_TEXTURE_HEIGHT
        )

        // ── Slot outlines for slots the blast-furnace texture doesn't cover ───
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.withDefaultNamespace("container/slot"),
            xo + 115,
            yo + 49,
            18,
            18   // byproduct slot
        )
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED,
            Identifier.withDefaultNamespace("container/slot"),
            xo + 150,
            yo + 58,
            18,
            18   // lava bucket slot
        )

        // ── Fire / lit-progress animation ─────────────────────────────────────
        if (menu.isBurning()) {
            val burnMax = menu.getFuelBurnMax()
            val burnLeft = menu.getFuelBurnLeft()
            val fireH = (burnLeft * 14 / burnMax).coerceIn(0, 14)
            val vOff = 14 - fireH
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                LIT_PROGRESS_SPRITE,
                14,
                14,                    // full sprite size
                0,
                vOff,                   // skip top vOff rows (shrinks from top)
                xo + 57,
                yo + 36 + vOff,  // screen pos follows the shrink
                14,
                fireH
            )
        }

        // ── Progress arrow ────────────────────────────────────────────────────
        val arrowW = (menu.getSmeltProgress() * 24 / menu.getSmeltTotal()).coerceIn(0, 24)
        if (arrowW > 0) {
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                BURN_PROGRESS_SPRITE,
                24,
                16,          // full sprite size
                0,
                0,
                xo + 79,
                yo + 34,
                arrowW,
                16       // partial width from left
            )
        }

        // ── Lava tank bar ─────────────────────────────────────────────────────
        // Dark frame
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, LAVA_BAR_BG, xo + BAR_X, yo + BAR_Y, BAR_W, BAR_H
        )
        // Orange fill, grows from the bottom
        val lavaPercent = menu.getLavaPercent()
        if (lavaPercent > 0) {
            val fillH = (lavaPercent * FILL_H / 100).coerceIn(1, FILL_H)
            val vOff = FILL_H - fillH
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                LAVA_BAR_FILL,
                FILL_W,
                FILL_H,
                0,
                vOff,
                xo + BAR_X + 1,
                yo + BAR_Y + 1 + vOff,
                FILL_W,
                fillH
            )
        }
    }

    /** Tooltips only — animated elements are in extractBackground. */
    override fun extractRenderState(
        graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        extractTooltip(graphics, mouseX, mouseY)
    }
}
