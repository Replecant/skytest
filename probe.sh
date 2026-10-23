C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
dec() { echo "===== DEC $1"; p=${1//.//}; rm -rf /tmp/d /tmp/o; mkdir -p /tmp/d/$(dirname $p) /tmp/o
  cp $C/$p.class $C/$p\$*.class /tmp/d/$(dirname $p)/ 2>/dev/null
  java -jar /tmp/vf/vf.jar -log=ERROR /tmp/d /tmp/o >/dev/null 2>&1; cat /tmp/o/$p.java; }
sig net.minecraft.world.level.chunk.ChunkAccess | grep -iE 'fillBiomes|markUnsaved|setUnsaved'
sig net.minecraft.world.level.biome.BiomeResolver
dec net.minecraft.server.commands.FillBiomeCommand | sed -n '/private static.*fill(/,/^   }/p' | head -80
sig net.minecraft.server.level.ServerChunkCache | grep -iE 'randomState'
sig net.minecraft.world.level.levelgen.RandomState | grep -iE 'sampler'
sig net.minecraft.world.damagesource.DamageSources | grep -iE 'fellOutOfWorld|generic\(|fall\('
ls $R/data/minecraft/timeline/; cat $R/data/minecraft/timeline/day.json | head -80
cat $R/data/minecraft/tags/timeline/in_overworld.json
sig net.minecraft.world.attribute.EnvironmentAttributes | grep -E 'static final' | head -60
