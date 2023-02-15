use aiken/bytearray.{from_string}
use aiken/hash.{Hash, Sha2_256, sha2_256}
use aiken/list
use aiken/option.{choice, is_none}

// Construction of the merkle tree shouldn't be done by hand, but via
// 'from_list'.
/// A purely functional implementation of MerkleTrees that is suitable for
/// usage on-chain. Note, however, that the construction of 'MerkleTree' and
/// membership proofs are still expected to happen *off-chain* while only the
/// proof verification should be done on-chain.
///
/// Code ported to Aiken from Hydra (https://github.com/input-output-hk/hydra/blob/master/plutus-merkle-tree/src/Plutus/MerkleTree.hs)
/// A MerkleTree representation, suitable for on-chain manipulation.
pub type MerkleTree {
  Empty
  Leaf { value: String, hash: Hash<Sha2_256, ByteArray> }
  Node { hash: Hash<Sha2_256, ByteArray>, left: MerkleTree, right: MerkleTree }
}

pub type Proof =
  List<ProofItem>

pub type ProofItem {
  Left { hash: Hash<Sha2_256, ByteArray> }
  Right { hash: Hash<Sha2_256, ByteArray> }
}

/// Deconstruct a 'MerkleTree' back to a list of elements.
pub fn to_list(self: MerkleTree) -> List<String> {
  when self is {
    Empty -> []
    Leaf { value, .. } -> [value]
    Node { left, right, .. } -> list.concat(to_list(left), to_list(right))
  }
}

test to_list_1() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let items = [dog, cat, mouse]

  let mt = from_list(items)

  items == to_list(mt)
}

// Function returning a hash of a given Merkle Tree element
pub fn root_hash(self: MerkleTree) -> Hash<Sha2_256, ByteArray> {
  when self is {
    Empty -> #""
    Leaf { hash, .. } -> hash
    Node { hash, .. } -> hash
  }
}

test root_hash_1() {
  let dog = "dog"
  let items = [dog]

  let mt = from_list(items)

  let node_hash = hash(dog)

  root_hash(mt) == node_hash
}

test root_hash_3() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let items = [dog, cat, mouse]

  let mt = from_list(items)

  let node_hash = sha2_256(bytearray.concat(hash(cat), hash(mouse)))
  let rh = sha2_256(bytearray.concat(hash(dog), node_hash))

  expect Node { hash: root_hash, .. } = mt

  rh == root_hash
}

test root_hash_2() {
  let items = []
  let mt = from_list(items)

  let node_hash = #""

  root_hash(mt) == node_hash
}

/// Function atests whether two Merkle Tress are equal, this is the case when their root hashes match.
pub fn is_equal(left: MerkleTree, right: MerkleTree) -> Bool {
  root_hash(left) == root_hash(right)
}

/// Function returns a total numbers of leaves in the tree.
pub fn size(self: MerkleTree) -> Int {
  when self is {
    Empty -> 0
    Leaf{..} -> 1
    Node { left, right, .. } -> size(left) + size(right)
  }
}

test size_1() {
  let items = []
  let mt = from_list(items)
  size(mt) == 0
}

test size_2() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"

  let mt = from_list([dog, cat, mouse])
  size(mt) == 3
}

test size_3() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let items = [dog, cat, mouse]

  let mt = from_list(items)
  size(mt) == 3
}

test size_4() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let horse = "horse"
  let pig = "pig"
  let bull = "bull"

  let items = [dog, cat, mouse, horse, pig, bull]

  let mt = from_list(items)
  size(mt) == 6
}

fn combine_hash(left: Hash<alg, a>, right: Hash<alg, a>) -> Hash<alg, a> {
  sha2_256(bytearray.concat(left, right))
}

/// Function that returns whether merkle tree has any elements
pub fn is_empty(self: MerkleTree) -> Bool {
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
  let dog = "dog"

  let mt = Leaf { value: dog, hash: hash(dog) }

  is_empty(mt) == False
}

