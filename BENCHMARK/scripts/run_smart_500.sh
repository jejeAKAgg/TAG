#!/bin/bash
#SBATCH --job-name=battleship_smart_500
#SBATCH --output=/globalscratch/jelechat/TAG/battleship_smart_500_%j.out
#SBATCH --error=/globalscratch/jelechat/TAG/battleship_smart_500_%j.err
#SBATCH --time=5-00:00:00
#SBATCH --ntasks=1
#SBATCH --cpus-per-task=1
#SBATCH --mem=32G

module load releases/2024a
module load Java/17.0.6
\nTMPDIR=/scratch/$SLURM_JOB_ID
mkdir -p $TMPDIR

java -Xms28g -Xmx28g -XX:+UseG1GC \
  -cp /globalscratch/jelechat/TAG/TAG.jar \
  games.battleship_smart.RunBattleshipTournament \
  seed=2000 \
  matchups=10000 \
  playerDirectory=/globalscratch/jelechat/TAG/agents_500 \
  destDir=$TMPDIR/results \
  verbose=false


# Copie vers scratch global
mkdir -p /globalscratch/jelechat/TAG/results/smart_500
cp -r $TMPDIR/results/* /globalscratch/jelechat/TAG/results/smart_500/
rm -rf $TMPDIR
