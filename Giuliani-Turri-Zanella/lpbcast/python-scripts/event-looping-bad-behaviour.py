import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
sns.set(color_codes=True)


def compute_message_propagation(file_name, df_name):
    message_propagation = pd.read_csv(file_name)

    # new data frame with split value columns
    message_propagation['message_propagation'] = message_propagation['message_propagation'].str[1:-1].str.split(",")

    event_ids = []

    # save all event ids found
    for row in message_propagation.itertuples():
        for event_propagation in row.message_propagation:
            if event_propagation:
                event_id = event_propagation.split('=')[0]
                #propagation = event_propagation.split('=')[1]
                if event_id not in event_ids:
                    event_ids.append(str(event_id).strip())

    # create column in dataset for each event
    for event_id in event_ids:
        column_name = 'event_' + event_id
        message_propagation[column_name] = message_propagation['message_propagation']\
            .apply(lambda row: get_message_propagation_by_id(row, event_id))

    message_propagation = message_propagation.apply(lambda x : pd.Series(x.dropna().values))
    message_propagation.to_pickle(df_name)


def get_message_propagation_by_id(message_propagation, event_id):
    message_propagation = str(message_propagation)
    event_id = str(event_id)

    if event_id in message_propagation:
        return int(message_propagation.split(event_id)[1].split("'")[0][1:])
    else:
        return 0


def plot_event_looping_behaviour(df_file_name):
    dataset = pd.read_pickle(df_file_name)
    print(dataset.iloc[:20, :2])

    nCol = len(dataset.columns)
    #skip first 2 columns as they do not contain events

    for i in range(2, nCol):
        ax = sns.lineplot(x='tick', y=dataset.columns[i], data=dataset)

    ax.set(xlabel='Tick', ylabel='#Delivery of Event')
    ax.hlines(y=100, color='black', linewidth=1, alpha=.7, xmin = 0, xmax = 200, ls='--')
    plt.tight_layout()
    plt.show()

if __name__ == "__main__":

    input_name = 'data/ModelEventLoopingBadBehaviour.2019.nov.21.10_00_24.txt'
    temp_name = 'event_looping_far.pkl'

    compute_message_propagation(input_name, temp_name)  #can take a long time to execute, run it only once
                                                        #then decomment the line below and comment this one in order to obtain plot efficiently
    plot_event_looping_behaviour(temp_name)