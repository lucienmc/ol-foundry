package dev.lucien.foundry.screen

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.menu.FoundryMenu
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.ChatFormatting

class FoundryScreen(
    menu: FoundryMenu, inventory: Inventory, title: Component
) : AbstractContainerScreen<FoundryMenu>(menu, inventory, title) {

    companion object {
        private val CONTAINER_TEXTURE = Identifier.fromNamespaceAndPath(
            Foundry.MOD_ID, "textures/gui/container/foundry.png"
        )
        private val LIT_PROGRESS_SPRITE  = Identifier.withDefaultNamespace("container/blast_furnace/lit_progress")
        private val BURN_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/blast_furnace/burn_progress")
        private val LAVA_BAR_BG          = Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "container/foundry/lava_bar_bg")

        // Animated lava fill sprite — 8 frames of 16×16 stacked vertically (16×128 PNG).
        // The GUI sprite animation system ticks this automatically.
        private val LAVA_FILL = Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "container/foundry/lava_fill")

        private const val BAR_X  = 152;  private const val BAR_Y  = 15
        private const val BAR_W  = 12;   private const val BAR_H  = 42
        private const val FILL_X = BAR_X + 1
        private const val FILL_Y = BAR_Y + 1
        private const val FILL_W = 10;   private const val FILL_H = 40
        private const val LAVA_FRAME = 16   // sprite is 16×16 per frame
    }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2
    }

    // ── Background + animated elements ────────────────────────────────────────

    override fun extractBackground(
        graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float
    ) {
        val xo = leftPos;  val yo = topPos

        // Static background sheet
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, CONTAINER_TEXTURE,
            xo, yo, 0.0f, 0.0f,
            imageWidth, imageHeight,
            BACKGROUND_TEXTURE_WIDTH, BACKGROUND_TEXTURE_HEIGHT
        )

        // Slot outlines for slots the blast-furnace texture doesn't cover
        val slot = Identifier.withDefaultNamespace("container/slot")
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, slot, xo + 115, yo + 49, 18, 18)
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, slot, xo + 150, yo + 58, 18, 18)

        // Flame (shrinks from top as fuel burns)
        if (menu.isBurning()) {
            val fireH = (menu.getFuelBurnLeft() * 14 / menu.getFuelBurnMax()).coerceIn(0, 14)
            val vOff  = 14 - fireH
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, LIT_PROGRESS_SPRITE,
                14, 14, 0, vOff,
                xo + 57, yo + 36 + vOff, 14, fireH
            )
        }

        // Progress arrow (grows left→right)
        val arrowW = (menu.getSmeltProgress() * 24 / menu.getSmeltTotal()).coerceIn(0, 24)
        if (arrowW > 0) {
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, BURN_PROGRESS_SPRITE,
                24, 16, 0, 0,
                xo + 79, yo + 34, arrowW, 16
            )
        }

        // Lava bar frame
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, LAVA_BAR_BG, xo + BAR_X, yo + BAR_Y, BAR_W, BAR_H)

        // Lava fill — animated sprite, grows from bottom
        val pct = menu.getLavaPercent()
        if (pct > 0) {
            val fillH = (pct * FILL_H / 100).coerceIn(1, FILL_H)
            renderLavaTile(graphics, xo + FILL_X, yo + FILL_Y + (FILL_H - fillH), FILL_W, fillH)
        }

    }

    // ── Foreground ────────────────────────────────────────────────────────────

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, delta)
        extractTooltip(graphics, mouseX, mouseY)

        // Lava bar tooltip — show "X / 4000 mB" when hovering the gauge
        val barAbsX = leftPos + BAR_X
        val barAbsY = topPos  + BAR_Y
        if (mouseX in barAbsX until barAbsX + BAR_W && mouseY in barAbsY until barAbsY + BAR_H) {
            val mb = menu.getLavaMb()
            graphics.setTooltipForNextFrame(
                Component.literal("$mb")
                    .append(Component.literal(" / 4000 mB").withStyle(ChatFormatting.GRAY)),
                mouseX, mouseY
            )
        }
    }

    // ── Lava texture helper ───────────────────────────────────────────────────

    /**
     * Tiles the animated [LAVA_FILL] GUI sprite vertically to fill [width]×[height].
     *
     * The sprite is a 16×128 PNG (8 frames of 16×16).  Minecraft's GUI sprite
     * animation system automatically advances frames using lava_fill.png.mcmeta —
     * no block-atlas access required.
     *
     * We draw in 16-px rows so the top and bottom edges are never stretched;
     * only the last (possibly partial) row is clipped via blitSprite's uvY offset.
     */
    private fun renderLavaTile(
        graphics: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int, height: Int,
    ) {
        var yy = 0
        while (yy < height) {
            val rowH = minOf(LAVA_FRAME, height - yy)
            // blitSprite(pipeline, sprite, spriteW, spriteH, uvX, uvY, destX, destY, destW, destH)
            // spriteW/H = the full logical size of one frame (16×16).
            // uvY = 0 always — we start from the top of the current animated frame.
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, LAVA_FILL,
                LAVA_FRAME, LAVA_FRAME,
                0, 0,
                x, y + yy,
                width, rowH,
            )
            yy += rowH
        }
    }
}