fn do_proof(
  self: MerkleTree,
  item_hash: Hash<Sha2_256, ByteArray>,
  proof: Proof,
) -> Option<Proof> {
  when self is {
    Empty -> None
    Leaf { hash, .. } ->
      if hash == item_hash {
        Some(proof)
      } else {
        None
      }
    Node { left, right, .. } -> {
      let rh = root_hash(right)
      let lh = root_hash(left)
      let go_left: Option<Proof> =
        do_proof(left, item_hash, list.push(proof, Right { hash: rh }))
      let go_right: Option<Proof> =
        do_proof(right, item_hash, list.push(proof, Left { hash: lh }))
      choice([go_left, go_right])
    }
  }
}

/// Construct a membership 'Proof' from an element and a 'MerkleTree'. Returns
/// 'None' if the element isn't a member of the tree to begin with.
/// Note function will return Some([]) in case root of the tree is also it's only one and only element
pub fn get_proof(self: MerkleTree, item: String) -> Option<Proof> {
  let empty: Proof = []
  do_proof(self, hash(item), empty)
}

test get_proof_1() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let horse = "horse"
  let pig = "pig"
  let bull = "bull"

  let items = [dog, cat, mouse, horse, pig, bull]
  let mt = from_list(items)

  let maybe_proof: Option<Proof> = get_proof(mt, "parrot")

  is_none(maybe_proof)
}

test get_proof_2() {
  let dog = "dog"

  let items = [dog]
  let mt = from_list(items)

  let maybe_proof: Option<Proof> = get_proof(mt, dog)

  expect Some(proof) = maybe_proof

  // when proof is empty list it actually means that root of the tree is in fact element
  proof == []
}

test get_proof_3() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"

  let items = [dog, cat, mouse]

  let mt = from_list(items)

  let node_hash = sha2_256(bytearray.concat(hash(cat), hash(mouse)))

  let maybe_proof: Option<Proof> = get_proof(mt, dog)

  expect Some(proof) = maybe_proof

  let size_match = list.length(proof) == 1

  expect Some(p1) = list.at(proof, 0)

  let h1: ByteArray = get_proof_item_value(p1)

  size_match && h1 == node_hash
}

test get_proof_4() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let horse = "horse"

  let items = [dog, cat, mouse, horse]

  let mt = from_list(items)

  let right_node_hash = sha2_256(bytearray.concat(hash(mouse), hash(horse)))
  let maybe_proof: Option<Proof> = get_proof(mt, dog)

  expect Some(proof) = maybe_proof

  let size_match = list.length(proof) == 2

  expect Some(p1) = list.at(proof, 0)
  expect Some(p2) = list.at(proof, 1)

  let h1: ByteArray = get_proof_item_value(p1)
  let h2: ByteArray = get_proof_item_value(p2)

  size_match && h1 == hash(cat) && h2 == right_node_hash
}

fn do_from_list(items: List<String>, len: Int) -> MerkleTree {
  when items is {
    [] -> Empty
    [value] -> Leaf { value, hash: sha2_256(bytearray.from_string(value)) }
    all -> {
      let cutoff: Int = len / 2
      let left =
        all
        |> list.take(cutoff)
        |> do_from_list(cutoff)
      let right =
        all
        |> list.drop(cutoff)
        |> do_from_list(len - cutoff)
      let hash = combine_hash(root_hash(left), root_hash(right))
      Node { hash, left, right }
    }
  }
}

/// Construct a 'MerkleTree' from a list of hashes.
/// Note that, while this operation is doable on-chain, it is expensive and
/// preferably done off-chain.
pub fn from_list(items: List<String>) -> MerkleTree {
  do_from_list(items, list.length(items))
}

test from_1() {
  let _a = -1
  let items = []
  let mt = from_list(items)

  Empty == mt
}

test from_2() {
  let dog = "dog"
  let items = [dog]
  let mt = from_list(items)

  Leaf { value: dog, hash: hash(dog) } == mt
}

test from_3() {
  let dog = "dog"
  let cat = "cat"
  let items = [dog, cat]

  let mt = from_list(items)

  let root_hash = sha2_256(bytearray.concat(hash(dog), hash(cat)))

  Node {
    hash: root_hash,
    left: Leaf { value: dog, hash: hash(dog) },
    right: Leaf { value: cat, hash: hash(cat) },
  } == mt
}

