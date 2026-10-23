C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
res() { echo "===== RES $1"; cat "$R/$1"; echo; }
sig net.minecraft.world.level.block.DoorBlock | grep -E 'DoorBlock\(|static final'
sig net.minecraft.world.item.DoubleHighBlockItem | grep 'DoubleHighBlockItem('
sig net.minecraft.world.level.block.state.properties.BlockSetType | grep -E 'IRON|OAK;'
sig net.minecraft.world.item.Item\$Properties | grep -E 'sword|useBlockDescriptionPrefix|repairable'
sig net.minecraft.world.item.ToolMaterial | grep -E 'static final'
sig net.minecraft.world.level.block.state.BlockBehaviour\$Properties | grep -E 'strength|noOcclusion|pushReaction|sound\(|mapColor\(net.minecraft.world.level.material.MapColor\)|requiresCorrect|forceSolid|isViewBlocking'
sig net.neoforged.neoforge.registries.DeferredRegister\$Blocks | grep -E 'register'
sig net.neoforged.neoforge.event.entity.living.LivingDropsEvent | grep -E 'class|getEntity|setCanceled'
sig net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent | grep -E 'class'
sig net.minecraft.world.entity.item.ItemEntity | grep -E 'setUnlimitedLifetime|setPickUpDelay|ItemEntity\('
sig net.minecraft.world.entity.ai.navigation.PathNavigation | grep -E ' moveTo\('
sig net.minecraft.world.entity.monster.skeleton.Skeleton | head -5
find $C -name 'Skeleton.class' | head
sig net.neoforged.neoforge.common.ModConfigSpec\$BooleanValue | head -3
res assets/minecraft/blockstates/iron_door.json
res assets/minecraft/models/block/iron_door_bottom_left.json
res assets/minecraft/models/block/door_bottom_left.json
res assets/minecraft/items/iron_door.json
res assets/minecraft/items/iron_sword.json
res assets/minecraft/models/item/iron_sword.json
