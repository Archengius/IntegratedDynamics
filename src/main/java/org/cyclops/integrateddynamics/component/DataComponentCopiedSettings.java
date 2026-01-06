package org.cyclops.integrateddynamics.component;

import net.minecraft.network.codec.ByteBufCodecs;
import org.cyclops.cyclopscore.config.extendedconfig.DataComponentConfig;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;
import org.cyclops.integrateddynamics.core.block.BlockSettingsTypeRegistry;

public class DataComponentCopiedSettings extends DataComponentConfig<IBlockSettings> {

    public DataComponentCopiedSettings() {
        super(IntegratedDynamics._instance, "copied_settings", builder -> builder
                .persistent(BlockSettingsTypeRegistry.getInstance().codec())
                .networkSynchronized(ByteBufCodecs.fromCodec(BlockSettingsTypeRegistry.getInstance().codec())));
    }
}
