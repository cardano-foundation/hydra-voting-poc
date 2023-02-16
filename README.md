# Hydra for Voting Proof-of-Concept

Check [Project Charter](./docs/project-charter.md) for details about this project


### Milestones
- M1 - Tally 1 mln Catalyst votes (as in compatible with Catalyst domain model) with min 3 and max 10 Hydra Head operators, perform distributed tally process and obtain only one result
    - 🟢 Port Merkle Tree from Hydra to Aiken (https://github.com/input-output-hk/hydra/blame/master/plutus-merkle-tree/src/Plutus/MerkleTree.hs)
    - 🟢 Write simple on chain contract that counts votes
    - Run on chain contract counting unencrupted Catalyst votes with off-chain code
- M2 - Using Partial Homomorphic Encryption tally the votes in such a way that each Hydra Head Operator cannot tally alone but all operators only can tally together  
