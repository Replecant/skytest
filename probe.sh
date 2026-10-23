C=/tmp/cls
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
sig net.neoforged.neoforge.client.event.ViewportEvent\$ComputeCameraAngles
sig net.neoforged.neoforge.event.entity.player.AttackEntityEvent | grep -E 'class|getTarget'
sig net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent | grep -E 'class|getAmount|setAmount|getSource'
sig net.neoforged.neoforge.event.entity.living.LivingDamageEvent\$Post | grep -E 'class|getNewDamage|getSource'
sig net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent | grep -E 'class|Strength|getRatio'
sig net.minecraft.world.entity.Entity | grep -E 'getKnownMovement|walkDist|horizontalCollision| moveDist'
sig net.minecraft.world.entity.LivingEntity | grep -E ' knockback\(|hurtTime|hurtDuration|getAttackStrengthScale'
sig net.minecraft.world.entity.player.Player | grep -E 'getAttackStrengthScale|resetAttackStrengthTicker'
sig net.minecraft.client.multiplayer.MultiPlayerGameMode | grep -E ' attack\('
sig net.minecraft.client.KeyMapping | grep -E 'setDown|isDown'
sig net.minecraft.client.Minecraft | grep -E ' gameMode;'
sig net.minecraft.world.damagesource.DamageSource | grep -E 'getEntity|getDirectEntity|isDirect'
sig net.minecraft.client.player.LocalPlayer | grep -E 'isSprinting|input;|ClientInput'
sig net.neoforged.neoforge.event.tick.PlayerTickEvent\$Post | head -3
