package de.melanx.packessentials.items;

import de.melanx.packessentials.Modpack;
import de.melanx.packessentials.PackConfig;
import de.melanx.packessentials.PackEssentials;
import de.melanx.packessentials.base.PackElement;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.moddingx.libx.base.ItemBase;
import org.moddingx.libx.mod.ModX;
import org.moddingx.libx.util.lazy.LazyValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class BuriedMobItem<T extends EntityType<?>> extends ItemBase implements PackElement {

    private final Random random = new Random();
    private final Set<Modpack> modpacks;

    public T entityType;
    public LazyValue<SpawnEggItem> spawnEggItem;
    private String descriptionId;

    public BuriedMobItem(ModX mod, Properties properties, T entityType, Modpack... modpacks) {
        super(mod, properties);
        this.entityType = entityType;
        this.spawnEggItem = new LazyValue<>(() -> ForgeSpawnEggItem.fromEntityType(this.entityType));
        this.modpacks = Set.of(modpacks);
    }

    @Nonnull
    @Override
    public InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();

        BlockState blockState = level.getBlockState(pos);
        if (!blockState.isAir() && !level.isClientSide()) {
            boolean b = this.trySummon((ServerLevel) level, pos.above(), context.getItemInHand());
            //noinspection DataFlowIssue
            if (!((ServerPlayer) context.getPlayer()).gameMode.isCreative()) {
                context.getItemInHand().shrink(1);
            }

            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Nonnull
    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        this.randomChance(stack);
        return stack;
    }

    private double generateBoundedRandom(double min, double max) {
        double randomValue = min + (this.random.nextDouble() * (max - min));
        randomValue = Math.round(randomValue * 100.0) / 100.0;
        return randomValue;
    }

    public void randomChance(ItemStack stack) {
        stack.getOrCreateTag().putDouble("Chance", this.generateBoundedRandom(PackConfig.BuriedMobs.minChance, PackConfig.BuriedMobs.maxChance));
    }

    public void setChance(ItemStack stack, double chance) {
        stack.getOrCreateTag().putDouble("Chance", chance);
    }

    public double getChance(ItemStack stack) {
        return stack.getOrCreateTag().getDouble("Chance");
    }

    public boolean trySummon(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (level.random.nextDouble() <= this.getChance(stack)) {
            this.summon(level, pos);
            return true;
        }

        return false;
    }

    public void summon(ServerLevel level, BlockPos pos) {
        Entity entity = this.entityType.create(level);
        if (entity == null) {
            return;
        }

        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        level.addFreshEntity(entity);
    }

    @Override
    public void inventoryTick(ItemStack stack, @Nonnull Level level, @Nonnull Entity entity, int slotId, boolean isSelected) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("Chance")) {
            this.randomChance(stack);
        }
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tooltipComponents, @Nonnull TooltipFlag flag) {
        if (flag.isAdvanced()) {
            tooltipComponents.add(Component.empty()
                    .append(Component.translatable("tooltip.packessentials.entity_type")
                            .append(this.entityType.getDescription().copy()
                                    .withStyle(Style.EMPTY.withColor(this.spawnEggItem.get().getColor(0))))));
            tooltipComponents.add(Component.empty()
                    .append(Component.translatable("tooltip.packessentials.chance")
                            .append(Component.literal(String.format("%.0f%%", this.getChance(stack) * 100))
                                    .withStyle(Style.EMPTY.withColor(this.spawnEggItem.get().getColor(1))))));
        }
    }

    @Nonnull
    @Override
    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = "item." + PackEssentials.getInstance().modid + ".buried_mob";
        }

        return this.descriptionId;
    }

    @Override
    public Set<Modpack> getModpacks() {
        return this.modpacks;
    }
}