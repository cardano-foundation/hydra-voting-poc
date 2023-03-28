# Hydra Vote POC Off-chain Application

The current version is importing votes on L1. After successful testing on L1, it will be changed to use
Hydra.

## Requirements

Java 17
Aiken with Sha1: 80f2fd746dae3dcfa85b6afbe3b1d1dd1713a89e

## To build

```
$> ./gradlew clean build -x test
```

## To run

```
$> java -jar build/libs/hydra-vote-offchain-0.0.1-SNAPSHOT.jar
```

### Create config file

- Create a folder "config" in project folder (in working dir)
- Create application.properties in config folder with the following content

```properties
# Delay between two import transactions
import.interval=50
cardano.network=preprod
bf.project_id=<Blockfrost project id>

operator.mnemonic=<24 words mnemonic for sender's account>

# settings for random vote generation
proposals.per.challenge=3
total.challenges=2

```


From spring shell

### Generate Random Votes

```
shell:> generate-votes --nVoters 20 --nVotes 1000 --outFile votes.json
```

```
{
"voter_key": <public ed25519 key of voter>,
"challenge": <The challenge id they voted on (int64)>,
"proposal": <The proposal they voted on in the challenge (int 64)>,
"choice": <What did they choose (int)>
}
```

### Import Votes to contract address
<i>This command imports offchain votes to onchain via multiple transactions executed in a loop, each transaction containing utxos equivalent to batchsize.</i>

```shell
shell:>import-votes --batchSize 100 --startIndex 0 --voteFile votes.json
```

### To create a vote batch
<i>This command can be executed multiple times till there is no individual vote utxo available.</i>

```shell
shell> create-batch --batchSize 10
```

### To reduce vote batches (Group vote batches and create a new batch with aggregated result)
<i>This command can be executed multiple times until only one batch remains to represent the final result.</i>

```shell
shell> reduce-batch --batchSize 10
```

## To check remaining imported votes or batches, use the below commands

### Get remaining n imported votes (Vote Utxos)

```shell
shell> get-votes --nVotes 100
```

### Get remaining n vote batches (Vote Result Batch)

```shell
shell> get-vote-batches --nBatch 10
```

