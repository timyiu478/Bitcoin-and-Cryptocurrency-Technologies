import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        Set<UTXO> claimedUTXOs = new HashSet<UTXO>();
        ArrayList<UTXO> utxoList = utxoPool.getAllUTXO();
        int claimedOutputs = 0;        
        double sumOfInputValue = 0;
        double sumOfOutputValue = 0;

        for (int i=0; i < tx.numInputs(); i++) {
            Transaction.Input in = tx.getInput(i);
            for (UTXO utxo : utxoList) {
                UTXO inUtxo = new UTXO(in.prevTxHash, in.outputIndex);
                if (inUtxo.equals(utxo) && claimedUTXOs.contains(utxo) == false) {
                    claimedOutputs += 1;
                    claimedUTXOs.add(utxo);

                    Transaction.Output output = utxoPool.getTxOutput(utxo);
                    byte[] txRawData = tx.getRawDataToSign(i);

                    // (2)
                    if (Crypto.verifySignature(output.address, txRawData, in.signature) == false) {
                        return false;
                    }
                    
                    // (4)
                    if (output.value < 0) {
                        return false;
                    }

                    sumOfInputValue += output.value;
                } else if (inUtxo.equals(utxo) && claimedUTXOs.contains(utxo)) {
                    // (3)
                    return false;   
                }
            }
        }
        if (claimedOutputs != tx.numInputs()) {
            // (1)
            return false;
        }

        for (int i=0; i < tx.numOutputs(); i++) {
            Transaction.Output output = tx.getOutput(i);
            if (output.value < 0) {
                // (4)
                return false;
            }
            sumOfOutputValue += output.value;
        }
        
        // (5)
        if (sumOfOutputValue > sumOfInputValue) {
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTxs = new ArrayList<Transaction>();

        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx) == true) {
                // remove previos unspent transaction output since they are input of new transaction
                for (int i=0; i < tx.numInputs(); i++) {
                    Transaction.Input in = tx.getInput(i);
                    utxoPool.removeUTXO(new UTXO(in.prevTxHash, in.outputIndex));
                }

                // add new transaction output in UTXO
                for (int i=0; i < tx.numOutputs(); i++) {
                    utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
                }

                acceptedTxs.add(tx);
            }
        }

        Transaction[] acceptedTxsArr = acceptedTxs.toArray(new Transaction[acceptedTxs.size()]);

        return acceptedTxsArr;
    }

}

