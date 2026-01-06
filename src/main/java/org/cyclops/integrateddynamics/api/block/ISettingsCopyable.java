package org.cyclops.integrateddynamics.api.block;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Capability for blocks that can have their settings copied and pasted with the wrench
 */
public interface ISettingsCopyable {
    /**
     * Copies settings from this block if possible
     * This will be called on both the client (as a prediction) and the server (as real action)
     *
     * @return copied settings, or empty optional if no settings can be copied
     */
    public IBlockSettings copySettings();

    /**
     * Checks whenever given settings can be pasted to this block
     * This will be called on both client and server
     *
     * @param blockSettings  settings to be pasted
     * @return empty optional if settings can be pasted, an explanation why settings cannot be pasted otherwise
     */
    public Optional<Component> canPasteSettings(IBlockSettings blockSettings, @Nullable Player player);

    /**
     * Attempts to paste the settings to this block
     * This will only be called on the server
     *
     * @param blockSettings settings previously copied via copySettings
     * @param player player initiating the action, can be null
     */
    public void pasteSettings(IBlockSettings blockSettings, @Nullable Player player);

}
