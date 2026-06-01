import re
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
import os

"""
Head-to-Head win rate heatmap generator for TAG Battleship tournament results.

Parses tournament result files (.txt) produced by the TAG framework and generates
a heatmap visualizing pairwise win rates between agents, for each determinization
strategy evaluated in TFE26-093.

This script was developed with the assistance of Claude AI.
By Jérôme Lechat, UCLouvain, 2025-2026.

"""

# INPUTs
TXT_FILES = {
    "Random Det.": "results/basic_100/TournamentResults.txt",
    "Constraint-Based Det.": "results/smart_1000/TournamentResults.txt",
    "Heatmap-Guided Det.": "results/best_1000_100/TournamentResults.txt",
}

# SETTINGs
AGENT_ORDER = ["OSLA", "RMHC", "IS-MCTS", "MAST", "Random"]

# OUTPUTs
OUTPUT_DIR  = "figs"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# === FUNCTIONs ===

# Parser
def parse_winrate_matrix(filepath, agents):

    """
    Parses lines like:
    'IS-MCTS won 46.3% of the 1000 games against RMHC.'
    Returns a DataFrame where matrix[row][col] = win rate of row vs col (%).

    """

    import pandas as pd
    matrix = pd.DataFrame(np.nan, index=agents, columns=agents)

    with open(filepath, "r") as f:
        content = f.read()

    pattern = r"(\S[\S ]*?) won ([\d.]+)% of the \d+ games against ([\S ]+?)\."
    for match in re.finditer(pattern, content):
        agent_a = match.group(1).strip()
        winrate = float(match.group(2))
        agent_b = match.group(3).strip()
        if agent_a in agents and agent_b in agents:
            matrix.loc[agent_a, agent_b] = winrate

    return matrix


# Plot
def plot_heatmap(matrix, title, filename):
    annot = matrix.copy().astype(object)
    for r in matrix.index:
        for c in matrix.columns:
            v = matrix.loc[r, c]
            annot.loc[r, c] = "—" if np.isnan(float(v)) else f"{v:.0f}%"

    fig, ax = plt.subplots(figsize=(7, 5))
    sns.heatmap(
        matrix,
        annot=annot,
        fmt="",
        cmap="RdBu_r",
        vmin=0, vmax=100,
        linewidths=0.5,
        linecolor="white",
        cbar_kws={"label": "Win rate (%)"},
        ax=ax,
        mask=matrix.isna(),
    )
    ax.set_title(title, fontsize=13, pad=12)
    ax.set_xlabel("Opponent", fontsize=10)
    ax.set_ylabel("Agent (row wins against col)", fontsize=10)
    ax.tick_params(axis="both", labelsize=9)
    plt.tight_layout()
    path = os.path.join(OUTPUT_DIR, filename)
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"Saved: {path}")


# === %AIN ===
for strategy, txt_path in TXT_FILES.items():
    matrix = parse_winrate_matrix(txt_path, AGENT_ORDER)
    safe   = strategy.lower().replace(" ", "_").replace(".", "").replace("-", "")
    plot_heatmap(matrix, title=f"Head-to-head - {strategy}", filename=f"heatmap_{safe}.pdf")

print("Done.")
