let accept_all string = Some string
let accept_empty_suffix = function
   | _::_ -> None
   | x -> Some x


type baby_nonterminals =
  | Awake | Eat | Play | Smile | Cry | Sleep


let baby_grammar =
  (Awake,
   function
     | Awake -> [[N Eat]; [N Smile]; [N Cry]; [N Sleep]]
     | Eat -> [[T"Nom Nom"]; [N Sleep];[N Play]]                                             
     | Play ->[[T "Bang Bang"; N Smile]]
     | Smile -> [[T "hehe"; N Sleep]] 
     | Cry -> [[T"AHHHH!"]; [T "hooooo"]]
     | Sleep ->[[T "zzzz"]; [T "Sleep talking"]]);;


let make_matcher_test0 =
  ((make_matcher baby_grammar accept_all ["zzzz"]) = Some [] )

let make_matcher_test1 =
  ((make_matcher baby_grammar accept_all ["zzzz"; "hehe"]) = Some ["hehe"])


let parser_tree_leaves_test0 =
  (parse_tree_leaves (Node ("*", [Node ("+", [Leaf 2; Leaf 6]); Node ("+", [Leaf 3; Leaf 7]); Node ("+", [Leaf 4; Leaf 5])]))
   = [2; 6; 3;7;4;5])

let baby_frag = ["Bang Bang"; "hehe"; "Sleep talking"]


let make_parser_tree_test1 = Some 
(Node (Awake,
   [Node (Eat,
     [Node (Play,
       [Leaf "Bang Bang";
        Node (Smile,
         [Leaf "hehe"; Node (Eat, [Node (Sleep, [Leaf "Sleep talking"])])])])])]))



let make_parser_test1 = (match (make_parser baby_grammar baby_frag) with
    | Some tree -> (parse_tree_leaves tree = baby_frag)
    | _ -> false) = true
