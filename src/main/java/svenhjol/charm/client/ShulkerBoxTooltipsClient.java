package svenhjol.charm.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ShulkerBoxTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import svenhjol.charm.base.CharmClientModule;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.base.CharmResources;
import svenhjol.charm.base.helper.ItemHelper;
import svenhjol.charm.base.helper.ItemNBTHelper;
import svenhjol.charm.handler.TooltipInventoryHandler;
import svenhjol.charm.mixin.accessor.ShulkerBoxTileEntityAccessor;

import java.util.List;

public class ShulkerBoxTooltipsClient extends CharmClientModule {
    public ShulkerBoxTooltipsClient(CharmModule module) {
        super(module);
    }

    @SubscribeEvent
    public void onRenderTooltip(RenderTooltipEvent.PostBackground event) {
        handleRenderTooltip(event.getMatrixStack(), event.getStack(), event.getLines(), event.getX(), event.getY());
    }

    private boolean handleRenderTooltip(MatrixStack matrices, ItemStack stack, List<? extends ITextProperties> lines, int tx, int ty) {
        final Minecraft mc = Minecraft.getInstance();

        if (ItemHelper.getBlockClass(stack) != ShulkerBoxBlock.class)
            return false;

        if (!stack.hasTag())
            return false;

        CompoundNBT tag = ItemNBTHelper.getCompound(stack, "BlockEntityTag", true);

        if (tag == null)
            return false;

        if (!tag.contains("id", 8)) {
            tag = tag.copy();
            tag.putString("id", "minecraft:shulker_box");
        }
        BlockItem blockItem = (BlockItem) stack.getItem();
        TileEntity tileEntity = TileEntity.readTileEntity(blockItem.getBlock().getDefaultState(), tag);
        if (tileEntity == null)
            return false;

        ShulkerBoxTileEntity shulkerbox = (ShulkerBoxTileEntity) tileEntity;
        NonNullList<ItemStack> items = ((ShulkerBoxTileEntityAccessor)shulkerbox).getItems();

        int size = shulkerbox.getSizeInventory();

        int x = tx - 5;
        int y = ty - 35;
        int w = 172;
        int h = 27;
        int right = x + w;

        if (right > mc.getMainWindow().getScaledWidth())
            x -= (right - mc.getMainWindow().getScaledWidth());

        if (y < 0)
            y = ty + lines.size() * 10 + 5;

        RenderSystem.pushMatrix();
        RenderHelper.enableStandardItemLighting();
        RenderSystem.enableRescaleNormal();
        RenderSystem.color3f(1f, 1f, 1f);
        RenderSystem.translatef(0, 0, 700);
        mc.getTextureManager().bindTexture(CharmResources.SLOT_WIDGET);

        RenderHelper.disableStandardItemLighting();
        TooltipInventoryHandler.renderTooltipBackground(mc, matrices, x, y, 9, 3, -1);
        RenderSystem.color3f(1f, 1f, 1f);

        ItemRenderer render = mc.getItemRenderer();
        RenderHelper.enableStandardItemLighting();
        RenderSystem.enableDepthTest();

        for (int i = 0; i < size; i++) {
            ItemStack itemstack;

            try {
                itemstack = items.get(i);
            } catch (Exception e) {
                // catch null issue with itemstack. Needs investigation. #255
                continue;
            }
            int xp = x + 6 + (i % 9) * 18;
            int yp = y + 6 + (i / 9) * 18;

            if (!itemstack.isEmpty()) {
                render.renderItemAndEffectIntoGUI(itemstack, xp, yp);
                render.renderItemOverlays(mc.fontRenderer, itemstack, xp, yp);
            }
        }

        RenderSystem.disableDepthTest();
        RenderSystem.disableRescaleNormal();
        RenderSystem.popMatrix();
        return true;
    }
}
