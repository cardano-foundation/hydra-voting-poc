use aiken/builtin
use aiken/dict.{Dict}
use aiken/list
use aiken/option
use aiken/transaction.{
  InlineDatum, Input, Output, OutputReference, ScriptContext, Spend,
}
use aiken/transaction/credential.{ScriptCredential}
use voting/address.{PubKeyHash}
use voting/challenge_proposal.{Challenge, ChallengeProposal, Proposal}
use voting/mt.{MerkleElement}
use voting/validators

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

// respective pubkey hashes for authorised parties
type VotingDatum {
}

type ResultsBatchDatum {
  results: Dict<ChallengeProposal, Result>,
  merkle_root_hash: Option<ByteArray>,
  iteration: Int,
}

type Redeemer {
  // Approve { merkle_tree: MerkleTree<Vote> }
  CreateVoteBatch { merkle_tree: MerkleElement<Vote> }
  ReduceVoteBatch {
    merkle_tree: MerkleElement<ResultsBatchDatum>,
    iteration: Int,
  }
}

// Note:
// Smart Contract assumes that votes are deduped across all batches
// otherwise it cannot possibly work because some votes could be counted twice
validator voting {
  fn(_voting_datum: VotingDatum, redeemer: Redeemer, sc: ScriptContext) -> Bool {
    // On L2 (Hydra) one will typically lock votes with 0 ADA but on L1 of course each eUTxO is guarded by ca. 1,7 ADA
    expect Spend(output_reference) = sc.purpose
    let inputs: List<Input> = sc.transaction.inputs
    let outputs: List<Output> = sc.transaction.outputs

    when redeemer is {
      CreateVoteBatch { merkle_tree } -> {
        let merkle_root_hash = mt.element_hash(merkle_tree)
        // we sum all votes and output in the ResultsBatchDatum
        // and somehow verify id of the batch and total votes in the batch
        // alternatively use normal inputs
        let votes: List<Vote> = get_votes_from_inputs(inputs)
        let results: Dict<ChallengeProposal, Result> = count_votes(votes)
        // now we need to check if any of the output UTxOs contains results
        // now we have to check if batch_id matches
        let output_datum =
          ResultsBatchDatum {
            results,
            iteration: 0,
            merkle_root_hash: Some(merkle_root_hash),
          }
        list.and(
          [
            validate_votes(votes, merkle_tree)?,
            results_must_match(
              outputs: sc.transaction.outputs,
              expected_results: output_datum,
            )?,
            result_sent_to_own_address_match(inputs, outputs, output_reference)?,
          ],
        )
      }
      ReduceVoteBatch { merkle_tree, iteration } -> {
        let merkle_tree_root_hash = mt.element_hash(merkle_tree)
        // collect results from all inputs (fail if there are still votes in smart contract)
        // take existing results and compress them, into one
        // check if resulting output datum contains votes counted up properly and that they send result back to the same address
        let new_iteration = iteration + 1
        let all_results: List<ResultsBatchDatum> =
          get_vote_results_from_inputs(inputs, iteration)
        let one_final_result: ResultsBatchDatum =
          count_vote_results(
            vote_results: all_results,
            new_iteration: new_iteration,
          )
        let one_final_result_with_hash =
          ResultsBatchDatum {
            ..one_final_result,
            merkle_root_hash: Some(merkle_tree_root_hash),
          }
        list.and(
          [
            validate_results(all_results, merkle_tree)?,
            results_must_match(outputs, one_final_result_with_hash)?,
            result_sent_to_own_address_match(inputs, outputs, output_reference)?,
          ],
        )
      }
      _ -> False
    }
  }
}

fn result_sent_to_own_address_match(
  inputs: List<Input>,
  outputs: List<Output>,
  output_reference: OutputReference,
) {
  let validator_hash =
    validators.own_validator_script_address_hash(inputs, output_reference)

  list.any(
    outputs,
    fn(output) {
      expect ScriptCredential(output_script_hash) =
        output.address.payment_credential

      output_script_hash == validator_hash
    },
  )
}

fn results_must_match(
  outputs: List<Output>,
  expected_results: ResultsBatchDatum,
) -> Bool {
  list.any(
    outputs,
    fn(output) {
      expect InlineDatum(given_results_data) = output.datum
      expect given_results_datum: ResultsBatchDatum = given_results_data
      expected_results == given_results_datum
    },
  )
}

fn validate_votes(votes: List<Vote>, merkle_tree: MerkleElement<Vote>) -> Bool {
  let ser_fn = fn(v: Vote) { builtin.serialise_data(v) }

  let all_votes_present_match =
    list.all(votes, fn(vote) { mt.member_from_tree(merkle_tree, vote, ser_fn) })

  let leaf_size_match = mt.size(merkle_tree) == list.length(votes)

  list.and([all_votes_present_match?, leaf_size_match?])
}

fn validate_results(
  results: List<ResultsBatchDatum>,
  merkle_tree: MerkleElement<ResultsBatchDatum>,
) -> Bool {
  let ser_fn = fn(r: ResultsBatchDatum) { builtin.serialise_data(r) }

  let all_results_present_match =
    list.all(
      results,
      fn(result) { mt.member_from_tree(merkle_tree, result, ser_fn) },
    )

  let leaf_size_match = mt.size(merkle_tree) == list.length(results)

  list.and([all_results_present_match?, leaf_size_match])
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

// get vote results but only for this iteration
fn get_vote_results_from_inputs(
  inputs: List<Input>,
  iteration: Int,
) -> List<ResultsBatchDatum> {
  list.filter_map(
    inputs,
    fn(input) {
      expect InlineDatum(vote_results_data) = input.output.datum
      expect results_data_datum: ResultsBatchDatum = vote_results_data
      if results_data_datum.iteration == iteration {
        Some(results_data_datum)
      } else {
        None
      }
    },
  )
}

fn count_vote_results(
  vote_results: List<ResultsBatchDatum>,
  new_iteration: Int,
) -> ResultsBatchDatum {
  let merging_fn = fn(rd1: ResultsBatchDatum, rd2: ResultsBatchDatum) {
    ResultsBatchDatum {
      results: dict.union_with(
        left: rd1.results,
        right: rd2.results,
        with: results_conflict_resolver,
        compare: challenge_proposal.compare,
      ),
      iteration: new_iteration,
      merkle_root_hash: None,
    }
  }

  // assign new iteration
  let zero =
    ResultsBatchDatum {
      results: dict.new(),
      iteration: new_iteration,
      merkle_root_hash: None,
    }

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

fn voting_power_for_choice(v: Vote, c: Choice) -> Option<VotingPower> {
  list.find(
    [0, 1, 2],
    fn(choice: Choice) { list.and([(v.choice == choice)?, c == choice]) },
  )
  |> option.map(fn(_) { v.voting_power })
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
