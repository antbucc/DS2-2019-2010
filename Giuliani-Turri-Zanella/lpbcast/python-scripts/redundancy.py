import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
sns.set(color_codes=True)


generated_messages_per_round = 30
number_of_processes = 100


def redundancy_per_process(row):
    return row["redundancies"]/number_of_processes


def plot_redundancy(opt_redundancies_file, not_opt_redundancies_file):
    opt_red_df = pd.read_csv("data/" + opt_redundancies_file)
    not_opt_red_df = pd.read_csv("data/" + not_opt_redundancies_file)
    opt_red_df["Redundancy Level"] = opt_red_df.apply(lambda row: redundancy_per_process(row), axis=1)
    opt_red_df["Round"] = opt_red_df.index
    opt_red_df["Optimization"] = "Yes"
    not_opt_red_df["Redundancy Level"] = not_opt_red_df.apply(lambda row: redundancy_per_process(row), axis=1)
    not_opt_red_df["Round"] = opt_red_df.index
    not_opt_red_df["Optimization"] = "No"

    df = pd.concat([not_opt_red_df, opt_red_df])

    g = sns.lmplot(x="Round", y="Redundancy Level", hue="Optimization",  data=df)
    g.fig.set_figheight(6)
    g.fig.set_figwidth(15)
    plt.tight_layout()
    plt.show()
    plt.close()


if __name__ == "__main__":
    opt_redundancies_file = "opt-redundancies.csv"
    not_opt_redundancies_file = "not-opt-redundancies.csv"

    plot_redundancy(opt_redundancies_file, not_opt_redundancies_file)

