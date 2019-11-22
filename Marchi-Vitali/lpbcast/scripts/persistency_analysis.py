import matplotlib.pyplot as plt
import matplotlib.mlab as mlab
import numpy as np
from scipy.stats import norm

MAX_EVENTS = 500
MAX_EVENT_DELAY = 80

log = open("LPBCast.log", "r")

# Discard the last line (could be incomplete/corrupted)
lines = log.readlines()
lines = lines[:-1]

persistency = [0] * MAX_EVENTS

for line in lines:
    if "Persistency" in line:
        line = line.split(";")
        if int(line[2]) < MAX_EVENTS:
            persistency[int(line[2])] = int(line[3])

mean = np.mean(persistency)
print(f"Mean: {mean}")
variance = np.var(persistency)
print(f"Variance: {variance}")

fig, ax = plt.subplots()

n, bins, patches = ax.hist(persistency, 50, normed=1)

pdf_x = np.linspace(np.min(persistency),np.max(persistency),100)
pdf_y = 1.0/np.sqrt(2*np.pi*variance)*np.exp(-0.5*(pdf_x-mean)**2/variance)
ax.plot(pdf_x,pdf_y,'k--')

plt.show()
