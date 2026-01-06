package org.cyclops.integrateddynamics.core.blocksettings;

import lombok.Getter;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;


public abstract class BlockSettingsActiveVariableBase implements IBlockSettings {

    @Getter protected final ItemStack variable;

    protected BlockSettingsActiveVariableBase(ItemStack variable) {
        this.variable = variable.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BlockSettingsActiveVariableBase that = (BlockSettingsActiveVariableBase) o;
        return ItemStack.matches(variable, that.variable);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(variable);
    }
}
