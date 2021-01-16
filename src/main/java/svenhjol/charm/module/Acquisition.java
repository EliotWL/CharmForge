package svenhjol.charm.module;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import svenhjol.charm.Charm;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.base.handler.ModuleHandler;
import svenhjol.charm.base.helper.EnchantmentsHelper;
import svenhjol.charm.base.helper.PlayerHelper;
import svenhjol.charm.base.iface.Module;
import svenhjol.charm.enchantment.AcquisitionEnchantment;

import java.util.List;

@Module(mod = Charm.MOD_ID, description = "Tools with the Acquisition enchantment automatically pick up drops.")
public class Acquisition extends CharmModule {
    private static final ThreadLocal<PlayerEntity> breakingPlayer = new ThreadLocal<>();
    public static AcquisitionEnchantment ACQUISITION;

    @Override
    public void register() {
        ACQUISITION = new AcquisitionEnchantment(this);
    }

    public static void startBreaking(PlayerEntity player, ItemStack tool) {
        if (ModuleHandler.enabled(Acquisition.class) && EnchantmentsHelper.has(tool, ACQUISITION)) {
            breakingPlayer.set(player);
        }
    }

    public static void stopBreaking() {
        breakingPlayer.remove();
    }

    public static boolean trySpawnToInventory(World world, ItemStack stack) {
        //copy checks from Block#spawnAsEntity
        if (!world.isRemote && !stack.isEmpty() && world.getGameRules().getBoolean(GameRules.DO_TILE_DROPS) && !world.restoringBlockSnapshots) {
            PlayerEntity player = breakingPlayer.get();
            if (player != null) {
                PlayerHelper.addOrDropStack(player, stack);
                return true;
            }
        }
        return false;
    }

    public static boolean trySpawnToInventory(PlayerEntity player, ItemStack tool, List<ItemStack> stacks) {
        if (ModuleHandler.enabled(Acquisition.class) && EnchantmentsHelper.has(tool, ACQUISITION)) {
            stacks.forEach(it -> PlayerHelper.addOrDropStack(player, it));
            return true;
        }
        return false;
    }
}
