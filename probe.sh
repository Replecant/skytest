C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
sig net.minecraft.client.renderer.ItemInHandRenderer | grep -E 'renderPlayerArm|renderArmWithItem|applyItemArmTransform|renderHandsWithItems|renderMapHand|renderOneHanded'
sig net.neoforged.neoforge.client.event.RenderHandEvent | grep -E 'public'
sig net.minecraft.client.renderer.SubmitNodeCollector | head -30
sig net.minecraft.client.renderer.OrderedSubmitNodeCollector | head -30
sig net.minecraft.client.renderer.entity.EntityRenderer | grep -E 'submit|createRenderState|extractRenderState|shouldRender'
sig net.minecraft.client.model.Model | grep -E 'public|protected'
sig net.minecraft.world.entity.projectile.Projectile | grep -E 'abstract|onHitEntity|onHit|shoot\(|getOwner'
sig net.minecraft.world.entity.projectile.ThrowableProjectile | grep -E 'abstract|getDefaultGravity|<init>'
sig net.minecraft.client.renderer.rendertype.RenderTypes | grep -E 'entityCutout|eyes|lightning|entityTranslucentEmissive|energySwirl|entityTranslucent\('
sig net.minecraft.world.effect.MobEffects | grep -E 'POISON'
sig net.minecraft.sounds.SoundEvents | grep -E ' (TRIDENT_THUNDER|LIGHTNING_BOLT_THUNDER|LIGHTNING_BOLT_IMPACT|BEACON_ACTIVATE|EVOKER_CAST_SPELL|LLAMA_SPIT|SILVERFISH_AMBIENT|ILLUSIONER_CAST_SPELL|BREEZE_SHOOT);'
sig net.minecraft.client.renderer.entity.player.AvatarRenderer | grep -E 'renderRightHand|renderLeftHand|renderHand'
