use acca/hash.{PubKeyHash} as ahash
use aiken/dict.{Dict}

pub type Namespace =
  String

pub type Category =
  Int

pub type Proposal =
  Int

pub type Vote {
  voter_key: PubKeyHash,
  namespace: Namespace,
  category: Category,
  proposal: Proposal,
}

//non_inclusion_proof: Proof,

pub type CategoryResults {
  namespace: Namespace,
  category: Category,
  results: Dict<Proposal, Int>,
  seq: Int,
}
/// Verify an Ed25519 signature from a associated verification key.
/// 
///
///
///
//    pub fn verify_ed25519_signature(
//            verification_key: ByteArray,
//            message: ByteArray,
//            signature: ByteArray,
//            ) -> ByteArray {
//        error
//    }
