package com.blackclovermod.registry;

import com.blackclovermod.BlackCloverMod;
import com.blackclovermod.item.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, BlackCloverMod.MODID);

    public static final RegistryObject<Item> GRIMOIRE = ITEMS.register("grimoire",
            () -> new GrimoireItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> TOWER_SUMMONS = ITEMS.register("tower_summons",
            () -> new TowerSummonsItem(new Item.Properties().stacksTo(16).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MANA_CRYSTAL = ITEMS.register("mana_crystal",
            () -> new ManaCrystalItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> ANCIENT_PAGE = ITEMS.register("ancient_page",
            () -> new AncientPageItem(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> DEMON_FRAGMENT = ITEMS.register("demon_fragment",
            () -> new LoreItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    public static final RegistryObject<Item> DUNGEON_KEY = ITEMS.register("dungeon_key",
            () -> new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), () -> ModEntities.DUNGEON_GUARDIAN.get()));
    public static final RegistryObject<Item> ELF_TEAR = ITEMS.register("elf_tear",
            () -> new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE), () -> ModEntities.ELF_KING.get()));
    public static final RegistryObject<Item> DEMON_HEART = ITEMS.register("demon_heart",
            () -> new BossSummonItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant(), () -> ModEntities.ARCHDEMON.get()));

    private ModItems() {}
}
