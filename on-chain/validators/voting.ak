use aiken/dict.{Dict}
use aiken/hash.{Blake2b_224, Hash}
use aiken/list
use aiken/option
use aiken/transaction.{Datum, InlineDatum, Input, Output, ScriptContext}
use voting/address.{PubKeyHash}
use voting/challenge_proposal.{Challenge, ChallengeProposal, Proposal}

// TODO
// 1. Improve code quality and add more tests
// 2. support not only inline datums?
// 3. maybe we need to spend those votes actually?
// 4. add merkle tree to validate votes
// 5. add ability for all parties to approve vit???

type VotingPower =
  Int

type Choice =
  Int

type Vote {
  voter_key: PubKeyHash,
  voting_power: VotingPower,
  challenge: Challenge,
  proposal: Proposal,
  choice: Choice,
}

type FlatVote {
  key: ChallengeProposal,
  yes: Int,
  no: Int,
  abstain: Int,
}

type Result {
  yes: VotingPower,
  no: VotingPower,
  abstain: VotingPower,
}

type VotingDatum {
}

type ResultsBatchDatum {
  results: Dict<ChallengeProposal, Result>,
}

type Redeemer {
  //Approved // if the votes submit and locked and are complete, we have to use CIP-31
  CreateVoteBatch { id: Hash<Blake2b_224, String> }
}

fn spend(
  _voting_datum: VotingDatum,
  redeemer: Redeemer,
  sc: ScriptContext,
) -> Bool {
  // we lock votes with 0 ADA into smart contract address (off chain)

  when redeemer is {
    CreateVoteBatch { id } -> {
      // we sum all votes and output in the ResultsBatchDatum
      // and somehow verify id of the batch and total votes in the batch
      // alternatively use normal inputs
      let votes: List<Vote> =
        get_votes_from_inputs(sc.transaction.reference_inputs)
      let results: Dict<ChallengeProposal, Result> = count_votes(votes)
      // now we need to check if any of the output UTxOs contains results
      // now we have to check if batch_id matches
      results_must_match(results: results, outputs: sc.transaction.outputs) && batch_id_must_match(
        votes,
        id,
      )
    }
    // Approved -> False
    _ -> False
  }
}

fn results_must_match(
  results: Dict<ChallengeProposal, Result>,
  outputs: List<Output>,
) -> Bool {
  let results_datum = ResultsBatchDatum { results }

  list.any(
    outputs,
    fn(o) {
      let d: Datum = o.datum
      let data: Data = get_data_from_datum(d)
      expect res: ResultsBatchDatum = data
      res == results_datum
    },
  )
}

fn batch_id_must_match(
  _votes: List<Vote>,
  _batch_id: Hash<Blake2b_224, String>,
) -> Bool {
  // pass merkle tree root and check if merkle tree matches
  True
}

fn get_data_from_datum(d: Datum) -> Option<Data> {
  when d is {
    InlineDatum(id) -> Some(id)
    _ -> None
  }
}

fn get_votes_from_inputs(inputs: List<Input>) -> List<Vote> {
  list.map(inputs, fn(i) { i.output.datum })
  |> list.filter_map(fn(d) { get_data_from_datum(d) })
  |> list.map(
  fn(d) {
    // casting to Vote
    expect v: Vote = d
    v
  })
  
}

fn do_count_votes(
  votes: List<Vote>,
  acc: Dict<ChallengeProposal, Result>,
) -> Dict<ChallengeProposal, Result> {
  when votes is {
    [] -> acc
    [h, ..t] -> {
      let fv: FlatVote = to_flat_vote(h)
      let c: Challenge = h.challenge
      let p: Proposal = h.proposal
      let conflict_resolver_fn: fn(ChallengeProposal, Result, Result) ->
        Option<Result> = fn(
        _key: ChallengeProposal,
        left_value: Result,
        right_value: Result,
      ) {
        let mr: Result =
          Result {
            yes: left_value.yes + right_value.yes,
            no: left_value.no + right_value.no,
            abstain: left_value.abstain + right_value.abstain,
          }
        Some(mr)
      }
      dict.union_with(
        left: do_count_votes(t, acc),
        right: dict.insert(
          self: dict.new(),
          key: ChallengeProposal { challenge: c, proposal: p },
          value: Result { yes: fv.yes, no: fv.no, abstain: fv.abstain },
          compare: challenge_proposal.compare,
        ),
        with: conflict_resolver_fn,
        compare: challenge_proposal.compare,
      )
    }
  }
}

