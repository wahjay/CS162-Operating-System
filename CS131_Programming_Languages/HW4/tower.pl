
% Matrix Transpose

transpose([[]|_],[]).
transpose(Matrix, [Row|Rows]) :- 
    transpose_col(Matrix, Row, RestMatrix), transpose(RestMatrix,Rows).

transpose_col([],[],[]).
transpose_col([[H|T]|Rows], [H|Hs], [T|Ts]) :- transpose_col(Rows, Hs,Ts).



% get counts
get_count([H | T], Count) :- get_count(T, H, 1, Count).

get_count([], _, N, N).

get_count([H | T], Max, N, Count) :-
    H > Max,
    N2 is N+1,
    get_count(T, H, N2, Count).

get_count([H | T], Max, N, Count) :- H < Max, get_count(T, Max, N, Count).


make_list(_, []).
make_list(N, [N | T]) :- N2 is N-1, make_list(N2, T).


% main

plain_tower(0, T, C) :- T=[], C=counts([],[],[],[]), !.
plain_tower(N, T, C) :- 
    length(T,N),
    counts(Top, Bottom, Left, Right) = C,
    length(Top, N),
    length(Bottom, N),
    length(Left, N),
    length(Right, N),
    create_matrix(N,T,Left,Right),
    transpose(T,R),
    create_matrix(N,R,Top,Bottom).


create_matrix(_, [], _, _).
create_matrix(N, [TH | TT], [LH | LT], [RH | RT]) :- 
    length(L, N),
    make_list(N, L),
    permutation(L, TH),
    member(LH, L),
    get_count(TH,LH),
    reverse(TH, THr),
    member(RH, L),
    get_count(THr,RH),
    create_matrix(N, TT, LT, RT).
    
tower(0, T, C) :- T=[], C=counts([],[],[],[]), !.
tower(N, T, C) :- 
    make_matrix(T, N),
    domain(T,N),
    maplist(fd_all_different,T),
    transpose(T,R),
    maplist(fd_all_different,R),
    maplist(fd_labeling,T),
    counts(Top, Bottom, Left, Right) = C,
    length(Top, N),
    length(Bottom, N),
    length(Left, N),
    length(Right, N),
    reverse_list(T, Tr),
    reverse_list(R, Rr),
    get_counts(R, Top),
    get_counts(Rr, Bottom),
    get_counts(T, Left),
    get_counts(Tr, Right).



make_matrix(T,N) :- length(T,N), matrix(N, T).

matrix(_, []).
matrix(N, [H | T]) :- length(H, N), matrix(N, T).

domain([],_).
domain([H|T], N) :- fd_domain(H, 1, N), domain(T,N).

reverse_list([], []).
reverse_list([H | T], [Hr | Tr]) :- reverse(H, Hr), reverse_list(T, Tr).

get_counts([],[]).
get_counts([H|T], [C|Cs]) :- get_count(H,C), get_counts(T,Cs).


% speed test


tower_time(D1) :-
    statistics(cpu_time, [T1|_]),

    tower(5, T,
         counts([2,3,2,1,4],
                [3,1,3,3,2],
                [4,1,2,5,2],
                [2,4,2,1,2])),
    
    statistics(cpu_time, [T2|_]),
    D1 is T2 - T1.

plain_time(D2) :-
    statistics(cpu_time, [T1|_]),
    
    plain_tower(5, T,
         counts([2,3,2,1,4],
                [3,1,3,3,2],
                [4,1,2,5,2],
                [2,4,2,1,2])),

    statistics(cpu_time, [T2|_]),
    D2 is T2 - T1.


speed_up(R) :- 
    tower_time(D1),
    plain_time(D2),
    R is D2 / D1.

ambiguous(N,C,T1,T2) :-
    tower(N,T1,C),
    tower(N,T2,C),
    T1 \= T2.

