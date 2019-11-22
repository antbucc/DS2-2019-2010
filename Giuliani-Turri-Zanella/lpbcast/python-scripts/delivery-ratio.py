import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
sns.set(color_codes=True)


generated_messages_per_round = 30
number_of_processes = 100;


def delivery_ratio(row):
    return (row["deliveries"]/number_of_processes)/generated_messages_per_round


def plot_delivery_ratio(optimized, not_optimized):
    opt_df = pd.read_csv("data/" + optimized)
    opt_df["Delivery-Ratio"] = opt_df.apply(lambda row: delivery_ratio(row), axis=1)
    opt_df["Round"] = opt_df.index
    opt_df["Optimization"] = 'Yes'
    not_opt_df = pd.read_csv("data/" + not_optimized)
    not_opt_df["Delivery-Ratio"] = not_opt_df.apply(lambda row: delivery_ratio(row), axis=1)
    not_opt_df["Round"] = not_opt_df.index
    not_opt_df["Optimization"] = 'No'

    df = pd.concat([opt_df, not_opt_df])
    sns.lmplot(x="Round", y="Delivery-Ratio", legend_out="true", legend="false", hue="Optimization", aspect=2, data=df)
    plt.show()
    plt.close()


if __name__ == "__main__":
    optimized = "optimized-deliveries.csv"
    not_optimized= "not-optimized-deliveries.csv"

    plot_delivery_ratio(optimized, not_optimized)
