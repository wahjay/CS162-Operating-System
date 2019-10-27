
let my_subset_test0 = subset [3;2] [1;2;3]
let my_subset_test1 = subset [] []
let my_subset_test2 = not (subset [1;3;7] [4;1;3;2])

let my_equal_sets_test0 = equal_sets [2;4] [4;2;4]
let my_equal_sets_test1 = equal_sets [] [] 
let my_equal_sets_test2 = not (equal_sets [1;3;4] [1;1;1])

let my_set_union_test0 = equal_sets (set_union [] [1;2;3]) [1;2;3]
let my_set_union_test1 = equal_sets (set_union [3;1;3] [1;2;3]) [3;2;1]
let my_set_union_test2 = equal_sets (set_union [1;3] [2]) (set_union [2;3] [1])

let my_set_intersection_test0 =
  equal_sets (set_intersection [1] [1;2;3]) [1]
let my_set_intersection_test1 =
  equal_sets (set_intersection [3;1;3;2] [1;2;3]) [1;3;2]
let my_set_intersection_test2 =
  equal_sets (set_intersection [] [4]) []

let my_set_diff_test0 = equal_sets (set_diff [1;3] [4;3;]) [1]
let my_set_diff_test1 = equal_sets (set_diff [] []) []
let my_set_diff_test2 = equal_sets (set_diff [4;3;1] [2;5]) [1;3;4]
let my_set_diff_test3 = equal_sets (set_diff [] [4;3;2;5]) []

let my_computed_fixed_point_test0 =
  computed_fixed_point (=) (fun x -> x / 2) 5000000000 = 0
let my_computed_fixed_point_test1 =
  computed_fixed_point (=) (fun x -> x **2.) 2. = infinity
let my_computed_fixed_point_test2 =
  computed_fixed_point (=) sqrt 200. = 1.
let my_computed_fixed_point_test3 =
  computed_fixed_point (=) (fun x -> x **2.) 1. = 1.
let my_computed_fixed_point_test4 =
  computed_fixed_point (=) (fun x -> x **2.) 0.1 = 0.



type baby_nonterminals =
  | Awake | Eat | Play | Smile | Cry | Sleep

let baby_grammar =
  Awake,
  [Eat, [T"Nom Nom"];
   Play, [T "Bang Bang"];
   Smile, [T "hehe"];
   Cry, [T"aaaaaaa!"];
   Sleep, [T "zzzzz"];
   Eat, [N Play];
   Eat, [N Smile];
   Eat, [N Cry];
   Awake, [N Eat];
   Awake, [N Sleep; T","; N Awake]]

let my_baby_test0 =
  filter_reachable baby_grammar = baby_grammar

let my_baby_test1 =
  filter_reachable (Eat, List.tl (snd baby_grammar)) =
    (Eat,
     [Play, [T "Bang Bang"]; Smile, [T "hehe"]; Cry, [T "aaaaaaa!"];
      Eat, [N Play]; Eat, [N Smile]; Eat, [N Cry]])

let my_baby_test2 =
  filter_reachable (Sleep, snd baby_grammar) = (Sleep, [Sleep, [T "zzzzz"]])
