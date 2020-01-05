#!/usr/bin/env python3

import sys
import csv
import os.path
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab
import numpy as np

log = open(sys.argv[1], "r")
tot_lookups = 0
ok_count = 0
err_count = 0
fail_count = 0
hops = []

MAX_HOPS = 20

lines = log.readlines()

from_tick = int(sys.argv[3])
to_tick = int(sys.argv[4])

for line in lines[:]:
    if from_tick <= float(line.split(";")[0]) <= to_tick:
        if "LookupStart" in line:
            tot_lookups += 1
        elif "LookupOK" in line:
            ok_count += 1
            hops.append(int(line.split(";")[-1]))
        elif "LookupERROR" in line:
            err_count += 1
        elif "LookupFAILED" in line:
            fail_count += 1

print(f"Total lookups: \t{tot_lookups}")
print(f"OKs: \t\t{ok_count} ({ok_count/tot_lookups*100:.1f})")
print(f"ERRORs: \t{err_count} ({err_count/tot_lookups*100:.1f})")
print(f"FAILED: \t{fail_count} ({fail_count/tot_lookups*100:.1f})")

ok_rate = ok_count/tot_lookups*100
err_rate = err_count/tot_lookups*100
fail_rate = fail_count/tot_lookups*100

import statistics
max_hops = max(hops)
print(f"Hops (Mean): {statistics.mean(hops)}")
print(f"Hops (Max): {max_hops}")

hops_count = [0] * (MAX_HOPS)

for i in hops:
    if i < MAX_HOPS:
        hops_count[i] += 1

print(f"1-percentile: {np.percentile(hops, 1)}")
print(f"99-percentile: {np.percentile(hops, 99)}")

for i in range(MAX_HOPS):
    hops_count[i] = hops_count[i] / ok_count

fig, ax = plt.subplots()
plt.vlines(list(range(0, max(hops))), 0, hops_count, color='C0', lw=4)
plt.xlim(0, 15)
# optionally set y-axis up nicely
#plt.ylim(0, max(hops_count) * 1.06)

plt.show()


# initialize the file if it does not exists 
if not os.path.isfile('lookups.csv'):
    with open('lookups.csv','a') as fd:
        writer = csv.writer(fd)
        writer.writerow(["nodes", "ok", "err", "fail"])

with open('lookups.csv','a') as fd:
    writer = csv.writer(fd)
    writer.writerow([sys.argv[2], ok_rate, err_rate, fail_rate])

# initialize the file if it does not exists 
if not os.path.isfile('hops.csv'):
    with open('hops.csv','a') as fd:
        writer = csv.writer(fd)
        writer.writerow(["nodes"] + list(range(MAX_HOPS)))

with open('hops.csv','a') as fd:
    writer = csv.writer(fd)
    writer.writerow([sys.argv[2]] + hops_count)
