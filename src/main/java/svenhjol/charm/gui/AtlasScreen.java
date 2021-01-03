package svenhjol.charm.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.util.InputMappings;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapData;
import svenhjol.charm.Charm;
import svenhjol.charm.base.CharmResources;
import svenhjol.charm.base.gui.AbstractCharmContainerScreen;
import svenhjol.charm.base.gui.CharmImageButton;
import svenhjol.charm.base.helper.MapRenderHelper;
import svenhjol.charm.container.AtlasContainer;
import svenhjol.charm.container.AtlasInventory;
import svenhjol.charm.container.AtlasInventory.Index;
import svenhjol.charm.message.ServerAtlasTransfer;
import svenhjol.charm.message.ServerAtlasTransfer.MoveMode;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author Lukas
 * @since 28.12.2020
 */
public class AtlasScreen extends AbstractCharmContainerScreen<AtlasContainer> {
    private static final ResourceLocation CONTAINER_BACKGROUND = new ResourceLocation(Charm.MOD_ID, "textures/gui/atlas_container.png");
    private static final int SIZE = 48;
    private static final int LEFT = 74;
    private static final int TOP = 16;
    private static final int BUTTON_SIZE = 9;
    private static final int BUTTON_DISTANCE = 3;
    private static final int CENTER = (SIZE - BUTTON_SIZE) / 2;
    private static final int MAX_MAPS = 8;
    private static final int NORMAL_SIZE = 128;
    private static final float BASE_SCALE = (float) SIZE / NORMAL_SIZE;
    private static final int LIGHT = 240;
    private final int slot;
    private final Map<ButtonDirection, CharmImageButton> buttons;
    private final WorldMap worldMap = new WorldMap();
    private final SingleMap singleMap = new SingleMap(null);
    private MapGui mapGui;
    private int lastSize;

    public AtlasScreen(AtlasContainer screenContainer, PlayerInventory inv, ITextComponent titleIn) {
        super(screenContainer, inv, titleIn, CONTAINER_BACKGROUND);
        this.xSize = 175;
        this.ySize = 168;
        this.slot = inv.getSlotFor(screenContainer.getAtlasInventory().getAtlasItem());
        Map<Index, AtlasInventory.MapInfo> mapInfos = screenContainer.getAtlasInventory().getMapInfos();
        lastSize = mapInfos.size();
        mapGui = mapInfos.size() > 1 ? getWorldMap() : getSingleMap(mapInfos.isEmpty() ? null : mapInfos.values().iterator().next());
        buttons = new EnumMap<>(ButtonDirection.class);
        for (ButtonDirection direction : ButtonDirection.values()) {
            buttons.put(direction, createButton(direction));
        }
    }

    private CharmImageButton createButton(ButtonDirection dir) {
        return new CharmImageButton(() -> guiLeft + LEFT + dir.left, () -> guiTop + TOP + dir.top, dir.size, dir.size,
                dir.texStart, 0, dir.size, 2 * dir.size, CharmResources.INVENTORY_BUTTONS, it -> mapGui.buttonClick(dir));
    }

    private static boolean isShiftClick() {
        long handle = Minecraft.getInstance().getMainWindow().getHandle();
        return InputMappings.isKeyDown(handle, 340) || InputMappings.isKeyDown(handle, 344);
    }

    private WorldMap getWorldMap() {
        return worldMap;
    }

    private SingleMap getSingleMap(AtlasInventory.MapInfo mapInfo) {
        singleMap.mapInfo = mapInfo;
        return singleMap;
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        updateGui();
        updateButtonState();
        super.render(matrices, mouseX, mouseY, delta);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrices, int mouseX, int mouseY) {
        super.drawGuiContainerForegroundLayer(matrices, mouseX, mouseY);
        MapRenderHelper.renderMapWithBackground(matrices, LEFT, TOP, 0, BASE_SCALE, LIGHT, bufferSource -> mapGui.render(matrices, bufferSource, mouseX, mouseY));
    }

