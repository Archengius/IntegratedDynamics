package org.cyclops.integrateddynamics.blocksettings;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.cyclops.integrateddynamics.api.block.BlockSettingsType;
import org.cyclops.integrateddynamics.core.blocksettings.BlockSettingsActiveVariableBase;

public class BlockSettingsMaterializer extends BlockSettingsActiveVariableBase {

    public static final MapCodec<BlockSettingsMaterializer> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("variable", ItemStack.EMPTY).forGetter(BlockSettingsMaterializer::getVariable)
        ).apply(instance, BlockSettingsMaterializer::new)
    );
    public static BlockSettingsType TYPE = new BlockSettingsType(CODEC);

    public BlockSettingsMaterializer(ItemStack variable) {
        super(variable);
    }

    @Override
    public BlockSettingsType getType() {
        return TYPE;
    }

    @Override
    public Component getSettingsTypeText() {
        return Component.translatable("gui.integrateddynamics.block_settings.materializer");
    }
}
