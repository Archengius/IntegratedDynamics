package org.cyclops.integrateddynamics.api.block;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.cyclops.cyclopscore.init.IRegistry;

import java.util.Optional;

/**
 * Registry for block settings types
 */
public interface IBlockSettingsTypeRegistry extends IRegistry {

    /**
     * Registers a new type of block settings
     *
     * @param identifier identifier of the block settings
     * @param blockSettingsType type to register
     */
    public void register(ResourceLocation identifier, BlockSettingsType blockSettingsType);

    /** @return block settings type by given ID */
    public Optional<BlockSettingsType> getBlockSettingsType(ResourceLocation identifier);

    /** @return an identifier for the given block settings type */
    public Optional<ResourceLocation> getIdentifier(BlockSettingsType blockSettingsType);

    /** @return the codec used to encode/decode block settings */
    public Codec<IBlockSettings> codec();
}
