package com.willfp.ecoenchants.enchantments.ecoenchants.artifact;

import com.willfp.ecoenchants.enchantments.itemtypes.Artifact;
import org.bukkit.Particle;
import org.jetbrains.annotations.NotNull;

public class DamageArtifact extends Artifact {
    public DamageArtifact() {
        super(
                "damage_artifact"
        );
    }

    @Override
    public @NotNull Particle getParticle() {
        return Particle.DAMAGE_INDICATOR;
    }
}
