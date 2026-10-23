C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
sig net.minecraft.client.renderer.entity.state.AvatarRenderState | grep -v 'static'
sig net.minecraft.client.renderer.entity.state.HumanoidRenderState | grep -v 'static'
sig net.minecraft.client.renderer.entity.state.ArmedEntityRenderState | grep -v 'static'
sig net.minecraft.client.renderer.entity.state.EntityRenderState | grep -E 'public [a-z].* [a-zA-Z]+;|RenderData'
sig net.neoforged.neoforge.client.event.RegisterRenderStateModifiersEvent
sig net.neoforged.neoforge.client.event.EntityRenderersEvent\$AddLayers | grep public
sig net.minecraft.client.renderer.entity.layers.RenderLayer
sig net.minecraft.client.renderer.entity.EntityRenderDispatcher | grep -E 'Player|Avatar|getRenderer'
sig net.minecraft.client.player.AbstractClientPlayer | grep -E 'getSkin'
sig net.minecraft.world.entity.player.PlayerSkin | grep -E 'public'
sig net.minecraft.core.ClientAsset\$Texture | grep -E 'public'
sig net.minecraft.client.renderer.SubmitNodeCollector\$CustomGeometryRenderer
sig net.minecraft.client.model.player.PlayerModel | grep -E 'public|Arm'
sig net.minecraft.client.model.HumanoidModel | grep -E 'rightArm|leftArm|translateToHand'
sig net.minecraft.util.context.ContextKey | grep public
