# Hydra Vote POC Off-chain Application

The current version is importing votes on L1. After successful testing on L1, it will be changed to use
Hydra.

## Requirements

Java 17

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
voting.batch.contract=addr_test1wrghjsumtcdrnfmvcm6qqp0dcw6aylmhcrz3mj95d9t9aqq878m2y
# Delay between two import transactions
import.interval=100
cardano.network=preprod
bf.project_id=<Blockfrost project id>

operator.mnemonic=<24 words mnemonic for sender's account>

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

```shell
shell:>import-votes --batchSize 100 --startIndex 0 --voteFile votes.json
```
