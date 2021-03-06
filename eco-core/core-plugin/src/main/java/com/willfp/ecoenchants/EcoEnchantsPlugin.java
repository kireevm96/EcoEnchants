package com.willfp.ecoenchants;

import com.willfp.eco.core.AbstractPacketAdapter;
import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.command.AbstractCommand;
import com.willfp.eco.core.display.DisplayModule;
import com.willfp.eco.core.integrations.IntegrationLoader;
import com.willfp.eco.util.TelekinesisUtils;
import com.willfp.ecoenchants.command.commands.CommandEcodebug;
import com.willfp.ecoenchants.command.commands.CommandEcoreload;
import com.willfp.ecoenchants.command.commands.CommandEnchantinfo;
import com.willfp.ecoenchants.command.commands.CommandGiverandombook;
import com.willfp.ecoenchants.command.commands.CommandRandomenchant;
import com.willfp.ecoenchants.command.tabcompleters.TabCompleterEnchantinfo;
import com.willfp.ecoenchants.config.RarityYml;
import com.willfp.ecoenchants.config.TargetYml;
import com.willfp.ecoenchants.config.VanillaEnchantsYml;
import com.willfp.ecoenchants.display.EnchantDisplay;
import com.willfp.ecoenchants.display.EnchantmentCache;
import com.willfp.ecoenchants.enchantments.EcoEnchants;
import com.willfp.ecoenchants.enchantments.meta.EnchantmentRarity;
import com.willfp.ecoenchants.enchantments.meta.EnchantmentTarget;
import com.willfp.ecoenchants.enchantments.meta.EnchantmentType;
import com.willfp.ecoenchants.enchantments.support.merging.anvil.AnvilListeners;
import com.willfp.ecoenchants.enchantments.support.merging.grindstone.GrindstoneListeners;
import com.willfp.ecoenchants.enchantments.support.obtaining.EnchantingListeners;
import com.willfp.ecoenchants.enchantments.support.obtaining.LootPopulator;
import com.willfp.ecoenchants.enchantments.support.obtaining.VillagerListeners;
import com.willfp.ecoenchants.enchantments.util.ItemConversions;
import com.willfp.ecoenchants.enchantments.util.TimedRunnable;
import com.willfp.ecoenchants.enchantments.util.WatcherTriggers;
import com.willfp.ecoenchants.integrations.essentials.EssentialsManager;
import com.willfp.ecoenchants.integrations.essentials.plugins.IntegrationEssentials;
import com.willfp.ecoenchants.proxy.proxies.FastGetEnchantsProxy;
import com.willfp.ecoenchants.util.ProxyUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.generator.BlockPopulator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
public class EcoEnchantsPlugin extends EcoPlugin {
    /**
     * Instance of the plugin.
     */
    @Getter
    private static EcoEnchantsPlugin instance;

    /**
     * Rarity.yml.
     */
    @Getter
    private final RarityYml rarityYml;

    /**
     * Target.yml.
     */
    @Getter
    private final TargetYml targetYml;

    /**
     * VanillaEnchants.yml.
     */
    @Getter
    private final VanillaEnchantsYml vanillaEnchantsYml;

    /**
     * Internal constructor called by bukkit on plugin load.
     */
    public EcoEnchantsPlugin() {
        super("EcoEnchants", 79573, 7666, "com.willfp.ecoenchants.proxy", "&a");
        instance = this;

        rarityYml = new RarityYml(this);
        targetYml = new TargetYml(this);
        vanillaEnchantsYml = new VanillaEnchantsYml(this);
    }

    /**
     * Code executed on plugin enable.
     */
    @Override
    public void enable() {
        this.getExtensionLoader().loadExtensions();

        if (this.getExtensionLoader().getLoadedExtensions().isEmpty()) {
            this.getLogger().info("&cNo extensions found");
        } else {
            this.getLogger().info("Extensions Loaded:");
            this.getExtensionLoader().getLoadedExtensions().forEach(extension -> this.getLogger().info("- " + extension.getName() + " v" + extension.getVersion()));
        }

        this.getLogger().info(EcoEnchants.values().size() + " Enchantments Loaded");

        TelekinesisUtils.registerTest(player -> ProxyUtils.getProxy(FastGetEnchantsProxy.class).getLevelOnItem(player.getInventory().getItemInMainHand(), EcoEnchants.TELEKINESIS) > 0);
    }

