package org.cyclops.integrateddynamics.blocksettings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integrateddynamics.api.block.BlockSettingsType;

@Getter
@EqualsAndHashCode(callSuper = true)
public class BlockSettingsDelay extends BlockSettingsProxy {

    public static final MapCodec<BlockSettingsDelay> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("variable", ItemStack.EMPTY).forGetter(BlockSettingsDelay::getVariable),
            Codec.INT.optionalFieldOf("capacity", 1).forGetter(BlockSettingsDelay::getCapacity),
            Codec.INT.optionalFieldOf("update_interval", 1).forGetter(BlockSettingsDelay::getUpdateInterval)
        ).apply(instance, BlockSettingsDelay::new)
    );
    public static BlockSettingsType TYPE = new BlockSettingsType(CODEC);

    private final int capacity;
    private final int updateInterval;

    public BlockSettingsDelay(ItemStack variable, int capacity, int updateInterval) {
        super(variable);
        this.capacity = capacity;
        this.updateInterval = updateInterval;
    }

    @Override
    public BlockSettingsType getType() {
        return TYPE;
    }

    @Override
    public Component getSettingsTypeText() {
        return Component.translatable("gui.integrateddynamics.block_settings.delay");
    }
}