    private void updateGui() {
        Map<Index, AtlasInventory.MapInfo> mapInfos = container.getAtlasInventory().getMapInfos();
        int size = mapInfos.size();
        if (mapGui instanceof WorldMap) {
            if (mapInfos.size() <= 1) {
                changeGui(getSingleMap(mapInfos.isEmpty() ? null : mapInfos.values().iterator().next()));
            }
        } else if (mapGui instanceof SingleMap) {
            if (size > lastSize) {
                mapInfos.values().stream().skip(size - 1).findAny().ifPresent(it -> changeGui(getSingleMap(it)));
            }
        }
        lastSize = size;
    }

    private void updateButtonState() {
        buttons.forEach((direction, button) -> {
            button.visible = mapGui.buttonVisible(direction);
            button.active = mapGui.buttonEnabled(direction);
            if (button.visible) {
                if (!super.buttons.contains(button)) addButton(button);
            } else {
                removeButton(button);
            }
        });
    }

    private void removeButton(Widget button) {
        super.buttons.remove(button);
        super.children.remove(button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mapGui.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void handleMouseClick(Slot slotIn, int slotId, int mouseButton, ClickType type) {
        if (type == ClickType.QUICK_MOVE) {
            Charm.PACKET_HANDLER.sendToServer(new ServerAtlasTransfer(slot, slotIn.getSlotIndex(), -1, MoveMode.FROM_INVENTORY));
        } else {
            super.handleMouseClick(slotIn, slotId, mouseButton, type);
        }
    }

    private void changeGui(MapGui gui) {
        mapGui = gui;
    }

    private enum ButtonDirection {
        LEFT(-BUTTON_SIZE - BUTTON_DISTANCE, CENTER, BUTTON_SIZE, 77, -1, 0),
        TOP(CENTER, -BUTTON_SIZE - BUTTON_DISTANCE, BUTTON_SIZE, 50, 0, -1),
        RIGHT(SIZE + BUTTON_DISTANCE, CENTER, BUTTON_SIZE, 68, 1, 0),
        BOTTOM(CENTER, SIZE + BUTTON_DISTANCE, BUTTON_SIZE, 59, 0, 1),
        BACK(82, -12, 16, 86, 0, 0);
        final int left;
        final int top;
        final int size;
        final int texStart;
        final int x;
        final int y;

        ButtonDirection(int left, int top, int size, int texStart, int x, int y) {
            this.left = left;
            this.top = top;
            this.size = size;
            this.texStart = texStart;
            this.x = x;
            this.y = y;
        }
    }

    private interface MapGui {
        void render(MatrixStack matrices, IRenderTypeBuffer.Impl bufferSource, int mouseX, int mouseY);

        default boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        void buttonClick(ButtonDirection direction);

        boolean buttonVisible(ButtonDirection direction);

        boolean buttonEnabled(ButtonDirection direction);
    }

    private class WorldMap implements MapGui {
        private Index corner = null;
        private int minX = 0;
        private int maxX = 0;
        private int minY = 0;
        private int maxY = 0;
        private int maxMapDistance = 1;

        private WorldMap() {
        }

        private boolean updateExtremes() {
            AtlasInventory inventory = container.getAtlasInventory();
            Map<Index, AtlasInventory.MapInfo> mapInfos = inventory.getMapInfos();
            if (mapInfos.isEmpty()) {
                return false;
            }
            minX = mapInfos.keySet().stream().map(it -> it.x).min(Integer::compareTo).orElseThrow(IllegalStateException::new);
            maxX = mapInfos.keySet().stream().map(it -> it.x).max(Integer::compareTo).orElseThrow(IllegalStateException::new);
            minY = mapInfos.keySet().stream().map(it -> it.y).min(Integer::compareTo).orElseThrow(IllegalStateException::new);
            maxY = mapInfos.keySet().stream().map(it -> it.y).max(Integer::compareTo).orElseThrow(IllegalStateException::new);
            maxMapDistance = Math.max(maxX + 1 - minX, maxY + 1 - minY);
            if (maxMapDistance > MAX_MAPS) {
                maxMapDistance = MAX_MAPS;
                if (corner == null) {
                    int x1 = inventory.convertCoordToIndex((int) (playerInventory.player.getPosX() + 64)) - MAX_MAPS / 2;
                    x1 = Math.max(minX, Math.min(maxX + 1 - MAX_MAPS, x1));
                    int y1 = inventory.convertCoordToIndex((int) (playerInventory.player.getPosZ() + 64)) - MAX_MAPS / 2;
                    y1 = Math.max(minY, Math.min(maxY + 1 - MAX_MAPS, y1));
                    corner = Index.of(x1, y1);
                }
            } else {
                corner = null;
            }
            return true;
        }

        @Override
        public void render(MatrixStack matrices, IRenderTypeBuffer.Impl bufferSource, int mouseX, int mouseY) {
            final Minecraft mc = Minecraft.getInstance();
            final World world = mc.world;
            if (world == null || !updateExtremes()) {
                return;
            }
            float mapSize = (float) NORMAL_SIZE / maxMapDistance;
            float mapScale = 1f / maxMapDistance;
            int currentMinX = corner != null ? corner.x : minX;
            int currentMinY = corner != null ? corner.y : minY;
            for (Map.Entry<Index, AtlasInventory.MapInfo> mapInfo : container.getAtlasInventory().getMapInfos().entrySet()) {
                Index key = mapInfo.getKey();
                if (corner != null && (corner.x > key.x || key.x >= corner.x + MAX_MAPS || corner.y > key.y || key.y >= corner.y + MAX_MAPS)) {
                    continue;
                }
                MapData mapData = world.getMapData(FilledMapItem.getMapName(mapInfo.getValue().id));
                if (mapData != null) {
                    matrices.push();
                    matrices.translate(mapSize * (key.x - currentMinX), mapSize * (key.y - currentMinY), 1.0);
                    matrices.scale(mapScale, mapScale, 1);
                    mc.gameRenderer.getMapItemRenderer().renderMap(matrices, bufferSource, mapData, false, LIGHT);
                    matrices.pop();
                }
            }
            bufferSource.finish();
            drawLines(matrices);
        }

        private void drawLines(MatrixStack matrices) {
            matrices.push();
            //need to revert the base scale to avoid some lines being to thin to be drawn
            matrices.scale( 0.5f / BASE_SCALE, 0.5f / BASE_SCALE, 1);
            for (int xLine = 1; xLine < maxMapDistance; ++xLine) {
                vLine(matrices, xLine * 2 * SIZE / maxMapDistance, 0, 2 * SIZE, -1);
            }
            for (int yLine = 1; yLine < maxMapDistance; ++yLine) {
                hLine(matrices, 0, 2 * SIZE, yLine * 2 * SIZE / maxMapDistance, -1);
            }
            matrices.pop();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double normX = normalizeForMapArea(LEFT + guiLeft, mouseX);
            double normY = normalizeForMapArea(TOP + guiTop, mouseY);
            if (button == 0 && 0 <= normX && normX < 1 && 0 <= normY && normY < 1) {
                ItemStack heldItem = playerInventory.getItemStack();
                if (!heldItem.isEmpty()) {
                    Charm.PACKET_HANDLER.sendToServer(new ServerAtlasTransfer(slot, -1, -1, MoveMode.FROM_HAND));
                } else {
                    if (updateExtremes()) {
                        int currentMinX = corner != null ? corner.x : minX;
                        int currentMinY = corner != null ? corner.y : minY;
                        int x = (int) (normX * maxMapDistance) + currentMinX;
                        int y = (int) (normY * maxMapDistance) + currentMinY;
                        AtlasInventory.MapInfo mapInfo = container.getAtlasInventory().getMapInfos().get(Index.of(x, y));
                        if (mapInfo != null) {
                            if (isShiftClick()) {
                                Charm.PACKET_HANDLER.sendToServer(
                                        new ServerAtlasTransfer(slot, mapInfo.x, mapInfo.z, MoveMode.TO_INVENTORY));
                            } else {
                                changeGui(getSingleMap(mapInfo));
                            }
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void buttonClick(ButtonDirection direction) {
            if (corner != null) {
                int x = corner.x + direction.x * MAX_MAPS;
                x = Math.max(minX, Math.min(maxX + 1 - MAX_MAPS, x));
                int y = corner.y + direction.y * MAX_MAPS;
                y = Math.max(minY, Math.min(maxY + 1 - MAX_MAPS, y));
                corner = Index.of(x, y);
            }
        }

        @Override
        public boolean buttonVisible(ButtonDirection direction) {
            return direction != ButtonDirection.BACK && corner != null;
        }

        @Override
        public boolean buttonEnabled(ButtonDirection direction) {
            if (corner != null) {
                switch (direction) {
                    case LEFT:
                        return corner.x > minX;
                    case TOP:
                        return corner.y > minY;
                    case RIGHT:
                        return corner.x + MAX_MAPS <= maxX;
                    case BOTTOM:
                        return corner.y + MAX_MAPS <= maxY;
                    case BACK:
                        return false;
                }
            }
            return false;
        }

        private double normalizeForMapArea(double base, double val) {
            return (val - base) / SIZE;
        }
    }

    private class SingleMap implements MapGui {
        private AtlasInventory.MapInfo mapInfo;

        public SingleMap(AtlasInventory.MapInfo mapInfo) {
            this.mapInfo = mapInfo;
        }

        @Override
        public void render(MatrixStack matrices, IRenderTypeBuffer.Impl bufferSource, int mouseX, int mouseY) {
            final Minecraft mc = Minecraft.getInstance();
            final World world = mc.world;
            if (world == null) {
                return;
            }
            if (mapInfo != null) {
                MapData mapData = world.getMapData(FilledMapItem.getMapName(mapInfo.id));
                if (mapData != null) {
                    matrices.push();
                    matrices.translate(0, 0, 1);
                    Minecraft.getInstance().gameRenderer.getMapItemRenderer().renderMap(matrices, bufferSource, mapData, false, LIGHT);
                    matrices.pop();
                }
            }
        }

        @Override
        public void buttonClick(ButtonDirection direction) {
            if (direction == ButtonDirection.BACK) {
                changeGui(getWorldMap());
            } else {
                AtlasInventory inventory = container.getAtlasInventory();
                int mapX = inventory.convertCoordToIndex(mapInfo.x);
                int mapY = inventory.convertCoordToIndex(mapInfo.z);
                AtlasInventory.MapInfo mapInfo1 = inventory.getMapInfos().get(Index.of(mapX + direction.x, mapY + direction.y));
                if (mapInfo1 != null) {
                    changeGui(getSingleMap(mapInfo1));
                }
            }
        }

        @Override
        public boolean buttonVisible(ButtonDirection direction) {
            return true;
        }

        @Override
        public boolean buttonEnabled(ButtonDirection direction) {
            AtlasInventory inventory = container.getAtlasInventory();
            Map<Index, AtlasInventory.MapInfo> mapInfos = inventory.getMapInfos();
            if (direction == ButtonDirection.BACK) {
                return mapInfo == null && !mapInfos.isEmpty() || mapInfos.size() > 1;
            }
            if (mapInfo != null) {
                int mapX = inventory.convertCoordToIndex(mapInfo.x);
                int mapY = inventory.convertCoordToIndex(mapInfo.z);
                return mapInfos.containsKey(Index.of(mapX + direction.x, mapY + direction.y));
            }
            return false;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0 && guiLeft + LEFT <= mouseX && mouseX < guiLeft + LEFT + SIZE && guiTop + TOP <= mouseY && mouseY < guiTop + TOP + SIZE) {
                ItemStack heldItem = playerInventory.getItemStack();
                if (!heldItem.isEmpty()) {
                    Charm.PACKET_HANDLER.sendToServer(new ServerAtlasTransfer(slot, -1, -1, MoveMode.FROM_HAND));
                } else if (mapInfo != null) {
                    if (isShiftClick()) {
                        Charm.PACKET_HANDLER.sendToServer(new ServerAtlasTransfer(slot, mapInfo.x, mapInfo.z, MoveMode.TO_INVENTORY));
                    } else {
                        Charm.PACKET_HANDLER.sendToServer(new ServerAtlasTransfer(slot, mapInfo.x, mapInfo.z, MoveMode.TO_HAND));
                    }
                    changeGui(getSingleMap(null));
                }
                return true;
            }
            return false;
        }
    }
}