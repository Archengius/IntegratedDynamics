package org.cyclops.integrateddynamics.blocksettings;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.cyclops.integrateddynamics.api.block.BlockSettingsType;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.core.part.PartTypeRegistry;

@EqualsAndHashCode
public class BlockSettingsPart implements IBlockSettings {

    public static final MapCodec<BlockSettingsPart> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PartTypeRegistry.getInstance().codec().fieldOf("part_type").forGetter(BlockSettingsPart::getPartType),
            BasicPartSettings.CODEC.codec().fieldOf("basic_settings").forGetter(BlockSettingsPart::getBasicPartSettings)
        ).apply(instance, BlockSettingsPart::new)
    );
    public static BlockSettingsType TYPE = new BlockSettingsType(CODEC);

    @Getter private final IPartType partType;
    @Getter private final BasicPartSettings basicPartSettings;

    public BlockSettingsPart(IPartType partType, BasicPartSettings basicPartSettings) {
        this.partType = partType;
        this.basicPartSettings = basicPartSettings;
    }

    @Override
    public BlockSettingsType getType() {
        return TYPE;
    }

    @Override
    public Component getSettingsTypeText() {
        return partType != null ? Component.translatable(partType.getTranslationKey()) : Component.empty();
    }
}
