import pandas as pd
import matplotlib.pyplot as plt

def read_csv(filename):
    return pd.read_csv('data/' + filename)

naive_df_go = read_csv("naive_go.csv")
line_df_go = read_csv("line_go.csv")

naive_df = read_csv("naive.csv")

line_df = read_csv("others/line_data.csv")
line_df_compare_block = read_csv("line_4096_10240.csv")
block_df = read_csv("block.csv")

complete_line_df = pd.concat(
    [
        line_df,
        line_df_compare_block,
    ],
    ignore_index=True,
)

line_df_multicore_inner = read_csv("others/multicore_inner.csv")
line_df_multicore_outer = read_csv("others/multicore_outer.csv")

# matrix multiplication: 2 * n ^ 3 floating points operations
flops_func = lambda df: 2 * df["matrix_size"] ** 3 / df["time"] / 1e6
for df in [complete_line_df, line_df_multicore_inner, line_df_multicore_outer]:
    df["flops"] = flops_func(df)

speedup_func = lambda line_df: lambda df: line_df["time"] / df["time"]
efficiency_func = lambda df: df["speedup"] / df["num_threads"]

for df in [line_df_multicore_inner, line_df_multicore_outer]:
    df["speedup"] = speedup_func(complete_line_df)(df)
    df["efficiency"] = efficiency_func(df)


def plot_line(data, label, y="time"):
    plt.plot(data["matrix_size"], data[y], label=label, marker="o")


def finish_plot(title, ylabel="Time (s)"):
    plt.xlabel("Matrix Size")
    plt.ylabel(ylabel)
    plt.title(title)

    plt.legend()

    plt.grid(True)
    plt.savefig(f"{title}.png")
    plt.clf()


def plot_line_vs_block(y="time", ylabel="Time (s)"):
    for size in block_df["block_size"].unique():
        data_subset = block_df[block_df["block_size"] == size]
        plot_line(data_subset, f"Block size: {size}", y=y)

    plot_line(
        line_df_compare_block, label="Line", y=y
    )  # create a line for the line multiplication algorithm
    finish_plot(f"Block vs Line Matrix Multiplication ({y})", ylabel=ylabel)


def plot_naive_vs_line(y="time", ylabel="Time (s)"):
    plot_line(naive_df, label="Naive", y=y)
    plot_line(line_df, label="Line", y=y)

    finish_plot(f"Naive vs Line Matrix Multiplication ({y})", ylabel=ylabel)


def plot_go_vs_cpp():
    plot_line(naive_df, label="Naive C++")
    plot_line(naive_df_go, label="Naive Go")
    plot_line(line_df, label="Line C++")
    plot_line(line_df_go, label="Line Go")

    finish_plot("Go vs C++ Matrix Multiplication")


def plot_naive_go_vs_cpp():
    plot_line(naive_df, label="Naive C++")
    plot_line(naive_df_go, label="Naive Go")

    finish_plot("Naive Matrix Multiplication: Go vs C++")


def plot_line_go_vs_cpp():
    plot_line(line_df, label="Line C++")
    plot_line(line_df_go, label="Line Go")

    finish_plot("Line Matrix Multiplication: Go vs C++")


def plot_multicore_flops():
    plot_line(line_df_multicore_outer, label="Line Multicore Outer", y="flops")
    plot_line(line_df_multicore_inner, label="Line Multicore Inner", y="flops")
    plot_line(complete_line_df, label="Line Single Core", y="flops")
    finish_plot("Line Matrix Multiplication: Multicore MFLOPS", ylabel="MFLOPS")


def plot_multicore_time():
    plot_line(line_df_multicore_outer, label="Line Multicore Outer")
    plot_line(line_df_multicore_inner, label="Line Multicore Inner")
    plot_line(complete_line_df, label="Line Single Core")
    finish_plot("Line Matrix Multiplication: Multicore Time")


def plot_multicore_speedup():
    plot_line(line_df_multicore_outer, label="Line Multicore Outer", y="speedup")
    plot_line(line_df_multicore_inner, label="Line Multicore Inner", y="speedup")
    finish_plot("Line Matrix Multiplication: Multicore Speedup", ylabel="Speedup")


def plot_multicore_efficiency():
    plot_line(line_df_multicore_outer, label="Line Multicore Outer", y="efficiency")
    plot_line(line_df_multicore_inner, label="Line Multicore Inner", y="efficiency")
    finish_plot("Line Matrix Multiplication: Multicore Efficiency", ylabel="Efficiency")


for variable, label in [("time", "Time (s)"), ("L1_DCM", "Number of L1 DCM"), ("L2_DCM", "Number of L2 DCM")]:
    plot_line_vs_block(variable, label)
    plot_naive_vs_line(variable, label)


plot_naive_go_vs_cpp()
plot_line_go_vs_cpp()
plot_go_vs_cpp()

plot_multicore_flops()
plot_multicore_time()
plot_multicore_speedup()
plot_multicore_efficiency()

