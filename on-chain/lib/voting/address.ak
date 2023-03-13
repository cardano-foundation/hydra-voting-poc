use aiken/hash.{Hash, Sha2_256}
use aiken/transaction/credential.{VerificationKey}

pub type PubKeyHash =
  Hash<Sha2_256, VerificationKey>
