% THIS FILE IS USED FOR WRITING SYNTACTIC PATTERNS TO lfe.pl, GIVEN ANNOTATED (TRAINING) EXAMPLES.
% INVOKED BY learn_lvps_and_parsing_rules.pl.
:- import append/3, member/2, reverse/2, length/2 from basics.

serialize_lvp(Lexeme,Template,POS,FrameName,PathsToRoles,Synonyms,Ontology) :-
    (
        Ontology = metaqa
        ->
        open('../lvps/lvps_metaqa.pl',append,Stream)
        ;
        Ontology = kalm
        ->
        open('../lvps/lvps_kalm.pl',append,Stream)
        ;
        open('../lvps/lvps_kalm2.pl',append,Stream)
    ),
    fmt_write(Stream,"%S(Lexeme,\'%S\',\'%S\',%S,",args(lvp,Template,FrameName,POS)),
    write(Stream,'['),
    serialize_paths_to_roles(Stream,PathsToRoles),
    write(Stream,']) :- '),
    fmt_write(Stream,"%S(\'%S\',\'%S\',Lexeme).\n",args(fn_synonym,Lexeme,FrameName)),
    serialize_synonyms(Stream,Lexeme,FrameName,Synonyms),
    close(Stream).


serialize_paths_to_roles(Stream,[pair(RoleName1,Index1,Path1,Flag1),pair(RoleName2,Index2,Path2,Flag2)|Rest]) :-
    fmt_write(Stream,"%S(\'%S\',%S,%S,%S),",args(pair,RoleName1,Index1,Path1,Flag1)),
    serialize_paths_to_roles(Stream,[pair(RoleName2,Index2,Path2,Flag2)|Rest]).

serialize_paths_to_roles(Stream,[pair(RoleName1,Index1,Path1,Flag1)]) :-
    fmt_write(Stream,"%S(\'%S\',%S,%S,%S)",args(pair,RoleName1,Index1,Path1,Flag1)).


serialize_synonyms(_,_,_,[]).
serialize_synonyms(Stream,Lexeme,FrameName,[Synonym|Rest]) :-
    (
        fn_synonym(Lexeme,FrameName,Synonym)
        ->
        true
        ;
        fmt_write(Stream,"%S(\'%S\',\'%S\',\'%S\').\n",args(fn_synonym,Lexeme,FrameName,Synonym)),
        assert(fn_synonym(Lexeme,FrameName,Synonym))
    ),
    serialize_synonyms(Stream,Lexeme,FrameName,Rest).


serialize_parsing_rules(Path,Ontology) :-
    (
        logical_syntactic_pattern(Path)
        ->
        true
        ;
        (
            Ontology = metaqa
            ->
            open('../lvps/parsing_rules_metaqa.pl',append,Stream)
            ;
            Ontology = kalm
            ->
            open('../lvps/parsing_rules_kalm.pl',append,Stream)
            ;
            open('../lvps/parsing_rules_kalm2.pl',append,Stream)
        ),
        write(Stream,'logical_syntactic_pattern('),
        write(Stream,Path),
        write(Stream,').\n'),
        (
            Path = [self]
            ->
            write(Stream,'apply_pattern_to_target([self],_,Index,Index).\n\n')
            ;
            write_rule_head(Stream,Path),
            write_rule_body(Stream,Path,1,_)
        ),
        close(Stream),
        assert(logical_syntactic_pattern(Path))
    ).


write_rule_head(Stream,Path) :-
    write(Stream,'apply_pattern_to_target('),
    write(Stream,Path),
    write(Stream,', DGSFacts, Index1, Index'),
    length(Path,L1),
    L2 is L1 + 1,
    write(Stream,L2),
    write(Stream,') :-').

write_rule_body(Stream,[Edge1,Edge2|RestEdges],Counter1,_) :-
    write_meta_pattern(Edge1,Stream,Counter1,Counter2),
    write(Stream,','),
    write_rule_body(Stream,[Edge2|RestEdges],Counter2,_).

write_rule_body(Stream,[Edge],Counter1,_) :-
    write_meta_pattern(Edge,Stream,Counter1,_),
    write(Stream,'.\n\n').

write_meta_pattern(Edge,Stream,Counter,Counter2) :-
    write(Stream,'\n    get_predicate_by_index(DGSFacts, Index'),
    write(Stream,Counter),
    write(Stream,', Fact'),
    write(Stream,Counter),
    write(Stream,'),'),
    write(Stream,'\n    get_target_index_of_given_edge'),
    write(Stream,'(DGSFacts, Fact'),
    write(Stream,Counter),
    write(Stream,', '),
    write(Stream,Edge),
    write(Stream, ', Index'),
    Counter2 is Counter + 1,
    write(Stream,Counter2),
    write(Stream,')').

write_path(Stream,[Edge1,Edge2|Rest]) :-
    (
        atom(Edge1)
        ->
        fmt_write(Stream,"%S,",args(Edge1))
        ;
        Edge1 =.. [_,Class,Subclass|_],
        fmt_write(Stream,"%S:%S,",args(Class,Subclass))
    ),
    write_path(Stream,[Edge2|Rest]).

write_path(Stream,[Edge]) :-
    (
        atom(Edge)
        ->
        fmt_write(Stream,"%S",args(Edge))
        ;
        Edge =.. [_,Class,Subclass|_],
        fmt_write(Stream,"%S:%S",args(Class,Subclass))
    ).





serialize_learned_sentence(Sentence,FrameName,LUIndex,LabeledRoles,Synonyms,lvp(Lexeme,FrameName,PathsToRoles),Ontology) :-
    (
        Ontology = metaqa
        ->
        open('../lvps/learned_sentences_metaqa.pl',append,Stream)
        ;
        Ontology = kalm
        ->
        open('../lvps/learned_sentences_kalm.pl',append,Stream)
        ;
        open('../lvps/learned_sentences_kalm2.pl',append,Stream)
    ),
    fmt_write(Stream,"%S(\'%S\',%S,\'%S\',",args(learned_sentence,Sentence,LUIndex,FrameName)),
    write(Stream,'['),
    serialize_labeled_roles(Stream,LabeledRoles),
    write(Stream,'],'),
    write(Stream,'['),
    serialize_synonyms(Stream,Synonyms),
    write(Stream,'],'),
    fmt_write(Stream,"%S(\'%S\',\'%S\',",args(lvp,Lexeme,FrameName)),
    write(Stream,'['),
    serialize_paths_to_roles(Stream,PathsToRoles),
    write(Stream,'])).\n'),
    close(Stream).

serialize_labeled_roles(Stream,[pair(RoleName1,Index1,Flag1),pair(RoleName2,Index2,Flag2)|Rest]) :-
    fmt_write(Stream,"%S(\'%S\',%S,%S),",args(pair,RoleName1,Index1,Flag1)),
    serialize_labeled_roles(Stream,[pair(RoleName2,Index2,Flag2)|Rest]).

serialize_labeled_roles(Stream,[pair(RoleName1,Index1,Flag1)]) :-
    fmt_write(Stream,"%S(\'%S\',%S,%S)",args(pair,RoleName1,Index1,Flag1)).

serialize_synonyms(_,[]).
serialize_synonyms(Stream,[Synonym1,Synonym2|Rest]) :-
    fmt_write(Stream,'\'%S\',',args(Synonym1)),
    serialize_synonyms(Stream,[Synonym2|Rest]).

serialize_synonyms(Stream,[Synonym]) :-
    fmt_write(Stream,'\'%S\'',args(Synonym)).
