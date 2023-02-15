# Hydra-vote-importer

## Requirements

Java 17

## To build

```
$> ./gradlew clean build
```

## To run

```
$> java -jar build/libs/hydra-vote-importer-0.0.1-SNAPSHOT.jar
```

From spring shell

```
shell:> generate-votes --nVoters 90000 --nVotes 300000 --outFile votes.json
```

```
{
"voter_key": <public ed25519 key of voter>,
"challenge": <The challenge id they voted on (int64)>,
"proposal": <The proposal they voted on in the challenge (int 64)>,
"choice": <What did they choose (int)>
}
```

challenge+proposal id are unique.
choice is just a number, say 0 = abstain, 1 = nay, 2 = yay
You can then generate some files with test data.
If you want to simulate Fund 9 size: generate 89K random ed25519 public keys, and assign a random voting power from say 500 to 10000000 (top value kinda arbitrary).  This would be your "voters".
Then generate X random Challenge ID's (say 20).  And make 100 random proposal id's per challenge.
Then just generate 300K random votes, where you pick a voter randomly from your voter list, a random challenge+proposal and a random choice.

