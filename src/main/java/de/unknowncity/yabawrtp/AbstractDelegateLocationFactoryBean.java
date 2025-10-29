package de.unknowncity.yabawrtp;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class AbstractDelegateLocationFactoryBean {
    private final YABAWRTPPlugin plugin;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();
    private final LoadingCache<String, Queue<Location>> cache;
    private final ListeningExecutorService reloadExecutor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(2));

    public AbstractDelegateLocationFactoryBean(YABAWRTPPlugin yabawrtpPlugin) {
        this.plugin = yabawrtpPlugin;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(plugin.configuration().keepInCache())
                .build(new CacheLoader<>() {
                    @Override
                    public @NotNull Queue<Location> load(@NotNull String world) {
                        var settings = plugin.configuration().worldSettings(world);
                        var queue = new ConcurrentLinkedQueue<Location>();
                        int trys = 0;
                        do {
                            findSaveLocation(
                                    world,
                                    settings.radius().min(),
                                    settings.radius().max(),
                                    settings.origin().x(),
                                    settings.origin().z()
                            ).ifPresent(queue::add);
                            plugin.getLogger().info("Loaded " + queue.size() + " safe locations for " + world + " after " + trys + " tries.");
                            trys++;
                        } while (queue.size() < plugin.configuration().keepInCache() && trys < plugin.configuration().maxTries());
                        return queue;
                    }

                    @Override
                    public @NotNull ListenableFuture<Queue<Location>> reload(@NotNull String world, @NotNull Queue<Location> oldQueue) {
                        return reloadExecutor.submit(() -> {
                            var settings = plugin.configuration().worldSettings(world);
                            var safeLoc = findSaveLocation(world, settings.radius().min(), settings.radius().max(), settings.origin().x(), settings.origin().z());
                            safeLoc.ifPresent(oldQueue::add);
                            return oldQueue;
                        });
                    }
                });
    }

    public void warmupCache() {
        plugin.configuration().worlds().forEach((world, settings) -> {
            try {
                cache.get(world);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public Optional<Location> getCachedSafeLocation(World world) {
        plugin.getLogger().info("Getting cached safe location for " + world.getName());
        var worldName = world.getName();
        try {
            Queue<Location> queue = cache.get(worldName);

            Location loc;
            int tries = 0;
            final int MAX_TRIES = plugin.configuration().maxTries();

            do {
                loc = queue.poll();
                cache.refresh(worldName);

                if (loc == null) {
                    continue;
                }

                if (isSafeLocation(loc)) {
                    return Optional.of(loc);
                }

                tries++;
            } while (tries < MAX_TRIES);

            var settings = plugin.configuration().worldSettings(worldName);
            plugin.getLogger().info("No safe location found for " + worldName + " after " + MAX_TRIES + " tries. Trying to find a new one.");
            return findSaveLocation(
                    worldName,
                    settings.radius().min(),
                    settings.radius().max(),
                    settings.origin().x(),
                    settings.origin().z()
            );
        } catch (ExecutionException exception) {
            plugin.getLogger().log(Level.WARNING, "Error while trying to get cached safe location for " + world.getName(), exception);
            return Optional.empty();
        }
    }

    public Optional<Location> findSaveLocation(String worldName, int minRadius, int maxRadius, int originX, int originZ) {
        for (int i = 0; i < plugin.configuration().maxTries(); i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = minRadius + (random.nextDouble() * (maxRadius - minRadius));
            double x = originX + distance * Math.cos(angle);
            double z = originZ + distance * Math.sin(angle);
            int y;
            var world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                return Optional.empty();
            }
            if (world.getEnvironment() == World.Environment.NETHER) {
                var yOpt = getHighestLocationInNether(world, x, z);
                if (yOpt.isEmpty()) {
                    continue;
                }
                y = yOpt.get();
            } else {
                y = world.getHighestBlockYAt((int) x, (int) z);
            }

            plugin.getLogger().info("Trying to find a safe location for " + world.getName() + " at " + x + ", " + y + ", " + z + " try: " + (i + 1));

            var location = new Location(world, x, y, z).toCenterLocation();
            location.setY(location.getY() - 0.5);

            if (isSafeLocation(location)) {
                return Optional.of(location);
            }
        }
        return Optional.empty();
    }


    private boolean isSafeLocation(Location location) {
        Block block = location.getBlock();
        Block blockBelow = block.getRelative(0, -1, 0);
        Block blockAbove = block.getRelative(0, 1, 0);

        if (!isSaveOnBlock(blockBelow)) return false;

        return isSaveInsideBlock(block) && isSaveInsideBlock(blockAbove);
    }

    private boolean isSaveInsideBlock(Block block) {
        if (block.isPassable()) return true;
        return block.isEmpty();
    }

    private boolean isSaveOnBlock(Block block) {
        if (block.getType() == Material.MAGMA_BLOCK) return false;
        if (block.getType() == Material.POWDER_SNOW) return false;
        return block.isSolid();
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
