use aiken/bytearray.{from_string}
use aiken/hash.{Hash, Sha2_256, sha2_256}
use aiken/list
use aiken/option.{choice, is_none}

pub type MerkleElement<a> {
  Empty
  Leaf { hash: Hash<Sha2_256, ByteArray> }
  Node {
    hash: Hash<Sha2_256, ByteArray>,
    left: MerkleElement<a>,
    right: MerkleElement<a>,
  }
}

pub type Proof =
  List<ProofItem>

pub type ProofItem {
  Left { hash: Hash<Sha2_256, ByteArray> }
  Right { hash: Hash<Sha2_256, ByteArray> }
}

// Function returning a hash of a given Merkle Tree element
pub fn root_hash(self: MerkleElement<a>) -> Hash<Sha2_256, ByteArray> {
  when self is {
    Empty -> #""
    Leaf { hash } -> hash
    Node { hash, .. } -> hash
  }
}

test root_hash_1() {
  let dog = @"dog"
  let items = [dog]
  let serialise_fn = create_string_item_serialise_fn()

  let mt = from_list(items, serialise_fn)

  let node_hash = sha2_256(serialise_fn(dog))

  root_hash(mt) == node_hash
}

test root_hash_3() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let items = [dog, cat, mouse]
  let serialise_fn = create_string_item_serialise_fn()

  let mt = from_list(items, serialise_fn)

  let node_hash =
    sha2_256(
      bytearray.concat(
        sha2_256(serialise_fn(cat)),
        sha2_256(serialise_fn(mouse)),
      ),
    )
  let rh = sha2_256(bytearray.concat(sha2_256(serialise_fn(dog)), node_hash))

  expect Node { hash: root_hash, .. } = mt

  rh == root_hash
}

test root_hash_2() {
  let items = []
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let node_hash = #""

  root_hash(mt) == node_hash
}

/// Function atests whether two Merkle Tress are equal, this is the case when their root hashes match.
pub fn is_equal(left: MerkleElement<a>, right: MerkleElement<a>) -> Bool {
  root_hash(left) == root_hash(right)
}

/// Function returns a total numbers of leaves in the tree.
pub fn size(self: MerkleElement<a>) -> Int {
  when self is {
    Empty -> 0
    Leaf{..} -> 1
    Node { left, right, .. } -> size(left) + size(right)
  }
}

test size_1() {
  let items = []
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)
  size(mt) == 0
}

test size_2() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let serialise_fn = create_string_item_serialise_fn()

  let mt = from_list([dog, cat, mouse], serialise_fn)
  size(mt) == 3
}

test size_3() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let items = [dog, cat, mouse]
  let serialise_fn = create_string_item_serialise_fn()

  let mt = from_list(items, serialise_fn)
  size(mt) == 3
}

test size_4() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"
  let pig = @"pig"
  let bull = @"bull"

  let serialise_fn = create_string_item_serialise_fn()
  let items = [dog, cat, mouse, horse, pig, bull]

  let mt = from_list(items, serialise_fn)
  size(mt) == 6
}

fn combine_hash(
  left: Hash<Sha2_256, a>,
  right: Hash<Sha2_256, a>,
) -> Hash<Sha2_256, a> {
  sha2_256(bytearray.concat(left, right))
}

/// Function that returns whether merkle tree has any elements
pub fn is_empty(self: MerkleElement<a>) -> Bool {
  when self is {
    Empty -> True
    _ -> False
  }
}

test is_empty_1() {
  let mt = Empty

  is_empty(mt)
}

test is_empty_2() {
  let dog = @"dog"
  let hash = create_string_item_serialise_fn()

  let mt = Leaf { hash: hash(dog) }

  is_empty(mt) == False
}

fn do_proof(
  self: MerkleElement<a>,
  item_hash: Hash<Sha2_256, ByteArray>,
  proof: Proof,
  serialise_fn: fn(a) -> ByteArray,
) -> Option<Proof> {
  when self is {
    Empty -> None
    Leaf { hash } ->
      if hash == item_hash {
        Some(proof)
      } else {
        None
      }
    Node { left, right, .. } -> {
      let rh = root_hash(right)
      let lh = root_hash(left)
      let go_left: Option<Proof> =
        do_proof(
          left,
          item_hash,
          list.push(proof, Right { hash: rh }),
          serialise_fn,
        )
      let go_right: Option<Proof> =
        do_proof(
          right,
          item_hash,
          list.push(proof, Left { hash: lh }),
          serialise_fn,
        )
      choice([go_left, go_right])
    }
  }
}

