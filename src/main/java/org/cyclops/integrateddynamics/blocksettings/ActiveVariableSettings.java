package org.cyclops.integrateddynamics.blocksettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public record ActiveVariableSettings(int variableSlot, ItemStack activeVariable) {

    public static final MapCodec<ActiveVariableSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.optionalFieldOf("variable_slot", 0).forGetter(ActiveVariableSettings::variableSlot),
        ItemStack.OPTIONAL_CODEC.optionalFieldOf("active_variable", ItemStack.EMPTY).forGetter(ActiveVariableSettings::activeVariable)
    ).apply(instance, ActiveVariableSettings::new));

    public ActiveVariableSettings(int variableSlot, ItemStack activeVariable) {
        this.variableSlot = variableSlot;
        this.activeVariable = activeVariable.copy();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ActiveVariableSettings that = (ActiveVariableSettings) o;
        return variableSlot == that.variableSlot && ItemStack.matches(activeVariable, that.activeVariable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableSlot, ItemStack.hashItemAndComponents(activeVariable));
    }
}
