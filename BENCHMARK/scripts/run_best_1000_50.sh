#!/bin/bash
#SBATCH --job-name=battleship_best_1000_50
#SBATCH --output=/globalscratch/jelechat/TAG/battleship_best_1000_50_%j.out
#SBATCH --error=/globalscratch/jelechat/TAG/battleship_best_1000_50_%j.err
#SBATCH --partition=long
#SBATCH --time=10-00:00:00
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=32G

module load releases/2024a
module load Java/17.0.6
TMPDIR=/scratch/$SLURM_JOB_ID
mkdir -p $TMPDIR

java -Xms28g -Xmx28g -XX:+UseG1GC \
  -cp /globalscratch/jelechat/TAG/TAG.jar \
  games.battleship_best.RunBattleshipTournament \
  seed=2000 \
  matchups=10000 \
  playerDirectory=/globalscratch/jelechat/TAG/agents_1000 \
  destDir=$TMPDIR/results \
  verbose=false

# Copie vers scratch global
mkdir -p /globalscratch/jelechat/TAG/results/best_1000_50
cp -r $TMPDIR/results/* /globalscratch/jelechat/TAG/results/best_1000_50/
rm -rf $TMPDIR
