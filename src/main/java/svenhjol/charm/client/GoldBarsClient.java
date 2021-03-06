package svenhjol.charm.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import svenhjol.charm.base.CharmClientModule;
import svenhjol.charm.base.CharmModule;
import svenhjol.charm.module.GoldBars;

public class GoldBarsClient extends CharmClientModule {
    public GoldBarsClient(CharmModule module) {
        super(module);
    }

    @Override
    public void register() {
        RenderTypeLookup.setRenderLayer(GoldBars.GOLD_BARS, RenderType.getCutout());
    }
}
