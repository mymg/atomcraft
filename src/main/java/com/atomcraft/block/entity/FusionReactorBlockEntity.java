package com.atomcraft.block.entity;

import com.atomcraft.Config;
import com.atomcraft.block.FusionReactorBlock;
import com.atomcraft.block.MachineBlock;
import com.atomcraft.block.ReactorPartBlock;
import com.atomcraft.chemistry.ChemicalElement;
import com.atomcraft.chemistry.Composition;
import com.atomcraft.fusion.FusionStructure;
import com.atomcraft.fusion.ModuleType;
import com.atomcraft.fusion.ReactorModuleItem;
import com.atomcraft.menu.GeneratorMenu;
import com.atomcraft.network.ModNetwork;
import com.atomcraft.registry.ModBlockEntities;
import com.atomcraft.research.ResearchData;
import com.atomcraft.storage.ElementNetwork;
import com.atomcraft.storage.FilteredItemHandler;
import com.atomcraft.upgrade.UpgradeType;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Controlador do Reator de Fusão (multibloco 7×7×5 em formato de tokamak).
 * <p>
 * Funde hidrogênio da rede em hélio (4 H → 1 He). O plasma precisa aquecer
 * (5 minutos por padrão) e a energia de cada fusão é proporcional à temperatura.
 * A estrutura é conferida a cada meio segundo; as Portas do Reator ligam o
 * controlador à rede de elementos e a cabos de energia. Melhorias vêm dos
 * módulos instalados na aba de módulos (só o maior nível de cada tipo vale).
 */
public class FusionReactorBlockEntity extends GeneratorBlockEntity implements GeoBlockEntity {

    public static final int MODULE_SLOTS = 4;
    public static final int DATA_HEAT = DATA_COMMON, DATA_HELIUM_LO = DATA_COMMON + 1, DATA_HELIUM_HI = DATA_COMMON + 2,
            DATA_FORMED = DATA_COMMON + 3, DATA_COUNT = DATA_COMMON + 4;
    private static final int VALIDATE_INTERVAL = 10;
    private static final Composition FUEL = new Composition(Map.of(ChemicalElement.HYDROGEN, 4L));
    private static final Composition RECYCLED = new Composition(Map.of(ChemicalElement.HYDROGEN, 1L));
    private static final Composition PRODUCT = new Composition(Map.of(ChemicalElement.HELIUM, 1L));
    private static final RawAnimation PLASMA = RawAnimation.begin().thenLoop("plasma");

