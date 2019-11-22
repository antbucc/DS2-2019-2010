import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

sns.set(color_codes=True)


def compute_fanout_recoveries(file, df):
    input_csv = pd.read_csv(file)

    # new data frame with split value columns

    input_csv['avg_recovery_requests'] = input_csv['avg_recovery_requests'].str.split(", ")
    input_csv['avg'] = input_csv.apply(lambda row: row['avg_recovery_requests'][0], axis=1);
    input_csv['fanout'] = input_csv.apply(lambda row: row['avg_recovery_requests'][1], axis=1);

    input_csv['avg'] = input_csv['avg'].str[13:]
    input_csv['fanout'] = input_csv['fanout'].str[7:]

    input_csv['avg'] = input_csv.apply(lambda row: float(row['avg']), axis=1);
    input_csv['fanout'] = input_csv.apply(lambda row: int(row['fanout']), axis=1);

    print(input_csv)

    ax = sns.barplot(x='fanout', y='avg', data=input_csv)
    ax.set(xlabel='Fanout Size', ylabel='Average Recovery Requests')

    plt.tight_layout()
    plt.show()


if __name__ == "__main__":

    input = "data/MessageRecovery.2019.nov.20.13_40_07.txt"
    output = "avg.pkl"

    compute_fanout_recoveries(input, output)

    #plot_average_message_propagation(output_names)