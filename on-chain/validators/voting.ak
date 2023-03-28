use acca/collections/hashed_list
use acca/datums as adatums
use acca/dict as adict
use acca/validators
use aiken/builtin.{append_string}
use aiken/cbor
use aiken/dict.{Dict}
use aiken/hash.{Hash, Sha2_256, sha2_256}
use aiken/list
use aiken/transaction.{InlineDatum, Input, Output, ScriptContext, Spend}
use voting/challenge_proposal.{Challenge, ChallengeProposal, Proposal}
use voting/result.{Result}
use voting/results_batch_datum.{ResultsBatchDatum}
use voting/vote.{FlatVote, Vote}

// respective pubkey hashes for authorised parties
type VotingDatum {
}

type Redeemer {
  // Approve { batch_hash: MerkleTree<Vote> }
  CreateVoteBatch { off_chain_batch_hash: Hash<Sha2_256, Vote> }
  ReduceResults {
    off_chain_batch_hash: Hash<Sha2_256, ResultsBatchDatum>,
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
      CreateVoteBatch { off_chain_batch_hash } -> {
        let votes: List<Vote> =
          get_votes_from_inputs(inputs)
        let results: Dict<ChallengeProposal, Result> =
          count_votes(votes)
        let on_chain_results =
          ResultsBatchDatum {
            results,
            iteration: 0,
            batch_hash: off_chain_batch_hash,
          }
        list.and(
          [
            validators.any_output_contains_own_validator_address(
              inputs,
              outputs,
              output_reference,
            )?,
            validate_votes(votes, off_chain_batch_hash)?,
            results_must_match(sc.transaction.outputs, on_chain_results)?,
          ],
        )
      }
      ReduceResults { off_chain_batch_hash, iteration } -> {
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
        let on_chain_results =
          ResultsBatchDatum { ..result, batch_hash: off_chain_batch_hash }
        list.and(
          [
            validators.any_output_contains_own_validator_address(
              inputs,
              outputs,
              output_reference,
            )?,
            validate_results(all_results, off_chain_batch_hash)?,
            results_must_match(sc.transaction.outputs, on_chain_results)?,
          ],
        )
      }
      _ ->
        False
    }
  }
}

fn results_must_match(
  outputs: List<Output>,
  on_chain_results: ResultsBatchDatum,
) -> Bool {
  let result_checker =
    fn(o: Output) {
      expect InlineDatum(off_chain_results_data) =
        o.datum

      expect unsorted_off_chain_results: ResultsBatchDatum =
        off_chain_results_data

      let off_chain_results_dict =
        adict.sort(
          unsorted_off_chain_results.results,
          challenge_proposal.compare,
        )

      let off_chain_results =
        ResultsBatchDatum {
          ..unsorted_off_chain_results,
          results: off_chain_results_dict,
        }

      trace append_string(@"on_results:", cbor.diagnostic(on_chain_results))
      trace append_string(@"off_results:", cbor.diagnostic(off_chain_results))

      off_chain_results == on_chain_results
    }

  list.filter(outputs, adatums.is_inline_datum)
    |> list.any(result_checker)
}

fn validate_votes(
  on_chain_votes: List<Vote>,
  batch_hash: Hash<Sha2_256, Vote>,
) -> Bool {
  let hash_fn =
    fn(v: Vote) { sha2_256(builtin.serialise_data(v)) }

  let hashed_list =
    hashed_list.from(on_chain_votes, hash_fn)

  let on_chain_batch_hash =
    hashed_list.hash

  on_chain_batch_hash == batch_hash
}

fn validate_results(
  on_chain_results: List<ResultsBatchDatum>,
  off_chain_batch_hash: Hash<Sha2_256, ResultsBatchDatum>,
) -> Bool {
  let hash_fn =
    fn(r: ResultsBatchDatum) { sha2_256(builtin.serialise_data(r)) }

  let hashed_list =
    hashed_list.from(on_chain_results, hash_fn)

  let on_chain_batch_hash =
    hashed_list.hash

  off_chain_batch_hash == on_chain_batch_hash
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
        vote.to_flat_vote(h)
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
