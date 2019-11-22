import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

sns.set(color_codes=True)


def compute_message_propagation(file_name, df_name):
    message_propagation = pd.read_csv("data/" + file_name)

    # new data frame with split value columns
    message_propagation['message_propagation'] = message_propagation['message_propagation'].str[1:-1].str.split(",")

    event_ids = []

    for row in message_propagation.itertuples():
        for event_propagation in row.message_propagation:
            if event_propagation:
                event_id = event_propagation.split('=')[0]
                # propagation = event_propagation.split('=')[1]
                if event_id not in event_ids:
                    event_ids.append(str(event_id).strip())

    for event_id in event_ids:
        column_name = 'event_' + event_id
        message_propagation[column_name] = message_propagation['message_propagation'] \
            .apply(lambda row: get_message_propagation_by_id(row, event_id))

    message_propagation = message_propagation.apply(lambda x: pd.Series(x.dropna().values))
    message_propagation.to_pickle(df_name)


def get_message_propagation_by_id(message_propagation, event_id):
    message_propagation = str(message_propagation)
    event_id = str(event_id)

    if event_id in message_propagation:
        return message_propagation.split(event_id)[1].split("'")[0][1:]
    else:
        return None


def plot_average_message_propagation(output_names):
    labels = ['fanout=2', 'fanout=3', 'fanout=4']
    for i in range(0, len(output_names)):
        mp = pd.read_pickle(output_names[i])
        mp['average'] = mp.iloc[:, 2:].apply(pd.to_numeric).mean(numeric_only=True, axis=1)
        ax = sns.lineplot(x='tick', y='average', data=mp.head(60), label=labels[i])
        ax.legend()
        ax.set(xlabel='time (ticks)', ylabel='# nodes reached')

    plt.tight_layout()
    plt.show()


if __name__ == "__main__":

    input_names = ['MessagePropagation.2019.nov.20.10_09_29.fanout2.txt',
                   'MessagePropagation.2019.nov.20.09_44_32.fanout3.txt',
                   'MessagePropagation.2019.nov.20.10_10_02.fanout4.txt']
    output_names = ['fanout2.pkl', 'fanout3.pkl', 'fanout4.pkl']

    for i in range(0, len(input_names)):
        compute_message_propagation(input_names[i], output_names[i])

    plot_average_message_propagation(output_names)