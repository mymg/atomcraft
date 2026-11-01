package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.menu.GeneratorMenu;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.upgrade.UpgradeType;
import com.atomcraft.upgrade.Upgrades;
import com.atomcraft.research.ResearchData;
import com.atomcraft.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Map;

/**
 * Reator de Fusão: funde hidrogênio da rede em hélio (4 H → 1 He), como no Sol.
 * <p>
 * O plasma precisa aquecer: a cada ciclo com combustível a temperatura sobe, e a
 * energia gerada é proporcional a ela. Sem hidrogênio, o reator esfria. O hélio
 * produzido volta para a rede (ou é liberado se não houver espaço).
 */
public class FusionReactorBlockEntity extends GeneratorBlockEntity {

    public static final int CYCLE_TICKS = 10;
    public static final int DATA_HEAT = DATA_COMMON, DATA_HELIUM_LO = DATA_COMMON + 1, DATA_HELIUM_HI = DATA_COMMON + 2,
            DATA_COUNT = DATA_COMMON + 3;
    private static final Composition FUEL = new Composition(Map.of(ChemicalElement.HYDROGEN, 4L));
    private static final Composition PRODUCT = new Composition(Map.of(ChemicalElement.HELIUM, 1L));

    /** Temperatura do plasma, de 0 a 1000 (‰). */
    private int heat;
    private int cycleTimer;
    private long pending;
    private long heliumProduced;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_HEAT -> heat;
                case DATA_HELIUM_LO -> (int) (heliumProduced & 0xFFFF);
                case DATA_HELIUM_HI -> (int) ((heliumProduced >>> 16) & 0xFFFF);
                default -> commonData(index);
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_REACTOR.get(), pos, state);
    }

    public ContainerData data() {
        return data;
    }

    public int heat() {
        return heat;
    }

    @Override
    protected int generate() {
        int perCycle = (int) boostedYield(Config.FUSION_FE_PER_CYCLE.get());
        // Chips de velocidade encurtam o ciclo (mais fusões por segundo).
        int cycleTicks = Math.max(2, (int) Math.round(CYCLE_TICKS / Upgrades.generatorRate(upgradeCount(UpgradeType.SPEED))));
        if (++cycleTimer >= cycleTicks) {
            cycleTimer = 0;
            runCycle(perCycle);
        }
        if (pending <= 0) return 0;
        // Distribui a energia do ciclo ao longo do ciclo, para a geração ficar estável.
        int rate = (int) Math.max(1, Math.ceil(perCycle * (heat / 1000.0) / cycleTicks));
        int generated = (int) Math.min(Math.min(pending, rate), room());
        pending -= generated;
        addEnergy(generated);
        return generated;
    }

    private void runCycle(int perCycle) {
        int step = Math.max(1, 1000 / Config.FUSION_WARMUP_CYCLES.get());
        if (room() < perCycle / 2 || pending > perCycle) {
            // Buffer cheio: mantém o plasma em espera, sem gastar combustível.
            status = MachineStatus.IDLE;
            return;
        }
        ElementNetwork network = network();
        if (!network.hasStorage()) {
            status = MachineStatus.NO_STORAGE;
            heat = Math.max(0, heat - step * 2);
            return;
        }
        if (!network.extract(FUEL, 1)) {
            status = MachineStatus.NO_ELEMENTS;
            heat = Math.max(0, heat - step * 2);
            return;
        }
        heat = Math.min(1000, heat + step);
        pending += (long) perCycle * heat / 1000;
        heliumProduced++;
        network.insert(PRODUCT, 1);
        discoverHelium();
        status = MachineStatus.WORKING;
        setChanged();
    }

    /** O hélio não existe em nenhum item: o dono do reator o descobre ao produzi-lo. */
    private void discoverHelium() {
        if (owner == null || level == null || level.getServer() == null) return;
        ResearchData research = ResearchData.get(level.getServer());
        if (research.isDiscovered(owner, ChemicalElement.HELIUM)) return;
        research.discover(owner, PRODUCT);
        ServerPlayer player = onlineOwner();
        if (player != null) {
            player.sendSystemMessage(Component.translatable("message.atomcraft.element_discovered",
                    Component.translatable(ChemicalElement.HELIUM.translationKey()), ChemicalElement.HELIUM.symbol())
                    .withStyle(ChatFormatting.AQUA));
            ModNetwork.syncResearch(player);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.atomcraft.fusion_reactor");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return GeneratorMenu.fusion(id, inventory, this);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        heat = input.getIntOr("heat", 0);
        pending = input.getLongOr("pending", 0L);
        heliumProduced = input.getLongOr("helium", 0L);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("heat", heat);
        output.putLong("pending", pending);
        output.putLong("helium", heliumProduced);
    }
}
