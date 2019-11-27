import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os.path
from os import path
sns.set(color_codes=True)


def plot_subscription_propagation(subscription_propagation_file_names, df_file_name,
                                  subscription_propagation_optimized_file_names, df_optimized_file_name):
    # non-optimized
    if path.exists("data/" + df_file_name):
        subscription_propagation_df = pd.read_pickle("data/" + df_file_name)
    else:
        subscription_propagation_df = None
        for i in range(0, len(subscription_propagation_file_names)):
            current_file_name = subscription_propagation_file_names[i]
            if subscription_propagation_df is None:
                subscription_propagation_df = pd.read_csv("data/" + current_file_name)
                subscription_propagation_df.rename(columns={'subscriber_aware_processes': 'subscriber_aware_processes_'
                                                                                          + str(i)}, inplace=True)
            else:
                to_join = pd.read_csv("data/" + current_file_name)
                to_join.rename(columns={'subscriber_aware_processes': 'subscriber_aware_processes_' + str(i)},
                               inplace=True)
                subscription_propagation_df = subscription_propagation_df.join(to_join.set_index('tick'), on='tick')

        subscription_propagation_df['mean'] = subscription_propagation_df.iloc[:, 1:].mean(axis=1)
        subscription_propagation_df.to_pickle('data/' + df_file_name)

    # optimized
    if path.exists("data/" + df_optimized_file_name):
        subscription_propagation_optimized_df = pd.read_pickle("data/" + df_optimized_file_name)
    else:
        subscription_propagation_optimized_df = None
        for i in range(0, len(subscription_propagation_optimized_file_names)):
            current_file_name = subscription_propagation_optimized_file_names[i]
            if subscription_propagation_optimized_df is None:
                subscription_propagation_optimized_df = pd.read_csv("data/" + current_file_name)
                subscription_propagation_optimized_df.rename(columns={'subscriber_aware_processes': 'subscriber_aware_processes_' + str(i)},
                                                             inplace=True)
            else:
                to_join = pd.read_csv("data/" + current_file_name)
                to_join.rename(columns={'subscriber_aware_processes': 'subscriber_aware_processes_' + str(i)}, inplace=True)
                subscription_propagation_optimized_df = subscription_propagation_optimized_df.join(to_join.set_index('tick'),
                                                                                                   on='tick')

        subscription_propagation_optimized_df['optimized_mean'] = subscription_propagation_optimized_df.iloc[:, 1:].mean(axis=1)
        subscription_propagation_optimized_df.to_pickle('data/' + df_optimized_file_name)

    # join
    subscription_propagation_join = subscription_propagation_df[['tick', 'mean']].set_index('tick')\
        .join(subscription_propagation_optimized_df[['tick', 'optimized_mean']].set_index('tick'))
    subscription_propagation_join['difference'] = subscription_propagation_join['mean'] - \
                                                  subscription_propagation_join['optimized_mean']

    #sns.regplot(x='tick', y='difference', data=subscription_propagation_join.reset_index())
    #sns.distplot(subscription_propagation_join['mean'])
    #sns.distplot(subscription_propagation_join['optimized_mean'])
    sns.lineplot(x='tick', y='mean', data=subscription_propagation_join.reset_index())
    sns.lineplot(x='tick', y='optimized_mean', data=subscription_propagation_join.reset_index())
    plt.legend(labels=['non optimized', 'optimized'])
    plt.tight_layout()
    plt.show()
    plt.close()


if __name__ == "__main__":
    subscription_propagation_file_names = ['subscription_not_optimized_0.txt']
    subscription_propagation_df_file_name = 'subscription_propagation.pkl'
    subscription_propagation_optimized_file_names = ['subscription_optimized_0.txt']
    subscription_propagation_optimized_df_file_name = 'subscription_propagation_optimized.pkl'

    plot_subscription_propagation(subscription_propagation_file_names, subscription_propagation_df_file_name,
                                  subscription_propagation_optimized_file_names,
                                  subscription_propagation_optimized_df_file_name)
