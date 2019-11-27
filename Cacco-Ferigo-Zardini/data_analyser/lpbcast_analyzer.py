#!/usr/bin/env python3

import numpy as np
import sys,os
import matplotlib.pyplot as plt
from itertools import chain

def wprint(s):
	print("\033[1;33;40m"+s+"\033[1;37;40m")

datasources = sys.argv[2:]
requests = sys.argv[1].split(",")

splits = 5
maxdelay = 10
resub_events = 10

datas = {}
events_set = {}

nodes = {}
alive = {}
subbed = {}
up = {}

subinfo = {}
resubbed = []
nodeknown = {}

for dsname in datasources:
	if dsname in events_set.keys():
		wprint("warning: file \"%s\" requested more then once."%(dsname))
	elif not os.path.isfile(dsname):
		wprint("warning: invalid file \"%s\"!"%(dsname))
	else:
		print("loading %s..."%(dsname))
		with open(dsname) as f:
			data = f.readlines()

		#remove header
		data = data[1:]

		#split and cast types
		print("parsing...")
		data = [d.split(";") for d in data]

		#(progress bar)
		toolbar_width = 60
		sys.stdout.write("[%s]" % (" " * toolbar_width))
		sys.stdout.flush()
		sys.stdout.write("\b" * (toolbar_width+1))
		events = {}
		rows = len(data)
		nodes_ids = set()
		for d_i in range(rows):
			d = data[d_i]
			d[0] = int(float(d[0]))
			d[1] = int(d[1])
			d[2] = [int(e) for e in d[2][2:-2].split(", ")] if len(d[2])>4 else []
			d[3] = [int(e) for e in d[3][2:-2].split(", ")] if len(d[3])>4 else []
			d[4] = [int(e) for e in d[4][2:-2].split(", ")] if len(d[4])>4 else []
			d[5] = [[int(evalue) for evalue in e.split(":")] for e in d[5][2:-2].split(", ")] if len(d[5])>4 else []
			#d[6] = [[int(evalue) for evalue in e.split(":")] for e in d[5][2:-2].split(", ")] if len(d[5])>4 else []
			d[6] = d[6]=="\"true\""
			d[7] = d[7]=="\"false\""

			nodes_ids.add(d[1])

			if d[0] not in alive.keys():
				alive[d[0]]=set()
			if not d[6]: #not crashed
				alive[d[0]].add(d[1])

			if d[0] not in subbed.keys():
				subbed[d[0]]=set()
			if d[7]: #subscribed
				subbed[d[0]].add(d[1])

			if d[0] not in up.keys():
				up[d[0]]=set()
			if d[7] or not d[6]: #subscribed and not crashed
				up[d[0]].add(d[1])
			#progress bar
			if d_i%(rows/toolbar_width)==rows/toolbar_width-1:
				sys.stdout.write("-")
				sys.stdout.flush()

			#id : tick : event    
			for event in d[5]:
				eventid = ("_".join([str(ev) for ev in event]))
				if eventid not in events.keys():
					events[eventid] = {}
				
				if d[1] not in events[eventid].keys():
					events[eventid][d[1]] = (d[0]-event[1])
				else:
					events[eventid][d[1]] = min(events[eventid][d[1]],d[0]-event[1])

			if d[1] not in subinfo.keys():
				subinfo[d[1]] = None
			elif d[1] and not subinfo[d[1]]: #node subbed back
				resubbed.append((d[0],d[1]))

			subinfo[d[0]] = d[7]

			if d[1] not in nodeknown.keys():
				nodeknown[d[1]] = []
			nodeknown[d[1]].append(d[2]+d[3])
			
		events_set[dsname] = events

		nodes[dsname] = len(nodes_ids)

		#datas[dsname] = data

		sys.stdout.write("]\n")

if "propagation_100" in requests or "all" in requests:
	plt.boxplot([
		[
			max(e.values()) 
			for e in (
				events_set[set_key]
			).values() 
			if len(e) == nodes[set_key]
		] 
		for set_key in events_set.keys()
	])
	plt.gca().set_xticklabels([k.split("/")[-1] for k in events_set.keys()], rotation=45)
	plt.ylabel("Rounds needed to spread a message")
	plt.xlabel("Configurations")
	plt.tight_layout()
	plt.savefig("plots/propagation.pdf")
	plt.show()

if "propagation_90" in requests or "all" in requests:
	plt.boxplot([
		[
			max(e.values()) 
			for e in (
				events_set[set_key]
			).values() 
			if len(e) > nodes[set_key]*0.9
		] 
		for set_key in events_set.keys()
	])
	plt.gca().set_xticklabels([k.split("/")[-1] for k in events_set.keys()], rotation=45)
	plt.ylabel("Rounds needed to spread a message")
	plt.xlabel("Configurations")
	plt.tight_layout()
	plt.savefig("plots/propagation90.pdf")
	plt.show()

if "round_propagation" in requests or "all" in requests:
	for set_key,events in events_set.items():
		plt.plot(
			[0]+[
				sum([
					len([
						d
						for d in e.values()
						if d<=delay
					])
					for e in events.values()
				])/(len(events.keys())*(nodes[set_key]))
				for delay in range(maxdelay)
			],
			label = set_key.split("/")[-1],
			marker = "o"
		)
	plt.legend()
	plt.ylabel("Node reached (%)")
	plt.xlabel("Rounds")
	plt.gca().set_xticks(range(maxdelay+1))
	plt.tight_layout()
	plt.savefig("plots/round_propagation.pdf")
	plt.show()

