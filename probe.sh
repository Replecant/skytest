cd /tmp/src
f() { find . -name "$1.java" | head -1; }
show() { p=$(f $1); echo "===== $1 ($p)"; cat "$p"; }
echo "== test"; ls | head
