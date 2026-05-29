package dev.lucien.foundry.screen

import dev.lucien.foundry.Foundry
import dev.lucien.foundry.block.entity.FoundryLavaTank
import dev.lucien.foundry.menu.FoundryMenu
import net.minecraft.ChatFormatting
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
        private val CONTAINER_TEXTURE =
            Identifier.fromNamespaceAndPath(Foundry.MOD_ID, "textures/gui/container/foundry.png")
        private val LIT_PROGRESS_SPRITE = Identifier.fromNamespaceAndPath(
            Foundry.MOD_ID, "container/foundry/lit_progress"
        )
        private val BURN_PROGRESS_SPRITE = Identifier.fromNamespaceAndPath(
            Foundry.MOD_ID, "container/foundry/burn_progress"
        )
        private val LAVA_FILL = Identifier.fromNamespaceAndPath(
            Foundry.MOD_ID, "container/foundry/lava_fill"
        )
        private val SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot")


        private const val FILL_X = FoundryMenu.BAR_X + 1
        private const val FILL_Y = FoundryMenu.BAR_Y + 1
        private const val FILL_W = FoundryMenu.BAR_W - 2
        private const val FILL_H = FoundryMenu.BAR_H - 2

        private const val LAVA_FRAME = 16   // height of one lava-fill sprite frame (16×16)
    }

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2
    }

    // ── Background ────────────────────────────────────────────────────────────

    override fun extractBackground(
        graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float
    ) {
        val xo = leftPos;
        val yo = topPos

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

        for ((sx, sy) in FoundryMenu.ALL_SLOT_POSITIONS) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_SPRITE, xo + sx, yo + sy, 18, 18)
        }

        if (menu.isBurning()) {
            val fireH =
                (menu.getFuelBurnLeft() * FoundryMenu.FLAME_H / menu.getFuelBurnMax()).coerceIn(
                    0, FoundryMenu.FLAME_H
                )
            val textureY = FoundryMenu.FLAME_H - fireH
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                LIT_PROGRESS_SPRITE,
                FoundryMenu.FLAME_W,
                FoundryMenu.FLAME_H,
                0,
                textureY,
                xo + FoundryMenu.FLAME_X + 1,
                yo + FoundryMenu.FLAME_Y + textureY,
                FoundryMenu.FLAME_W,
                fireH
            )
        }

        val arrowW =
            (menu.getSmeltProgress() * FoundryMenu.ARROW_W / menu.getSmeltTotal()).coerceIn(
                0, FoundryMenu.ARROW_W
            )
        if (arrowW > 0) {
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                BURN_PROGRESS_SPRITE,
                FoundryMenu.ARROW_W,
                FoundryMenu.ARROW_H,
                0,
                0,
                xo + FoundryMenu.ARROW_X,
                yo + FoundryMenu.ARROW_Y,
                arrowW,
                FoundryMenu.ARROW_H
            )
        }

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

        val barAbsX = leftPos + FoundryMenu.BAR_X
        val barAbsY = topPos + FoundryMenu.BAR_Y
        if (mouseX in barAbsX until barAbsX + FoundryMenu.BAR_W && mouseY in barAbsY until barAbsY + FoundryMenu.BAR_H) {
            graphics.setTooltipForNextFrame(
                Component.literal("${menu.getLavaMb()}")
                    .append(
                        Component.literal(" / ${FoundryLavaTank.CAPACITY_MB} mB")
                            .withStyle(ChatFormatting.GRAY)
                    ),
                mouseX,
                mouseY
            )
        }
    }

    // ── Lava fill helper ──────────────────────────────────────────────────────

    private fun renderLavaTile(
        graphics: GuiGraphicsExtractor,
        x: Int, y: Int, width: Int, height: Int,
    ) {
        var yy = 0
        while (yy < height) {
            val rowH = minOf(LAVA_FRAME, height - yy)
            graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED, LAVA_FILL,
                LAVA_FRAME, LAVA_FRAME, 0, 0,
                x, y + yy, width, rowH,
            )
            yy += rowH
        }
    }
}
