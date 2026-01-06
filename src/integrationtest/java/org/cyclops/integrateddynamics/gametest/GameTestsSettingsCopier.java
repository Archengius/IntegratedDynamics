package org.cyclops.integrateddynamics.gametest;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.cyclops.cyclopscore.inventory.SimpleInventory;
import org.cyclops.integrateddynamics.Reference;
import org.cyclops.integrateddynamics.RegistryEntries;
import org.cyclops.integrateddynamics.api.evaluate.EvaluationException;
import org.cyclops.integrateddynamics.api.network.INetwork;
import org.cyclops.integrateddynamics.api.part.IPartType;
import org.cyclops.integrateddynamics.api.part.PartPos;
import org.cyclops.integrateddynamics.api.part.PartTarget;
import org.cyclops.integrateddynamics.api.part.aspect.IAspectWrite;
import org.cyclops.integrateddynamics.api.part.aspect.property.IAspectProperties;
import org.cyclops.integrateddynamics.blockentity.BlockEntityDelay;
import org.cyclops.integrateddynamics.blockentity.BlockEntityVariablestore;
import org.cyclops.integrateddynamics.core.evaluate.operator.Operators;
import org.cyclops.integrateddynamics.core.evaluate.variable.*;
import org.cyclops.integrateddynamics.core.helper.NetworkHelpers;
import org.cyclops.integrateddynamics.core.helper.PartHelpers;
import org.cyclops.integrateddynamics.core.network.PartNetworkElement;
import org.cyclops.integrateddynamics.core.part.PartTypes;
import org.cyclops.integrateddynamics.core.part.read.PartStateReaderBase;
import org.cyclops.integrateddynamics.core.part.write.PartStateWriterBase;
import org.cyclops.integrateddynamics.core.part.write.PartTypeWriteBase;
import org.cyclops.integrateddynamics.part.PartTypeBlockReader;
import org.cyclops.integrateddynamics.part.PartTypePanelDisplay;
import org.cyclops.integrateddynamics.part.PartTypeRedstoneWriter;
import org.cyclops.integrateddynamics.part.aspect.Aspects;
import org.cyclops.integrateddynamics.part.aspect.write.AspectWriteBuilders;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.cyclops.integrateddynamics.gametest.GameTestHelpersIntegratedDynamics.*;

@GameTestHolder(Reference.MOD_ID)
@PrefixGameTestTemplate(false)
public class GameTestsSettingsCopier {

    public static final String TEMPLATE_EMPTY = "empty10";
    public static final BlockPos POS_SRC = BlockPos.ZERO.offset(2, 1, 2);
    public static final BlockPos POS_DEST = BlockPos.ZERO.offset(2, 1, 1);

    private InteractionResult playerFirstUseItemInHand(Player player, BlockPos blockPos, boolean shiftKeyDown) {
        InteractionHand interactionHand = InteractionHand.MAIN_HAND;
        ItemStack itemInMainHand = player.getItemInHand(interactionHand);
        BlockHitResult blockHitResult = new BlockHitResult(blockPos.getCenter(), Direction.SOUTH, blockPos, false);
        player.setShiftKeyDown(shiftKeyDown);
        return itemInMainHand.getItem().onItemUseFirst(itemInMainHand, new UseOnContext(player, interactionHand, blockHitResult));
    }

    @SuppressWarnings("rawtypes")
    private InteractionResult playerUseItemInHandOnPart(GameTestHelper helper, Player player, PartPos partPos, boolean shiftKeyDown) {
        InteractionHand interactionHand = InteractionHand.MAIN_HAND;
        ItemStack itemInMainHand = player.getItemInHand(interactionHand);
        BlockHitResult blockHitResult = new BlockHitResult(partPos.getPos().getBlockPos().getCenter(), Objects.requireNonNull(partPos.getSide()), partPos.getPos().getBlockPos(), false);

        player.setShiftKeyDown(shiftKeyDown);
        PartHelpers.PartStateHolder partStateHolder = PartHelpers.getPart(partPos);
        helper.assertTrue(partStateHolder != null, "part does not exist at pos " + partPos);
        return partStateHolder.getPart().onPartActivated(partStateHolder.getState(), partPos.getPos().getBlockPos(), helper.getLevel(), player, interactionHand, itemInMainHand, blockHitResult);
    }