/// Construct a membership 'Proof' from an element and a 'MerkleTree'. Returns
/// 'None' if the element isn't a member of the tree to begin with.
/// Note function will return Some([]) in case root of the tree is also it's only one and only element
pub fn get_proof(
  self: MerkleElement<a>,
  item: a,
  serialise_fn: fn(a) -> ByteArray,
) -> Option<Proof> {
  let empty: Proof = []

  do_proof(self, sha2_256(serialise_fn(item)), empty, serialise_fn)
}

test get_proof_1() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"
  let pig = @"pig"
  let bull = @"bull"

  let items = [dog, cat, mouse, horse, pig, bull]
  let serialise_fn = create_string_item_serialise_fn()

  let mt = from_list(items, serialise_fn)
  let serialise_fn = create_string_item_serialise_fn()

  let maybe_proof: Option<Proof> = get_proof(mt, @"parrot", serialise_fn)

  is_none(maybe_proof)
}

test get_proof_2() {
  let dog = @"dog"

  let items = [dog]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let maybe_proof: Option<Proof> = get_proof(mt, dog, serialise_fn)

  expect Some(proof) = maybe_proof

  // when proof is empty list it actually means that root of the tree is in fact element
  proof == []
}

test get_proof_3() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"

  let items = [dog, cat, mouse]

  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let node_hash =
    sha2_256(
      bytearray.concat(
        sha2_256(serialise_fn(cat)),
        sha2_256(serialise_fn(mouse)),
      ),
    )

  let maybe_proof: Option<Proof> = get_proof(mt, dog, serialise_fn)

  expect Some(proof) = maybe_proof

  let size_match = list.length(proof) == 1

  expect Some(p1) = list.at(proof, 0)

  let h1: ByteArray = get_proof_item_value(p1)

  size_match && h1 == node_hash
}

test get_proof_4() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"

  let items = [dog, cat, mouse, horse]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let right_node_hash =
    sha2_256(
      bytearray.concat(
        sha2_256(serialise_fn(mouse)),
        sha2_256(serialise_fn(horse)),
      ),
    )
  let maybe_proof: Option<Proof> = get_proof(mt, dog, serialise_fn)

  expect Some(proof) = maybe_proof

  let size_match = list.length(proof) == 2

  expect Some(p1) = list.at(proof, 0)
  expect Some(p2) = list.at(proof, 1)

  let h1: ByteArray = get_proof_item_value(p1)
  let h2: ByteArray = get_proof_item_value(p2)

  size_match && h1 == sha2_256(serialise_fn(cat)) && h2 == right_node_hash
}

fn do_from_list(
  items: List<a>,
  len: Int,
  serialise_fn: fn(a) -> ByteArray,
) -> MerkleElement<a> {
  when items is {
    [] -> Empty
    [item] -> {
      let hashed_item = sha2_256(serialise_fn(item))
      Leaf { hash: hashed_item }
    }
    all -> {
      let cutoff: Int = len / 2
      let left =
        all
        |> list.take(cutoff)
        |> do_from_list(cutoff, serialise_fn)
      let right =
        all
        |> list.drop(cutoff)
        |> do_from_list(len - cutoff, serialise_fn)
      let hash = combine_hash(root_hash(left), root_hash(right))
      Node { hash, left, right }
    }
  }
}

/// Construct a 'MerkleTree' from a list of elements.
/// Note that, while this operation is doable on-chain, it is expensive and
/// preferably done off-chain.
pub fn from_list(
  items: List<a>,
  serialise_fn: fn(a) -> ByteArray,
) -> MerkleElement<a> {
  do_from_list(items, list.length(items), serialise_fn)
}

test from_1() {
  let _a = -1
  let items = []
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  Empty == mt
}

test from_2() {
  let dog = @"dog"
  let items = [dog]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  Leaf { hash: sha2_256(serialise_fn(dog)) } == mt
}

test from_3() {
  let dog = @"dog"
  let cat = @"cat"
  let items = [dog, cat]

  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let root_hash =
    sha2_256(
      bytearray.concat(sha2_256(serialise_fn(dog)), sha2_256(serialise_fn(cat))),
    )

  Node {
    hash: root_hash,
    left: Leaf { hash: sha2_256(serialise_fn(dog)) },
    right: Leaf { hash: sha2_256(serialise_fn(cat)) },
  } == mt
}

