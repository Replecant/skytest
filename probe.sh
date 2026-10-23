C=/tmp/cls
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
sig net.minecraft.client.KeyMapping | grep -E 'KeyMapping\(|consumeClick|Category'
sig net.minecraft.client.KeyMapping\$Category
sig net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
sig net.neoforged.neoforge.event.tick.EntityTickEvent\$Pre
sig net.neoforged.neoforge.network.PacketDistributor | grep -E 'static'
sig net.minecraft.client.player.ClientInput | grep -E 'moveVector|getMoveVector|forwardImpulse|leftImpulse|keyPresses'
sig net.minecraft.world.phys.Vec2 | grep -E ' x;| y;'
