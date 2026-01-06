package org.cyclops.integrateddynamics.blocksettings;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.cyclops.integrateddynamics.api.block.BlockSettingsType;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.core.part.PartTypeRegistry;

@EqualsAndHashCode(callSuper = true)
public class BlockSettingsActiveVariablePart extends BlockSettingsPart {

    public static final MapCodec<BlockSettingsActiveVariablePart> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PartTypeRegistry.getInstance().codec().fieldOf("part_type").forGetter(BlockSettingsActiveVariablePart::getPartType),
            BasicPartSettings.CODEC.codec().fieldOf("basic_settings").forGetter(BlockSettingsActiveVariablePart::getBasicPartSettings),
            ActiveVariableSettings.CODEC.codec().fieldOf("active_variable_settings").forGetter(BlockSettingsActiveVariablePart::getActiveVariableSettings)
        ).apply(instance, BlockSettingsActiveVariablePart::new)
    );
    public static BlockSettingsType TYPE = new BlockSettingsType(CODEC);

    @Getter private final ActiveVariableSettings activeVariableSettings;

    public BlockSettingsActiveVariablePart(IPartType partType, BasicPartSettings basicPartSettings, ActiveVariableSettings activeVariableSettings) {
        super(partType, basicPartSettings);
        this.activeVariableSettings = activeVariableSettings;
    }

    @Override
    public BlockSettingsType getType() {
        return TYPE;
    }
}