    /**
     * Code executed on plugin disable.
     */
    @Override
    public void disable() {
        Bukkit.getServer().getWorlds().forEach(world -> {
            List<BlockPopulator> populators = new ArrayList<>(world.getPopulators());
            populators.forEach((blockPopulator -> {
                if (blockPopulator instanceof LootPopulator) {
                    world.getPopulators().remove(blockPopulator);
                }
            }));
        });

        this.getExtensionLoader().unloadExtensions();
    }

    /**
     * Nothing is called on plugin load.
     */
    @Override
    public void load() {
        // Nothing needs to be called on load
    }

    /**
     * Code executed on /ecoreload.
     */
    @Override
    public void onReload() {
        targetYml.update();
        rarityYml.update();
        ((EnchantDisplay) this.getDisplayModule()).update();
        EcoEnchants.values().forEach((ecoEnchant -> {
            HandlerList.unregisterAll(ecoEnchant);

            this.getScheduler().runLater(() -> {
                if (ecoEnchant.isEnabled()) {
                    this.getEventManager().registerListener(ecoEnchant);

                    if (ecoEnchant instanceof TimedRunnable) {
                        this.getScheduler().syncRepeating((TimedRunnable) ecoEnchant, 5, ((TimedRunnable) ecoEnchant).getTime());
                    }
                }
            }, 1);
        }));
    }

    /**
     * Code executed after server is up.
     */
    @Override
    public void postLoad() {
        if (this.getConfigYml().getBool("loot.enabled")) {
            Bukkit.getServer().getWorlds().forEach(world -> {
                List<BlockPopulator> populators = new ArrayList<>(world.getPopulators());
                populators.forEach((blockPopulator -> {
                    if (blockPopulator instanceof LootPopulator) {
                        world.getPopulators().remove(blockPopulator);
                    }
                }));
                world.getPopulators().add(new LootPopulator(this));
            });
        }
        EssentialsManager.registerEnchantments();
    }

    /**
     * EcoEnchants-specific integrations.
     *
     * @return A list of all integrations.
     */
    @Override
    public List<IntegrationLoader> getIntegrationLoaders() {
        return Arrays.asList(
                new IntegrationLoader("Essentials", () -> EssentialsManager.register(new IntegrationEssentials()))
        );
    }

    /**
     * EcoEnchants-specific commands.
     *
     * @return A list of all commands.
     */
    @Override
    public List<AbstractCommand> getCommands() {
        return Arrays.asList(
                new CommandEcodebug(this),
                new CommandEcoreload(this),
                new CommandEnchantinfo(this),
                new CommandRandomenchant(this),
                new CommandGiverandombook(this)
        );
    }

    /**
     * Packet Adapters for enchant display.
     *
     * @return A list of packet adapters.
     */
    @Override
    public List<AbstractPacketAdapter> getPacketAdapters() {
        return new ArrayList<>();
    }

    /**
     * EcoEnchants-specific listeners.
     *
     * @return A list of all listeners.
     */
    @Override
    public List<Listener> getListeners() {
        return Arrays.asList(
                new EnchantingListeners(this),
                new GrindstoneListeners(this),
                new AnvilListeners(this),
                new WatcherTriggers(this),
                new VillagerListeners(this),
                new ItemConversions(this)
        );
    }

    @Override
    public List<Class<?>> getUpdatableClasses() {
        return Arrays.asList(
                EnchantmentCache.class,
                EnchantmentRarity.class,
                EnchantmentTarget.class,
                EcoEnchants.class,
                TabCompleterEnchantinfo.class,
                EnchantmentType.class,
                WatcherTriggers.class
        );
    }

    @Override
    @Nullable
    protected DisplayModule createDisplayModule() {
        return new EnchantDisplay(this);
    }
}
