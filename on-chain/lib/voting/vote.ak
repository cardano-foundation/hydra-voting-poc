use acca/hash.{PubKeyHash} as ahash
use voting/challenge_proposal.{Challenge, ChallengeProposal, Proposal}

pub type VotingPower =
  Int

pub type Choice =
  Int

pub type Vote {
  voter_key: PubKeyHash,
  voting_power: VotingPower,
  challenge: Challenge,
  proposal: Proposal,
  choice: Choice,
}

pub type FlatVote {
  key: ChallengeProposal,
  yes: VotingPower,
  no: VotingPower,
  abstain: VotingPower,
}
