use aiken/dict.{Dict}
use voting/challenge_proposal.{ChallengeProposal}
use voting/result.{Result}
use voting/results_batch_datum.{ResultsBatchDatum}

test compare_results_0() {
  let left: Dict<ChallengeProposal, Result> =
    dict.new()
      |> dict.insert(
      ChallengeProposal(1, 1),
      Result(1, 0, 0),
      challenge_proposal.compare,
    )

  let right: Dict<ChallengeProposal, Result> =
    dict.new()
      |> dict.insert(
      ChallengeProposal(1, 1),
      Result(1, 0, 0),
      challenge_proposal.compare,
    )

  results_batch_datum.compare_results(left, right) == Equal
}

test compare_results_1() {
  let left: Dict<ChallengeProposal, Result> =
    dict.new()
      |> dict.insert(
      ChallengeProposal(1, 1),
      Result(1, 0, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 2),
      Result(1, 1, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 3),
      Result(0, 0, 0),
      challenge_proposal.compare,
    )

  let right: Dict<ChallengeProposal, Result> =
    dict.new()
      |> dict.insert(
      ChallengeProposal(1, 1),
      Result(1, 0, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 3),
      Result(0, 0, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 2),
      Result(1, 1, 0),
      challenge_proposal.compare,
    )

  results_batch_datum.compare_results(left, right) == Equal
}

test compare_results_batch_datum_0() {
  let left: Dict<ChallengeProposal, Result> =
    dict.new()
      |> dict.insert(
      ChallengeProposal(1, 1),
      Result(1, 0, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 2),
      Result(1, 1, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 3),
      Result(0, 0, 0),
      challenge_proposal.compare,
    )

  let right: Dict<ChallengeProposal, Result> =
    dict.new()
      |> dict.insert(
      ChallengeProposal(1, 1),
      Result(1, 0, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 3),
      Result(0, 0, 0),
      challenge_proposal.compare,
    )
      |> dict.insert(
      ChallengeProposal(1, 2),
      Result(1, 1, 0),
      challenge_proposal.compare,
    )

  results_batch_datum.compare(
    ResultsBatchDatum(left, "", 1),
    ResultsBatchDatum(right, "", 1),
  ) == Equal
}
