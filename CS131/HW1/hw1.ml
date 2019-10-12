

type ('nonterminal, 'terminal) symbol =
  | N of 'nonterminal
  | T of 'terminal;;


let rec subset a b = match a with
     | [] -> true
     | h::t -> if (List.mem h b) then subset t b
               else false;;

let set_union a b = match a with
    | [] -> b
    | _-> a@b;;

let rec set_intersection a b = match a with
    | [] -> []
    | h::t -> if List.exists(fun x -> x=h) b then h::set_intersection t b
              else set_intersection t b;; 

let rec set_diff a b = match a with
   | [] -> []
   | h::t -> if not (List.mem h b) then h::set_diff t b
             else set_diff t b;;



let rec equal_sets a b = match a with
    | _ -> subset a b && subset b a;;



let rec computed_fixed_point eq f x =
   if eq x (f x) then x
   else computed_fixed_point eq f (f x);;


(*gets the rules from the grammar*)
let rules g = snd g;;

(*check if it is a nonterminal or a terminal*)
let is_Nonterminal a = match a with
| N _ -> true
| _ -> false;;


(*the function is just like set_intersection function*)
let rec get_reachable a b =  match a with 
| [] -> []
| h::t -> if List.exists ( fun x-> x = (N(fst h)) ) b 
          then h::(get_reachable t b) else (get_reachable t b);;


(*filter out all the terminal symbols and append all the nonterminal symbols togehter*)
let create_rules g x = (N (fst g))::(List.filter is_Nonterminal ( List.concat(List.map snd (get_reachable (rules g) x ) ) ) );;


(* Sort of like repeating to create all those reachable rules except this time we dont filter out those 
   terminal symbols and maintain the list pattern *)
let get_rules g =  get_reachable (rules g) (computed_fixed_point (equal_sets) (create_rules g) [N (fst g)] );; 
let filter_reachable g = ( fst g , get_rules g);;
