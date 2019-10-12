


type ('nonterminal, 'terminal) symbol =
  | N of 'nonterminal
  | T of 'terminal;;


type ('nonterminal, 'terminal) parse_tree =
  | Node of 'nonterminal * ('nonterminal, 'terminal) parse_tree list
  | Leaf of 'terminal;;


(* convert_grammar *)

let rec get_reachable a b =  match a with 
| [] -> []
| h::t -> if List.exists ( fun x-> x = (fst h) ) b then (snd h)::(get_reachable t b) else (get_reachable t b);;


let convert gram1 = function
   | x -> get_reachable gram1 [x];;

let convert_grammar gram1 = fst gram1, convert (snd gram1);;




(* parse_tree_leaves *)

let decide a = match a with 
     | Node(_,_) -> true
     | Leaf _  -> false;;


let rec parse_tree_leaves = function
    | Node (x,y) -> if decide (List.hd y) then parse_tree_leaves (List.hd y)@list_to_tree (List.tl y ) 
                    else parse_tree_leaves (List.hd y)@list_to_tree (List.tl y)
    | Leaf x -> [x]

and list_to_tree treelist = match treelist with
  | h::t -> (parse_tree_leaves h)@list_to_tree t
  | [] -> [];;







(* make_matcher *)

let rec match_nonterminal rules pro_func acceptor frag = match rules with
| [] -> None
| head::tail -> match (match_terminal pro_func head acceptor frag) with
              | None -> match_nonterminal tail pro_func acceptor frag
              | Some x -> Some x

and match_terminal pro_func rules acceptor frag = match rules with
| [] -> (acceptor frag)
| (N nt_head)::tail -> (match_nonterminal (pro_func nt_head) pro_func (match_terminal pro_func tail acceptor) frag)
| (T t_head)::tail -> match frag with
                     | [] -> None
                     | f_head::f_tail -> if t_head = f_head then match_terminal pro_func tail acceptor f_tail else None


let make_matcher gram acceptor frag = match_nonterminal ((snd gram) (fst gram)) (snd gram) acceptor frag;;



(* make_parser *)


let rec match_nt start_sym rules pro_func acceptor derive frag = match rules with
| [] -> None
| head::tail -> match (match_t pro_func head acceptor (derive@[head]) frag) with
                | None -> match_nt start_sym tail pro_func acceptor derive frag
                | Some x -> Some x

and match_t pro_func rules acceptor derive frag = match rules with
| [] -> (acceptor derive frag)
| (N nt_head)::tail -> (match_nt nt_head (pro_func nt_head) pro_func (match_t pro_func tail acceptor) derive frag)
| (T t_head)::tail -> match frag with
                      | [] -> None
                      | f_head::f_tail -> if t_head = f_head then match_t pro_func tail acceptor derive f_tail else None



let result gram acceptor frag = match_nt (fst gram) ((snd gram) (fst gram)) (snd gram) acceptor [] frag;;


let get_result result = match result with
| Some x -> Some (fst x)
| _ -> None;;



(* convert the result into a tree *)
let rec traverse sym list = match sym with 
| [] -> (list, []) 
| head::tail -> match (expand head list) with 
                | (l,x) -> match traverse tail l with 
                | (y,z) -> (y, x::z)

and expand sym list = match sym with 
| (T x) -> (match list with 
          | [] -> ([], Leaf x) 
          | head::tail -> (head::tail, Leaf x)) 

| (N x) -> match list with 
         | [] -> ([], Node (x, [])) 
         | head::tail -> match traverse head tail with 
                         | (w,y) -> (w, Node (x, y));; 


let acceptor derive string = Some (derive, string);;



let make_parser gram frag = match (get_result (result gram acceptor frag)) with 
| None -> None 
| Some [] -> None 
| Some x -> match (traverse [N (fst gram)] x) with  
                                          | (_,a) -> match a with 
                                                     | h::t -> Some h;;



