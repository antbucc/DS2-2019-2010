import csv
import matplotlib.pyplot as plt
import matplotlib.mlab as mlab
import matplotlib.ticker as ticker
import numpy as np
from scipy.stats import norm

import pandas as pd


data = pd.read_csv('delivery_rates.csv')
print(data)

ax = plt.gca()

data.transpose().plot()

plt.gca().legend(('y0','y1', 'y2'))

plt.show()
