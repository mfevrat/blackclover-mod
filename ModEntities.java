package com.blackclovermod.registry;

import com.blackclovermod.BlackCloverMod;
import com.blackclovermod.entity.FloatingGrimoireEntity;
import com.blackclovermod.entity.MagicProjectileEntity;
import com.blackclovermod.entity.boss.ArchdemonEntity;
import com.blackclovermod.entity.boss.DungeonGuardianEntity;
import com.blackclovermod.entity.boss.ElfKingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BlackCloverMod.MODID);

    public static final RegistryObject<EntityType<MagicProjectileEntity>> MAGIC_PROJECTILE = ENTITIES.register("magic_projectile",
            () -> EntityType.Builder.<MagicProjectileEntity>of(MagicProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(8).updateInterval(1)
                    .setShouldReceiveVelocityUpdates(true).build("magic_projectile"));

    public static final RegistryObject<EntityType<FloatingGrimoireEntity>> FLOATING_GRIMOIRE = ENTITIES.register("floating_grimoire",
            () -> EntityType.Builder.<FloatingGrimoireEntity>of(FloatingGrimoireEntity::new, MobCategory.MISC)
                    .sized(0.6f, 0.5f).clientTrackingRange(10).updateInterval(1).build("floating_grimoire"));

    public static final RegistryObject<EntityType<DungeonGuardianEntity>> DUNGEON_GUARDIAN = ENTITIES.register("dungeon_guardian",
            () -> EntityType.Builder.of(DungeonGuardianEntity::new, MobCategory.MONSTER)
                    .sized(1.0f, 3.1f).clientTrackingRange(10).build("dungeon_guardian"));

    public static final RegistryObject<EntityType<ElfKingEntity>> ELF_KING = ENTITIES.register("elf_king",
            () -> EntityType.Builder.of(ElfKingEntity::new, MobCategory.MONSTER)
                    .sized(0.8f, 2.3f).clientTrackingRange(10).build("elf_king"));

    public static final RegistryObject<EntityType<ArchdemonEntity>> ARCHDEMON = ENTITIES.register("archdemon",
            () -> EntityType.Builder.of(ArchdemonEntity::new, MobCategory.MONSTER)
                    .sized(1.1f, 3.3f).fireImmune().clientTrackingRange(10).build("archdemon"));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(DUNGEON_GUARDIAN.get(), DungeonGuardianEntity.createAttributes().build());
        event.put(ELF_KING.get(), ElfKingEntity.createAttributes().build());
        event.put(ARCHDEMON.get(), ArchdemonEntity.createAttributes().build());
    }

    private ModEntities() {}
}
