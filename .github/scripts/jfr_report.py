"""Summarise a JFR recording: hot methods per thread and time spent in mod code."""
import collections
import json
import subprocess
import sys

path = sys.argv[1]
raw = subprocess.run(["jfr", "print", "--json", "--stack-depth", "64", "--events", "jdk.ExecutionSample", path],
                     capture_output=True, text=True, check=True).stdout
events = json.loads(raw)["recording"]["events"]


def frame_name(frame):
    method = frame["method"]
    return method["type"]["name"].split(".")[-1] + "." + method["name"]


def full_name(frame):
    method = frame["method"]
    return method["type"]["name"] + "." + method["name"]


threads = collections.Counter()
self_time = collections.defaultdict(collections.Counter)
inclusive = collections.defaultdict(collections.Counter)
mod_inclusive = collections.Counter()
mod_samples = collections.Counter()

for event in events:
    values = event["values"]
    thread = (values.get("sampledThread") or {}).get("javaName") or "?"
    frames = ((values.get("stackTrace") or {}).get("frames")) or []
    if not frames:
        continue
    threads[thread] += 1
    self_time[thread][frame_name(frames[0])] += 1
    seen = set()
    touched_mod = False
    for frame in frames:
        name = full_name(frame)
        short = frame_name(frame)
        if short not in seen:
            inclusive[thread][short] += 1
            seen.add(short)
        if name.startswith("com.replecant"):
            touched_mod = True
            if name not in seen:
                mod_inclusive[name] += 1
                seen.add(name)
    if touched_mod:
        mod_samples[thread] += 1

print("=== samples per thread")
for thread, count in threads.most_common(12):
    print(f"{count:7d}  {thread}   (with mod code: {mod_samples[thread]})")
for thread in ("Render thread", "Server thread"):
    total = max(1, threads[thread])
    print(f"\n=== {thread}: top self methods")
    for name, count in self_time[thread].most_common(25):
        print(f"{100 * count / total:6.1f}%  {name}")
    print(f"\n=== {thread}: top inclusive methods")
    for name, count in inclusive[thread].most_common(40):
        print(f"{100 * count / total:6.1f}%  {name}")
print("\n=== mod methods (inclusive samples, all threads)")
for name, count in mod_inclusive.most_common(30):
    print(f"{count:7d}  {name}")
