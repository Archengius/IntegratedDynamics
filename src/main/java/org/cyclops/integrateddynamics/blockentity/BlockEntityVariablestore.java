package org.cyclops.integrateddynamics.blockentity;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.compress.utils.Lists;
import org.cyclops.cyclopscore.datastructure.DimPos;
import org.cyclops.cyclopscore.helper.MinecraftHelpers;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.cyclopscore.persist.IDirtyMarkListener;
import org.cyclops.integrateddynamics.Capabilities;
import org.cyclops.integrateddynamics.IntegratedDynamics;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.block.IBlockSettings;
import org.cyclops.integrateddynamics.api.block.ISettingsCopyable;
import org.cyclops.integrateddynamics.api.block.IVariableContainer;
import org.cyclops.integrateddynamics.api.evaluate.variable.ValueDeseralizationContext;
import org.cyclops.integrateddynamics.api.item.IVariableFacade;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandler;
import org.cyclops.integrateddynamics.api.item.IVariableFacadeHandlerRegistry;
import org.cyclops.integrateddynamics.api.network.INetworkElement;
import org.cyclops.integrateddynamics.api.network.INetworkElementProvider;
import org.cyclops.integrateddynamics.api.network.INetworkEventListener;
import org.cyclops.integrateddynamics.api.network.event.INetworkEvent;
import org.cyclops.integrateddynamics.blocksettings.BlockSettingsVariableStore;
import org.cyclops.integrateddynamics.capability.networkelementprovider.NetworkElementProviderSingleton;
import org.cyclops.integrateddynamics.capability.variablecontainer.VariableContainerDefault;
import org.cyclops.integrateddynamics.core.blockentity.BlockEntityCableConnectableInventory;
import org.cyclops.integrateddynamics.core.network.event.VariableContentsUpdatedEvent;
import org.cyclops.integrateddynamics.inventory.container.ContainerVariablestore;
import org.cyclops.integrateddynamics.network.VariablestoreNetworkElement;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * A part entity used to store variables.
 * Internally, this also acts as an expression cache
 * @author rubensworks
 */
