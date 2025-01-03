## Assignment 1 - Transaction Handler

- A transaction involves number of inputs and number of outputs.
- A transaction output will be an input of another transaction.
- Unspent transaction output (*UTXO*) is an transaction output that is not an inpu of any transaction.
- Unspent transaction output pool (*UTXOPool*) is a set of *UTXO* to allow validating double spent attempt efficiently.

## Assignment 2 - Consensus from trust

- the factors that affect the number of nodes have a consensus transactions after certain rounds of transaction propagation
	- number of malicious nodes
	- the pairwise connectivity
	- number rounds of transaction propagation

## Assignment 3 - Blockchain

- A blockchain can maintain the blocks by hashmap where the hash of the block as the hashmap key. This way we can easily to get the parent of the block easily since the block contains the hash of parent block. 
- A blockchain should use some cutoff mechnaism to maintain limited number of blocks only to avoid memory overflow.
- A blockchain is not always maintain a single branch of blocks. It accepts multiple branches.
