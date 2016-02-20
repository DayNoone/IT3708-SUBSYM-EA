import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class EA implements EvolutionaryCycle{

    int generationNumber;
    Boolean running;
    ArrayList<Hypothesis> population;
    ArrayList<Hypothesis> adults = new ArrayList<Hypothesis>();
    ArrayList<Hypothesis> parents = new ArrayList<Hypothesis>();

    Boolean solutionFound = false;

    private Random random = new Random();

    public EA(ArrayList<Hypothesis> initialGeneration){
        population = initialGeneration;
        running = true;

        generationNumber = 0;
    }
    public void stopCycle() {
        running = false;
    }
    public void restartCycle(ArrayList<Hypothesis> initialGeneration) {
        // Could be replaced by just creating new EA
        population = initialGeneration;
        generationNumber = 0;
        running = true;
    }

    public void iteration() {
        development();
        Hypothesis fittest = getFittest(population);
        if (fittest.getFitness() == 1){
            solutionFound = true;
            setRunning(false);
        } else {
            adultSelection();
            parentSelection();
            reproduction();
        }
        generationNumber++;
    }

    @Override
    public void development() {
        for(Hypothesis hyp: population) {
            hyp.development();
            hyp.calculateFitness();
        }

    }

    @Override
    public void adultSelection() {
        ArrayList<Hypothesis> tempPopulation = new ArrayList<Hypothesis>(population);
        double totalFitness;
        switch (Constants.ADULT_SELECTION){
            case FULL_GENERATION:
                adults.clear();
                adults.addAll(population);
                break;
            case OVER_PRODUCTION:
                adults.clear();
                while(adults.size() <= Constants.ADULTS_SIZE) {
                    Hypothesis hyp = fitnessRoulette(tempPopulation);
                    adults.add(hyp);
                    tempPopulation.remove(hyp);
                }
                break;
            case GENERATIONAL_MIXING:
                ArrayList<Hypothesis> selectionPopulation = new ArrayList<Hypothesis>();
                selectionPopulation.addAll(population);
                selectionPopulation.addAll(adults);
                adults.clear();
                while(adults.size() <= Constants.ADULTS_SIZE) {
                    Hypothesis hyp = fitnessRoulette(selectionPopulation);
                    adults.add(hyp);
                    tempPopulation.remove(hyp);
                }
                break;
        }
    }

    private Hypothesis fitnessRoulette(ArrayList<Hypothesis> population) {
        double totalFitness = getTotalFitness(population);
        double x = random.nextDouble() * totalFitness;
        for (Hypothesis hypothesis : population) {
            x -= hypothesis.getFitness();
            if (x <= 0) {
                return hypothesis;
            }
        }
        throw new NullPointerException("No blank piece found!");
    }

    @Override
    public void parentSelection() {
        parents.clear();
        Hypothesis parent1;
        Hypothesis parent2;
        ArrayList<Hypothesis> tempAdults = new ArrayList<>();
        tempAdults.addAll(adults);
        switch (Constants.PARENT_SELECTION){
            case FITNESS_PROPORTIONATE:
                while(parents.size() < Constants.PARENTS_SIZE) {
                    parent1 = fitnessRoulette(adults);
                    adults.remove(parent1);
                    parent2 = fitnessRoulette(adults);
                    adults.add(parent1);
                    parents.add(parent1);
                    parents.add(parent2);
                }
                break;
            case SIGMA_SCALING:
                while(parents.size() < Constants.PARENTS_SIZE) {
                    parent1 = sigmaRoulette(tempAdults);
                    tempAdults.remove(parent1);
                    parent2 = sigmaRoulette(tempAdults);
                    tempAdults.add(parent1);
                    parents.add(parent1);
                    parents.add(parent2);
                }
                break;
            case TOURNAMENT_SELECTION:
                while(parents.size() < Constants.PARENTS_SIZE) {
                    tempAdults.clear();
                    tempAdults.addAll(adults);
                    ArrayList<Hypothesis> tournamentGroup = new ArrayList<Hypothesis>();
                    for (int i = 0; i < Constants.TOURNAMENT_GROUP_SIZE; i++){
                        Hypothesis chosen = adults.get(random.nextInt(adults.size()));
                        tournamentGroup.add(chosen);
                        tempAdults.remove(chosen);
                    }
                    tempAdults.addAll(tournamentGroup);
                    parents.add(findTournamentWinner(tournamentGroup));
                    parents.add(findTournamentWinner(tournamentGroup));
                }
                break;
            case UNIFORM_SELECTION:
                while(parents.size() < Constants.PARENTS_SIZE) {
                    parent1 = adults.get(random.nextInt(adults.size()));
                    adults.remove(parent1);
                    parent2 = adults.get(random.nextInt(adults.size()));
                    adults.add(parent1);
                    parents.add(parent1);
                    parents.add(parent2);
                }
                break;
        }
    }

    private Hypothesis findTournamentWinner(ArrayList<Hypothesis> tournamentGroup) {
        if(random.nextDouble() < Constants.TOURNAMENT_PROBABILITY) {
            return getFittest(tournamentGroup);
        } else {
            return tournamentGroup.get(random.nextInt(tournamentGroup.size()));
        }
    }

    public Hypothesis getFittest(ArrayList<Hypothesis> population) {
        Hypothesis bestHypothesis = population.get(0);
        for (Hypothesis hypothesis: population){
            if (hypothesis.getFitness() > bestHypothesis.getFitness()){
                bestHypothesis = hypothesis;
            }
        }
        return bestHypothesis;
    }

    private Hypothesis sigmaRoulette(ArrayList<Hypothesis> population) {
        double averageFitness = getAverageFitness(population);
        double standardDeviation = getStandardDeviation(population, averageFitness);
        for (Hypothesis hypothesis: population){
            hypothesis.calculateSigma(averageFitness, standardDeviation);
        }
        double totalSigma = getTotalSigma(population);
        double x = random.nextDouble() * totalSigma;
        for (Hypothesis hypothesis : population) {
            x -= hypothesis.getSigma();
            if (x <= 0) {
                return hypothesis;
            }
        }
        throw new NullPointerException("Sigma roulette not returning hypothesis");
    }

    public double getAverageFitness(ArrayList<Hypothesis> population) {
        return getTotalFitness(population) / population.size();
    }


    @Override
    public void reproduction() {
        // Elitism
        ArrayList<Hypothesis> fittestGroup = new ArrayList<Hypothesis>();
        Hypothesis fittest;
        for (int i = 0; i < Constants.ELITISM_SIZE; i++) {
            fittest = getFittest(population);
            fittestGroup.add(fittest);
            population.remove(fittest);
        }
        population.clear();
        population.addAll(fittestGroup);



        if (Constants.CROSSOVER) {
            for (int i = 0; i < parents.size(); i = i + 2) {
                if (random.nextDouble() < Constants.CROSSOVER_RATE) {
                    Hypothesis parent1 = parents.get(i);
                    Hypothesis parent2 = parents.get(i+1);
                    population.addAll(parent1.crossover(parent2));
                } else {
                    population.add(parents.get(i));
                    population.add(parents.get(i + 1));
                }
            }
        }

        if (Constants.MUTATION){
            for(Hypothesis child: population) {
                // Fyller opp population med muterte parents. Flere runder.
                child.mutate();
            }
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean getSolution() {
        return solutionFound;
    }

    public double getTotalFitness(ArrayList<Hypothesis> population) {
        double total = 0;
        for (Hypothesis hypothesis: population){
            total += hypothesis.getFitness();
        }
        return total;
    }
    private double getTotalSigma(ArrayList<Hypothesis> population) {
        double total = 0;
        for (Hypothesis hypothesis: population){
            total += hypothesis.getSigma();
        }
        return total;
    }
    public double getStandardDeviation(ArrayList<Hypothesis> population, double averageFitness) {
        double standardDeviation = 0.0;
        for(Hypothesis adult: population){
            standardDeviation += Math.pow((adult.getFitness() - averageFitness), 2);
        }
        return Math.sqrt(standardDeviation / population.size());

    }

    public int getGenerationNumber() {
        return generationNumber;
    }
}
