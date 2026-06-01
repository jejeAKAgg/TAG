import pandas as pd
import matplotlib.pyplot as plt
import os

"""
Drama score distribution plotter for TAG Battleship tournament results.

Reads CSV/TSV result files and plots the distribution of Drama_HPDifference
per determinization strategy, aggregated across all matchups.

This script was developed with the assistance of Claude AI.
By Jérôme Lechat, UCLouvain, 2025-2026.

"""

# INPUTs
CSV_FILES = {
    "Random Det.": "results/basic_100/BASIC_Battleship_SUMMARY.csv",
    "Constraint-Based Det.": "results/smart_1000/SMART_Battleship_SUMMARY.csv",
    "Heatmap-Guided Det.": "results/best_1000_100/BEST_Battleship_SUMMARY.csv",
}

# SETTINGs
COLORS = {
    "Random Det.": "gray",
    "Constraint-Based Det.": "steelblue",
    "Heatmap-Guided Det.": "darkorange",
}

# OUTPUTs
OUTPUT_DIR = "figs"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# === FUNCTIONs ===

# Loader & Plotter
fig, ax = plt.subplots(figsize=(9, 5))

for strategy, filepath in CSV_FILES.items():
    df = pd.read_csv(filepath, sep="\t")
    counts = (
        df["Drama_HPDifference"]
        .value_counts(normalize=True)
        .sort_index()
        .mul(100)
    )
    ax.plot(
        counts.index,
        counts.values,
        marker="o",
        label=strategy,
        color=COLORS[strategy],
        linewidth=1.8,
        markersize=5,
    )

ax.set_xlabel("HP difference at game end", fontsize=11)
ax.set_ylabel("Frequency (%)", fontsize=11)
ax.set_title("Game outcome margin distribution", fontsize=13)
ax.legend(fontsize=10)
ax.set_xticks(range(1, 18))
ax.grid(axis="y", linestyle="--", alpha=0.4)

plt.tight_layout()
path = os.path.join(OUTPUT_DIR, "drama_distribution.pdf")
fig.savefig(path, dpi=150, bbox_inches="tight")
plt.close(fig)
print(f"Saved: {path}")
