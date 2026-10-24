package com.replecant.skytest.registry;

import java.util.EnumMap;

import com.replecant.skytest.SkyTest;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
    /** Points to assets/skytest/equipment/circus.json. */
    public static final ResourceKey<EquipmentAsset> CIRCUS_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(SkyTest.MODID, "circus"));

    public static final ArmorMaterial CIRCUS = new ArmorMaterial(
            8,
            defense(1, 2, 3, 1, 3),
            15,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            0.0F,
            0.0F,
            ItemTags.REPAIRS_LEATHER_ARMOR,
            CIRCUS_ASSET);

    /** Points to assets/skytest/equipment/warden_lamellar.json. */
    public static final ResourceKey<EquipmentAsset> WARDEN_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(SkyTest.MODID, "warden_lamellar"));

    /** Ламеллярный доспех стража — по защите и прочности как кожаная броня. */
    public static final ArmorMaterial WARDEN_LAMELLAR = new ArmorMaterial(
            5,
            defense(1, 2, 3, 1, 3),
            15,
            SoundEvents.ARMOR_EQUIP_CHAIN,
            0.0F,
            0.0F,
            ItemTags.REPAIRS_LEATHER_ARMOR,
            WARDEN_ASSET);

    private static EnumMap<ArmorType, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
        EnumMap<ArmorType, Integer> map = new EnumMap<>(ArmorType.class);
        map.put(ArmorType.BOOTS, boots);
        map.put(ArmorType.LEGGINGS, leggings);
        map.put(ArmorType.CHESTPLATE, chestplate);
        map.put(ArmorType.HELMET, helmet);
        map.put(ArmorType.BODY, body);
        return map;
    }

    private ModArmorMaterials() {}
}
