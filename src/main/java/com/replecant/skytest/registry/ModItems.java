package com.replecant.skytest.registry;

import com.replecant.skytest.SkyTest;
import com.replecant.skytest.item.DullSwordItem;
import com.replecant.skytest.item.GrapplingHookItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.equipment.ArmorType;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SkyTest.MODID);

    /** Крюк-хват в виде скелетной руки. */
    public static final DeferredItem<GrapplingHookItem> GRAPPLING_HOOK = ITEMS.register("grappling_hook",
            id -> new GrapplingHookItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .durability(384)
                    .rarity(Rarity.UNCOMMON)));

    /** Сапоги циркача: дают двойной прыжок. */
    public static final DeferredItem<Item> CIRCUS_BOOTS = ITEMS.register("circus_boots",
            id -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .humanoidArmor(ModArmorMaterials.CIRCUS, ArmorType.BOOTS)
                    .rarity(Rarity.UNCOMMON)));

    public static final DeferredItem<DoubleHighBlockItem> PRISON_GATE = ITEMS.register("prison_gate",
            id -> new DoubleHighBlockItem(ModBlocks.PRISON_GATE.get(), new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .useBlockDescriptionPrefix()));

    /** Затупленный меч, который бросает Незнакомец. Бьёт слабо, зато умеет парировать (ПКМ). */
    public static final DeferredItem<DullSwordItem> DULL_SWORD = ITEMS.register("dull_sword",
            id -> new DullSwordItem(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .sword(ToolMaterial.STONE, 2.0F, -2.4F)));

    /** Сырое крысиное мясо: немного сытости и шанс отравиться — как сырая курица. */
    public static final DeferredItem<Item> RAW_RAT_MEAT = ITEMS.register("raw_rat_meat",
            id -> new Item(new Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, id))
                    .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build(),
                            Consumables.defaultFood()
                                    .onConsume(new ApplyStatusEffectsConsumeEffect(
                                            new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F))
                                    .build())));

    private ModItems() {}
}
