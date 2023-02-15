use aiken/bytearray
use aiken/dict.{Dict}
use aiken/hash.{Blake2b_224, Hash}
use aiken/list
use aiken/option
use aiken/transaction.{Datum, InlineDatum, Input, Output, ScriptContext}
use aiken/transaction/credential.{VerificationKey}

// TODO
// 1. Improve code quality and add tests
// 2. support not only inline datums?
// 3. maybe we need to spend those votes actually?
// 4. add merkle tree to validate votes
// 5. add ability for all parties to approve???

type PubKeyHash =
  Hash<Blake2b_224, VerificationKey>

type VotingPower =
  Int

type Challenge =
  Int

type Proposal =
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

type ChallengeProposal {
  challenge: Challenge,
  proposal: Proposal,
}

// TODO probably there is some way to do this from standard library??
fn encode_challenge_proposal(cp: ChallengeProposal) -> ByteArray {
  #""
  |> bytearray.push(cp.challenge)
  |> bytearray.push(0)
  |> bytearray.push(cp.proposal)
}

fn compare_challenge_proposal(
  left: ChallengeProposal,
  right: ChallengeProposal,
) -> Ordering {
  bytearray.compare(
    encode_challenge_proposal(left),
    encode_challenge_proposal(right),
  )
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
          compare: compare_challenge_proposal,
        ),
        with: conflict_resolver_fn,
        compare: compare_challenge_proposal,
      )
    }
  }
}

fn count_votes(votes: List<Vote>) -> Dict<ChallengeProposal, Result> {
  let empty = dict.new()
  do_count_votes(votes, empty)
}

fn to_flat_vote(v: Vote) -> FlatVote {
  let cp = ChallengeProposal { challenge: v.challenge, proposal: v.proposal }
  let y =
    voting_power_for_choice(v)
    |> opt_filter(fn(x) { x == 2 })
    |> option.or_else(0)
  let n =
    voting_power_for_choice(v)
    |> opt_filter(fn(x) { x == 1 })
    |> option.or_else(0)
  let a =
    voting_power_for_choice(v)
    |> opt_filter(fn(x) { x == 0 })
    |> option.or_else(0)

  FlatVote { key: cp, yes: y, no: n, abstain: a }
}

fn voting_power_for_choice(v: Vote) -> Option<VotingPower> {
  list.find([0, 1, 2], fn(choice: Choice) { v.choice == choice })
  |> option.map(fn(_c: Choice) { v.voting_power })
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

  voting_power_for_choice(v1) == Some(100)
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

  voting_power_for_choice(v1) == None
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

  voting_power_for_choice(v1) == Some(100)
}

// remove when PR: https://github.com/aiken-lang/stdlib/pull/21/files is merged in
fn opt_filter(self: Option<a>, predicate: fn(a) -> Bool) -> Option<a> {
  when self is {
    Some(x) ->
      if predicate(x) {
        Some(x)
      } else {
        None
      }
    None -> None
  }
}
