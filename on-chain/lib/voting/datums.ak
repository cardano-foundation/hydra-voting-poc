use acca/hash.{PubKeyHash} as ahash
use aiken/dict.{Dict}

pub type Category =
  Int

pub type Proposal =
  Int

pub type Vote {
  voter_key: PubKeyHash,
  category: Category,
  proposal: Proposal,
}

//non_inclusion_proof: Proof,

pub type CategoryResults {
  category: Category,
  results: Dict<Proposal, Int>,
}
/// 
///
///
///
