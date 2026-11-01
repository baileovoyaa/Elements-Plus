package com.elementsplus;

import com.elementsplus.entity.SyringeProjectile;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityType<SyringeProjectile> SYRINGE_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ElementsPlus.id("syringe_projectile"),
            EntityType.Builder.<SyringeProjectile>of(SyringeProjectile::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .noSave()
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("syringe_projectile")
    );

    public static void initialize() {
        ElementsPlus.LOGGER.info("Registered custom entity type(s)");
    }
}
