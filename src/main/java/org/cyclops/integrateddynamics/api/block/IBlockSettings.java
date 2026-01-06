package org.cyclops.integrateddynamics.api.block;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface IBlockSettings {
    /**
     * Returns the type of these block settings used for serialization/deserialization
     *
     * @return type of these block settings
     */
    BlockSettingsType getType();

    /**
     * Returns type of the settings this object represents
     *
     * @return name of the type of the settings this object represents
     */
    Component getSettingsTypeText();

    /**
     * Allows block settings to add additional information to the tooltip
     *
     * @param tooltipLines lines to add tooltip text to
     */
    default void addTooltipText(List<Component> tooltipLines) {
    }
}
