use aiken/hash.{Blake2b_224, Hash}
use aiken/list
use aiken/transaction.{Input, OutputReference}
use aiken/transaction/credential.{Script, ScriptCredential}

pub type ValidatorHash =
  Hash<Blake2b_224, Script>

pub fn own_validator_script_address_hash(
  inputs: List<Input>,
  output_reference: OutputReference,
) -> ValidatorHash {
  expect Some(input) =
    list.find(inputs, fn(input) { input.output_reference == output_reference })
  expect ScriptCredential(hash) = input.output.address.payment_credential

  hash
}