    /** Temperatura do plasma, de 0 a 1000 (‰). */
    private double heat;
    private int cycleTimer;
    private long pending;
    private long heliumProduced;
    private boolean formed;
    private List<BlockPos> ports = List.of();
    private int validateTimer;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_HEAT -> (int) heat;
                case DATA_HELIUM_LO -> (int) (heliumProduced & 0xFFFF);
                case DATA_HELIUM_HI -> (int) ((heliumProduced >>> 16) & 0xFFFF);
                case DATA_FORMED -> formed ? 1 : 0;
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
        super(ModBlockEntities.FUSION_REACTOR.get(), pos, state, Config.FUSION_CAPACITY.get(), Config.FUSION_MAX_OUTPUT.get());
    }

    public ContainerData data() {
        return data;
    }

    public int heat() {
        return (int) heat;
    }

    public boolean isFormed() {
        return formed;
    }

    public List<BlockPos> ports() {
        return ports;
    }

    public Direction facing() {
        return getBlockState().getValue(MachineBlock.FACING);
    }

    // ----- Módulos -----

    @Override
    protected FilteredItemHandler createUpgrades() {
        return new FilteredItemHandler(MODULE_SLOTS, 1, (slot, resource) -> resource.getItem() instanceof ReactorModuleItem,
                this::setChanged);
    }

    @Override
    public boolean supportsUpgrade(UpgradeType type) {
        return false;
    }

    /** Maior nível instalado de um tipo de módulo (0 = nenhum). */
    public int moduleTier(ModuleType type) {
        FilteredItemHandler modules = upgrades();
        int tier = 0;
        for (int i = 0; i < modules.size(); i++) {
            if (modules.getAmountAsInt(i) > 0 && modules.getResource(i).getItem() instanceof ReactorModuleItem module
                    && module.type() == type) {
                tier = Math.max(tier, module.tier());
            }
        }
        return tier;
    }

    public int cycleTicks() {
        return ModuleType.cycleTicks(moduleTier(ModuleType.INJECTOR));
    }

    public int energyPerCycle() {
        return (int) Math.min(Integer.MAX_VALUE / 2,
                Math.round(Config.FUSION_FE_PER_CYCLE.get() * ModuleType.energyMultiplier(moduleTier(ModuleType.CONTAINMENT))));
    }

    /** Ticks para o plasma ir de frio à temperatura máxima. */
    public int warmupTicks() {
        return (int) Math.max(1, Math.round(Config.FUSION_WARMUP_TICKS.get() * ModuleType.warmupFactor(moduleTier(ModuleType.IGNITION))));
    }

    // ----- Estrutura -----

    @Override
    protected Collection<BlockPos> networkOrigins() {
        List<BlockPos> origins = new ArrayList<>(ports);
        origins.add(worldPosition);
        return origins;
    }

    @Override
    protected Collection<BlockPos> energyOutputs() {
        return networkOrigins();
    }

    /** Confere a estrutura e atualiza o estado "formado" de todas as peças. */
    public void validate() {
        if (level == null || level.isClientSide()) return;
        FusionStructure.Result result = FusionStructure.check(level, worldPosition, facing());
        if (result.formed() == formed && result.ports().equals(ports)) return;

        if (formed) setMembersFormed(blueprintPositions(), false);
        formed = result.formed();
        ports = List.copyOf(result.ports());
        if (formed) setMembersFormed(result.members(), true);
        for (BlockPos port : result.ports()) {
            if (level.getBlockEntity(port) instanceof ReactorPortBlockEntity entity) entity.setController(formed ? worldPosition : null);
        }
        updateBlockState();
        invalidateNetwork();
        setChanged();
    }

    private void setMembersFormed(List<BlockPos> members, boolean value) {
        if (level == null) return;
        for (BlockPos pos : members) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(ReactorPartBlock.FORMED) && state.getValue(ReactorPartBlock.FORMED) != value) {
                level.setBlock(pos, state.setValue(ReactorPartBlock.FORMED, value), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** Ao quebrar o controlador, as peças voltam a aparecer. */
    public void disassemble() {
        if (level == null || level.isClientSide()) return;
        // Todas as posições do projeto: algumas peças podem já ter sido trocadas.
        setMembersFormed(blueprintPositions(), false);
        for (BlockPos port : ports) {
            if (level.getBlockEntity(port) instanceof ReactorPortBlockEntity entity) entity.setController(null);
        }
        formed = false;
    }

    private List<BlockPos> blueprintPositions() {
        List<BlockPos> all = new ArrayList<>();
        for (FusionStructure.Slot slot : FusionStructure.blueprint(worldPosition, facing())) all.add(slot.pos());
        return all;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        disassemble();
    }

    private void updateBlockState() {
        if (level == null) return;
        BlockState state = getBlockState();
        boolean active = formed && heat > 0;
        if (state.getValue(FusionReactorBlock.FORMED) != formed || state.getValue(FusionReactorBlock.ACTIVE) != active) {
            level.setBlock(worldPosition, state.setValue(FusionReactorBlock.FORMED, formed).setValue(FusionReactorBlock.ACTIVE, active),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void serverTick() {
        if (++validateTimer >= VALIDATE_INTERVAL) {
            validateTimer = 0;
            validate();
        }
        super.serverTick();
        updateBlockState();
    }

    @Override
    protected void disabledTick() {
        if (++validateTimer >= VALIDATE_INTERVAL) {
            validateTimer = 0;
            validate();
        }
        // Desligado, o plasma esfria aos poucos.
        heat = Math.max(0, heat - heatStep() * 2 / cycleTicks());
        super.disabledTick();
        updateBlockState();
    }

    // ----- Geração -----

    /** Aquecimento por ciclo (‰). */
    private double heatStep() {
        return 1000.0 * cycleTicks() / warmupTicks();
    }

    @Override
    protected int generate() {
        if (!formed) {
            status = MachineStatus.INCOMPLETE;
            heat = Math.max(0, heat - heatStep() * 2 / cycleTicks());
            return 0;
        }
        int perCycle = energyPerCycle();
        int cycleTicks = cycleTicks();
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
        double step = heatStep();
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
        double recycle = ModuleType.recycleChance(moduleTier(ModuleType.RECYCLER));
        if (recycle > 0 && level != null && level.getRandom().nextDouble() < recycle) network.insert(RECYCLED, 1);
        heat = Math.min(1000, heat + step);
        pending += (long) (perCycle * heat / 1000);
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

    // ----- GeckoLib (modelo grande e animação do plasma) -----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<FusionReactorBlockEntity>("plasma", 10, test -> {
            BlockState state = test.animatable().getBlockState();
            return state.hasProperty(FusionReactorBlock.ACTIVE) && state.getValue(FusionReactorBlock.ACTIVE)
                    ? test.setAndContinue(PLASMA) : PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ----- Menu e dados -----

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
        heat = input.getDoubleOr("heat", 0);
        pending = input.getLongOr("pending", 0L);
        heliumProduced = input.getLongOr("helium", 0L);
        formed = input.getBooleanOr("formed", false);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("heat", heat);
        output.putLong("pending", pending);
        output.putLong("helium", heliumProduced);
        output.putBoolean("formed", formed);
    }
}