test from_4() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"

  let items = [dog, cat, mouse]
  let serialise_fn = create_string_item_serialise_fn()

  let mt = from_list(items, serialise_fn)

  let node_hash =
    sha2_256(
      bytearray.concat(
        sha2_256(serialise_fn(cat)),
        sha2_256(serialise_fn(mouse)),
      ),
    )
  let root_hash =
    sha2_256(bytearray.concat(sha2_256(serialise_fn(dog)), node_hash))

  Node {
    hash: root_hash,
    left: Leaf { hash: sha2_256(serialise_fn(dog)) },
    right: Node {
      hash: node_hash,
      left: Leaf { hash: sha2_256(serialise_fn(cat)) },
      right: Leaf { hash: sha2_256(serialise_fn(mouse)) },
    },
  } == mt
}

test from_5() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"

  let serialise_fn = create_string_item_serialise_fn()

  let items = [dog, cat, mouse, horse]
  let mt = from_list(items, serialise_fn)

  let left_node_hash =
    sha2_256(
      bytearray.concat(sha2_256(serialise_fn(dog)), sha2_256(serialise_fn(cat))),
    )
  let right_node_hash =
    sha2_256(
      bytearray.concat(
        sha2_256(serialise_fn(mouse)),
        sha2_256(serialise_fn(horse)),
      ),
    )

  let root_hash = sha2_256(bytearray.concat(left_node_hash, right_node_hash))

  Node {
    hash: root_hash,
    left: Node {
      hash: left_node_hash,
      left: Leaf { hash: sha2_256(serialise_fn(dog)) },
      right: Leaf { hash: sha2_256(serialise_fn(cat)) },
    },
    right: Node {
      hash: right_node_hash,
      left: Leaf { hash: sha2_256(serialise_fn(mouse)) },
      right: Leaf { hash: sha2_256(serialise_fn(horse)) },
    },
  } == mt
}

// fn do_from_hashes_list(
//   items: List<Hash<Sha2_256, a>>,
//   len: Int,
// ) -> MerkleElement<a> {
//   when items is {
//     [] -> Empty
//     [hashed_item] -> Leaf { hash: hashed_item }
//     all -> {
//       let cutoff: Int = len / 2
//       let left =
//         all
//         |> list.take(cutoff)
//         |> do_from_hashes_list(cutoff)
//       let right =
//         all
//         |> list.drop(cutoff)
//         |> do_from_hashes_list(len - cutoff)
//       let hash = combine_hash(root_hash(left), root_hash(right))
//       Node { hash, left, right }
//     }
//   }
// }

// /// Construct a 'MerkleTree' from a list of hashes.
// /// Note that, while this operation is doable on-chain, it is expensive and
// /// preferably done off-chain.
// pub fn from_hashes_list(items: List<Hash<Sha2_256, a>>) -> MerkleElement<a> {
//   do_from_hashes_list(items, list.length(items))
// }

// test from_hashes_list_5() {
//   let dog = @"dog"
//   let cat = @"cat"
//   let mouse = @"mouse"
//   let horse = @"horse"

//   let serialise_fn = create_string_item_serialise_fn()

//   let items = [dog, cat, mouse, horse]
//   let hashes_items = list.map(items, fn(item) { sha2_256(serialise_fn(item)) })

//   let mt = from_hashes_list(hashes_items)

//   let left_node_hash =
//     sha2_256(
//       bytearray.concat(sha2_256(serialise_fn(dog)), sha2_256(serialise_fn(cat))),
//     )
//   let right_node_hash =
//     sha2_256(
//       bytearray.concat(
//         sha2_256(serialise_fn(mouse)),
//         sha2_256(serialise_fn(horse)),
//       ),
//     )

//   let root_hash = sha2_256(bytearray.concat(left_node_hash, right_node_hash))

//   Node {
//     hash: root_hash,
//     left: Node {
//       hash: left_node_hash,
//       left: Leaf { hash: sha2_256(serialise_fn(dog)) },
//       right: Leaf { hash: sha2_256(serialise_fn(cat)) },
//     },
//     right: Node {
//       hash: right_node_hash,
//       left: Leaf { hash: sha2_256(serialise_fn(mouse)) },
//       right: Leaf { hash: sha2_256(serialise_fn(horse)) },
//     },
//   } == mt
// }

