package com.willfp.ecoenchants.display.options.sorting.implementations;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.PluginDependent;
import com.willfp.ecoenchants.display.EnchantDisplay;
import com.willfp.ecoenchants.display.EnchantmentCache;
import com.willfp.ecoenchants.display.options.sorting.EnchantmentSorter;
import com.willfp.ecoenchants.display.options.sorting.SortParameters;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RarityTypeLengthSorter extends PluginDependent implements EnchantmentSorter {
    /**
     * Instantiate sorter.
     *
     * @param plugin Instance of EcoEnchants.
     */
    public RarityTypeLengthSorter(@NotNull final EcoPlugin plugin) {
        super(plugin);
    }

    @Override
    public void sortEnchantments(@NotNull final List<Enchantment> toSort) {
        if (((EnchantDisplay) this.getPlugin().getDisplayModule()).getOptions().getSortedRarities().isEmpty()
                || ((EnchantDisplay) this.getPlugin().getDisplayModule()).getOptions().getSortedTypes().isEmpty()) {
            ((EnchantDisplay) this.getPlugin().getDisplayModule()).update();
        }

        List<Enchantment> sorted = new ArrayList<>();
        ((EnchantDisplay) this.getPlugin().getDisplayModule()).getOptions().getSortedTypes().forEach(enchantmentType -> {
            List<Enchantment> typeEnchants = new ArrayList<>();
            for (Enchantment enchantment : toSort) {
                if (EnchantmentCache.getEntry(enchantment).getType().equals(enchantmentType)) {
                    typeEnchants.add(enchantment);
                }
            }

            typeEnchants.sort(Comparator.comparingInt(enchantment -> EnchantmentCache.getEntry(enchantment).getRawName().length()));

            ((EnchantDisplay) this.getPlugin().getDisplayModule()).getOptions().getSortedRarities().forEach(enchantmentRarity -> {
                List<Enchantment> rarityEnchants = new ArrayList<>();
                for (Enchantment enchantment : typeEnchants) {
                    if (EnchantmentCache.getEntry(enchantment).getRarity().equals(enchantmentRarity)) {
                        rarityEnchants.add(enchantment);
                    }
                }
                rarityEnchants.sort(Comparator.comparingInt(enchantment -> EnchantmentCache.getEntry(enchantment).getRawName().length()));
                sorted.addAll(rarityEnchants);
            });
        });

        toSort.clear();
        toSort.addAll(sorted);
    }

    @Override
    public SortParameters[] getParameters() {
        return new SortParameters[]{SortParameters.RARITY, SortParameters.TYPE, SortParameters.LENGTH};
    }
}
