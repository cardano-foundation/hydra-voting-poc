use acca/collections/hashed_list
use aiken/builtin
use aiken/hash.{sha2_256}
use voting/datums.{Vote}

test vote_hash1() {
  let v1 =
    Vote(
      #"13d63c9b04478d2fc3e0f9553f79094a83111e3ad03f3f779f5ba6a6f37ac647",
      5540767,
      258650178,
      2888851,
      0,
    )

  let v2 =
    Vote(
      #"b68ebf15fd6e47df43fa2c11c2735a4e5e359cf3af9afb93b5f732ff9bef2c1f",
      2604515,
      374145371,
      2675120,
      0,
    )

  let v3 =
    Vote(
      #"41bb39b472d3875b6e77da2841782e65effbf85447982cd15297ebc28a348809",
      253840,
      72140352,
      3057554,
      0,
    )

  let v4 =
    Vote(
      #"e040b132e36fae26c7b0d727c2e859b85ae56b7169b00e004f3d27cd0e3fd3fe",
      4654383,
      59549376,
      3637300,
      1,
    )

  let v5 =
    Vote(
      #"1b026f9b9b0d172dfc2fa4a450569fe8cf10a83ce2b616eb1dcd5f786ea199b0",
      3353282,
      214998680,
      4269173,
      1,
    )

  let v6 =
    Vote(
      #"e88e25351f3a23ebca1e4cadf7609b6a087f61d5252f870af5def7578e1bb54e",
      4620080,
      116457209,
      617559,
      2,
    )

  let v7 =
    Vote(
      #"13d63c9b04478d2fc3e0f9553f79094a83111e3ad03f3f779f5ba6a6f37ac647",
      5540767,
      214998680,
      1123389,
      0,
    )

  let v8 =
    Vote(
      #"6cf6f14a83145ee447c165de5b4f529f66bd5522b26aa8c038e352ed0b6a41bc",
      881560,
      258650178,
      442754,
      2,
    )

  let v9 =
    Vote(
      #"b68ebf15fd6e47df43fa2c11c2735a4e5e359cf3af9afb93b5f732ff9bef2c1f",
      2604515,
      72140352,
      5776693,
      2,
    )

  let v10 =
    Vote(
      #"e88e25351f3a23ebca1e4cadf7609b6a087f61d5252f870af5def7578e1bb54e",
      4620080,
      59549376,
      1247152,
      1,
    )
  let items =
    [v1, v2, v3, v4, v5, v6, v7, v8, v9, v10]

  let hash_fn =
    fn(v: Vote) { sha2_256(builtin.serialise_data(v)) }

  let hashed =
    hashed_list.from(items, hash_fn)

  #"dc8d0ec45f9f57ad93834c87c6027cba01f401d91e52e4294d34393fb2c1ed52" == hashed.hash
}