// Check whether a hashed element is part of a 'MerkleTree' using only its root hash
// and a 'Proof'. The proof is guaranteed to be in log(n) of the size of the
// tree, which is why we are interested in such data-structure in the first
// place.
pub fn member_from_hash(
  item_hash: Hash<Sha2_256, a>,
  root_hash: Hash<Sha2_256, a>,
  proof: Proof,
  serialise_fn: fn(a) -> ByteArray,
) -> Bool {
  when proof is {
    [] -> root_hash == item_hash
    [head, ..tail] ->
      when head is {
        Left { hash: l } ->
          member_from_hash(
            combine_hash(l, item_hash),
            root_hash,
            tail,
            serialise_fn,
          )
        Right { hash: r } ->
          member_from_hash(
            combine_hash(item_hash, r),
            root_hash,
            tail,
            serialise_fn,
          )
      }
  }
}

// Check whether an element is part of a 'MerkleTree' using only its root hash
// and a 'Proof'.
pub fn member(
  item: a,
  root_hash: Hash<Sha2_256, ByteArray>,
  proof: Proof,
  serialise_fn: fn(a) -> ByteArray,
) -> Bool {
  let item_hash = sha2_256(serialise_fn(item))
  member_from_hash(item_hash, root_hash, proof, serialise_fn)
}

test member_1() {
  let dog = @"dog"
  let items = [dog]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let item = dog
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item, serialise_fn)
  member(item: item, root_hash: rh, proof: proof, serialise_fn: serialise_fn)
}

test member_2() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"

  let items = [dog, cat, mouse, horse]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let item = cat
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item, serialise_fn)
  member(item: item, root_hash: rh, proof: proof, serialise_fn: serialise_fn)
}

test member_3() {
  let dog = @"dog"
  let cat = @"cat"

  let items = [dog, cat]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let item = cat
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item, serialise_fn)
  member(item: item, root_hash: rh, proof: proof, serialise_fn: serialise_fn)
}

test member_4() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"

  let items = [dog, cat, mouse]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let item = mouse
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item, serialise_fn)
  member(item: item, root_hash: rh, proof: proof, serialise_fn: serialise_fn)
}

test member_5() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"
  let pig = @"pig"
  let bull = @"bull"

  let items = [dog, cat, mouse, horse, pig, bull]
  let serialise_fn = create_string_item_serialise_fn()
  let mt = from_list(items, serialise_fn)

  let item = pig
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item, serialise_fn)
  member(item: item, root_hash: rh, proof: proof, serialise_fn: serialise_fn)
}

test member_6() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"
  let pig = @"pig"
  let bull = @"bull"

  let serialise_fn = create_string_item_serialise_fn()

  let items = [dog, cat, mouse, horse, pig, bull]
  let mt = from_list(items, serialise_fn)

  let item = @"parrot"

  is_none(get_proof(mt, item, serialise_fn))
}

pub fn member_from_tree(
  tree: MerkleElement<a>,
  item: a,
  serialise_fn: fn(a) -> ByteArray,
) -> Bool {
  let proof: Option<Proof> = get_proof(tree, item, serialise_fn)
  let rh = root_hash(tree)

  when proof is {
    Some(p) -> member(item, rh, p, serialise_fn)
    None -> False
  }
}

test member_from_tree_1() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"
  let pig = @"pig"
  let bull = @"bull"

  let serialise_fn = create_string_item_serialise_fn()

  let items = [dog, cat, mouse, horse, pig, bull]
  let mt = from_list(items, serialise_fn)

  let item = pig

  member_from_tree(tree: mt, item: item, serialise_fn: serialise_fn)
}

test member_from_tree_2() {
  let dog = @"dog"
  let cat = @"cat"
  let mouse = @"mouse"
  let horse = @"horse"
  let pig = @"pig"
  let bull = @"bull"

  let serialise_fn = create_string_item_serialise_fn()

  let items = [dog, cat, mouse, horse, pig, bull]
  let mt = from_list(items, serialise_fn)

  let item = @"parrot"

  member_from_tree(tree: mt, item: item, serialise_fn: serialise_fn) == False
}

fn get_proof_item_value(proof_item: ProofItem) -> Hash<Sha2_256, ByteArray> {
  when proof_item is {
    Left(hash) -> hash
    Right(hash) -> hash
  }
}

// needed only for tests
fn create_string_item_serialise_fn() -> fn(String) -> ByteArray {
  fn(x: String) { from_string(x) }
}