fn count_votes(votes: List<Vote>) -> Dict<ChallengeProposal, Result> {
  let empty = dict.new()
  do_count_votes(votes, empty)
}

test do_count_votes_1() {
  let v1 =
    Vote {
      voter_key: #"01",
      voting_power: 10,
      challenge: 1,
      proposal: 1,
      choice: 2,
    }
  let v2 =
    Vote {
      voter_key: #"02",
      voting_power: 20,
      challenge: 1,
      proposal: 2,
      choice: 2,
    }
  let v3 =
    Vote {
      voter_key: #"03",
      voting_power: 30,
      challenge: 1,
      proposal: 2,
      choice: 2,
    }
  let v4 =
    Vote {
      voter_key: #"04",
      voting_power: 30,
      challenge: 2,
      proposal: 1,
      choice: 0,
    }

  let votes = [v1, v2, v3, v4]
  let results: Dict<ChallengeProposal, Result> = count_votes(votes)
  let cp1: ChallengeProposal = ChallengeProposal { challenge: 1, proposal: 1 }
  let cp2: ChallengeProposal = ChallengeProposal { challenge: 1, proposal: 2 }
  let cp3: ChallengeProposal = ChallengeProposal { challenge: 2, proposal: 1 }

  expect Some(r1) = dict.get(results, cp1)
  expect Some(r2) = dict.get(results, cp2)
  expect Some(r3) = dict.get(results, cp3)

  r1.yes == 10 && r1.no == 0 && r1.abstain == 0 && r2.yes == 50 && r2.no == 0 && r2.abstain == 0 && r3.yes == 0 && r3.no == 0 && r3.abstain == 30
}

fn to_flat_vote(v: Vote) -> FlatVote {
  let cp = ChallengeProposal { challenge: v.challenge, proposal: v.proposal }
  let y =
    voting_power_for_choice(v, 2)
    |> option.or_else(0)
  let n =
    voting_power_for_choice(v, 1)
    |> option.or_else(0)
  let a =
    voting_power_for_choice(v, 0)
    |> option.or_else(0)

  FlatVote { key: cp, yes: y, no: n, abstain: a }
}

test to_flat_vote_1() {
  let v1 =
    Vote {
      voter_key: #"01",
      voting_power: 10,
      challenge: 1,
      proposal: 1,
      choice: 2,
    }

  to_flat_vote(v1) == FlatVote {
    key: ChallengeProposal(1, 1),
    yes: 10,
    no: 0,
    abstain: 0,
  }
}

test to_flat_vote_2() {
  let v1 =
    Vote {
      voter_key: #"01",
      voting_power: 10,
      challenge: 1,
      proposal: 1,
      choice: 1,
    }

  to_flat_vote(v1) == FlatVote {
    key: ChallengeProposal(1, 1),
    yes: 0,
    no: 10,
    abstain: 0,
  }
}

test to_flat_vote_3() {
  let v1 =
    Vote {
      voter_key: #"01",
      voting_power: 10,
      challenge: 1,
      proposal: 1,
      choice: 0,
    }

  to_flat_vote(v1) == FlatVote {
    key: ChallengeProposal(1, 1),
    yes: 0,
    no: 0,
    abstain: 10,
  }
}

fn voting_power_for_choice(v: Vote, c: Choice) -> Option<VotingPower> {
  list.find([0, 1, 2], fn(choice: Choice) { v.choice == choice && c == choice })
  |> option.map(fn(_) { v.voting_power })
}

test voting_power_for_choice_1() {
  let v1 =
    Vote {
      voter_key: #"01",
      voting_power: 100,
      challenge: 1,
      proposal: 1,
      choice: 0,
    }

  voting_power_for_choice(v1, 0) == Some(100)
}

test voting_power_for_choice_2() {
  let v1 =
    Vote {
      voter_key: #"01",
      voting_power: 100,
      challenge: 1,
      proposal: 1,
      choice: -1,
    }

  voting_power_for_choice(v1, 0) == None
}

test voting_power_for_choice_3() {
  let v1 =
    Vote {
      voter_key: #"02",
      voting_power: 100,
      challenge: 2,
      proposal: -1,
      choice: 2,
    }

  voting_power_for_choice(v1, 2) == Some(100)
}
