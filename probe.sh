C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
res() { echo "===== RES $1"; cat "$R/$1"; echo; }
sig net.minecraft.world.entity.Entity | grep -E 'abstract'
sig net.minecraft.client.renderer.entity.NoopRenderer | head -3
sig net.minecraft.world.level.EntityGetter | grep -E 'getNearestPlayer'
sig net.minecraft.world.entity.ai.control.MoveControl | grep -E 'strafe|setWantedPosition'
sig net.neoforged.neoforge.event.entity.living.LivingDeathEvent | grep -E 'class|getSource'
res assets/minecraft/models/block/door_bottom_right.json
res assets/minecraft/models/block/door_top_left.json
res assets/minecraft/models/block/door_bottom_left_open.json
