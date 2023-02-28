use aiken/dict.{Dict}
use aiken/hash.{Blake2b_224, Hash}
use aiken/list
use aiken/option
use aiken/transaction.{
  InlineDatum, Input, Output, OutputReference, ScriptContext, Spend,
}
use aiken/transaction/credential.{ScriptCredential}
use voting/address.{PubKeyHash}
use voting/challenge_proposal.{Challenge, ChallengeProposal, Proposal}
use voting/validators

// TODO
// 1. add merkle tree to validate votes
// 2. What shall we call datums and what not???
// 3. Reduce vote batches redeemer

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
  yes: VotingPower,
  no: VotingPower,
  abstain: VotingPower,
}

type Result {
  yes: VotingPower,
  no: VotingPower,
  abstain: VotingPower,
}

// batch_size: Int?
// merkle_tree: MerkleTree
type VotingDatum {
}

type ResultsBatchDatum {
  //merke_tree_root: String
  results: Dict<ChallengeProposal, Result>,
}

type Redeemer {
  // if the votes submit and locked and are complete, we have to use CIP-31
  CreateVoteBatch {
    // merkle_tree: MerkleTree,
    // id = in fact merkle root hash
    id: Hash<Blake2b_224, String>,
  }
  ReduceVoteBatch { id: Hash<Blake2b_224, String> }
}

// Note:
// Smart Contract assumes that votes are deduped across all batches
// otherwise it cannot possibly work because some votes could be counted twice
validator voting {
  fn(_voting_datum: VotingDatum, redeemer: Redeemer, sc: ScriptContext) -> Bool {
    // On L2 (Hydra) one will typically lock votes with 0 ADA but on L1 of course each eUTxO is guarded by ca. 1,7 ADA
    expect Spend(output_reference) = sc.purpose

    when redeemer is {
      CreateVoteBatch { id } -> {
        // we sum all votes and output in the ResultsBatchDatum
        // and somehow verify id of the batch and total votes in the batch
        // alternatively use normal inputs
        let inputs: List<Input> = sc.transaction.inputs
        let outputs: List<Output> = sc.transaction.outputs
        let votes: List<Vote> = get_votes_from_inputs(inputs)
        let results: Dict<ChallengeProposal, Result> = count_votes(votes)
        // now we need to check if any of the output UTxOs contains results
        // now we have to check if batch_id matches
        results_must_match(
          expected_results: results,
          outputs: sc.transaction.outputs,
        ) && batch_id_must_match(votes, id) && result_sent_to_own_address_match(
          inputs,
          outputs,
          results,
          output_reference,
        )
      }
      ReduceVoteBatch { id: _id } -> {
        // collect results from all inputs (fail if there are still votes in smart contract)
        let inputs: List<Input> = sc.transaction.inputs
        let all_results: List<ResultsBatchDatum> =
          get_vote_results_from_inputs(inputs)
        // take existing results and compress them, into one
        let one_final_result: ResultsBatchDatum =
          count_vote_results(all_results)
        // check if resulting output datum contains votes counted up properly
        // todo
        // check if we are one final vote results are sent back to the same contract / validator address (reduced)
        dict.is_empty(one_final_result.results) == False
      }
      // TODO: this operation can be fully recursive but we need sequence number to know how deeply nested we are
      // first we handle only seq = 0
      // then seq = 1
      _ -> False
    }
  }
}

fn result_sent_to_own_address_match(
  inputs: List<Input>,
  outputs: List<Output>,
  results: Dict<ChallengeProposal, Result>,
  output_reference: OutputReference,
) {
  let validator_hash =
    validators.own_validator_script_address_hash(inputs, output_reference)

  let expected_results_data: Data = ResultsBatchDatum { results }

  list.any(
    outputs,
    fn(output) {
      expect ScriptCredential(output_script_hash) =
        output.address.payment_credential
      expect InlineDatum(output_results_data) = output.datum

      output_script_hash == validator_hash && output_results_data == expected_results_data
    },
  )
}

fn results_must_match(
  expected_results: Dict<ChallengeProposal, Result>,
  outputs: List<Output>,
) -> Bool {
  let expected_results_datum = ResultsBatchDatum { results: expected_results }

  list.any(
    outputs,
    fn(output) {
      expect InlineDatum(given_results_data) = output.datum
      expect given_results_datum: ResultsBatchDatum = given_results_data

      expected_results_datum == given_results_datum
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

fn get_votes_from_inputs(inputs: List<Input>) -> List<Vote> {
  list.map(
    inputs,
    fn(input) {
      expect InlineDatum(vote_data) = input.output.datum
      expect vote: Vote = vote_data

      vote
    },
  )
}

fn get_vote_results_from_inputs(inputs: List<Input>) -> List<ResultsBatchDatum> {
  // what to do in case when there are still some votes in smart contract address
  // should we ignore them or should we simply blow up reduction operation and smart contract fails?
  // for now we will fail... in fact this is even good because it means there are still votes to reduce
  list.map(
    inputs,
    fn(input) {
      expect InlineDatum(vote_results_data) = input.output.datum
      expect results_data_datum: ResultsBatchDatum = vote_results_data

      results_data_datum
    },
  )
}

fn count_vote_results(
  vote_results: List<ResultsBatchDatum>,
) -> ResultsBatchDatum {
  let merging_fn: fn(ResultsBatchDatum, ResultsBatchDatum) -> ResultsBatchDatum = fn(
    rd1: ResultsBatchDatum,
    rd2: ResultsBatchDatum,
  ) {
    ResultsBatchDatum {
      results: dict.union_with(
        left: rd1.results,
        right: rd2.results,
        with: results_conflict_resolver,
        compare: challenge_proposal.compare,
      ),
    }
  }
  let zero = ResultsBatchDatum { results: dict.new() }

  list.foldl(vote_results, merging_fn, zero)
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
      dict.union_with(
        left: do_count_votes(t, acc),
        right: dict.insert(
          self: dict.new(),
          key: ChallengeProposal { challenge: c, proposal: p },
          value: Result { yes: fv.yes, no: fv.no, abstain: fv.abstain },
          compare: challenge_proposal.compare,
        ),
        with: results_conflict_resolver,
        compare: challenge_proposal.compare,
      )
    }
  }
}

fn results_conflict_resolver(
  _key: ChallengeProposal,
  left: Result,
  right: Result,
) -> Option<Result> {
  Some(
    Result {
      yes: left.yes + right.yes,
      no: left.no + right.no,
      abstain: left.abstain + right.abstain,
    },
  )
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
