use acca/collections/hashed_list
use aiken/builtin
use aiken/hash.{sha2_256}
use voting/datums.{Vote}

test vote_hash1() {
  let v0 =
    Vote(
      #"1b026f9b9b0d172dfc2fa4a450569fe8cf10a83ce2b616eb1dcd5f786ea199b0",
      3353282,
      449181773,
      2365581,
      0,
    )

  let v1 =
    Vote(
      #"41906483433636815ccf41357aa2cd1acf78ff11159f72a9a5b0819fb223414c",
      5215698,
      449181773,
      1932430,
      0,
    )

  let v2 =
    Vote(
      #"e040b132e36fae26c7b0d727c2e859b85ae56b7169b00e004f3d27cd0e3fd3fe",
      4654383,
      303167468,
      4988558,
      0,
    )

  let v3 =
    Vote(
      #"41bb39b472d3875b6e77da2841782e65effbf85447982cd15297ebc28a348809",
      253840,
      116457209,
      2297573,
      0,
    )

  let v4 =
    Vote(
      #"41bb39b472d3875b6e77da2841782e65effbf85447982cd15297ebc28a348809",
      253840,
      131249474,
      588916,
      0,
    )

  let v5 =
    Vote(
      #"41bb39b472d3875b6e77da2841782e65effbf85447982cd15297ebc28a348809",
      253840,
      303167468,
      3301549,
      0,
    )

  let v6 =
    Vote(
      #"1b026f9b9b0d172dfc2fa4a450569fe8cf10a83ce2b616eb1dcd5f786ea199b0",
      3353282,
      374145371,
      5342861,
      0,
    )

  let v7 =
    Vote(
      #"e040b132e36fae26c7b0d727c2e859b85ae56b7169b00e004f3d27cd0e3fd3fe",
      4654383,
      214998680,
      226531,
      0,
    )

  let v8 =
    Vote(
      #"41bb39b472d3875b6e77da2841782e65effbf85447982cd15297ebc28a348809",
      253840,
      51052969,
      3531179,
      0,
    )

  let v9 =
    Vote(
      #"e88e25351f3a23ebca1e4cadf7609b6a087f61d5252f870af5def7578e1bb54e",
      4620080,
      282723914,
      5558632,
      0,
    )
  let items =
    [v0, v1, v2, v3, v4, v5, v6, v7, v8, v9]

  let hash_fn =
    fn(v: Vote) { sha2_256(builtin.serialise_data(v)) }

  let hashed =
    hashed_list.from(items, hash_fn)

  #"d8257d0e22945d17961b4475b589d773b62402a3bf6ce9838c61712ae851d007" == hashed.hash
}
