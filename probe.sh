C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
dec() { echo "===== DEC $1"; p=${1//.//}; rm -rf /tmp/d /tmp/o; mkdir -p /tmp/d/$(dirname $p) /tmp/o
  cp $C/$p.class $C/$p\$*.class /tmp/d/$(dirname $p)/ 2>/dev/null
  java -jar /tmp/vf/vf.jar -log=ERROR /tmp/d /tmp/o >/dev/null 2>&1; cat /tmp/o/$p.java; }
res() { echo "===== RES $1"; cat "$R/$1"; echo; }
sig net.minecraft.world.level.block.Blocks | grep -E 'DEEPSLATE|CHAIN|LANTERN| LIGHT;|SKULL|COBWEB|BONE_BLOCK| CHEST;|CARPET|HAY|TUFF|IRON_BARS|CANDLE|END_ROD|SCULK;|MOSS|ROOTS|CAMPFIRE|BARRIER'
sig net.minecraft.server.level.ServerLevel | grep -iE 'spawn|respawn|gamerule|getGameRules|setDayTime'
sig net.minecraft.world.level.storage.LevelData\$RespawnData
sig net.minecraft.world.entity.Entity | grep -E 'snapTo|moveTo|setYHeadRot|setYBodyRot|setInvulnerable|setCustomName|setSilent'
sig net.minecraft.commands.CommandSourceStack | grep -iE 'permission|getPlayerOrException|sendSuccess|getServer|getLevel'
sig net.minecraft.commands.Commands | grep -iE 'static|hasPermission|LEVEL_'
sig net.minecraft.client.resources.sounds.SimpleSoundInstance | grep forUI
sig net.neoforged.neoforge.client.event.RenderGuiEvent\$Post
sig net.neoforged.neoforge.event.entity.player.PlayerEvent\$PlayerRespawnEvent
sig net.neoforged.neoforge.event.entity.player.PlayerEvent\$PlayerLoggedInEvent
sig net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent
sig net.minecraft.client.renderer.rendertype.RenderTypes | grep -iE 'eyes|cutout'
sig net.minecraft.world.entity.ai.goal.LookAtPlayerGoal | grep 'LookAtPlayerGoal('
sig net.minecraft.world.effect.MobEffectInstance | grep 'MobEffectInstance('
sig net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket | grep 'Packet('
sig net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket | grep 'Packet('
sig net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket | grep 'Packet('
sig net.minecraft.world.level.block.state.properties.BlockStateProperties | grep -E 'HANGING|ROTATION_16| LEVEL;|FACING;|HORIZONTAL_FACING|AXIS;'
sig net.minecraft.world.level.block.LightBlock | grep -E 'static'
sig net.minecraft.world.level.block.Block | grep -E 'updateFromNeighbourShapes|UPDATE_'
sig net.minecraft.world.level.block.entity.ChestBlockEntity | grep -E 'setItem|setLootTable'
sig net.minecraft.world.food.FoodData | grep -E 'set'
sig net.minecraft.world.entity.Mob | grep -E 'removeWhenFarAway|setNoAi|requiresCustomPersistence|checkDespawn'
sig net.minecraft.world.entity.LivingEntity | grep -E 'addEffect|isPushable|doPush|pushEntities'
dec net.minecraft.world.level.dimension.DimensionType | sed -n '1,140p'
dec net.minecraft.world.level.levelgen.presets.WorldPreset | sed -n '1,80p'
res data/minecraft/dimension_type/overworld.json
ls $R/data/minecraft/timeline 2>/dev/null; ls $R/data/minecraft/tags/timeline 2>/dev/null
ls $R/data/minecraft/worldgen/world_preset
