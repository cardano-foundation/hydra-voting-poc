use acca/list as alist
use aiken/int
use aiken/list
use voting/vote.{VotingPower}

pub type Result {
  yes: VotingPower,
  no: VotingPower,
  abstain: VotingPower,
}

pub fn compare(left: Result, right: Result) -> Ordering {
  let equal =
    list.and(
      [
        left.yes == right.yes,
        left.no == right.no,
        left.abstain == right.abstain,
      ],
    )

  if equal {
    Equal
  } else {
    let left_sum =
      alist.sum([left.yes, left.no, left.abstain])
    let right_sum =
      alist.sum([right.yes, right.no, right.abstain])

    int.compare(left_sum, right_sum)
  }
}
