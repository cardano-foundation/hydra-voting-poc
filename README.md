# Hydra for Voting Proof-of-Concept

Check [Project Charter](./docs/project-charter.md) for details about this project

![image](https://user-images.githubusercontent.com/335933/219307601-7c5fc745-c19e-489b-a63e-586fd8ee8e8d.png)

### Milestones
- M1 - Tally 1 mln Catalyst votes (as in compatible with Catalyst domain model) with min 3 and max 10 Hydra Head operators, perform distributed tally process and obtain only one result
    - 🟢 Port Merkle Tree from Hydra to Aiken (https://github.com/input-output-hk/hydra/blame/master/plutus-merkle-tree/src/Plutus/MerkleTree.hs) -> https://github.com/aiken-lang/trees/pull/1/files
    - 🟢 Write simple on chain contract that counts votes
    - Run on chain contract counting unencrypted Catalyst votes with off-chain code
    - Hook up Merkle Tree to contract counting votes to attest if vote which is on chain is part of the tree
    - Perform final votes batch calculation merking all Merkle Trees together and giving a final result

![image](https://user-images.githubusercontent.com/335933/219307471-2b9a367c-2586-4fe5-92a7-97e582f35c12.png)

- M2 - Using Partial Homomorphic Encryption tally the votes in such a way that each Hydra Head Operator cannot tally alone but all operators only can tally together
