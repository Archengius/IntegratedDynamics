package org.cyclops.integrateddynamics.core.blockentity;

import com.google.common.collect.Sets;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.cyclops.cyclopscore.persist.IDirtyMarkListener;
import org.cyclops.cyclopscore.persist.nbt.NBTClassType;
import org.cyclops.integrateddynamics.Capabilities;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;
import org.cyclops.integrateddynamics.api.block.ISettingsCopyable;
import org.cyclops.integrateddynamics.api.evaluate.variable.IValue;
import org.cyclops.integrateddynamics.api.evaluate.variable.IVariable;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandlerRegistry;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.network.INetworkEventListener;
import org.cyclops.integrateddynamics.api.network.IPartNetwork;
import org.cyclops.integrateddynamics.api.network.event.INetworkEvent;
import org.cyclops.integrateddynamics.core.blocksettings.BlockSettingsActiveVariableBase;
import org.cyclops.integrateddynamics.core.evaluate.InventoryVariableEvaluator;
import org.cyclops.integrateddynamics.core.evaluate.variable.ValueTypes;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.network.event.VariableContentsUpdatedEvent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Base part entity that can hold variables.
 * @param <E> The type of event listener
 * @author rubensworks
 */
public abstract class BlockEntityActiveVariableBase<E> extends BlockEntityCableConnectableInventory implements IDirtyMarkListener, INetworkEventListener<E> {

    private final InventoryVariableEvaluator<IValue> evaluator;
    @Getter
    private final ISettingsCopyable settingsCopyable;

    public BlockEntityActiveVariableBase(BlockEntityType<?> type, BlockPos blockPos, BlockState blockState, int inventorySize) {
        super(type, blockPos, blockState, inventorySize, 1);
        getInventory().addDirtyMarkListener(this);

        this.evaluator = createEvaluator();
        this.settingsCopyable = new SettingsCopyable();
    }

    public static class CapabilityRegistrar<T extends BlockEntityActiveVariableBase<?>> extends BlockEntityCableConnectableInventory.CapabilityRegistrar<T> {
        public CapabilityRegistrar(Supplier<BlockEntityType<? extends T>> blockEntityType) {
            super(blockEntityType);
        }

        @Override
        public void populate() {
            super.populate();

            add(
                    Capabilities.ValueInterface.BLOCK,
                    (blockEntity, context) -> () -> {
                        INetwork network = blockEntity.getNetwork();
                        IPartNetwork partNetwork = NetworkHelpers.getPartNetworkChecked(network);
                        if (blockEntity.hasVariable()) {
                            IVariable<?> variable = blockEntity.getVariable(partNetwork);
                            if (variable != null) {
                                return Optional.of(variable.getValue());
                            }
                        }
                        return Optional.empty();
                    }
            );
            add(
                    Capabilities.SettingsCopyable.BLOCK,
                    (blockEntity, context) -> blockEntity.getSettingsCopyable()
            );
        }
    }

    protected InventoryVariableEvaluator<IValue> createEvaluator() {
        return new InventoryVariableEvaluator<>(this.getInventory(), getSlotRead(), () -> ValueDeseralizationContext.of(getLevel()), ValueTypes.CATEGORY_ANY);
    }

    public InventoryVariableEvaluator getEvaluator() {
        return evaluator;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        List<MutableComponent> errors = evaluator.getErrors();
        NBTClassType.writeNbt(List.class, "errors", errors, tag, provider);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider provider) {
        evaluator.setErrors(NBTClassType.readNbt(List.class, "errors", tag, provider));
        super.read(tag, provider);
    }

    public abstract int getSlotRead();

    public boolean hasVariable() {
        return !getInventory().getItem(getSlotRead()).isEmpty();
    }

    protected void updateReadVariable(boolean sendVariablesUpdateEvent) {
        evaluator.refreshVariable(getNetwork(), sendVariablesUpdateEvent);
        sendUpdate();
    }

    protected abstract BlockSettingsActiveVariableBase copySettings();