    private void copyBlockSettings(GameTestHelper helper, Player player) {
        ItemStack settingsCopierItem = new ItemStack(RegistryEntries.ITEM_SETTINGS_COPIER.get());
        player.getInventory().add(settingsCopierItem.copy());
        player.getInventory().setPickedItem(settingsCopierItem);

        InteractionResult copyResult = playerFirstUseItemInHand(player, helper.absolutePos(POS_SRC), true);
        helper.assertValueEqual(copyResult, InteractionResult.SUCCESS, "copyResult");
        InteractionResult pasteResult = playerFirstUseItemInHand(player, helper.absolutePos(POS_DEST), false);
        helper.assertValueEqual(pasteResult, InteractionResult.SUCCESS, "pasteResult");
    }

    public void copyPartSettings(GameTestHelper helper, Player player, PartPos srcPartPos, PartPos destPartPos) {
        ItemStack settingsCopierItem = new ItemStack(RegistryEntries.ITEM_SETTINGS_COPIER.get());
        player.getInventory().add(settingsCopierItem.copy());
        player.getInventory().setPickedItem(settingsCopierItem);

        InteractionResult copyResult = playerUseItemInHandOnPart(helper, player, srcPartPos, true);
        helper.assertValueEqual(copyResult, InteractionResult.SUCCESS, "copyResult");
        InteractionResult pasteResult = playerUseItemInHandOnPart(helper, player, destPartPos, false);
        helper.assertValueEqual(pasteResult, InteractionResult.SUCCESS, "pasteResult");
    }

