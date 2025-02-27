package name.modid;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

public class AutoElytraChestplateSwapperClient implements ClientModInitializer {
    public static KeyBinding spaceBar;

    public void initializeKeybinding() {
        KeyBinding binding = new KeyBinding("key.mod.jumpaction", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_SPACE, "key.categories.mod");
        spaceBar = KeyBindingHelper.registerKeyBinding(binding);
    }

    @Override
    public void onInitializeClient() {
        initializeKeybinding();
        ClientTickEvents.END_CLIENT_TICK.register(this::clientTick);
    }

    public void clientTick(MinecraftClient client) {
        if (spaceBar.wasPressed()) {
            ClientPlayerEntity player = client.player;
            assert player != null;
            boolean inAir = !player.isOnGround();

            if (inAir) {
                swapChestplate(player, client, Swap.ToElytra);
            }
        }
    }

    public enum Swap {
        ToElytra, ToChestplate
    }

    // Stolen from https://github.com/Saphjyr/ElytraChestplateSwapper, with a few modifications by me
    // https://github.com/Saphjyr/ElytraChestplateSwapper/blob/eacfe87dad8aec1869cd8b954d7e3bfa1aeb10f5/src/main/java/com/saphjyr/ElytraChestplateSwapper/InventoryUtils.java#L15
    public static void swapChestplate(PlayerEntity player, MinecraftClient client, Swap swap) {
        int HOTBAR_SIZE = PlayerInventory.getHotbarSize(); // 9
        int MAIN_SIZE = PlayerInventory.MAIN_SIZE; // 36
        int TOTAL_SIZE = MAIN_SIZE + 1; // 37

        // List inventory slots, in a special order so the selected slot is the most top-left chestplate : 9 - 35 40 0 - 8
        int[] range = new int[TOTAL_SIZE];

        // Main inventory
        for (int i = 0; i < MAIN_SIZE - HOTBAR_SIZE; i++) {
            range[i] = i + HOTBAR_SIZE;
        }

        // Off hand
        range[MAIN_SIZE - HOTBAR_SIZE] = PlayerInventory.OFF_HAND_SLOT; // 40

        // Hotbar
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            range[i + MAIN_SIZE - HOTBAR_SIZE + 1] = i;
        }

        // Find elytraSlot and chestplateSlot
        for (int slot : range) {
            // Get the itemstack in the slot
            ItemStack stack = player.getInventory().getStack(slot);

            boolean shouldSwap = switch (swap) {
                case ToElytra -> isElytra(stack);
                case ToChestplate -> isChestplate(stack);
            };

            if (shouldSwap) {
                sendSwapPackets(slot, client);
                return;
            }
        }
    }

    // Completely stolen from https://github.com/Saphjyr/ElytraChestplateSwapper
    // https://github.com/Saphjyr/ElytraChestplateSwapper/blob/eacfe87dad8aec1869cd8b954d7e3bfa1aeb10f5/src/main/java/com/saphjyr/ElytraChestplateSwapper/InventoryUtils.java#L90
    private static boolean isElytra(ItemStack stack) {
        return stack.getItem() == Items.ELYTRA;
    }

    // Stolen from https://github.com/Saphjyr/ElytraChestplateSwapper, with a few modifications by me
    // https://github.com/Saphjyr/ElytraChestplateSwapper/blob/eacfe87dad8aec1869cd8b954d7e3bfa1aeb10f5/src/main/java/com/saphjyr/ElytraChestplateSwapper/InventoryUtils.java#L94
    private static boolean isChestplate(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem && stack.getItem().getDefaultStack().getName().getString().toLowerCase().contains("chestplate");
    }

    // Completely stolen from https://github.com/Saphjyr/ElytraChestplateSwapper, with some very minor modifications by me
    // https://github.com/Saphjyr/ElytraChestplateSwapper/blob/eacfe87dad8aec1869cd8b954d7e3bfa1aeb10f5/src/main/java/com/saphjyr/ElytraChestplateSwapper/InventoryUtils.java#L104
    private static void sendSwapPackets(int slot, MinecraftClient client) {
        int sentSlot = slot;
        if (sentSlot == PlayerInventory.OFF_HAND_SLOT)
            sentSlot += 5; // Off Hand offset
        if (sentSlot < PlayerInventory.getHotbarSize())
            sentSlot += PlayerInventory.MAIN_SIZE;   // Toolbar offset

        ClientPlayerInteractionManager interactionManager = client.interactionManager;
        assert interactionManager != null;

        // Take the item to swap to
        interactionManager.clickSlot(0, sentSlot, 0, SlotActionType.PICKUP, client.player);

        // Put it in the armor slot
        interactionManager.clickSlot(0, 6, 0, SlotActionType.PICKUP, client.player);

        // Put back what was in the armor slot (can be air)
        interactionManager.clickSlot(0, sentSlot, 0, SlotActionType.PICKUP, client.player);
    }
}