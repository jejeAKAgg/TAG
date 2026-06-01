import re
import numpy as np
import matplotlib.pyplot as plt
import os

"""
Win rate bar plot generator for TAG Battleship tournament results.

Produces two plots:
  1. One bar chart per determinization strategy (individual)
  2. One grouped bar chart combining all three strategies

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
STRATEGY_COLORS = {
    "Random Det.": "#7f7f7f",
    "Constraint-Based Det.": "#1f77b4",
    "Heatmap-Guided Det.": "#ff7f0e",
}

# OUTPUTs
OUTPUT_DIR = "figs"
os.makedirs(OUTPUT_DIR, exist_ok=True)

# === FUNCTIONs ===

# Parser
def parse_winrates(filepath, agents):
    winrates = {}
    with open(filepath, "r") as f:
        for line in f:
            line = line.strip()
            match = re.search(
                r"(\w[\w-]*) won ([\d.]+)% of the \d+ games it played during the tournament",
                line
            )
            if match:
                agent = match.group(1).strip()
                winrate = float(match.group(2))
                if agent in agents:
                    winrates[agent] = winrate
    return winrates


# First Plot
def plot_individual(all_data):
    colors = ["#1f77b4", "#ff7f0e", "#2ca02c", "#9467bd", "#7f7f7f"]

    for strategy, winrates in all_data.items():
        fig, ax = plt.subplots(figsize=(7, 4))
        agents = [a for a in AGENT_ORDER if a in winrates]
        values = [winrates[a] for a in agents]

        bars = ax.bar(agents, values, color=colors[:len(agents)],
                      edgecolor="white", width=0.6)

        for bar, val in zip(bars, values):
            ax.text(
                bar.get_x() + bar.get_width() / 2,
                bar.get_height() + 1,
                f"{val:.0f}%",
                ha="center", va="bottom", fontsize=9
            )

        ax.set_ylim(0, 110)
        ax.set_ylabel("Win rate (%)", fontsize=11)
        ax.set_title(f"Win rates — {strategy}", fontsize=12)
        ax.axhline(50, color="black", linestyle="--", linewidth=0.8, alpha=0.5)
        ax.grid(axis="y", linestyle="--", alpha=0.3)
        plt.tight_layout()

        safe = strategy.lower().replace(" ", "_").replace(".", "").replace("-", "")
        path = os.path.join(OUTPUT_DIR, f"winrate_{safe}.pdf")
        fig.savefig(path, dpi=150, bbox_inches="tight")
        plt.close(fig)
        print(f"Saved: {path}")


# Second Plot
def plot_combined(all_data):
    strategies = list(all_data.keys())
    agents = AGENT_ORDER
    n_agents = len(agents)
    n_strat = len(strategies)

    x = np.arange(n_agents)
    width = 0.25

    fig, ax = plt.subplots(figsize=(10, 5))

    for i, strategy in enumerate(strategies):
        winrates = all_data[strategy]
        values = [winrates.get(a, 0) for a in agents]
        offset = (i - n_strat / 2 + 0.5) * width

        bars = ax.bar(
            x + offset, values,
            width=width,
            label=strategy,
            color=list(STRATEGY_COLORS.values())[i],
            edgecolor="white",
        )

        for bar, val in zip(bars, values):
            ax.text(
                bar.get_x() + bar.get_width() / 2,
                bar.get_height() + 0.8,
                f"{val:.0f}%",
                ha="center", va="bottom", fontsize=7
            )

    ax.set_xticks(x)
    ax.set_xticklabels(agents, fontsize=10)
    ax.set_ylim(0, 110)
    ax.set_ylabel("Win rate (%)", fontsize=11)
    ax.set_title("Win rates per agent - all determinization strategies", fontsize=12)
    ax.axhline(50, color="black", linestyle="--", linewidth=0.8, alpha=0.5)
    ax.legend(fontsize=9, loc="upper right")
    ax.grid(axis="y", linestyle="--", alpha=0.3)
    plt.tight_layout()

    path = os.path.join(OUTPUT_DIR, "winrate_combined.pdf")
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)
    print(f"Saved: {path}")


# === MAIN ===
all_data = {}
for strategy, filepath in TXT_FILES.items():
    all_data[strategy] = parse_winrates(filepath, AGENT_ORDER)
    print(f"{strategy}: {all_data[strategy]}")

plot_individual(all_data)
plot_combined(all_data)

print("Done.")