if "coverage" in requests or "all" in requests:
	plts = []
	for index in range(splits):
		factor = float(index+1)/float(splits)
		p = plt.bar(
			events_set.keys(),
			[
				100*len([
					e
					for e in (
						events_set[set_key]
					).values() 
					#if (len(e) < (nodes[set_key]-1) * (factor+0.1)) and (len(e) >= (nodes[set_key]-1) * factor)
					if (len(e) >= (nodes[set_key]-1) * factor)
				])/len(events_set[set_key].keys())
				for set_key in sorted(events_set.keys())
			],
			color="C"+str(splits-1-index)
		)
		plts.append(p)

	plt.gca().set_xticklabels([k.split("/")[-1] for k in events_set.keys()], rotation=45)
	plt.ylabel("Average message coverage (%)")
	plt.xlabel("Configurations")
	plt.tight_layout()
	plt.legend([p[0] for p in plts],["%.2f%s nodes"%((100/splits)*(split+1),"%") for split in range(splits)],loc="lower right")
	plt.savefig("plots/coverage.pdf")
	plt.show()

if "coverage_top50" in requests or "all" in requests:
	plts = []
	for index in range(splits):
		factor = float(index+1)/float(splits)
		p = plt.bar(
			events_set.keys(),
			[
				100*len([
					e
					for e in (
						events_set[set_key]
					).values() 
					#if (len(e) < (nodes[set_key]-1) * (factor+0.1)) and (len(e) >= (nodes[set_key]-1) * factor)
					if (len(e) >= (nodes[set_key]-1) * factor)
				])/len(events_set[set_key].keys())
				for set_key in sorted(events_set.keys())
			],
			color="C"+str(splits-1-index)
		)
		plts.append(p)

	plt.gca().set_xticklabels([k.split("/")[-1] for k in events_set.keys()], rotation=45)
	plt.gca().set_ylim((50,100))
	plt.ylabel("Average message coverage (%)")
	plt.xlabel("Configurations")
	plt.tight_layout()
	plt.legend([p[0] for p in plts],["%.2f%s nodes"%((100/splits)*(split+1),"%") for split in range(splits)],loc="lower right")
	plt.savefig("plots/coverage_top50.pdf")
	plt.show()

if "coverage_top10" in requests or "all" in requests:
	plts = []
	for index in range(splits):
		factor = float(index+1)/float(splits)
		p = plt.bar(
			events_set.keys(),
			[
				100*len([
					e
					for e in (
						events_set[set_key]
					).values() 
					#if (len(e) < (nodes[set_key]-1) * (factor+0.1)) and (len(e) >= (nodes[set_key]-1) * factor)
					if (len(e) >= (nodes[set_key]-1) * factor)
				])/len(events_set[set_key].keys())
				for set_key in sorted(events_set.keys())
			],
			color="C"+str(splits-1-index)
		)
		plts.append(p)

	plt.gca().set_xticklabels([k.split("/")[-1] for k in events_set.keys()], rotation=45)
	plt.gca().set_ylim((90,100))
	plt.ylabel("Average message coverage (%)")
	plt.xlabel("Configurations")
	plt.tight_layout()
	plt.legend([p[0] for p in plts],["%.2f%s nodes"%((100/splits)*(split+1),"%") for split in range(splits)],loc="lower right")
	plt.savefig("plots/coverage_top10.pdf")
	plt.show()

if "event_delivery" in requests or "all" in requests:
	plts = []

	plt.plot(
		[
			np.mean([
				np.mean([
					len([
						rec 
						for rec in events_set[set_key][ek].keys()
						if rec in up[int(ek.split("_")[1])]
					])/float(len(up[int(ek.split("_")[1])]))
					for ek in [
						e
						for e in events_set[set_key].keys()
						if (int(e.split("_")[0]) == n)
					]
				])
				for n in range(nodes[set_key])
			])
			for set_key in sorted(events_set.keys())
		],
	)
	plt.gca().set_xticks(list(range(len(events_set.keys()))))
	plt.gca().set_xticklabels([k.split("/")[-1] for k in events_set.keys()], rotation=45)
	plt.gca().set_ylim((0.9,1.0))
	plt.ylabel("Average message reach per node (%)")
	plt.xlabel("Configurations")
	plt.tight_layout()
	plt.savefig("plots/event_delivery.pdf")
	plt.show()

if "membership_propagation" in requests or "all" in requests:
	for set_key in events_set.keys():
		for rs in range(resub_events):
			plt.plot([
				len([
					n
					for n in range(nodes[set_key])
					if resubbed[rs][1] in list(chain.from_iterable([
						nodeknown[n][resubbed[rs][0]+r_i]
						for r_i in range(r)
					]))
				])
				for r in range(maxdelay)	#rounds after the resub
			])
		plt.ylabel("Node reached by membership information")
		plt.xlabel("Rounds")
		plt.gca().set_xticks(range(maxdelay+1))
		plt.tight_layout()
		plt.savefig("plots/membership_propagation_"+set_key.split("/")[-1]+".pdf")
		plt.show()