import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import os.path
from os import path
sns.set(color_codes=True)

def plot_view_distribution(file_name, num_of_processes, df_name):
    if path.exists("data/" + df_name):
        view_distribution_df = pd.read_pickle("data/" + df_name)
    else:
        view_distribution_df = pd.read_csv("data/" + file_name)

        for i in range(0, num_of_processes):
            column_name = 'process_' + str(i)
            view_distribution_df[column_name] = view_distribution_df['view_distribution'] \
                .apply(lambda row: int(str(row)[1:-1].split(',')[i]))
        #    n_bins = max(view_distribution[column_name])
        #    sns.distplot(view_distribution[column_name], kde=False, bins=n_bins)
        #    fig_name = "figures/process_" + str(i) + ".png"
        #    plt.tight_layout()
        #    plt.savefig(fig_name)
        #    plt.close()

        view_distribution_df.to_pickle("data/" + df_name)

    sns.distplot(view_distribution_df.iloc[:,2:].mean())
    plt.tight_layout()
    plt.show()
    plt.savefig('figures/mean_view_distribution.png')
    plt.close()


if __name__ == "__main__":
    num_of_processes = 100
    view_distribution_file_name = 'ModelOutput.2019.nov.20.11_27_25.txt'
    view_distribution_df_file_name = 'view_distribution.pkl'

    plot_view_distribution(view_distribution_file_name, num_of_processes, view_distribution_df_file_name)