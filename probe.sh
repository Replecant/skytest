C=/tmp/cls
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
sig net.minecraft.sounds.SoundEvents | grep -E ' (SHIELD_BLOCK|SHIELD_BREAK|IRON_GOLEM_STEP|IRON_GOLEM_HURT|IRON_GOLEM_DEATH|ARMOR_EQUIP_CHAIN|ARMOR_EQUIP_IRON|PLAYER_ATTACK_SWEEP|PLAYER_ATTACK_STRONG|ZOMBIE_ATTACK_IRON_DOOR|ANVIL_LAND|CHAIN_STEP|IRON_CHAIN_STEP|WITHER_SKELETON_AMBIENT|WARDEN_HEARTBEAT|RAVAGER_ROAR|EVOKER_PREPARE_ATTACK|TRIDENT_THROW|BLAZE_SHOOT|ITEM_BREAK|MACE_SMASH_GROUND|MACE_SMASH_GROUND_HEAVY|BREEZE_JUMP);'
sig net.minecraft.server.level.ServerBossEvent | grep 'ServerBossEvent('
sig net.minecraft.world.BossEvent\$BossBarColor | grep PURPLE
sig net.minecraft.world.BossEvent\$BossBarOverlay | grep NOTCHED
sig net.minecraft.world.entity.Mob | grep -E 'customServerAiStep|playStepSound|getAmbientSound|getHurtSound'
sig net.minecraft.world.entity.LivingEntity | grep -E ' hurtServer|getHurtSound|getDeathSound|playStepSound|die\('
sig net.minecraft.world.entity.Entity | grep -E 'onSyncedDataUpdated|playStepSound|startSeenByPlayer|stopSeenByPlayer'
sig net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal | grep 'Goal('
sig net.minecraft.world.entity.ai.goal.Goal | grep -E 'Flag|setFlags|requiresUpdateEveryTick'
sig net.minecraft.world.damagesource.DamageSources | grep -E 'mobAttack'
