package de.unknowncity.yabawrtp;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.incendo.cloud.bukkit.parser.location.Location2D;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

public class AbstractDelegateLocationFactoryBean {
    private final YABAWRTPPlugin plugin;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Map<String, List<Location>> cachedSaveLocations = new HashMap<>();

    private static final Set<Biome> ALLOWED_BIOMES = Set.of(
            Biome.PLAINS,
            Biome.SAVANNA,
            Biome.DESERT,
            Biome.FOREST,
            Biome.BIRCH_FOREST,
            Biome.DARK_FOREST,
            Biome.TAIGA,
            Biome.SUNFLOWER_PLAINS,
            Biome.WINDSWEPT_HILLS,
            Biome.MEADOW,
            Biome.JUNGLE,
            Biome.SPARSE_JUNGLE
    );

    public AbstractDelegateLocationFactoryBean(YABAWRTPPlugin yabawrtpPlugin) {
        this.plugin = yabawrtpPlugin;
    }

    public Optional<Location> getCachedSaveLocation(World world) {
        if (!cachedSaveLocations.containsKey(world.getName()) || cachedSaveLocations.get(world.getName()).isEmpty()) {
            return Optional.empty();
        }
        var location = cachedSaveLocations.get(world.getName()).getFirst();
        cachedSaveLocations.get(world.getName()).removeFirst();
        recache();
        return Optional.of(location);
    }

    private void cacheLocation(World world) {
        var settings = plugin.configuration().worldSettings(world.getName());
        findSaveLocation(world, settings.radius().min(), settings.radius().max(), Location2D.from(world, settings.origin().x(), settings.origin().z()))
                .whenComplete((location, throwable) -> {
                    cachedSaveLocations.compute(world.getName(), (k, v) -> {
                        if (v == null) {
                            v = new ArrayList<>();
                        }
                        if (location.isPresent()) {
                            v.add(location.get());
                        }
                        return v;
                    });
                });
    }

    public void warmupCache() {
        plugin.configuration().worlds().forEach((name, rtpSettings) -> {
            cachedSaveLocations.putIfAbsent(name, new ArrayList<>());
            recache();
        });
    }

    public void recache() {
        cachedSaveLocations.forEach((name, locations) -> {
           for (int i = 0; i < plugin.configuration().maxTries(); i++) {
               if (locations.size() >= plugin.configuration().keepInCache()) {
                   plugin.getLogger().info("Cache for world " + name + " is full");
                   break;
               }
               var world = plugin.getServer().getWorld(name);
               if (world == null) return;
               cacheLocation(world);
           }
        });
    }

    public CompletableFuture<Optional<Location>> findSaveLocation(World world, int minRadius, int maxRadius, Location2D origin) {
        return CompletableFuture.supplyAsync(() -> {

            for (int i = 0; i < plugin.configuration().maxTries(); i++) {

                System.out.println("Searching for safe location in try " + i);
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = minRadius + (random.nextDouble() * (maxRadius - minRadius));
                double x = origin.getX() + distance * Math.cos(angle);
                double z = origin.getZ() + distance * Math.sin(angle);


                int y;
                if (world.getEnvironment() == World.Environment.NETHER) {
                    var yOpt = getHighestLocationInNether(world, x, z);
                    if (yOpt.isEmpty()) {
                        continue;
                    }
                    y = yOpt.get();
                } else {
                    y = world.getHighestBlockYAt((int) x, (int) z);
                }

                System.out.println("Searching for safe location at " + x + ", " + y + ", " + z);

                var location = new Location(world, x, y, z).toCenterLocation();
                location.setY(location.getY() - 0.5);

                if (isSafeLocation(location)) {
                    return Optional.of(location);
                }
            }

            return Optional.empty();
        });
    }


    private boolean isSafeLocation(Location location) {
        Block block = location.getBlock();
        Block blockBelow = block.getRelative(0, -1, 0);
        Block blockAbove = block.getRelative(0, 1, 0);
        System.out.println("Checking safe location at " + blockBelow.getType() + ", " + block.getType() + ", " + blockAbove.getType());

        if (!blockBelow.getType().isSolid()) return false;
        if (!isSaveOnBlock(blockBelow)) return false;

        return isSaveInsideBlock(block) && isSaveInsideBlock(blockAbove);
    }

    private boolean isSaveInsideBlock(Block block) {
        if (block.isSolid()) return false;
        if (block.isLiquid()) return false;
        if (block.isSuffocating()) return false;
        if (block.getType() == Material.POWDER_SNOW) return false;
        if (block.getType() == Material.FIRE) return false;
        return true;
    }

    private boolean isSaveOnBlock(Block block) {
        if (block.getType() == Material.MAGMA_BLOCK) return false;
        if (block.getType() == Material.POWDER_SNOW) return false;
        return true;
    }

    private Optional<Integer> getHighestLocationInNether(World world, double x, double z) {
        for (int y = 122; y > 4; y--) {
            var location = new Location(world, x, y, z);
            if (location.getBlock().getType() == Material.AIR &&
                    location.getBlock().getRelative(0, -1, 0).isSolid() &&
                    location.getBlock().getRelative(0, 1, 0).getType() == Material.AIR) {
                return Optional.of(y);
            }
        }
        return Optional.empty();
    }
}
