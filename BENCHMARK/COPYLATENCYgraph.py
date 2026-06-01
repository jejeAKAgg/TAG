import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import os

"""
Copy latency and simulation count comparison between Constraint-Based
and Heatmap-Guided determinization strategies (10, 50, 100 iterations).

This script was developed with the assistance of Claude AI.
By Jérôme Lechat, UCLouvain, 2025-2026.

"""

# INPUTs
CSV_FILES = {
    "Constraint-Based": "results/smart_1000/SMART_Battleship_SUMMARY.csv",
    "Heatmap (10 iter)": "results/best_1000_10/BEST_Battleship_SUMMARY.csv",
    "Heatmap (50 iter)": "results/best_1000_50/BEST_Battleship_SUMMARY.csv",
    "Heatmap (100 iter)": "results/best_1000_100/BEST_Battleship_SUMMARY.csv",
}

# SETTINGs
COLORS = {
    "Constraint-Based": "#1f77b4",
    "Heatmap (10 iter)": "#ffbb78",
    "Heatmap (50 iter)": "#ff7f0e",
    "Heatmap (100 iter)": "#d62728",
}
AGENT_ORDER = ["OSLA", "RMHC", "IS-MCTS", "MAST", "Random"]

# OUTPUTs
OUTPUT_DIR = "figs"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# === FUNCTIONs ===

# Loader
dfs = {}
for strategy, filepath in CSV_FILES.items():
    df = pd.read_csv(filepath, sep="\t")
    df = df[df["P0_Agent"].isin(AGENT_ORDER)]
    dfs[strategy] = df

# Plot Helper
def grouped_bar(dfs, metric, ylabel, title, filename, scale=1, unit=""):
    n_agents = len(AGENT_ORDER)
    n_strat = len(dfs)
    x = np.arange(n_agents)
    width = 0.18

    fig, ax = plt.subplots(figsize=(11, 5))

    for i, (strategy, df) in enumerate(dfs.items()):
        means = [df[df["P0_Agent"] == a][metric].mean() / scale
                  for a in AGENT_ORDER]
        offset = (i - n_strat / 2 + 0.5) * width
        bars = ax.bar(x + offset, means, width=width,
                        label=strategy, color=COLORS[strategy],
                        edgecolor="white")

        for bar, val in zip(bars, means):
            ax.text(
                bar.get_x() + bar.get_width() / 2,
                bar.get_height() * 1.02,
                f"{val:.0f}{unit}",
                ha="center", va="bottom", fontsize=7
            )

    ax.set_xticks(x)
    ax.set_xticklabels(AGENT_ORDER, fontsize=10)
    ax.set_ylabel(ylabel, fontsize=11)
    ax.set_title(title, fontsize=12)
    ax.legend(fontsize=9, loc="upper right")
    ax.grid(axis="y", linestyle="--", alpha=0.3)
    plt.tight_layout()

    path = os.path.join(OUTPUT_DIR, filename)
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"Saved: {path}")


# Copy Latency Plots
grouped_bar(
    dfs,
    metric = "P0_AvgTimeCopyNS",
    ylabel = "Avg copy latency (µs)",
    title = "Average copy latency per agent\nConstraint-Based vs Heatmap-Guided (10/50/100 iter)",
    filename = "copy_latency.pdf",
    scale = 1e3,
    unit = "µs",
)

# FM Calls Plots
grouped_bar(
    dfs,
    metric = "P0_TotalFMCalls",
    ylabel = "Avg total FM calls per game",
    title = "Simulation count per agent\nConstraint-Based vs Heatmap-Guided (10/50/100 iter)",
    filename = "fm_calls.pdf",
    scale = 1,
    unit = "",
)

# === SUMMARY ===
print("\n Summary")
print(f"{'Agent':<12} {'Strategy':<22} {'AvgCopyµs':>10} {'AvgFMCalls':>12}")
print("-" * 60)
for strategy, df in dfs.items():
    for agent in AGENT_ORDER:
        sub = df[df["P0_Agent"] == agent]
        if sub.empty:
            continue
        avg_copy = sub["P0_AvgTimeCopyNS"].mean() / 1e3
        avg_fm = sub["P0_TotalFMCalls"].mean()
        print(f"{agent:<12} {strategy:<22} {avg_copy:>10.1f} {avg_fm:>12.0f}")
print()

print("Done.")
