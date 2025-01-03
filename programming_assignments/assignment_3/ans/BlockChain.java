import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;

    private int maxHeight;  
    private int clock;  

    private TransactionPool transactionPool;

    private HashMap<ByteArrayWrapper, Block> blocks;
    private HashMap<ByteArrayWrapper, UTXOPool> blockUTXOPools;
    private HashMap<ByteArrayWrapper, Integer> blockHeights;
    private HashMap<ByteArrayWrapper, Integer> blockAddTimes;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        transactionPool = new TransactionPool();
        blocks = new HashMap<ByteArrayWrapper, Block>();
        blockUTXOPools = new HashMap<ByteArrayWrapper, UTXOPool>();
        blockHeights = new HashMap<ByteArrayWrapper, Integer>();
        blockAddTimes = new HashMap<ByteArrayWrapper, Integer>();
        
        ByteArrayWrapper genBlockHash = new ByteArrayWrapper(genesisBlock.getHash());

        clock = 0;
        maxHeight = 1;

        // get block transactions
        ArrayList<Transaction> txs = genesisBlock.getTransactions();
        // ArrayList to array
        Transaction[] txsArr = new Transaction[txs.size()];
        txsArr = txs.toArray(txsArr);
        // create txHandler
        TxHandler txHandler = new TxHandler(new UTXOPool());
        Transaction[] acceptedTxsArr = txHandler.handleTxs(txsArr);
        UTXOPool utxoPool = txHandler.getUTXOPool();
        // add coinbase transaction to utxoPool
        Transaction coinbase = genesisBlock.getCoinbase();
        int i = 0;
        for (Transaction.Output output : coinbase.getOutputs()) {
            utxoPool.addUTXO(new UTXO(coinbase.getHash(), i), output);
            i++;
        }
        blocks.put(genBlockHash, genesisBlock);
        blockHeights.put(genBlockHash, maxHeight);
        blockAddTimes.put(genBlockHash, clock);
        blockUTXOPools.put(genBlockHash, utxoPool);

    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return blocks.get(getMaxHeightBlockHash());
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return blockUTXOPools.get(getMaxHeightBlockHash());
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        if (block.getPrevBlockHash() == null || block.getHash() == null) {
            return false; 
        }

        ByteArrayWrapper bHash = new ByteArrayWrapper(block.getHash());
        ByteArrayWrapper pBHash = new ByteArrayWrapper(block.getPrevBlockHash());

        // ensure we have its parent block
        if (blocks.containsKey(pBHash) == false || blockUTXOPools.containsKey(pBHash) == false || blockHeights.containsKey(pBHash) == false || blockAddTimes.containsKey(pBHash) == false) {
            return false;
        }

        // avoid re-add the same block
        if (blocks.containsKey(bHash) == true) {
            return true;
        }

        // copy utxoPool from its parent
        UTXOPool pUtxoPool = blockUTXOPools.get(pBHash);
        // get block transactions
        ArrayList<Transaction> txs = block.getTransactions();
        // ArrayList to array
        Transaction[] txsArr = new Transaction[txs.size()];
        txsArr = txs.toArray(txsArr);
        // create txHandler
        TxHandler txHandler = new TxHandler(pUtxoPool);
        // validate transactions, all transactions need to be valid
        Transaction[] acceptedTxsArr = txHandler.handleTxs(txsArr);
        UTXOPool utxoPool = txHandler.getUTXOPool();
        if (utxoPool == null || acceptedTxsArr == null || acceptedTxsArr.length != txsArr.length) {
            return false;
        }
        // get parent block height
        int pHeight = blockHeights.get(pBHash); 
        // set current block height
        int blockHeight = pHeight + 1;
        // block should be at {@code height > (maxHeight - CUT_OFF_AGE)}
        if (blockHeight <= (maxHeight - CUT_OFF_AGE)) {
            return false;
        }
        // update maxHeight
        if (blockHeight > maxHeight) {
            maxHeight = blockHeight;

            ArrayList<ByteArrayWrapper> useLessblocks = new ArrayList<ByteArrayWrapper>();

            // remove blocks if their heights < (maxHeight - CUT_OFF_AGE)
            // becuase new block's parent cant refer to them
            for (Map.Entry<ByteArrayWrapper, Integer> entry : blockHeights.entrySet()) {
                int height = entry.getValue();
                if (height < (maxHeight - CUT_OFF_AGE)) {
                    ByteArrayWrapper blockHash = entry.getKey();
                    useLessblocks.add(blockHash);
                }
            }
            for (ByteArrayWrapper blockHash : useLessblocks) {
                blocks.remove(blockHash);
                blockUTXOPools.remove(blockHash);
                blockHeights.remove(blockHash);
                blockAddTimes.remove(blockHash);
            }
        }
        // remove transactions from the {@code transactionPool} if the block is added
        for (Transaction tx : acceptedTxsArr) {
            transactionPool.removeTransaction(tx.getHash());
        }
        // add coinbase transaction to utxoPool
        Transaction coinbase = block.getCoinbase();
        int i = 0;
        for (Transaction.Output output : coinbase.getOutputs()) {
            utxoPool.addUTXO(new UTXO(coinbase.getHash(), i), output);
            i++;
        }
        // add new block
        clock++;
        blocks.put(bHash, block);
        blockUTXOPools.put(bHash, utxoPool);
        blockHeights.put(bHash, blockHeight);
        blockAddTimes.put(bHash, clock);

        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }

    /** Get the maximum height block hash */
    private ByteArrayWrapper getMaxHeightBlockHash() {
        ByteArrayWrapper maxHeightBlockHash = null;
        int blockAddTime = -1;

        for (Map.Entry<ByteArrayWrapper, Integer> entry : blockHeights.entrySet()) {
            boolean isMaxHeight = false;
            if (maxHeight != entry.getValue()) {
                continue;
            }
            if (maxHeight == entry.getValue()) {
                if (blockAddTime == -1 || blockAddTimes.get(entry.getKey()) < blockAddTime) {
                    maxHeightBlockHash = entry.getKey();
                    blockAddTime = blockAddTimes.get(maxHeightBlockHash);
                }
            }
        }

        return maxHeightBlockHash;
    }
}
