use acca/collections/hashed_list
use acca/validators
use aiken/builtin.{append_string}
use aiken/cbor
use aiken/dict.{Dict}
use aiken/hash.{Hash, Sha2_256, sha2_256}
use aiken/list
use aiken/option
use aiken/string
use aiken/transaction.{
  InlineDatum, Input, Output, OutputReference, ScriptContext, Spend,
}
use aiken/transaction/credential.{ScriptCredential}
use voting/challenge_proposal.{Challenge, ChallengeProposal, Proposal}
use voting/datums.{
  Choice, FlatVote, Result, ResultsBatchDatum, Vote, VotingDatum, VotingPower,
}

type Redeemer {
  // Approve { batch_hash: MerkleTree<Vote> }
  CreateVoteBatch { batch_hash: Hash<Sha2_256, Vote> }
  ReduceResults {
    batch_hash: Hash<Sha2_256, ResultsBatchDatum>,
    iteration: Int,
  }
}

validator {
  fn voting(
    _voting_datum: VotingDatum,
    redeemer: Redeemer,
    sc: ScriptContext,
  ) -> Bool {
    // On L2 (Hydra) one will typically lock votes with 0 ADA but on L1 of course each eUTxO is guarded by ca. 1,7 ADA
    expect Spend(output_reference) =
      sc.purpose

    let inputs: List<Input> =
      sc.transaction.inputs

    let outputs: List<Output> =
      sc.transaction.outputs
    when redeemer is {
      CreateVoteBatch { batch_hash } -> {
        trace @"get_votes_from_inputs..."
        let votes: List<Vote> =
          get_votes_from_inputs(inputs)
        trace @"got__votes_from_inputs."
        let results: Dict<ChallengeProposal, Result> =
          count_votes(votes)
        let result_with_hash =
          ResultsBatchDatum { results, iteration: 0, batch_hash }
        list.and(
          [
            //(bytearray.length(batch_hash) == 32)?,
            result_sent_to_own_address_match(inputs, outputs, output_reference)?,
            results_must_match(sc.transaction.outputs, result_with_hash)?,
            validate_votes(votes, batch_hash)?,
          ],
        )
      }
      ReduceResults { batch_hash, iteration } -> {
        // collect results from all inputs (fail if there are still votes in smart contract)
        // take existing results and compress them, into one
        // check if resulting output datum contains votes counted up properly and that they send result back to the same address
        let new_iteration =
          iteration + 1
        let all_results: List<ResultsBatchDatum> =
          get_vote_results_from_inputs(inputs, iteration)
        let result: ResultsBatchDatum =
          count_vote_results(
            vote_results: all_results,
            new_iteration: new_iteration,
          )
        let result_with_hash =
          ResultsBatchDatum { ..result, batch_hash: batch_hash }
        list.and(
          [
            result_sent_to_own_address_match(inputs, outputs, output_reference)?,
            validate_results(all_results, batch_hash)?,
            results_must_match(
              outputs: sc.transaction.outputs,
              expected_results: result_with_hash,
            )?,
          ],
        )
      }
      _ ->
        False
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

  let script_creds =
    list.filter(
      outputs,
      fn(o) {
        when o.address.payment_credential is {
          ScriptCredential(_) ->
            True
          _ ->
            False
        }
      },
    )

  list.any(
    script_creds,
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
  let inline_datums =
    list.filter(
      outputs,
      fn(o) {
        when o.datum is {
          InlineDatum(_) ->
            True
          _ ->
            False
        }
      },
    )
  list.any(
    inline_datums,
    fn(output) {
      expect InlineDatum(given_results_data) =
        output.datum

      expect given_results_datum: ResultsBatchDatum =
        given_results_data

      trace append_string(
        @"given_results_datum:",
        cbor.diagnostic(given_results_datum),
      )

      trace append_string(
        @"expected_results:",
        cbor.diagnostic(expected_results),
      )
      expected_results == given_results_datum
    },
  )
}

fn validate_votes(votes: List<Vote>, batch_hash: Hash<Sha2_256, Vote>) -> Bool {
  trace append_string(
    @"Validating votes..., size:",
    string.from_int(list.length(votes)),
  )

  let hash_fn =
    fn(v: Vote) { sha2_256(builtin.serialise_data(v)) }

  let hashed_list =
    hashed_list.from(votes, hash_fn)

  let on_chain_batch_hash =
    hashed_list.hash

  trace builtin.append_string(
    @"on_chain_hash:",
    cbor.diagnostic(on_chain_batch_hash),
  )
  trace builtin.append_string(@"off_chain_hash:", cbor.diagnostic(batch_hash))

  on_chain_batch_hash == batch_hash
}

fn validate_results(
  results: List<ResultsBatchDatum>,
  batch_hash: Hash<Sha2_256, ResultsBatchDatum>,
) -> Bool {
  let hash_fn =
    fn(r: ResultsBatchDatum) { sha2_256(builtin.serialise_data(r)) }

  let hashed_list =
    hashed_list.from(results, hash_fn)

  let on_chain_batch_hash =
    hashed_list.hash

  batch_hash == on_chain_batch_hash
}

fn get_votes_from_inputs(inputs: List<Input>) -> List<Vote> {
  list.map(
    inputs,
    fn(input) {
      expect InlineDatum(vote_data) =
        input.output.datum
      expect vote: Vote =
        vote_data

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
      expect InlineDatum(vote_results_data) =
        input.output.datum
      expect results_data_datum: ResultsBatchDatum =
        vote_results_data
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
  let merging_fn =
    fn(rd1: ResultsBatchDatum, rd2: ResultsBatchDatum) {
      ResultsBatchDatum {
        results: dict.union_with(
          left: rd1.results,
          right: rd2.results,
          with: results_conflict_resolver,
          compare: challenge_proposal.compare,
        ),
        iteration: new_iteration,
        batch_hash: #"",
      }
    }

  // assign new iteration
  let zero =
    ResultsBatchDatum {
      results: dict.new(),
      iteration: new_iteration,
      batch_hash: #"",
    }

  list.foldl(vote_results, merging_fn, zero)
}

fn do_count_votes(
  votes: List<Vote>,
  acc: Dict<ChallengeProposal, Result>,
) -> Dict<ChallengeProposal, Result> {
  when votes is {
    [] ->
      acc
    [h, ..t] -> {
      let fv: FlatVote =
        to_flat_vote(h)
      let c: Challenge =
        h.challenge
      let p: Proposal =
        h.proposal
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
  let empty =
    dict.new()
  do_count_votes(votes, empty)
}

fn voting_power_for_choice(v: Vote, c: Choice) -> Option<VotingPower> {
  list.find(
    [0, 1, 2],
    fn(choice: Choice) { list.and([v.choice == choice, c == choice]) },
  )
    |> option.map(fn(_) { v.voting_power })
}

fn to_flat_vote(v: Vote) -> FlatVote {
  let cp =
    ChallengeProposal { challenge: v.challenge, proposal: v.proposal }
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
