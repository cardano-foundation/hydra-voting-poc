use acca/ordering as aordering
use aiken/builtin
use aiken/bytearray
use aiken/dict.{Dict}
use aiken/int
use aiken/list
use voting/challenge_proposal.{ChallengeProposal}
use voting/result.{Result}

pub type ResultsBatchDatum {
  results: Dict<ChallengeProposal, Result>,
  batch_hash: ByteArray,
  iteration: Int,
}

pub fn compare(left: ResultsBatchDatum, right: ResultsBatchDatum) -> Ordering {
  aordering.force_reduce(
    [
      int.compare(left.iteration, right.iteration),
      bytearray.compare(left.batch_hash, right.batch_hash),
      compare_results(left.results, right.results),
    ],
  )
}

type Compound =
  (ChallengeProposal, Result)

pub fn compare_results(
  left: Dict<ChallengeProposal, Result>,
  right: Dict<ChallengeProposal, Result>,
) -> Ordering {
  let comparer: fn(Compound, Compound) -> Ordering =
    fn(l, r) {
      aordering.compare_tuple(l, r, challenge_proposal.compare, result.compare)
    }

  let left_data =
    dict.to_list(left) |> list.sort(comparer) |> builtin.serialise_data

  let right_data =
    dict.to_list(right) |> list.sort(comparer) |> builtin.serialise_data

  bytearray.compare(left_data, right_data)
}
