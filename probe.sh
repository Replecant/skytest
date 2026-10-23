C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
dec() { echo "===== DEC $1"; p=${1//.//}; rm -rf /tmp/d /tmp/o; mkdir -p /tmp/d/$(dirname $p) /tmp/o
  cp $C/$p.class $C/$p\$*.class /tmp/d/$(dirname $p)/ 2>/dev/null
  java -jar /tmp/vf/vf.jar -log=ERROR /tmp/d /tmp/o >/dev/null 2>&1; cat /tmp/o/$p.java; }
sig net.minecraft.client.Screenshot
sig net.minecraft.client.Minecraft | grep -E 'gameDirectory|void stop|getMainRenderTarget|options;|gui;|quickPlay|hideGui'
dec net.minecraft.client.quickplay.QuickPlay | sed -n '1,120p'
sig net.minecraft.client.Options | grep -iE 'hideGui|gamma|guiScale'
