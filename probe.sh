C=/tmp/cls
dec() { echo "===== DEC $1"; p=${1//.//}; rm -rf /tmp/d /tmp/o; mkdir -p /tmp/d/$(dirname $p) /tmp/o
  cp $C/$p.class $C/$p\$*.class /tmp/d/$(dirname $p)/ 2>/dev/null
  java -jar /tmp/vf/vf.jar -log=ERROR /tmp/d /tmp/o >/dev/null 2>&1; cat /tmp/o/$p.java; }
dec net.minecraft.world.level.levelgen.WorldDimensions | grep -n -A40 'Lifecycle\|isStable\|checkStability' | head -150
echo "===== grep experimental in neoforge"
cd $C && grep -rl -i 'experimental' net/neoforged | head -20
javap -p -cp $C net.neoforged.neoforge.common.NeoForgeConfig\$Common net.neoforged.neoforge.common.NeoForgeConfig\$Client net.neoforged.neoforge.common.NeoForgeConfig\$Server 2>&1 | grep -i -E 'experiment|lifecycle|warning'
