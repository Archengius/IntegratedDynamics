package org.cyclops.integrateddynamics.blocksettings;

import net.minecraft.resources.ResourceLocation;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.Reference;
import org.cyclops.integrateddynamics.api.block.IBlockSettingsTypeRegistry;

public class BlockSettingsTypes {

    public static IBlockSettingsTypeRegistry REGISTRY = IntegratedDynamics._instance.getRegistryManager()
            .getRegistry(IBlockSettingsTypeRegistry.class);

    public static void load() {
        REGISTRY.register(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "variable_store"), BlockSettingsVariableStore.TYPE);
        REGISTRY.register(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "proxy"), BlockSettingsProxy.TYPE);
        REGISTRY.register(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "delay"), BlockSettingsDelay.TYPE);
        REGISTRY.register(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "materializer"), BlockSettingsMaterializer.TYPE);
        REGISTRY.register(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "part"), BlockSettingsPart.TYPE);
        REGISTRY.register(ResourceLocation.fromNamespaceAndPath(Reference.MOD_ID, "active_variable_part"), BlockSettingsActiveVariablePart.TYPE);
    }
}
