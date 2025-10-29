package de.unknowncity.yabawrtp;

import jogamp.common.util.locks.SingletonInstanceFileLock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.incendo.cloud.bukkit.parser.location.Location2D;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class AbstractDelegateLocationFactoryBean {
    private final YABAWRTPPlugin plugin;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final Map<String, List<Location>> cachedSaveLocations = new ConcurrentHashMap<>();

    public AbstractDelegateLocationFactoryBean(YABAWRTPPlugin yabawrtpPlugin) {
        this.plugin = yabawrtpPlugin;
    }

    public Optional<Location> getCachedSaveLocation(World world) {
        if (!cachedSaveLocations.containsKey(world.getName()) || cachedSaveLocations.get(world.getName()).isEmpty()) {
            return Optional.empty();
        }
        for (int i = 0; i < cachedSaveLocations.get(world.getName()).size(); i++) {
            var location = cachedSaveLocations.get(world.getName()).getFirst();
            if (!isSafeLocation(location)) {
                continue;
            }

            cachedSaveLocations.get(world.getName()).remove(i);
            recache();
            return Optional.of(location);
        }
        return Optional.empty();
    }

    private void cacheLocation(World world, int tryCount) {
        if (tryCount >= plugin.configuration().maxTries()) {
            return;
        }
        var locations = cachedSaveLocations.getOrDefault(world.getName(), new LinkedList<>());
        if (locations.size() >= plugin.configuration().keepInCache()) {
            plugin.getLogger().info("Cache for world " + world.getName() + " is full");
            return;
        }
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
                    cacheLocation(world, tryCount + 1);
                });
    }

    public void warmupCache() {
        plugin.configuration().worlds().forEach((name, rtpSettings) -> {
            cachedSaveLocations.putIfAbsent(name, new LinkedList<>());
            recache();
        });
    }

    public void recache() {
        cachedSaveLocations.forEach((name, locations) -> {
            plugin.getLogger().info("Recache for world " + name + " with " + plugin.configuration().keepInCache() + " locations");
            var world = plugin.getServer().getWorld(name);
            if (world == null) {
                return;
            }
            cacheLocation(world, 0);
        });
    }

    public CompletableFuture<Optional<Location>> findSaveLocation(World world, int minRadius, int maxRadius, Location2D origin) {
        return CompletableFuture.supplyAsync(() -> {

            for (int i = 0; i < plugin.configuration().maxTries(); i++) {
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

                //plugin.getLogger().info("Trying to find a safe location for " + world.getName() + " at " + x + ", " + y + ", " + z + " try: " + (i + 1));

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