test from_4() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"

  let items = [dog, cat, mouse]
  let mt = from_list(items)

  let node_hash = sha2_256(bytearray.concat(hash(cat), hash(mouse)))
  let root_hash = sha2_256(bytearray.concat(hash(dog), node_hash))

  Node {
    hash: root_hash,
    left: Leaf { value: dog, hash: hash(dog) },
    right: Node {
      hash: node_hash,
      left: Leaf { value: cat, hash: hash(cat) },
      right: Leaf { value: mouse, hash: hash(mouse) },
    },
  } == mt
}

test from_5() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let horse = "horse"

  let items = [dog, cat, mouse, horse]
  let mt = from_list(items)

  let left_node_hash = sha2_256(bytearray.concat(hash(dog), hash(cat)))
  let right_node_hash = sha2_256(bytearray.concat(hash(mouse), hash(horse)))

  let root_hash = sha2_256(bytearray.concat(left_node_hash, right_node_hash))

  Node {
    hash: root_hash,
    left: Node {
      hash: left_node_hash,
      left: Leaf { value: dog, hash: hash(dog) },
      right: Leaf { value: cat, hash: hash(cat) },
    },
    right: Node {
      hash: right_node_hash,
      left: Leaf { value: mouse, hash: hash(mouse) },
      right: Leaf { value: horse, hash: hash(horse) },
    },
  } == mt
}

// Check whether a hashed element is part of a 'MerkleTree' using only its root hash
// and a 'Proof'. The proof is guaranteed to be in log(n) of the size of the
// tree, which is why we are interested in such data-structure in the first
// place.
pub fn member_from_hash(
  item_hash: Hash<Sha2_256, ByteArray>,
  root_hash: Hash<Sha2_256, ByteArray>,
  proof: Proof,
) -> Bool {
  when proof is {
    [] -> root_hash == item_hash
    [head, ..tail] ->
      when head is {
        Left { hash: l } ->
          member_from_hash(combine_hash(l, item_hash), root_hash, tail)
        Right { hash: r } ->
          member_from_hash(combine_hash(item_hash, r), root_hash, tail)
      }
  }
}

// Check whether an element is part of a 'MerkleTree' using only its root hash
// and a 'Proof'.
pub fn member(
  item: String,
  root_hash: Hash<Sha2_256, ByteArray>,
  proof: Proof,
) -> Bool {
  let item_hash = hash(item)
  member_from_hash(item_hash, root_hash, proof)
}

test member_1() {
  let dog = "dog"
  let items = [dog]
  let mt = from_list(items)

  let item = dog
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item)
  member(item: item, root_hash: rh, proof: proof)
}

test member_2() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let horse = "horse"

  let items = [dog, cat, mouse, horse]
  let mt = from_list(items)

  let item = cat
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item)
  member(item: item, root_hash: rh, proof: proof)
}

test member_3() {
  let dog = "dog"
  let cat = "cat"

  let items = [dog, cat]
  let mt = from_list(items)

  let item = cat
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item)
  member(item: item, root_hash: rh, proof: proof)
}

test member_4() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"

  let items = [dog, cat, mouse]
  let mt = from_list(items)

  let item = mouse
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item)
  member(item: item, root_hash: rh, proof: proof)
}

test member_5() {
  let dog = "dog"
  let cat = "cat"
  let mouse = "mouse"
  let horse = "horse"
  let pig = "pig"
  let bull = "bull"

  let items = [dog, cat, mouse, horse, pig, bull]
  let mt = from_list(items)

  let item = pig
  let rh = root_hash(mt)

  expect Some(proof) = get_proof(mt, item)
  member(item: item, root_hash: rh, proof: proof)
}

fn get_proof_item_value(proof_item: ProofItem) -> Hash<Sha2_256, ByteArray> {
  when proof_item is {
    Left(x) -> x
    Right(y) -> y
  }
}

fn hash(item: String) -> Hash<Sha2_256, ByteArray> {
  sha2_256(from_string(item))
}