    private void placeDisplaysAndCheckValues(GameTestHelper helper, ItemStack sourceVariable, ItemStack destVariable, int expectedSourceValue, int expectedDestValue) {
        // Place two cables with displays above the variable stores to evaluate the values
        helper.setBlock(POS_SRC.above(), RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(POS_DEST.above(), RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS_SRC.above()), Direction.EAST, PartTypes.DISPLAY_PANEL, new ItemStack(PartTypes.DISPLAY_PANEL.getItem()));
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS_DEST.above()), Direction.EAST, PartTypes.DISPLAY_PANEL, new ItemStack(PartTypes.DISPLAY_PANEL.getItem()));

        // Place source and dest operator variables in displays
        PartTypePanelDisplay.State srcDisplayState = placeVariableInDisplayPanel(helper.getLevel(), PartPos.of(helper.getLevel(), helper.absolutePos(POS_SRC.above()), Direction.EAST), sourceVariable).getRight();
        PartTypePanelDisplay.State destDisplayState = placeVariableInDisplayPanel(helper.getLevel(), PartPos.of(helper.getLevel(), helper.absolutePos(POS_DEST.above()), Direction.EAST), destVariable).getRight();

        // Give network one frame to update the values
        helper.succeedOnTickWhen(1, () -> {
            // Variable display should show 2 distinct values
            assertValueEqual(srcDisplayState.getDisplayValue(), ValueTypeInteger.ValueInteger.of(expectedSourceValue));
            assertValueEqual(destDisplayState.getDisplayValue(), ValueTypeInteger.ValueInteger.of(expectedDestValue));
        });
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testVariableStoreCopying(GameTestHelper helper) {
        // Place source and destination variable stores
        helper.setBlock(POS_SRC, RegistryEntries.BLOCK_VARIABLE_STORE.value());
        helper.setBlock(POS_DEST, RegistryEntries.BLOCK_VARIABLE_STORE.value());

        BlockEntityVariablestore sourceVariableStore = helper.getBlockEntity(POS_SRC);

        // Create a 13 + 7 expression
        ItemStack srcVariable1 = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(13));
        ItemStack srcVariable2 = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(7));
        ItemStack srcVariable3 = createVariableForOperator(helper.getLevel(), Operators.ARITHMETIC_ADDITION, new int[] {
                getVariableFacade(helper.getLevel(), srcVariable1).getId(),
                getVariableFacade(helper.getLevel(), srcVariable2).getId()
        });

        // Place the expression at the source variable store
        sourceVariableStore.getInventory().setItem(2, srcVariable1);
        sourceVariableStore.getInventory().setItem(3, srcVariable2);
        sourceVariableStore.getInventory().setItem(4, srcVariable3);

        // Place one card in destination inventory that will be removed
        BlockEntityVariablestore destinationVariableStore = helper.getBlockEntity(POS_DEST);
        ItemStack destInitialVariable = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(13));
        destinationVariableStore.getInventory().setItem(0, destInitialVariable);

        // Copy the variable store settings now
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addItem(new ItemStack(RegistryEntries.ITEM_VARIABLE.get(), 3));
        copyBlockSettings(helper, player);

        // Should have only one card now in our inventory and that card would be the initial variable card
        int itemVariableCards = ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get()), 0, true);
        helper.assertValueEqual(itemVariableCards, 1, "itemVariableCards");
        helper.assertTrue(player.getInventory().findSlotMatchingItem(destInitialVariable) != -1, "dest initial variable not in player inventory");

        ItemStack destVariable1 = destinationVariableStore.getInventory().getItem(2);
        ItemStack destVariable2 = destinationVariableStore.getInventory().getItem(3);
        ItemStack destVariable3 = destinationVariableStore.getInventory().getItem(4);

        // Check that copied variables have different IDs from the originals
        List<Integer> originalVariableIds = ImmutableList.of(getVariableFacade(helper.getLevel(), srcVariable1).getId(),
                getVariableFacade(helper.getLevel(), srcVariable2).getId(), getVariableFacade(helper.getLevel(), srcVariable3).getId());
        helper.assertTrue(!originalVariableIds.contains(getVariableFacade(helper.getLevel(), destVariable1).getId()), "dest variable 1 has overlapping IDs with src variables");
        helper.assertTrue(!originalVariableIds.contains(getVariableFacade(helper.getLevel(), destVariable2).getId()), "dest variable 2 has overlapping IDs with src variables");
        helper.assertTrue(!originalVariableIds.contains(getVariableFacade(helper.getLevel(), destVariable3).getId()), "dest variable 3 has overlapping IDs with src variables");

        // Update second variable in the destination store
        destVariable2 = setVariableToValue(helper.getLevel(), destVariable2.copy(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(1));
        destinationVariableStore.getInventory().setItem(3, destVariable2);

        // Check values from source and destination now
        placeDisplaysAndCheckValues(helper, srcVariable3, destVariable3, 20, 14);
    }

    @GameTest(template = TEMPLATE_EMPTY)
    public void testDelayerCopying(GameTestHelper helper) {
        // Place source and destination delayers
        helper.setBlock(POS_SRC, RegistryEntries.BLOCK_DELAY.value());
        helper.setBlock(POS_DEST, RegistryEntries.BLOCK_DELAY.value());

        BlockEntityDelay sourceDelay = helper.getBlockEntity(POS_SRC);

        // Configure settings on the source delay
        ItemStack srcVariable = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(22));
        sourceDelay.getInventory().setItem(sourceDelay.getSlotRead(), srcVariable);
        sourceDelay.setUpdateInterval(20);
        sourceDelay.setCapacity(3);

        // Place initial variable in destination delay that will be removed by paste
        BlockEntityDelay destinationDelay = helper.getBlockEntity(POS_DEST);
        ItemStack destInitialVariable = new ItemStack(RegistryEntries.ITEM_VARIABLE.get());
        destinationDelay.getInventory().setItem(destinationDelay.getSlotRead(), destInitialVariable);

        // Copy the delayer settings now
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addItem(new ItemStack(RegistryEntries.ITEM_VARIABLE.get(), 1));
        copyBlockSettings(helper, player);

        // Should have only one card now in our inventory and that card would be the initial variable card
        int itemVariableCards = ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get()), 0, true);
        helper.assertValueEqual(itemVariableCards, 1, "itemVariableCards");
        helper.assertTrue(player.getInventory().findSlotMatchingItem(destInitialVariable) != -1, "dest initial variable not in player inventory");

        // Check that copied variable has different ID from the original
        ItemStack destinationVariable = destinationDelay.getInventory().getItem(destinationDelay.getSlotRead());
        helper.assertTrue(getVariableFacade(helper.getLevel(), srcVariable).getId() != getVariableFacade(helper.getLevel(), destinationVariable).getId(), "dest variable did not get unique ID");

        // Check that rest of the settings are copied
        helper.assertValueEqual(sourceDelay.getUpdateInterval(), destinationDelay.getUpdateInterval(), "updateInterval");
        helper.assertValueEqual(sourceDelay.getCapacity(), destinationDelay.getCapacity(), "capacity");

        placeDisplaysAndCheckValues(helper, srcVariable, destinationVariable, 22, 22);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @GameTest(template = TEMPLATE_EMPTY)
    public void testBlockReaderCopying(GameTestHelper helper) {
        // Place two cables with block readers
        helper.setBlock(POS_SRC, RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(POS_DEST, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS_SRC), Direction.EAST, PartTypes.BLOCK_READER, new ItemStack(PartTypes.BLOCK_READER.getItem()));
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS_DEST), Direction.EAST, PartTypes.BLOCK_READER, new ItemStack(PartTypes.BLOCK_READER.getItem()));

        PartPos srcPartPos = PartPos.of(helper.getLevel(), helper.absolutePos(POS_SRC), Direction.EAST);
        PartPos destPartPos = PartPos.of(helper.getLevel(), helper.absolutePos(POS_DEST), Direction.EAST);
        PartHelpers.PartStateHolder<PartTypeBlockReader, PartStateReaderBase<PartTypeBlockReader>> srcBlockReader = (PartHelpers.PartStateHolder) PartHelpers.getPart(srcPartPos);
        PartHelpers.PartStateHolder<PartTypeBlockReader, PartStateReaderBase<PartTypeBlockReader>> destBlockReader = (PartHelpers.PartStateHolder) PartHelpers.getPart(destPartPos);

        // Configure source reader with update interval, channel, priority, target side and offsets
        srcBlockReader.getState().setUpdateInterval(11);
        INetwork network = NetworkHelpers.getNetworkChecked(srcPartPos);
        PartNetworkElement networkElement = new PartNetworkElement<>((IPartType) srcBlockReader.getPart(), srcPartPos);
        network.setPriorityAndChannel(networkElement, -5, 30);
        srcBlockReader.getPart().setTargetSideOverride(srcBlockReader.getState(), Direction.DOWN);
        srcBlockReader.getState().setMaxOffset(4);
        srcBlockReader.getState().setTargetOffset(new Vec3i(1, 2, 0));

        // Set offset variable as well
        ItemStack sourceOffsetVariable = createVariableForValue(helper.getLevel(), ValueTypes.INTEGER, ValueTypeInteger.ValueInteger.of(2));
        SimpleInventory offsetVariablesInventory = new SimpleInventory(3, 1);
        offsetVariablesInventory.setItem(2, sourceOffsetVariable);
        srcBlockReader.getState().saveInventoryNamed("offsetVariablesInventory", offsetVariablesInventory);

        // Place two blocks that will be read
        Vec3i actualReadOffset = new Vec3i(1, 2, 2).relative(Direction.EAST);
        helper.setBlock(POS_SRC.offset(actualReadOffset), Blocks.BOOKSHELF);
        helper.setBlock(POS_DEST.offset(actualReadOffset), Blocks.COAL_BLOCK);

        // Copy the part settings now
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addItem(new ItemStack(RegistryEntries.ITEM_VARIABLE.get(), 1));
        ItemStack offsetEnhancement = new ItemStack(RegistryEntries.ITEM_ENHANCEMENT_OFFSET.get(), 2);
        RegistryEntries.ITEM_ENHANCEMENT_OFFSET.value().setEnhancementValue(offsetEnhancement, 2);
        player.addItem(offsetEnhancement);
        copyPartSettings(helper, player, srcPartPos, destPartPos);

        // Should not have any variable cards or offset enhancements left
        int leftoverCards = ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get()), 0, true);
        int leftoverEnhancements = ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get()), 0, true);
        helper.assertValueEqual(leftoverCards, 0, "leftoverCards");
        helper.assertValueEqual(leftoverEnhancements, 0, "leftoverEnhancements");

        // Make sure settings match now between src and dest parts
        helper.assertValueEqual(srcBlockReader.getState().getUpdateInterval(), destBlockReader.getState().getUpdateInterval(), "updateInterval");
        helper.assertValueEqual(srcBlockReader.getState().getPriority(), destBlockReader.getState().getPriority(), "priority");
        helper.assertValueEqual(srcBlockReader.getState().getChannel(), destBlockReader.getState().getChannel(), "channel");
        helper.assertValueEqual(Optional.ofNullable(srcBlockReader.getState().getTargetSideOverride()), Optional.ofNullable(destBlockReader.getState().getTargetSideOverride()), "targetSideOverride");
        helper.assertValueEqual(srcBlockReader.getState().getMaxOffset(), destBlockReader.getState().getMaxOffset(), "maxOffset");
        helper.assertValueEqual(srcBlockReader.getState().getTargetOffset(), destBlockReader.getState().getTargetOffset(), "targetOffset");

        SimpleInventory destOffsetVariablesInventory = new SimpleInventory(3, 1);
        destBlockReader.getState().loadInventoryNamed("offsetVariablesInventory", destOffsetVariablesInventory);
        ItemStack destOffsetVariable = destOffsetVariablesInventory.getItem(2);

        // Make sure offset variable cards have different IDs
        helper.assertTrue(getVariableFacade(helper.getLevel(), sourceOffsetVariable).getId() != getVariableFacade(helper.getLevel(), destOffsetVariable).getId(), "offset variable ID is not different");

        helper.succeedOnTickWhen(1, () -> {
            // Check that readers read the expected block states
            try {
                assertValueEqual(srcBlockReader.getState().getVariable(Aspects.Read.Block.BLOCK).getValue(), ValueObjectTypeBlock.ValueBlock.of(Blocks.BOOKSHELF.defaultBlockState()));
                assertValueEqual(destBlockReader.getState().getVariable(Aspects.Read.Block.BLOCK).getValue(), ValueObjectTypeBlock.ValueBlock.of(Blocks.COAL_BLOCK.defaultBlockState()));
            } catch (EvaluationException e) {
                throw new GameTestAssertException("Failed to evaluate block aspect for block readers: " + e.getMessage());
            }
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @GameTest(template = TEMPLATE_EMPTY)
    public void testRedstoneWriterCopying(GameTestHelper helper) {
        // Place two cables with effect writers
        helper.setBlock(POS_SRC, RegistryEntries.BLOCK_CABLE.value());
        helper.setBlock(POS_DEST, RegistryEntries.BLOCK_CABLE.value());
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS_SRC), Direction.EAST, PartTypes.REDSTONE_WRITER, new ItemStack(PartTypes.REDSTONE_WRITER.getItem()));
        PartHelpers.addPart(helper.getLevel(), helper.absolutePos(POS_DEST), Direction.EAST, PartTypes.REDSTONE_WRITER, new ItemStack(PartTypes.REDSTONE_WRITER.getItem()));

        PartPos srcPartPos = PartPos.of(helper.getLevel(), helper.absolutePos(POS_SRC), Direction.EAST);
        PartPos destPartPos = PartPos.of(helper.getLevel(), helper.absolutePos(POS_DEST), Direction.EAST);
        PartHelpers.PartStateHolder<PartTypeRedstoneWriter, PartStateWriterBase<PartTypeRedstoneWriter>> srcRedstoneWriter = (PartHelpers.PartStateHolder) PartHelpers.getPart(srcPartPos);
        PartHelpers.PartStateHolder<PartTypeRedstoneWriter, PartStateWriterBase<PartTypeRedstoneWriter>> destRedstoneWriter = (PartHelpers.PartStateHolder) PartHelpers.getPart(destPartPos);

        // Set source part aspect variable
        List<IAspectWrite> writeAspectsList = ((PartTypeWriteBase) srcRedstoneWriter.getPart()).getWriteAspects();
        int redstoneBoolSlotIndex = writeAspectsList.indexOf(Aspects.Write.Redstone.BOOLEAN);
        helper.assertTrue(redstoneBoolSlotIndex != -1, "redstone writer has no boolean redstone aspect");
        ItemStack sourceVariableCard = createVariableForValue(helper.getLevel(), ValueTypes.BOOLEAN, ValueTypeBoolean.ValueBoolean.of(true));
        srcRedstoneWriter.getState().getInventory().setItem(redstoneBoolSlotIndex, sourceVariableCard);

        // Need to create and pass the player early to make sure all the relevant updates are posted (otherwise part assumes this is network init and skips posting the updates)
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ((PartTypeWriteBase) srcRedstoneWriter.getPart()).updateActivation(PartTarget.fromCenter(srcPartPos), srcRedstoneWriter.getState(), player);

        // Set part aspect properties to emit strong power
        IAspectProperties aspectProperties = Aspects.Write.Redstone.BOOLEAN.getDefaultProperties().clone();
        aspectProperties.setValue(AspectWriteBuilders.Redstone.PROP_STRONG_POWER, ValueTypeBoolean.ValueBoolean.of(true));
        Aspects.Write.Redstone.BOOLEAN.setProperties((PartTypeWriteBase) srcRedstoneWriter.getPart(), PartTarget.fromCenter(srcPartPos), (PartStateWriterBase) srcRedstoneWriter.getState(), aspectProperties);

        // Set up stone blocks with lamps nearby to accept redstone output
        helper.setBlock(POS_SRC.relative(Direction.EAST), Blocks.STONE);
        helper.setBlock(POS_DEST.relative(Direction.EAST), Blocks.STONE);
        helper.setBlock(POS_SRC.relative(Direction.EAST).relative(Direction.UP), Blocks.REDSTONE_LAMP);
        helper.setBlock(POS_DEST.relative(Direction.EAST).relative(Direction.UP), Blocks.REDSTONE_LAMP);

        // Copy the part settings now
        player.addItem(new ItemStack(RegistryEntries.ITEM_VARIABLE.get(), 1));
        copyPartSettings(helper, player, srcPartPos, destPartPos);

        // Should not have any variable cards left
        int leftoverCards = ContainerHelper.clearOrCountMatchingItems(player.getInventory(), itemStack -> itemStack.is(RegistryEntries.ITEM_VARIABLE.get()), 0, true);
        helper.assertValueEqual(leftoverCards, 0, "leftoverCards");

        // Make sure offset variable cards have different IDs
        ItemStack destVariableCard = destRedstoneWriter.getState().getInventory().getItem(redstoneBoolSlotIndex);
        helper.assertTrue(getVariableFacade(helper.getLevel(), sourceVariableCard).getId() != getVariableFacade(helper.getLevel(), destVariableCard).getId(), "variable ID is not different");

        helper.succeedOnTickWhen(1, () -> {
            // Check that both lamps are lit up
            helper.assertBlockState(POS_SRC.relative(Direction.EAST).relative(Direction.UP), blockState -> blockState.getValue(RedstoneLampBlock.LIT), () -> "source redstone writer did not emit strong signal");
            helper.assertBlockState(POS_DEST.relative(Direction.EAST).relative(Direction.UP), blockState -> blockState.getValue(RedstoneLampBlock.LIT), () -> "destination redstone writer did not emit strong signal");
        });
    }
}
