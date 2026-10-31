package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.chemistry.AtomicValues;
import com.atomcraft.menu.AnalyzerMenu;
import com.atomcraft.network.ModNetwork;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.research.ResearchData;
import com.atomcraft.storage.FilteredItemHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Analisador: pesquisa itens para o dono da máquina, no estilo do modo Journey
 * do Terraria. Cada unidade analisada é consumida. Ao atingir a quantidade
 * exigida pela raridade, o item entra na biblioteca do Replicador.
 */
public class AnalyzerBlockEntity extends MachineBlockEntity {

    public static final int SLOTS = 9;
    public static final int DATA_PROGRESS = 0, DATA_STATUS = 1, DATA_ITEM_LO = 2, DATA_ITEM_HI = 3,
            DATA_RESEARCHED = 4, DATA_REQUIRED = 5, DATA_COUNT = 6;

    private final FilteredItemHandler buffer = new FilteredItemHandler(SLOTS, 99,
            (slot, resource) -> canAccept(resource.getItem()), this::setChanged);
    private final ResourceHandler<ItemResource> automation = buffer.insertOnly();

    private int progress;
    private @Nullable Item current;
    private MachineStatus status = MachineStatus.IDLE;
    private int currentResearched;
    private int currentRequired;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int itemId = current == null ? 0 : BuiltInRegistries.ITEM.getId(current) + 1;
            return switch (index) {
                case DATA_PROGRESS -> permille(progress, Config.ANALYZE_TICKS.get());
                case DATA_STATUS -> status.ordinal();
                case DATA_ITEM_LO -> itemId & 0xFFFF;
                case DATA_ITEM_HI -> (itemId >>> 16) & 0xFFFF;
                case DATA_RESEARCHED -> currentResearched;
                case DATA_REQUIRED -> currentRequired;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public AnalyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANALYZER.get(), pos, state);
    }

    public FilteredItemHandler buffer() {
        return buffer;
    }

    public ResourceHandler<ItemResource> automation() {
        return automation;
    }

    public ContainerData data() {
        return data;
    }

    private boolean canAccept(Item item) {
        if (AtomicValues.get(item, isClientSide()) == null) return false;
        if (isClientSide() || owner == null || level == null || level.getServer() == null) return true;
        return !ResearchData.get(level.getServer()).isResearched(owner, item);
    }

    @Override
    public void serverTick() {
        if (level == null || level.getServer() == null) return;
        if (owner == null) {
            status = MachineStatus.NO_OWNER;
            return;
        }
        ResearchData research = ResearchData.get(level.getServer());

        int slot = -1;
        for (int i = 0; i < SLOTS; i++) {
            Item item = buffer.getResource(i).getItem();
            if (buffer.getAmountAsInt(i) > 0 && AtomicValues.get(item, false) != null && !research.isResearched(owner, item)) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            if (current != null) setChanged();
            current = null;
            progress = 0;
            currentResearched = currentRequired = 0;
            status = buffer.size() > 0 && isEmpty() ? MachineStatus.IDLE : MachineStatus.NOTHING_TO_RESEARCH;
            return;
        }

        ItemStack stack = buffer.getStack(slot);
        Item item = stack.getItem();
        if (item != current) {
            current = item;
            progress = 0;
        }
        currentResearched = research.getProgress(owner, item);
        currentRequired = ResearchData.required(item);
        status = MachineStatus.WORKING;

        if (++progress >= Config.ANALYZE_TICKS.get()) {
            progress = 0;
            buffer.shrink(slot, 1);
            currentResearched = research.addProgress(owner, item, 1);
            ServerPlayer player = onlineOwner();
            if (player != null) {
                ModNetwork.syncResearch(player);
                if (currentResearched >= currentRequired) {
                    player.sendSystemMessage(Component.translatable("message.atomcraft.research_complete",
                            stack.getHoverName()).withStyle(ChatFormatting.GREEN));
                }
            }
        }
        setChanged();
    }

    private boolean isEmpty() {
        for (int i = 0; i < SLOTS; i++) {
            if (buffer.getAmountAsInt(i) > 0) return false;
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.analyzer");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AnalyzerMenu(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) dropContents(level, pos, buffer);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        buffer.deserialize(input.childOrEmpty("buffer"));
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        buffer.serialize(output.child("buffer"));
    }
}
