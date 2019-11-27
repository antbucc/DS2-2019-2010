import ast
import csv
import os.path

MAX_EVENTS = 500
MAX_EVENT_DELAY = 80

log = open("LPBCast.log", "r")

# Discard the last line (could be incomplete/corrupted)
lines = log.readlines()
lines = lines[:-1]

delivery_rates = [0] * MAX_EVENT_DELAY
event_count = 0

for event_id in range(MAX_EVENTS):
    for index, line in enumerate(lines):
        line = line.split(";")
        if line[1] == "NewEvent" and int(line[2]) == event_id:
            event_count += 1
            activeNodes = ast.literal_eval(line[4])
            activeNodes.remove(int(line[3]))  # Remove the source of the event
            activeNodesSize = len(activeNodes)
            delays = []
            tick_event = int(float(line[0]))

            event_deliveries = [0] * MAX_EVENT_DELAY

            for line2 in lines[index+1:]:
                line2 = line2.split(";")

                delta = int(float(line2[0])) - tick_event
                if delta >= MAX_EVENT_DELAY:
                    break

                if (line2[1] == "EventDelivery" and int(line2[2]) == event_id
                        and int(line2[3]) in activeNodes):
                    activeNodes.remove(int(line2[3]))
                    delays.append(float(line2[0]) - float(line[0]))
                    event_deliveries[delta] += 1
            
            for i in range(1, MAX_EVENT_DELAY):
                event_deliveries[i] += event_deliveries[i-1]

            for i in range(MAX_EVENT_DELAY):
                event_deliveries[i] = event_deliveries[i] / activeNodesSize
            
            print(f"EVENT {event_id} ({activeNodesSize})")
            print(event_deliveries)

            delivery_rates = [sum(x) for x in zip(delivery_rates, event_deliveries)]

# initialize the file if it does not exists 
if not os.path.isfile('delivery_rates.csv'):
    with open('delivery_rates.csv','a') as fd:
        writer = csv.writer(fd)
        writer.writerow(range(MAX_EVENT_DELAY))

with open('delivery_rates.csv','a') as fd:
    writer = csv.writer(fd)
    writer.writerow(delivery_rates)

print(delivery_rates)
print(event_count)
delivery_rates = [deliveries/event_count for deliveries in delivery_rates]
print(delivery_rates)
