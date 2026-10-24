package com.example.walkietalkie;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * ПКМ — включить/выключить. Shift+ПКМ — сменить частоту (1..99).
 * Состояние хранится в CUSTOM_DATA предмета.
 */
public class RadioItem extends Item {
    public static final int MAX_FREQ = 99;

    public RadioItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient) {
            if (user.isSneaking()) {
                int next = getFrequency(stack) % MAX_FREQ + 1;
                NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putInt("Freq", next));
                user.sendMessage(Text.literal("Рация: частота " + next), true);
            } else {
                boolean on = !isOn(stack);
                NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean("On", on));
                user.sendMessage(Text.literal("Рация: " + (on ? "ВКЛ" : "ВЫКЛ") + ", частота " + getFrequency(stack)), true);
            }
            world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_LEVER_CLICK,
                    SoundCategory.PLAYERS, 0.6f, 1.4f);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.literal(isOn(stack) ? "ВКЛ" : "ВЫКЛ")
                .formatted(isOn(stack) ? Formatting.GREEN : Formatting.RED));
        tooltip.add(Text.literal("Частота: " + getFrequency(stack)).formatted(Formatting.GRAY));
        tooltip.add(Text.literal("ПКМ — вкл/выкл, Shift+ПКМ — частота").formatted(Formatting.DARK_GRAY));
    }

    // ---------- helpers ----------
    private static NbtCompound data(ItemStack s) {
        return s.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt();
    }

    public static boolean isOn(ItemStack s) {
        return s.getItem() instanceof RadioItem && data(s).getBoolean("On");
    }

    public static int getFrequency(ItemStack s) {
        int f = data(s).getInt("Freq");
        return f <= 0 ? 1 : f;
    }

    /** Рация, из которой игрок говорит: включённая, в одной из рук. */
    public static ItemStack getTransmitter(PlayerEntity p) {
        if (isOn(p.getMainHandStack())) return p.getMainHandStack();
        if (isOn(p.getOffHandStack())) return p.getOffHandStack();
        return null;
    }

    /** Игрок слышит эфир, если в инвентаре есть включённая рация на этой частоте. */
    public static boolean canReceive(PlayerEntity p, int freq) {
        for (ItemStack s : p.getInventory().main) if (isOn(s) && getFrequency(s) == freq) return true;
        for (ItemStack s : p.getInventory().offHand) if (isOn(s) && getFrequency(s) == freq) return true;
        return false;
    }
}
