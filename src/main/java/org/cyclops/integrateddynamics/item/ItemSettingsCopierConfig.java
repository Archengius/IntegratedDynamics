package org.cyclops.integrateddynamics.item;

import net.minecraft.world.item.Item;
import org.cyclops.cyclopscore.config.extendedconfig.ItemConfig;
import org.cyclops.integrateddynamics.IntegratedDynamics;

public class ItemSettingsCopierConfig extends ItemConfig {

    public ItemSettingsCopierConfig() {
        super(
                IntegratedDynamics._instance,
                "settings_copier",
                eConfig -> new ItemSettingsCopier(new Item.Properties().stacksTo(1))
        );
    }

}
