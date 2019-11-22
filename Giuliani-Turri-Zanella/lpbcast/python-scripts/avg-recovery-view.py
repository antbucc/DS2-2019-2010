import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

sns.set(color_codes=True)


def compute_fanout_recoveries(file):
    input_csv = pd.read_csv(file)

    # new data frame with split value columns

    input_csv['avg_recovery_requests'] = input_csv['avg_recovery_requests'].str.split(", ")
    input_csv['avg'] = input_csv.apply(lambda row: row['avg_recovery_requests'][0], axis=1);
    input_csv['view'] = input_csv.apply(lambda row: row['avg_recovery_requests'][2], axis=1);

    input_csv['avg'] = input_csv['avg'].str[13:]
    input_csv['view'] = input_csv['view'].str[5:]

    input_csv['avg'] = input_csv.apply(lambda row: float(row['avg']), axis=1);
    input_csv['view'] = input_csv.apply(lambda row: int(row['view']), axis=1);

    print(input_csv)

    ax = sns.barplot(x='view', y='avg', data=input_csv, palette=sns.color_palette(["#34495e"]))
    ax.set(xlabel='View Size', ylabel='Average Recovery Requests')


    plt.tight_layout()
    plt.show()


if __name__ == "__main__":

    input = "data/MessageRecoveryView.2019.nov.20.15_40_07.txt"

    compute_fanout_recoveries(input)

    #plot_average_message_propagation(output_names)