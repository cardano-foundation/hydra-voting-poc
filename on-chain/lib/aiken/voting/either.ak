pub type Either<a, b> {
  Left { value: a }
  Right { value: b }
}

test either_1() {
  let l: Either<Int, String> = Left { value: 1 }
  let r: Either<Int, String> = Right { value: "" }

  l == Left { value: 1 } && r == Right { value: "" }
}

pub fn left(self: Either<a, b>) {
  expect Left { value } = self

  value
}

test left_1() {
  left(Left { value: 1 }) == 1
}

pub fn right(self: Either<a, b>) {
  expect Right { value } = self

  value
}

test right_1() {
  right(Right { value: 1 }) == 1
}

pub fn contains(self: Either<a, b>, item: b) -> Bool {
  when self is {
    Left(_) -> False
    Right(x) -> x == item
  }
}

test contains_1() {
  contains(Left { value: 1 }, 1) == False
}

test contains_2() {
  contains(Left { value: 1 }, 0) == False
}

test contains_4() {
  contains(Right { value: 1 }, 1)
}

pub fn fold(
  self: Either<a, b>,
  with_left: fn(a) -> c,
  with_right: fn(b) -> c,
) -> c {
  when self is {
    Left(a) -> with_left(a)
    Right(b) -> with_right(b)
  }
}

test fold_1() {
  let either: Either<Int, Int> = Left { value: 1 }

  fold(self: either, with_left: fn(x) { x > 0 }, with_right: fn(y) { y < 0 }) == True
}

test fold_2() {
  let either: Either<Int, Int> = Right { value: 1 }

  fold(self: either, with_left: fn(x) { x > 0 }, with_right: fn(y) { y < 0 }) == False
}

pub fn to_option(self: Either<a, b>) -> Option<b> {
  when self is {
    Left(_) -> None
    Right(x) -> Some(x)
  }
}

test to_option_1() {
  to_option(Left { value: 1 }) == None
}

test to_option_2() {
  to_option(Right { value: 1 }) == Some(1)
}

pub fn to_list(self: Either<a, b>) -> List<b> {
  when self is {
    Left(_) -> []
    Right(x) -> [x]
  }
}

test to_list_1() {
  to_list(Left { value: 1 }) == []
}

test to_list_2() {
  to_list(Right { value: 1 }) == [1]
}

pub fn is_left(self: Either<a, b>) -> Bool {
  when self is {
    Left(_) -> True
    _ -> False
  }
}

pub fn is_right(self: Either<a, b>) -> Bool {
  when self is {
    Right(_) -> True
    _ -> False
  }
}

pub fn exists(self: Either<a, b>, with: fn(b) -> Bool) -> Bool {
  when self is {
    Left(_) -> False
    Right(y) ->
      if with(y) {
        True
      } else {
        False
      }
  }
}

test exists_1() {
  exists(Left { value: 1 }, fn(x) { x }) == False
}

test exists_2() {
  exists(Right { value: 1 }, fn(x) { x > 0 }) == True
}

test exists_3() {
  exists(Right { value: -1 }, fn(x) { x > 0 }) == False
}

pub fn swap(self: Either<a, b>) -> Either<b, a> {
  when self is {
    Left(x) -> Right(x)
    Right(y) -> Left(y)
  }
}

test swap_1() {
  swap(Right { value: 1 }) == Left { value: 1 }
}

test swap_2() {
  swap(Left { value: 1 }) == Right { value: 1 }
}

pub fn map(self: Either<a, b>, with: fn(b) -> c) {
  when self is {
    Left(x) -> Left(x)
    Right(y) -> Right(with(y))
  }
}

test map_1() {
  map(Right { value: 12 }, fn(x) { x + 1 }) == Right { value: 13 }
}

test map_2() {
  map(Left { value: 12 }, fn(x) { x + 1 }) == Left { value: 12 }
}
