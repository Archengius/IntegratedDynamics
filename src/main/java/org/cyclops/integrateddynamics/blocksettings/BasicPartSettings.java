package org.cyclops.integrateddynamics.blocksettings;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integrateddynamics.api.part.aspect.IAspect;
import org.cyclops.integrateddynamics.core.part.aspect.AspectRegistry;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record BasicPartSettings(int updateInterval, int priority, int channel, Optional<Direction> targetSide, int maxOffset, Vec3i targetOffset, List<ItemStack> offsetVariables, Map<IAspect, CompoundTag> aspectProperties) {

    public static final MapCodec<BasicPartSettings> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("update_interval", 1).forGetter(BasicPartSettings::updateInterval),
            Codec.INT.optionalFieldOf("priority", 0).forGetter(BasicPartSettings::priority),
            Codec.INT.optionalFieldOf("channel", 0).forGetter(BasicPartSettings::channel),
            Direction.CODEC.optionalFieldOf("target_side").forGetter(BasicPartSettings::targetSide),
            Codec.INT.optionalFieldOf("max_offset", 0).forGetter(BasicPartSettings::maxOffset),
            Vec3i.CODEC.optionalFieldOf("target_offset", Vec3i.ZERO).forGetter(BasicPartSettings::targetOffset),
            Codec.list(ItemStack.OPTIONAL_CODEC, 3, 3).optionalFieldOf("offset_variables", NonNullList.withSize(3, ItemStack.EMPTY)).forGetter(BasicPartSettings::offsetVariables),
            // TODO: Encode aspect properties via a codec and not as a compound tag, but right now they rely on having registry lookup provider which cannot be easily retrieved from DynamicOps
            Codec.unboundedMap(AspectRegistry.getInstance().codec(), CompoundTag.CODEC).fieldOf("aspect_properties").forGetter(BasicPartSettings::aspectProperties)
    ).apply(instance, BasicPartSettings::new));

    public BasicPartSettings(int updateInterval, int priority, int channel, Optional<Direction> targetSide, int maxOffset, Vec3i targetOffset, List<ItemStack> offsetVariables, Map<IAspect, CompoundTag> aspectProperties) {
        this.updateInterval = updateInterval;
        this.priority = priority;
        this.channel = channel;
        this.maxOffset = maxOffset;
        this.targetSide = targetSide;
        this.targetOffset = targetOffset;
        this.offsetVariables = offsetVariables.stream().map(ItemStack::copy).toList();
        this.aspectProperties = ImmutableMap.copyOf(aspectProperties);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BasicPartSettings that = (BasicPartSettings) o;
        return channel == that.channel && priority == that.priority && maxOffset == that.maxOffset && updateInterval == that.updateInterval &&
                Objects.equals(targetOffset, that.targetOffset) &&
                Objects.equals(targetSide, that.targetSide) &&
                ItemStack.listMatches(offsetVariables, that.offsetVariables) &&
                Objects.equals(aspectProperties, that.aspectProperties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(updateInterval, priority, channel, targetSide, maxOffset, targetOffset, ItemStack.hashStackList(offsetVariables), aspectProperties);
    }
}
