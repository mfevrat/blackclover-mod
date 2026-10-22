package com.blackclovermod.client;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final String CATEGORY = "key.categories.blackclover";
    public static final KeyMapping CAST = new KeyMapping("key.blackclover.cast", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping CYCLE = new KeyMapping("key.blackclover.cycle", GLFW.GLFW_KEY_G, CATEGORY);

    private KeyBindings() {}
}
