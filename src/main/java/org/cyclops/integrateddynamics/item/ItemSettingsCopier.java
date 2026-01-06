package org.cyclops.integrateddynamics.item;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.cyclops.cyclopscore.helper.BlockEntityHelpers;
import org.cyclops.integrateddynamics.Capabilities;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;
import org.cyclops.integrateddynamics.api.block.ISettingsCopyable;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.part.IPartState;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.core.helper.CableHelpers;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

/**
 * Item capable of copying and pasting settings across blocks and network parts
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ItemSettingsCopier extends Item {

    public ItemSettingsCopier(Properties properties) {
        super(properties);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader world, BlockPos pos, Player player) {
        if (world instanceof Level level) {
            // Allow pass-through of shift click to the block if the block we are looking at is a cable (to allow interaction with parts)
            return CableHelpers.getCable(level, pos, null, world.getBlockState(pos)).isPresent();
        }
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        ItemStack itemStack = context.getItemInHand();
        if (context.getPlayer() != null) {
            Optional<ISettingsCopyable> settingsCopyable = BlockEntityHelpers.getCapability(context.getLevel(), context.getClickedPos(), context.getClickedFace(), Capabilities.SettingsCopyable.BLOCK);
            if (settingsCopyable.isPresent()) {
                if (context.getPlayer().isSecondaryUseActive()) {
                    return handleSettingsCopyInteraction(itemStack, settingsCopyable.get(), context.getPlayer());
                } else {
                    return handleSettingsPasteInteraction(context.getLevel(), itemStack, settingsCopyable.get(), context.getPlayer());
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemStack, context, list, flag);

        if (itemStack.has(RegistryEntries.DATACOMPONENT_COPIED_SETTINGS)) {
            IBlockSettings blockSettings = itemStack.get(RegistryEntries.DATACOMPONENT_COPIED_SETTINGS);
            list.add(Component.translatable("item.integrateddynamics.settings_copier.contents", blockSettings.getSettingsTypeText().plainCopy().withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.YELLOW));
            blockSettings.addTooltipText(list);
        }
    }

    public <P extends IPartType<P, S>, S extends IPartState<P>> InteractionResult copyPastePartSettings(IPartType<P, S> partType, IPartState<P> partState, ItemStack itemStack, Player player, InteractionHand hand, PartPos center) {
        // Network is not available on the client, as such local prediction will never be accurate.
        // So we just consume the action and let the server handle it
        if (player.level().isClientSide()) {
            return InteractionResult.CONSUME;
        }

        Optional<INetwork> network = NetworkHelpers.getNetwork(center);
        @SuppressWarnings("unchecked")
        Optional<ISettingsCopyable> settingsCopyable = network.flatMap(NetworkHelpers::getPartNetwork).flatMap(partNetwork ->
                ((IPartState<IPartType<P, S>>) partState).getCapability(partType, Capabilities.SettingsCopyable.PART, network.get(), partNetwork, PartTarget.fromCenter(center)));

        if (settingsCopyable.isPresent()) {
            if (player.isSecondaryUseActive()) {
                return handleSettingsCopyInteraction(itemStack, settingsCopyable.get(), player);
            } else {
                return handleSettingsPasteInteraction(player.level(), itemStack, settingsCopyable.get(), player);
            }
        }
        return InteractionResult.PASS;
    }

    protected InteractionResult handleSettingsCopyInteraction(ItemStack itemStack, ISettingsCopyable settingsCopyable, Player player) {
        IBlockSettings copiedSettings = settingsCopyable.copySettings();
        itemStack.set(RegistryEntries.DATACOMPONENT_COPIED_SETTINGS, copiedSettings);
        player.displayClientMessage(Component.translatable("item.integrateddynamics.settings_copier.copy.success", copiedSettings.getSettingsTypeText()), true);
        return InteractionResult.SUCCESS;
    }

    protected InteractionResult handleSettingsPasteInteraction(Level level, ItemStack itemStack, ISettingsCopyable settingsCopyable, Player player) {
        if (itemStack.has(RegistryEntries.DATACOMPONENT_COPIED_SETTINGS)) {
            IBlockSettings blockSettings = itemStack.get(RegistryEntries.DATACOMPONENT_COPIED_SETTINGS);
            Optional<Component> pasteErrorMessage = settingsCopyable.canPasteSettings(blockSettings, player);

            if (pasteErrorMessage.isPresent()) {
                player.displayClientMessage(pasteErrorMessage.get().plainCopy().withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }

            if (!level.isClientSide()) {
                settingsCopyable.pasteSettings(blockSettings, player);
            }
            player.displayClientMessage(Component.translatable("item.integrateddynamics.settings_copier.paste.success"), true);
            return InteractionResult.SUCCESS;
        } else {
            player.displayClientMessage(Component.translatable("item.integrateddynamics.settings_copier.paste.no_settings").withStyle(ChatFormatting.RED), true);
            return InteractionResult.CONSUME;
        }
    }
}
