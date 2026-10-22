package com.blackclovermod.ceremony;

import com.blackclovermod.BlackCloverMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

/**
 * La Tour des Grimoires : une dimension vide dans laquelle on construit (une seule fois)
 * une immense tour cylindrique couverte d'étagères, avec une plateforme centrale suspendue.
 */
public final class GrimoireTower {
    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, BlackCloverMod.id("grimoire_tower"));

    public static final int RADIUS = 18;
    public static final int FLOOR_Y = 40;
    public static final int PLATFORM_Y = 80;
    public static final int TOP_Y = 128;

    public static Vec3 platformCenter() {
        return new Vec3(0.5, PLATFORM_Y, 0.5);
    }

    public static void ensureBuilt(ServerLevel level) {
        if (level.getBlockState(new BlockPos(0, PLATFORM_Y - 1, 0)).is(Blocks.CHISELED_QUARTZ_BLOCK)) return;
        BlackCloverMod.LOGGER.info("Construction de la Tour des Grimoires...");
        build(level);
    }

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        l.setBlock(new BlockPos(x, y, z), s, 2);
    }

    private static void build(ServerLevel l) {
        RandomSource rnd = RandomSource.create(1337L);
        final int R = RADIUS;
        BlockState shelf = Blocks.BOOKSHELF.defaultBlockState();
        BlockState planks = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState();
        BlockState fence = Blocks.DARK_OAK_FENCE.defaultBlockState();
        BlockState lantern = Blocks.LANTERN.defaultBlockState();
        BlockState shell = Blocks.DEEPSLATE_BRICKS.defaultBlockState();

        for (int x = -R - 2; x <= R + 2; x++) {
            for (int z = -R - 2; z <= R + 2; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > R + 1.8) continue;
                double ang = Math.atan2(z, x);
                int seg = (int) Math.floor((ang + Math.PI) / (2 * Math.PI) * 32);
                boolean pillar = seg % 4 == 0;
                Direction inward = Math.abs(x) > Math.abs(z) ? (x > 0 ? Direction.WEST : Direction.EAST) : (z > 0 ? Direction.NORTH : Direction.SOUTH);

                for (int y = FLOOR_Y; y <= TOP_Y; y++) {
                    int rel = y - FLOOR_Y;
                    BlockState s = null;
                    if (d < R - 0.5) {
                        // intérieur de la tour
                        if (y == FLOOR_Y) s = floorBlock(d);
                        else if (y == TOP_Y) s = (seg % 4 == 0 && ((int) d) % 5 == 0) ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
                        else if (rel % 10 == 0 && d >= R - 3.5) s = planks; // balcons
                        else if (rel % 10 == 1 && d >= R - 3.5 && d < R - 2.5) s = (seg % 4 == 2) ? lantern : fence; // rambardes
                    } else if (d < R + 0.5) {
                        // mur d'étagères
                        if (rel % 10 == 0 || y == TOP_Y) s = planks;
                        else if (pillar) s = log;
                        else if (rel % 10 == 6 && seg % 4 == 2) s = Blocks.SHROOMLIGHT.defaultBlockState();
                        else if (rnd.nextFloat() < 0.18f) s = chiseled(inward, rnd);
                        else s = shelf;
                    } else {
                        s = shell; // coque extérieure
                    }
                    if (s != null) set(l, x, y, z, s);
                }
            }
        }

        // Pilier central en quartz sous la plateforme
        for (int y = FLOOR_Y + 1; y <= PLATFORM_Y - 2; y++)
            for (int x = -1; x <= 1; x++)
                for (int z = -1; z <= 1; z++)
                    if (x * x + z * z <= 2) set(l, x, y, z, Blocks.QUARTZ_PILLAR.defaultBlockState());

        // Plateforme centrale
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d <= 3.6) set(l, x, PLATFORM_Y - 1, z, d < 2.2 ? Blocks.QUARTZ_BRICKS.defaultBlockState() : Blocks.SMOOTH_QUARTZ.defaultBlockState());
                else if (d <= 4.6) set(l, x, PLATFORM_Y - 1, z, Blocks.GOLD_BLOCK.defaultBlockState());
            }
        }
        set(l, 3, PLATFORM_Y - 1, 0, Blocks.SEA_LANTERN.defaultBlockState());
        set(l, -3, PLATFORM_Y - 1, 0, Blocks.SEA_LANTERN.defaultBlockState());
        set(l, 0, PLATFORM_Y - 1, 3, Blocks.SEA_LANTERN.defaultBlockState());
        set(l, 0, PLATFORM_Y - 1, -3, Blocks.SEA_LANTERN.defaultBlockState());

        // Lustres suspendus
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            int cx = (int) Math.round(Math.cos(a) * 9), cz = (int) Math.round(Math.sin(a) * 9);
            for (int y = TOP_Y - 1; y > TOP_Y - 15; y--) set(l, cx, y, cz, Blocks.CHAIN.defaultBlockState());
            set(l, cx, TOP_Y - 15, cz, lantern.setValue(LanternBlock.HANGING, true));
        }

        // Marqueur "tour construite" (au centre de la plateforme), posé en dernier
        set(l, 0, PLATFORM_Y - 1, 0, Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState());
    }

    private static BlockState floorBlock(double d) {
        if (d >= 10 && d < 11) return Blocks.GLOWSTONE.defaultBlockState();
        if (((int) d) % 4 == 0) return Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState();
        return Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
    }

    private static BlockState chiseled(Direction facing, RandomSource rnd) {
        BlockState s = Blocks.CHISELED_BOOKSHELF.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, facing);
        for (BooleanProperty prop : ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES) s = s.setValue(prop, rnd.nextFloat() < 0.6f);
        return s;
    }

    private GrimoireTower() {}
}
