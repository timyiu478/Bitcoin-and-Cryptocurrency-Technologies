import java.util.Random;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private Set<Transaction> pendingTransactions;
    private Set<Transaction> consensusTransactions;
    private Set<Integer> followees;
    private double p_acceptTx;
    private int numRounds;
    private int currentRound;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.numRounds = numRounds;
        this.currentRound = 0;
        this.p_acceptTx = 1 - p_graph * p_txDistribution * p_malicious;
        this.followees = new HashSet<Integer>();
    }

    public void setFollowees(boolean[] followees) {
        for (int i = 0; i < followees.length; i++) {
            if (followees[i] == true) {
                this.followees.add(i);
            }
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        this.pendingTransactions = new HashSet<>(pendingTransactions);
        this.consensusTransactions = new HashSet<>(pendingTransactions);
    }

    public Set<Transaction> sendToFollowers() {
        currentRound++;

        Set<Transaction> transactions;
        if (currentRound >= numRounds) {
            return consensusTransactions;
        } else {
            transactions = new HashSet<>(pendingTransactions);
            pendingTransactions.clear();
        }
        return transactions;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        for (Candidate cand : candidates) {
            if (consensusTransactions.contains(cand.tx) == false && Math.random() < p_acceptTx) {
                pendingTransactions.add(cand.tx);
                consensusTransactions.add(cand.tx);
            }
        }
    }
}
