C=/tmp/cls
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
sig net.minecraft.client.input.KeyEvent
sig net.minecraft.client.input.MouseButtonEvent
sig net.minecraft.client.gui.screens.Screen | grep -E 'mouseClicked|keyPressed|charTyped'
sig net.minecraft.client.gui.components.events.GuiEventListener | grep -E 'mouseClicked|keyPressed'
sig net.neoforged.neoforge.client.event.ComputeFovModifierEvent
sig net.neoforged.neoforge.client.event.ViewportEvent\$ComputeFov
sig net.minecraft.network.codec.ByteBufCodecs | grep -E ' list\(| STRING_UTF8| VAR_INT|collection'
sig net.minecraft.network.codec.StreamCodec | grep composite | head -3
sig net.minecraft.client.gui.Font | grep -E ' split\(|wordWrapHeight| width\('
sig net.minecraft.client.Minecraft | grep -E 'getFps|fpsString'
sig net.minecraft.server.MinecraftServer | grep -iE 'averageTick|tickTime'
sig net.minecraft.world.entity.Entity | grep -E 'getEyePosition|yRotO|xRotO|setYRot|setXRot'
sig net.minecraft.sounds.SoundEvents | grep -E 'NOTE_BLOCK_BASS|NOTE_BLOCK_HAT|NOTE_BLOCK_BIT'
