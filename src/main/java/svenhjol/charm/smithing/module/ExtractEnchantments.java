package svenhjol.charm.smithing.module;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import svenhjol.charm.Charm;
import svenhjol.charm.base.CharmCategories;
import svenhjol.meson.MesonModule;
import svenhjol.meson.iface.Config;
import svenhjol.meson.iface.Module;

import java.util.HashMap;
import java.util.Map;

@Module(mod = Charm.MOD_ID, category = CharmCategories.SMITHING, hasSubscriptions = true)
public class ExtractEnchantments extends MesonModule
{
    @Config(name = "Base XP cost", description = "Minimum cost before adding XP equivalent to the enchantment level(s) of the item.")
    public static int baseCost = 1;

    @Config(name = "Enchantment levels lost", description = "Number of levels that enchantments are weakened when converting.")
    public static int levelsLost = 1;

    @SubscribeEvent
    public void onAnvilUpdate(AnvilUpdateEvent event)
    {
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        ItemStack out;

        if (left.isEmpty() || right.isEmpty()) return;
        if (right.getItem() != Items.BOOK) return;

        ListNBT leftTags = left.getEnchantmentTagList();
        ListNBT rightTags = right.getEnchantmentTagList();

        if (leftTags.isEmpty() || !rightTags.isEmpty()) return;

        int cost = baseCost;

        Map<Enchantment, Integer> inEnchants = EnchantmentHelper.getEnchantments(left);
        Map<Enchantment, Integer> outEnchants = new HashMap<>();

        // get all enchantments from the left item and create a map of weakened enchantments
        for (Map.Entry<Enchantment, Integer> entry : inEnchants.entrySet()) {
            Enchantment ench = entry.getKey();
            if (ench == null) return;

            int level = entry.getValue();
            int newLevel = level - levelsLost;
            if (newLevel > 0) {
                outEnchants.put(ench, newLevel);
                cost += newLevel;
            }
        }

        if (outEnchants.values().size() == 0) {
            event.setCanceled(true);
            return;
        }

        // add repair cost on the input item
        if (left.getTag() != null && !left.getTag().isEmpty()) {
            cost += left.getTag().getInt("RepairCost");
        }

        // apply enchantments to the book
        out = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.setEnchantments(outEnchants, out);

        // set the display name on the returned item
        String name = event.getName();
        if (!name.isEmpty()) {
            out.setDisplayName(new StringTextComponent(name));
        }

        event.setCost(cost);
        event.setMaterialCost(1);
        event.setOutput(out);
    }
}