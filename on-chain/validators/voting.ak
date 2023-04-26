use acca/datums as adatums
use acca/dict as adict
use acca/list as alist
use acca/validators
use aiken/dict
use aiken/int
use aiken/list
use aiken/transaction.{InlineDatum, Input, Output, ScriptContext, Spend}
use voting/datums.{Category, CategoryResults, Vote}

type Redeemer {
  ValidateVote
  Close
}

validator(category: Category) {
  fn voting(
    _datum: Void,
    // first input
    redeemer: Redeemer,
    sc: ScriptContext,
  ) -> Bool {
    expect Spend(output_reference) =
      sc.purpose

    let inputs: List<Input> =
      sc.transaction.inputs

    let outputs: List<Output> =
      sc.transaction.outputs

    when redeemer is {
      ValidateVote -> {
        let vote: Vote =
          get_first_vote_from_inputs(inputs)

        let category_results: CategoryResults =
          get_first_category_result_from_inputs(inputs)

        let new_category_results: CategoryResults =
          add_vote(vote, category, category_results)

        list.and(
          [
            // online validate votes from our category
            (vote.category == category)?,
            // make sure we are spending to the same validator
            validators.any_output_contains_own_validator_address(
              inputs,
              outputs,
              output_reference,
            )?,
            // output must contain accumulated result, where vote is added to it
            output_contains_accumulated_results(outputs, new_category_results),
          ],
        )
      }

      Close ->
        False

      // makes sense to close only after to_slot
      // atime.must_start_after(sc.transaction.validity_range, to_slot)
      _ ->
        False
    }
  }
}

fn output_contains_accumulated_results(
  outputs: List<Output>,
  category_results: CategoryResults,
) -> Bool {
  let result_checker =
    fn(r: CategoryResults) { r == category_results }

  let mapper =
    fn(output) {
      if adatums.is_inline_datum(output) {
        expect InlineDatum(data) =
          output.datum

        expect cat_results: CategoryResults =
          data

        Some(cat_results)
      } else {
        None
      }
    }

  list.filter_map(outputs, mapper)
    |> list.any(result_checker)
}

fn add_vote(
  vote: Vote,
  category: Category,
  category_results: CategoryResults,
) -> CategoryResults {
  let p =
    vote.proposal
  let results =
    category_results.results

  let count =
    adict.get_or_default(results, p, 0)

  let new_results =
    dict.insert(self: results, key: p, value: count, compare: int.compare)

  CategoryResults { category, results: new_results }
}

// TODO what is there are more votes?
fn get_first_vote_from_inputs(inputs: List<Input>) -> Vote {
  list.filter_map(
    inputs,
    fn(input) {
      if adatums.is_inline_datum(input.output) {
        expect InlineDatum(data) =
          input.output.datum

        // TODO instance_of operator
        expect v: Vote =
          data

        Some(v)
      } else {
        None
      }
    },
  )
    |> alist.force_head
}

fn get_first_category_result_from_inputs(inputs: List<Input>) -> CategoryResults {
  list.filter_map(
    inputs,
    fn(input) {
      if adatums.is_inline_datum(input.output) {
        expect InlineDatum(data) =
          input.output.datum
        // TODO instance_of operator
        expect v: CategoryResults =
          data

        Some(v)
      } else {
        None
      }
    },
  )
    |> alist.force_head
}
