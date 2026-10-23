C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled'; }
dec() { # decompile class (+inner) by fqcn
  echo "===== DEC $1"; p=${1//.//}; rm -rf /tmp/d /tmp/o; mkdir -p /tmp/d/$(dirname $p) /tmp/o
  cp $C/$p.class $C/$p\$*.class /tmp/d/$(dirname $p)/ 2>/dev/null
  java -jar /tmp/vf/vf.jar -log=ERROR /tmp/d /tmp/o >/dev/null 2>&1; cat /tmp/o/$p.java; }
res() { echo "===== RES $1"; cat "$R/$1"; echo; }
fnd() { echo "===== FIND $1"; (cd $C; find . -name "$1" | sort | head -40); }

fnd 'RenderTypes.class'; fnd '*EyesLayer*.class'; fnd 'GameRules*.class'
dec net.minecraft.client.renderer.entity.layers.EyesLayer
dec net.minecraft.client.renderer.entity.IronGolemRenderer
dec net.minecraft.client.model.animal.golem.IronGolemModel
fnd 'IronGolemModel.class'
sig net.minecraft.client.renderer.entity.MobRenderer
sig net.minecraft.client.renderer.entity.LivingEntityRenderer
sig net.minecraft.client.renderer.entity.state.LivingEntityRenderState
sig net.minecraft.client.model.EntityModel
sig net.minecraft.client.model.geom.ModelLayerLocation
sig net.neoforged.neoforge.client.event.EntityRenderersEvent\$RegisterLayerDefinitions
sig net.minecraft.client.gui.screens.Screen
sig net.minecraft.client.gui.GuiGraphics
sig net.neoforged.neoforge.attachment.AttachmentType\$Builder
sig net.neoforged.neoforge.attachment.AttachmentType
sig net.minecraft.world.level.portal.TeleportTransition
sig net.minecraft.world.entity.player.Player | grep -i 'message\|food\|persistent'
sig net.minecraft.server.level.ServerPlayer | grep -i 'teleport\|message\|gamemode\|setGameMode\|respawn'
sig net.minecraft.world.level.gamerules.GameRules
sig net.minecraft.world.level.GameRules
sig net.neoforged.neoforge.client.gui.VanillaGuiLayers
sig net.neoforged.neoforge.client.event.RenderGuiLayerEvent
sig net.minecraft.world.entity.PathfinderMob
sig net.minecraft.world.entity.Mob | grep -i 'interact\|createMob\|registerGoals\|persist\|setPersist'
sig net.minecraft.world.entity.ai.attributes.Attributes | head -30
sig net.minecraft.world.effect.MobEffects | head -20
res data/minecraft/dimension_type/the_nether.json
res data/minecraft/dimension_type/overworld_caves.json
res data/minecraft/worldgen/biome/the_void.json
res data/minecraft/worldgen/biome/basalt_deltas.json
res data/minecraft/worldgen/world_preset/flat.json
res data/minecraft/worldgen/flat_level_generator_preset/the_void.json
res data/minecraft/tags/worldgen/world_preset/normal.json
