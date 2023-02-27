# Hydra for Voting Proof-of-Concept

Check [Project Charter](./docs/project-charter.md) for details about this project

![image](https://user-images.githubusercontent.com/335933/219307601-7c5fc745-c19e-489b-a63e-586fd8ee8e8d.png)

### Problem statement
Currently IOG is the only party doing tallying of the votes (meaning checking result of the votes). We would like to see how Hydra could be used to decentralise this process and allow more members (ideally from Cardano community) to tally / count Catalyst votes independently while reaching the same final verdict.

### Idea
Using Hydra experiment with a distributed Catalyst tally process such that more parties can attest and verify validity of Catalyst Fund results.

### Slides
- Hydra Voting PoC Kick-Off: https://docs.google.com/presentation/d/1oVB_J3eBKbhWc_yAr5w8cFz2TpL2Zngv8pZjK6Gtruk/edit#slide=id.g124655d21b1_2_509
- Rough Plan and focusing on M1 first: https://docs.google.com/presentation/d/16wnWy9Br0ewTJMZefeGBkNEj3jDyfjif/edit#slide=id.p1
- Feb 2023 M1 Status Update: https://docs.google.com/presentation/d/1ee7vQLIUcoDfHeU6ln_-_YlKvXM-5WvM/edit#slide=id.p1

### Milestones
- M1 - Tally ca 1 mln Catalyst votes (as in compatible with Catalyst domain model) with min 3 and max 10 Hydra Head operators, perform distributed tally process and obtain only one result
    - 🟢 Port Merkle Tree from Hydra to Aiken (https://github.com/input-output-hk/hydra/blame/master/plutus-merkle-tree/src/Plutus/MerkleTree.hs) -> https://github.com/aiken-lang/trees/pull/1/files
    - 🟢 Write simple on chain contract that counts votes (https://github.com/cardano-foundation/hydra-voting-poc/blob/master/on-chain/validators/voting.ak) 
    - 🟢 Write fake votes generator compatible with Catalyst domain model
    - :hourglass_flowing_sand: Write and run on chain contract counting unencrypted Catalyst votes with off-chain code without merkle trees on L1
    - :hourglass_flowing_sand: Use one of Open Source Merkle Trees implementation in offchain infra or port our Merkle Tree to (Java / Scala / Kotlin) on L1
    - 🟢 Spin up local dev Hydra Head network
    - 🟢 Develop PoC version of Hydra-Java-Client which allows us to easily access Hydra from java code: https://github.com/cardano-foundation/hydra-java-client
    - Port and run on chain contract counting unencrypted Catalyst votes with off-chain code on Hydra Dev Network
    - Use one of Open Source Merkle Trees implementation in offchain infra or port our Merkle Tree to (Java / Scala / Kotlin)
    - Hook up Merkle Tree to contract counting votes to attest if vote which is on chain is part of the tree
    - Perform final votes batch calculation merking all Merkle Trees together and giving a final result, when closing head final results plus global merkle tree root should be committed to L1.

![image](https://user-images.githubusercontent.com/335933/219307471-2b9a367c-2586-4fe5-92a7-97e582f35c12.png)

- M2 - Since Catalyst votes are encrypted on jormungandr sidechain, we will be using Partial Homomorphic Encryption tally the votes in such a way that each Hydra Head Operator cannot tally alone but all operators only can tally together
- M3 - Authentication for Hydra Head Operators via NFTs

### Deployment
While overall goal is that anybody could tally but intially we expect that IOG / Emurgo / CF will tally the votes using Hydra Heads. The idea is that we want to avoid in initial versions having to pay and lock up people funds for slashing (in case cheating by the group was detected).

General Statements:
- M1: Since votes are handled in batches - they only way system can work is that votes are not duplicated, this means that votes votes coming from vote domain / (currently jormungandr sidechain) are already unique and deduplicated (since we won't have one big Merkle Tree but a lot of small ones).
- Security assumptions: the current design of the system has currently security equal to 1 honest Hydra Head operator, meaning only if all Hydra Head Operators form a group and agree to .e.g. drop certain votes system could be compromised. There is an idea to explore in the future to use so called Mithril Committe on L1 that could also verify operation of Hydra Head Operators. Effectively this committee could independently tally the votes and publish their tally result on L1. This could increase security of the system with additional complexity of additional technology (Mithril) and Committe doing the work.
- One of the problematic things mentioned in Problems / Questions section where we rely on one operator performing importing of votes, by extending Hydra and doing atomic transactions we can make sure that all Hydra Operator verify that 1 operator that performed importing of votes imported all votes. This can be enabled thanks to: atomic transactions (meaning that multiple transactions either all happen or all fail) and Hydra extension (to be developed) which allows to sign off and validate transaction by Hydra Head Opererator if and only iff the transactions are containing what they should contain. Note that for this to work fine we require that all Hydra Head operators have access to original Catalyst votes (which should not be a problem at all).

## General Open Problems / Questions
- Election: we would like to anybody could potentially run Hydra Head nodes but we do know that number of these people cannot exceed certain amount, e.g. 10
- M1: if we separate voting from tallying not only from the business domain (which it is separated anyway) but also from technical point of view then votes in an encrypted form need to be imported by one Hydra Head Operator into a new network. Question: how do we make sure that this operator is trusted and that the same copy of votes which other operators have is in fact imported (idea: use smart contracts for this)
- General: if jormungandr continues to be run in a private network for now, how will Hydra Head operators obtain access to individual votes (especially if they are outside of 3 founding entities)

- M2: How split the key in such a way that each operator only gets a part of the key
- M2: How to count the votes together, in Hydra one operator needs to sign a transaction and all need to simply validate (full consensus)
- If we ever submit votes directly to Hydra - how to assure that one operator being temporary offline won't disturb the whole voting session, one idea, hydra can deliver n x m signing, meaning we can configure Hydra such that only ca 80% of operators need to sign. Unless voters directly interact with Hydra network - this is not really a needed feature.
- M2: Since Partial Homomorphic Encryption is CPU intensive, how can we actually run it in smart contract. Regardless of the fact that these are very resource intensitve tasks, right now there are no implementations for this available neither in Aiken nor in PlutusTx (to our knowledge)
- General: Once we open up the tallying to all people, how can we in a decentralised way select Hydra Head Operators doing the tally process? One idea is that we conduct voting on L1 and Hydra Head operators + backup operators receive ca 1 to 2 years licence, such a license such cover operation for a few Catalyst rounds but question is whether people will want to participate in selection of Hydra Head Operators doing the tallying.
- General: Currently Hydra does not support external transaction validation, meaning just like in Cardano transaction is automatically validated. External transaction validation could be useful in scenarios where a Hydra Head operator would also check validity of transaction against their off chain infrastructure.