    protected Optional<Component> canPasteSettings(BlockSettingsActiveVariableBase blockSettings, @Nullable Player player) {
        if (blockSettings.getType() != copySettings().getType()) {
            return Optional.of(Component.translatable("gui.integrateddynamics.block_settings.error.invalid_target"));
        }

        if (player != null && !blockSettings.getVariable().isEmpty() && !player.hasInfiniteMaterials()) {
            int variableCardsAvailable = player.getInventory().countItem(RegistryEntries.ITEM_VARIABLE.get());
            int variableCardsNeeded = 1;
            if (variableCardsAvailable < variableCardsNeeded) {
                return Optional.of(Component.translatable("gui.integrateddynamics.block_settings.error.no_variable_cards"));
            }
        }
        return Optional.empty();
    }

    protected void pasteSettings(BlockSettingsActiveVariableBase blockSettings, @Nullable Player player) {
        ItemStack copiedVariableItemStack = blockSettings.getVariable();

        if (player != null && !copiedVariableItemStack.isEmpty() && !player.hasInfiniteMaterials()) {
            int variableCardsNeeded = 1;
            ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get()), variableCardsNeeded, false);
        }

        // Remove our currently inserted variable card
        ItemStack currentVariableItemStack = getInventory().getItem(getSlotRead()).copy();
        if (player != null && !currentVariableItemStack.isEmpty() && !player.addItem(currentVariableItemStack)) {
            player.drop(currentVariableItemStack, false);
        }
        getInventory().setItem(getSlotRead(), ItemStack.EMPTY);

        // Install a copy of the variable card to the slot now if we have one
        if (!copiedVariableItemStack.isEmpty()) {
            IVariableFacadeHandlerRegistry facadeHandlerRegistry = IntegratedDynamics._instance.getRegistryManager().getRegistry(IVariableFacadeHandlerRegistry.class);
            ItemStack pastedVariableItemStack = facadeHandlerRegistry.copy(true, copiedVariableItemStack);
            getInventory().setItem(getSlotRead(), pastedVariableItemStack);
        }

        // Notify that our inventory has changed
        getInventory().setChanged();
    }

    @Override
    public void onDirty() {
        if(!level.isClientSide()) {
            updateReadVariable(true);
        }
    }

    @Nullable
    public IVariable<?> getVariable(IPartNetwork network) {
        return evaluator.getVariable(getNetwork(), network);
    }

    @Override
    public boolean hasEventSubscriptions() {
        return true;
    }

    @Override
    public Set<Class<? extends INetworkEvent>> getSubscribedEvents() {
        return Sets.<Class<? extends INetworkEvent>>newHashSet(VariableContentsUpdatedEvent.class);
    }

    @Override
    public void onEvent(INetworkEvent event, E networkElement) {
        if(event instanceof VariableContentsUpdatedEvent) {
            updateReadVariable(false);
        }
    }

    @Override
    public void afterNetworkReAlive() {
        super.afterNetworkReAlive();
        updateReadVariable(true);
    }

    protected class SettingsCopyable implements ISettingsCopyable {
        @Override
        public IBlockSettings copySettings() {
            return BlockEntityActiveVariableBase.this.copySettings();
        }

        @Override
        public Optional<Component> canPasteSettings(IBlockSettings blockSettings, @Nullable Player player) {
            if (blockSettings instanceof BlockSettingsActiveVariableBase blockSettingsActiveVariableBase) {
                return BlockEntityActiveVariableBase.this.canPasteSettings(blockSettingsActiveVariableBase, player);
            }
            return Optional.of(Component.translatable("gui.integrateddynamics.block_settings.error.invalid_target"));
        }

        @Override
        public void pasteSettings(IBlockSettings blockSettings, @Nullable Player player) {
            if (blockSettings instanceof BlockSettingsActiveVariableBase blockSettingsActiveVariableBase &&
                    BlockEntityActiveVariableBase.this.canPasteSettings(blockSettingsActiveVariableBase, player).isEmpty() &&
                    !BlockEntityActiveVariableBase.this.getLevel().isClientSide()) {
                BlockEntityActiveVariableBase.this.pasteSettings(blockSettingsActiveVariableBase, player);
            }
        }
    }
}
