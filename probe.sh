C=/tmp/cls; R=/tmp/res
sig() { echo "===== SIG $1"; javap -p -cp $C "$1" 2>&1 | grep -v '^Compiled\|lambda\$'; }
sig net.minecraft.world.entity.monster.skeleton.AbstractSkeleton | grep -E 'reassess|isShaking|Sun|burn|registerGoals|aiStep|performRangedAttack|populateDefault|createAttributes|<init>'
sig net.minecraft.world.entity.monster.skeleton.Skeleton | grep -E '<init>|createAttributes|class'
sig net.minecraft.world.entity.Mob | grep -iE 'sun|burn|daylight'
sig net.minecraft.world.entity.LivingEntity | grep -iE 'burnsInDaylight|isSunBurn'
sig net.minecraft.world.level.block.CaveVines | grep -E 'BERRIES'
sig net.minecraft.world.level.block.SweetBerryBushBlock | grep -E 'AGE'
sig net.minecraft.world.level.block.MultifaceBlock | grep -E 'getFaceProperty|static'
