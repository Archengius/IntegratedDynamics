package org.cyclops.integrateddynamics.core.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.resources.ResourceLocation;
import org.cyclops.integrateddynamics.api.block.BlockSettingsType;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;
import org.cyclops.integrateddynamics.api.block.IBlockSettingsTypeRegistry;

import java.util.Map;
import java.util.Optional;

public class BlockSettingsTypeRegistry implements IBlockSettingsTypeRegistry {

    private static final BlockSettingsTypeRegistry INSTANCE = new BlockSettingsTypeRegistry();

    private final Map<ResourceLocation, BlockSettingsType> blockSettingsTypeLookup = Maps.newHashMap();
    private final Map<BlockSettingsType, ResourceLocation> identifierLookup = Maps.newHashMap();

    private final Codec<BlockSettingsType> typeCodec = ResourceLocation.CODEC.flatXmap(
            identifier -> getBlockSettingsType(identifier).map(DataResult::success).orElse(DataResult.error(() -> "Unknown block settings type: " + identifier)),
            blockSettingsType -> getIdentifier(blockSettingsType).map(DataResult::success).orElse(DataResult.error(() -> "Unregistered block settings type: " + blockSettingsType)));
    private final Codec<IBlockSettings> codec = typeCodec.dispatch("settings", IBlockSettings::getType, BlockSettingsType::codec);

    private BlockSettingsTypeRegistry() {
    }

    /**
     * @return The unique instance.
     */
    public static BlockSettingsTypeRegistry getInstance() {
        return INSTANCE;
    }

    @Override
    public void register(ResourceLocation identifier, BlockSettingsType blockSettingsType) {
        if (blockSettingsTypeLookup.containsKey(identifier)) {
            throw new IllegalArgumentException("Attempt to register block settings ID that is already registered: " + identifier);
        }
        if (identifierLookup.containsKey(blockSettingsType)) {
            throw new IllegalArgumentException("Attempt to register block settings type that is already registered: " + blockSettingsType + " with identifier " + identifier);
        }
        this.blockSettingsTypeLookup.put(identifier, blockSettingsType);
        this.identifierLookup.put(blockSettingsType, identifier);
    }

    @Override
    public Optional<BlockSettingsType> getBlockSettingsType(ResourceLocation identifier) {
        return Optional.of(blockSettingsTypeLookup.get(identifier));
    }

    @Override
    public Optional<ResourceLocation> getIdentifier(BlockSettingsType blockSettingsType) {
        return Optional.ofNullable(identifierLookup.get(blockSettingsType));
    }

    @Override
    public Codec<IBlockSettings> codec() {
        return codec;
    }
}
