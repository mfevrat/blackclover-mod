package com.blackclovermod.magic;

/** Copie côté client des données magiques du joueur local (remplie par SyncMagicPacket). */
public final class ClientMagicData {
    public static boolean has;
    public static int element;
    public static int leaves = 3;
    public static int level = 1;
    public static int xp;
    public static int xpNext = 100;
    public static float mana;
    public static float maxMana = 100;
    public static int selected;
    public static int unlocked = 1;
    public static int cooldown;

    public static void tick() {
        if (cooldown > 0) cooldown--;
    }

    private ClientMagicData() {}
}
