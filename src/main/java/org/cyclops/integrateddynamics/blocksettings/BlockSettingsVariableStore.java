package org.cyclops.integrateddynamics.blocksettings;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integrateddynamics.api.block.BlockSettingsType;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;

import java.util.List;
import java.util.function.Predicate;

public class BlockSettingsVariableStore implements IBlockSettings {

    public static final MapCodec<BlockSettingsVariableStore> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.list(ItemStack.OPTIONAL_CODEC).optionalFieldOf("variable_slots", ImmutableList.of()).forGetter(BlockSettingsVariableStore::getVariableSlots)
        ).apply(instance, BlockSettingsVariableStore::new)
    );
    public static BlockSettingsType TYPE = new BlockSettingsType(CODEC);

    @Getter private final List<ItemStack> variableSlots;

    public BlockSettingsVariableStore(List<ItemStack> variableSlots) {
        this.variableSlots = variableSlots.stream().map(ItemStack::copy).toList();
    }

    @Override
    public BlockSettingsType getType() {
        return TYPE;
    }

    @Override
    public Component getSettingsTypeText() {
        return Component.translatable("gui.integrateddynamics.block_settings.variable_store");
    }

    @Override
    public void addTooltipText(List<Component> tooltipLines) {
        int variablesStored = (int) variableSlots.stream().filter(Predicate.not(ItemStack::isEmpty)).count();
        tooltipLines.add(Component.translatable("gui.integrateddynamics.block_settings.variable_store.variables", variablesStored).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BlockSettingsVariableStore that = (BlockSettingsVariableStore) o;
        return ItemStack.listMatches(variableSlots, that.variableSlots);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(variableSlots);
    }
}