public class BlockEntityVariablestore extends BlockEntityCableConnectableInventory
        implements IDirtyMarkListener, INetworkEventListener<VariablestoreNetworkElement>, MenuProvider {

    public static final int ROWS = 5;
    public static final int COLS = 9;
    public static final int INVENTORY_SIZE = ROWS * COLS;

    private final IVariableContainer variableContainer;
    @Getter
    private final ISettingsCopyable settingsCopyable;

    private boolean shouldSendUpdateEvent = false;

    public BlockEntityVariablestore(BlockPos blockPos, BlockState blockState) {
        super(RegistryEntries.BLOCK_ENTITY_VARIABLE_STORE.get(), blockPos, blockState, BlockEntityVariablestore.INVENTORY_SIZE, 1);
        getInventory().addDirtyMarkListener(this);
        variableContainer = new VariableContainerDefault();
        this.settingsCopyable = new SettingsCopyable();
    }

    public static class CapabilityRegistrar extends BlockEntityCableConnectableInventory.CapabilityRegistrar<BlockEntityVariablestore> {
        public CapabilityRegistrar(Supplier<BlockEntityType<? extends BlockEntityVariablestore>> blockEntityType) {
            super(blockEntityType);
        }

        @Override
        public void populate() {
            super.populate();

            add(
                    net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,
                    (blockEntity, context) -> blockEntity.getInventory().getItemHandler()
            );
            add(
                    Capabilities.NetworkElementProvider.BLOCK,
                    (blockEntity, context) -> blockEntity.getNetworkElementProvider()
            );
            add(
                    Capabilities.VariableContainer.BLOCK,
                    (blockEntity, context) -> blockEntity.getVariableContainer()
            );
            add(
                    Capabilities.SettingsCopyable.BLOCK,
                    (blockEntity, context) -> blockEntity.getSettingsCopyable()
            );
        }
    }

    @Override
    public INetworkElementProvider getNetworkElementProvider() {
        return new NetworkElementProviderSingleton() {
            @Override
            public INetworkElement createNetworkElement(Level world, BlockPos blockPos) {
                return new VariablestoreNetworkElement(DimPos.of(world, blockPos));
            }
        };
    }

    @Override
    protected SimpleInventory createInventory(int inventorySize, int stackSize) {
        return new SimpleInventory(inventorySize, stackSize) {
            @Override
            public boolean canPlaceItem(int slot, ItemStack itemStack) {
                return super.canPlaceItem(slot, itemStack)
                        && (itemStack.isEmpty() || itemStack.getCapability(Capabilities.VariableFacade.ITEM) != null);
            }
        };
    }

    public IVariableContainer getVariableContainer() {
        return variableContainer;
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider provider) {
        super.read(tag, provider);
        shouldSendUpdateEvent = true;
    }

    protected void refreshVariables(boolean sendVariablesUpdateEvent) {
        variableContainer.refreshVariables(getNetwork(), getInventory(), sendVariablesUpdateEvent, ValueDeseralizationContext.of(getLevel()));
    }

    protected BlockSettingsVariableStore copySettings() {
        return new BlockSettingsVariableStore(Arrays.stream(getInventory().getItemStacks()).map(ItemStack::copy).toList());
    }

    protected Optional<Component> canPasteSettings(BlockSettingsVariableStore blockSettings, @Nullable Player player) {
        if (blockSettings.getType() != BlockSettingsVariableStore.TYPE ||
                blockSettings.getVariableSlots().size() != getInventory().getContainerSize()) {
            return Optional.of(Component.translatable("gui.integrateddynamics.block_settings.error.invalid_target"));
        }

        if (player != null && !player.hasInfiniteMaterials()) {
            int variablesNeeded = (int) blockSettings.getVariableSlots().stream().filter(itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get())).count();
            int variablesAvailable = player.getInventory().countItem(RegistryEntries.ITEM_VARIABLE.get());

            if (variablesAvailable < variablesNeeded) {
                return Optional.of(Component.translatable("gui.integrateddynamics.block_settings.error.no_variable_cards"));
            }
        }
        return Optional.empty();
    }

    protected void pasteSettings(BlockSettingsVariableStore blockSettings, @Nullable Player player) {
        // Consume cards needed to fill the slots
        if (player != null && !player.hasInfiniteMaterials()) {
            int variablesNeeded = (int) blockSettings.getVariableSlots().stream().filter(itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get())).count();
            ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get()), variablesNeeded, false);
        }

        // Clear the contents of our current inventory (and drop existing cards in player inventory
        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            ItemStack stackInSlot = getInventory().getItem(i).copy();
            if (player != null && !stackInSlot.isEmpty() && !player.addItem(stackInSlot)) {
                player.drop(stackInSlot, false);
            }
        }
        getInventory().clearContent();

        List<Pair<Integer, Pair<ItemStack, Optional<IVariableFacade>>>> slotsToCopiedVariableFacades = Lists.newArrayList();
        Int2IntOpenHashMap newVariableIdLookup = new Int2IntOpenHashMap();

        // Deserialize variables in the original inventory and build the lookup of original variable IDs to copied variable IDs
        IVariableFacadeHandlerRegistry facadeHandlerRegistry = IntegratedDynamics._instance.getRegistryManager().getRegistry(IVariableFacadeHandlerRegistry.class);
        ValueDeseralizationContext valueDeseralizationContext = ValueDeseralizationContext.of(level);

        for (int i = 0; i < getInventory().getContainerSize(); i++) {
            ItemStack originalItemStack = blockSettings.getVariableSlots().get(i);

            if (originalItemStack.is(RegistryEntries.ITEM_VARIABLE.get())) {
                ItemStack copiedItemStack = new ItemStack(RegistryEntries.ITEM_VARIABLE.get());
                Optional<IVariableFacade> optionalCopiedVariableFacade = Optional.empty();
                IVariableFacade originalVariableFacade = RegistryEntries.ITEM_VARIABLE.get().getVariableFacade(valueDeseralizationContext, originalItemStack);

                // If the original facade is valid, we can copy it now with a new ID
                if (originalVariableFacade != null && originalVariableFacade.isValid()) {
                    ItemStack potentialCopiedItemStack = facadeHandlerRegistry.copy(true, originalItemStack);
                    IVariableFacade copiedVariableFacade = RegistryEntries.ITEM_VARIABLE.get().getVariableFacade(valueDeseralizationContext, potentialCopiedItemStack);

                    if (copiedVariableFacade != null && copiedVariableFacade.isValid()) {
                        // We have copied the variable facade with a new ID, store the new facade to the list and add ID lookup from old facade to the new one
                        newVariableIdLookup.put(originalVariableFacade.getId(), copiedVariableFacade.getId());
                        copiedItemStack = potentialCopiedItemStack;
                        optionalCopiedVariableFacade = Optional.of(copiedVariableFacade);
                    }
                }
                slotsToCopiedVariableFacades.add(Pair.of(i, Pair.of(copiedItemStack, optionalCopiedVariableFacade)));
            }
        }

        // Update variable references on item stacks now using the mapping we built previously, and add items to our inventory
        for (Pair<Integer, Pair<ItemStack, Optional<IVariableFacade>>> pair : slotsToCopiedVariableFacades) {
            ItemStack copiedItemStack = pair.getSecond().getFirst();

            if (pair.getSecond().getSecond().isPresent()) {
                IVariableFacadeHandler variableFacadeHandler = facadeHandlerRegistry.getHandler(copiedItemStack);
                IVariableFacade updatedVariableFacade = pair.getSecond().getSecond().get();
                updatedVariableFacade.replaceVariableReferences(newVariableIdLookup);
                copiedItemStack = facadeHandlerRegistry.writeVariableFacadeItem(copiedItemStack, updatedVariableFacade, variableFacadeHandler);
            }
            getInventory().setItem(pair.getFirst(), copiedItemStack);
        }

        // Notify that our inventory has changed
        getInventory().setChanged();
    }

    @Override
    public void onDirty() {
        if(!level.isClientSide()) {
            refreshVariables(true);
        }
    }

    // Make sure that when this TE is loaded, and after the network has been set,
    // that we trigger a variable update event in the network.

    @Override
    public void onLoad() {
        super.onLoad();
        if(!MinecraftHelpers.isClientSide()) {
            shouldSendUpdateEvent = true;
        }
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
    public void onEvent(INetworkEvent event, VariablestoreNetworkElement networkElement) {
        if(event instanceof VariableContentsUpdatedEvent) {
            refreshVariables(false);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player playerEntity) {
        return new ContainerVariablestore(id, playerInventory, this.getInventory());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.integrateddynamics.variablestore");
    }

    public static class Ticker extends BlockEntityCableConnectableInventory.Ticker<BlockEntityVariablestore> {
        @Override
        protected void update(Level level, BlockPos pos, BlockState blockState, BlockEntityVariablestore blockEntity) {
            super.update(level, pos, blockState, blockEntity);

            if (blockEntity.shouldSendUpdateEvent && blockEntity.getNetwork() != null) {
                blockEntity.shouldSendUpdateEvent = false;
                blockEntity.refreshVariables(true);
            }
        }
    }

    private class SettingsCopyable implements ISettingsCopyable {
        @Override
        public IBlockSettings copySettings() {
            return BlockEntityVariablestore.this.copySettings();
        }

        @Override
        public Optional<Component> canPasteSettings(IBlockSettings blockSettings, @Nullable Player player) {
            if (blockSettings instanceof BlockSettingsVariableStore blockSettingsVariableStore) {
                return BlockEntityVariablestore.this.canPasteSettings(blockSettingsVariableStore, player);
            }
            return Optional.of(Component.translatable("gui.integrateddynamics.block_settings.error.invalid_target"));
        }

        @Override
        public void pasteSettings(IBlockSettings blockSettings, @Nullable Player player) {
            if (blockSettings instanceof BlockSettingsVariableStore blockSettingsVariableStore &&
                    BlockEntityVariablestore.this.canPasteSettings(blockSettingsVariableStore, player).isEmpty() &&
                    !BlockEntityVariablestore.this.getLevel().isClientSide()) {
                BlockEntityVariablestore.this.pasteSettings(blockSettingsVariableStore, player);
            }
        }
    }
}
