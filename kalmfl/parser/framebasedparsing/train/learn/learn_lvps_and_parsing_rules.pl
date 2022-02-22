% THIS FILE IS USED FOR ANNOTATING SENTENCES TO GET LVPS.
% INPUT IS LIKE
% ?- learn_lvps_and_parsing_rules('John works as a salesman on a program at IBM in a n:sales in London.',
%                      'LUIndx' = 2,
%                      'Being_employed',
%                      ['Employee'=1+required,'Employer'=10+optional,'Place'=17+optional,'Field'=15+optional,'Position'=5+optional,'Task'=8+optional],
%                      ['perform'], LVP).
% OUTPUT TO lvps.pl, parsing_rules.pl (by serialize_lvps_and_parsing_rules.pl) and learned_sentences.pl TO GET THE LVPS AND LOGICAL SYNTACTIC PATTERNS (LFE).
:- table bfs/4.
:- import append/3, member/2, length/2, reverse/2 from basics.
:- import str_cat/3 from string.


% Annotate narrative sentences
learn_all(Ontology) :-
    StartingSentenceIndex = 1,
    train_sentence(StartingSentenceIndex,Ontology).
    
train_sentence(SentenceID,Ontology) :-
    train(Sentence,FrameName,index(SentenceID,LUIndex),LabeledRoles,Synonyms,Template),
    check_synonyms_type(Synonyms,SynType),
    train_helper(Sentence,Template,FrameName,index(SentenceID,LUIndex),LabeledRoles,Synonyms,SynType,1,Ontology),
    NewSentenceID is SentenceID + 1,
    train_sentence(NewSentenceID,Ontology).

check_synonyms_type([],other).
check_synonyms_type([Synonym|Rest],SynType) :-
    (
        lemma(Synonym,'JJ',Synonym),  % if this is an adj
        lemma(Synonym,'VBN',SynonymBase),  % and this adj can be used as a past participle
        Synonym \= SynonymBase
        ->
        SynType = adj
        ;
        lemma(Synonym,'VB',Synonym)  % if this is a verb in base form
        ->
        SynType = verb
        ;
        check_synonyms_type(Rest,SynType)
    ).

train_helper(Sentence,Template,FrameName,LUIndex,LabeledRoles,Synonyms,SynType,ParseID,Ontology) :-
    LUIndex = index(SentenceID,_),
    findall(word(index(SentenceID,WordID),Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag),word(index(SentenceID,WordID,ParseID),Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag),DGSFacts),  % generate the DGS list of the sentence
    (
        DGSFacts \= []
        ->
        get_all_indices_of_roles(LabeledRoles,LUIndex,Indices),
        learn_and_serialize_parsing_rules(DGSFacts,LUIndex,Indices,LabeledRoles,PathsToRoles,Ontology), % get syntactic patterns from annotated data, write to ###parsing_rules.pl###
        get_predicate_by_index(DGSFacts,LUIndex,word(LUIndex,Lexeme,_,_,UPOS,XPOS,_,_,_,_)), % get the LU word
        LVP = lvp(Lexeme,FrameName,PathsToRoles), % generate lvp
        basics:memberchk(word(_,_,_,_,_,_,_,AdjTag,_,_),DGSFacts),
        rewrite_synonyms(AdjTag,SynType,Synonyms,NewSynonyms),
        (
            learned_sentence(Sentence,FrameName,LUIndex,LabeledRoles,NewSynonyms,LVP) % if this kind of form of sentence is written in learned_sentences.pl
            ->
            true % pass
            ;
            serialize_learned_sentence(Sentence,FrameName,LUIndex,LabeledRoles,NewSynonyms,LVP,Ontology), % otherwise write the annotated sentences with lvp in ###learned_sentences.pl###
            assert(learned_sentence(Sentence,FrameName,LUIndex,LabeledRoles,NewSynonyms,LVP)),
            % get_training_template(DGSFacts,PathsToRoles,Template),
            serialize_lvp(Lexeme,Template,UPOS,FrameName,PathsToRoles,NewSynonyms,Ontology)
        ),
        NewParseID is ParseID + 1,
        train_helper(Sentence,Template,FrameName,LUIndex,LabeledRoles,Synonyms,SynType,NewParseID,Ontology)
        ;
        true
    ).


% get_training_template([word(Index,Word,_,_,_,_,_,_,_)],PathsToRoles,Token) :-
%     get_template_token(Index,Word,PathsToRoles,Token1),
%     atom_concat(Token1,'.',Token),
%     !.

% get_training_template([word(Index,Word,_,_,_,_,_,_,_)|Rest],PathsToRoles,Template) :-
%     (
%         number(Word)
%         ->
%         number_codes(Word,Codes),
%         append([95],Codes,Codes1),
%         atom_codes(NewWord,Codes1),
%         get_template_token(Index,NewWord,PathsToRoles,Token)
%         ;
%         get_template_token(Index,Word,PathsToRoles,Token)
%     ),
%     get_training_template(Rest,PathsToRoles,RestTemplate),
%     atom_concat(Token,' ',Template1),
%     atom_concat(Template1,RestTemplate,Template).

get_template_token(Index,Word,[],Word).
get_template_token(Index,Word,[pair(RoleName,RoleFillerIndex,_,Flag)|RestPathsToRoles],Token) :-
    (
        Index = RoleFillerIndex,
        Flag = required
        ->
        atom_concat('[',RoleName,Token1),
        atom_concat(Token1,':',Token2),
        atom_concat(Token2,'required',Token3),
        atom_concat(Token3,']',Token)
        ;
        Index = RoleFillerIndex,
        Flag = optional
        ->
        atom_concat('[',RoleName,Token1),
        atom_concat(Token1,']',Token)
        ;
        get_template_token(Index,Word,RestPathsToRoles,Token)
    ).
    

get_all_indices_of_roles([],LUIndex,[LUIndex]).
get_all_indices_of_roles([pair(_,RoleFillerIndex,_)|Rest],LUIndex,[RoleFillerIndex|RestIndices]) :-
    get_all_indices_of_roles(Rest,LUIndex,RestIndices).


% does not consider corefs
learn_and_serialize_parsing_rules(_,_,_,[],[],_).
learn_and_serialize_parsing_rules(DGSFacts,LUIndex,Indices,[pair(RoleName,RoleFillerIndex,Flag)|Rest],[pair(RoleName,RoleFillerIndex,Path,Flag)|PathsToRoles],Ontology) :-
    get_predicate_by_index(DGSFacts,RoleFillerIndex,word(_,_,_,_,_,_,_,_,NerTag,_)),
    string:substring(NerTag,0,1,Prefix),
    string:substring(NerTag,2,-1,Suffix),
    (
        Prefix \= o,
        Prefix \= s,
        Suffix \= money,
        Suffix \= date,
        reverse(DGSFacts,ReversedDGSFacts),
        get_ner_left_bound(ReversedDGSFacts,RoleFillerIndex,Left),
        get_ner_right_bound(DGSFacts,RoleFillerIndex,Right),
        only_one_role_in_the_interval(Indices,[Left,Right])
        ->
        get_interval_root(DGSFacts,[Left,Right],NewRoleFillerIndex),
        construct_path(DGSFacts,LUIndex,NewRoleFillerIndex,Path)
        ;
        construct_path(DGSFacts,LUIndex,RoleFillerIndex,Path)
    ),
    serialize_parsing_rules(Path,Ontology),
    learn_and_serialize_parsing_rules(DGSFacts,LUIndex,Indices,Rest,PathsToRoles,Ontology).





construct_path(_,LUIndex,LUIndex,[self]).
construct_path(DGSFacts,LUIndex,TargetIndex,Path) :-
    LUIndex \= TargetIndex, % the LU and the destination word is not the same one
    reach(DGSFacts,LUIndex,TargetIndex,Path).


reach(DGSFacts,LUIndex,TargetIndex,Path):-
    get_predicate_by_index(DGSFacts,LUIndex,LUPredicate),
    LUPredicate = word(index(_,WordID),_,_,_,_,_,_,_,_,_),
    bfs(DGSFacts,[[LUIndex,[WordID],[]]],TargetIndex,Path).


bfs(_,[[TargetIndex,_,Path]|_],TargetIndex,Path).
bfs(DGSFacts,[[CurrentIndex,CurrentNodes,CurrentPath]|Queue],TargetIndex,Path) :-
    get_predicate_by_index(DGSFacts,CurrentIndex,CurrentPredicate),
    CurrentPredicate = word(_,_,Edges,_,_,_,_,_,_,_),
    get_children(DGSFacts,Edges,Edges,CurrentNodes,CurrentPath,Children),
    append(Queue,Children,NextQueue),
    bfs(DGSFacts,NextQueue,TargetIndex,Path).


get_children(_,[],_,_,_,[]).
get_children(DGSFacts,[Edge|Rest],Edges,CurrentNodes,CurrentPath,NewChildren) :-
    Edge = edge(ChildIndex,EdgeType),
    get_predicate_by_index(DGSFacts,ChildIndex,ChildPredicate),
    ChildPredicate = word(index(_,WordID),_,ChildEdges,_,ChildUPOS,_,_,_,_,_),
    (
        \+ member(WordID,CurrentNodes)
        ->
        (
            EdgeType = obl,
            basics:memberchk(edge(PrepIndex,case),ChildEdges),
            get_predicate_by_index(DGSFacts,PrepIndex,word(_,Preposition,_,_,adp,_,_,_,_,_))
            ->
            str_cat(obl_,Preposition,NewEdgeType),
            append(CurrentPath,[NewEdgeType],NewPath)
            ;
            EdgeType = nmod,
            basics:memberchk(edge(PrepIndex,case),ChildEdges),
            get_predicate_by_index(DGSFacts,PrepIndex,word(_,Preposition,_,_,adp,_,_,_,_,_))
            ->
            str_cat(nmod_,Preposition,NewEdgeType),
            append(CurrentPath,[NewEdgeType],NewPath)
            ;
            EdgeType = lbo,
            basics:memberchk(edge(PrepIndex,case),Edges),
            get_predicate_by_index(DGSFacts,PrepIndex,word(_,Preposition,_,_,adp,_,_,_,_,_))
            ->
            str_cat(lbo_,Preposition,NewEdgeType),
            append(CurrentPath,[NewEdgeType],NewPath)
            ;
            EdgeType = domn,
            basics:memberchk(edge(PrepIndex,case),Edges),
            get_predicate_by_index(DGSFacts,PrepIndex,word(_,Preposition,_,_,adp,_,_,_,_,_))
            ->
            str_cat(domn_,Preposition,NewEdgeType),
            append(CurrentPath,[NewEdgeType],NewPath)
            ;
            EdgeType = poc
            ->
            (
                basics:memberchk(edge(PrepIndex,case),ChildEdges),
                get_predicate_by_index(DGSFacts,PrepIndex,word(_,Preposition,_,_,adp,_,_,_,_,_))
                ->
                str_cat(poc_,Preposition,NewEdgeType),
                append(CurrentPath,[NewEdgeType],NewPath)
                ;
                str_cat(poc_null_,ChildUPOS,NewEdgeType),
                append(CurrentPath,[NewEdgeType],NewPath)
            )
            ;
            append(CurrentPath,[EdgeType],NewPath)
        ),
        append(CurrentNodes,[WordID],NewNodes),
        ChildNode = [[ChildIndex,NewNodes,NewPath]]
        ;
        ChildNode = []
    ),
    get_children(DGSFacts,Rest,Edges,CurrentNodes,CurrentPath,Children),
    append(ChildNode,Children,NewChildren).





rewrite_synonyms(AdjTag,SynType,Synonyms,NewSynonyms) :-
    (
        AdjTag = adj,
        SynType = verb
        ->
        convert_to_adj(Synonyms,NewSynonyms)
        ;
        AdjTag = verb,
        SynType = adj
        ->
        convert_to_verb(Synonyms,NewSynonyms)
        ;
        NewSynonyms = Synonyms
    ).

convert_to_adj([],[]).
convert_to_adj([Synonym|Rest],NewSynonyms) :-
    (
        lemma(NewSynonym,'VBN',Synonym),  % if Synonym is a base form of a past participle
        lemma(NewSynonym,'JJ',NewSynonym),  % and this past participle can be an adjective
        Synonym \= NewSynonym
        ->
        convert_to_adj(Rest,RestNewSynonyms),
        append([NewSynonym],RestNewSynonyms,NewSynonyms)  % use the adjective
        ;
        convert_to_adj(Rest,RestNewSynonyms),
        append([Synonym],RestNewSynonyms,NewSynonyms)  % use the original one
    ).

convert_to_verb([],[]).
convert_to_verb([Synonym|Rest],NewSynonyms) :-
    (
        lemma(Synonym,'JJ',Synonym),  % if Synonym is an adjective
        lemma(Synonym,'VBN',NewSynonym),  % and this adjective can be a past participle
        Synonym \= NewSynonym
        ->
        convert_to_verb(Rest,RestNewSynonyms),
        append([NewSynonym],RestNewSynonyms,NewSynonyms)  % use the base form of the past participle
        ;
        convert_to_verb(Rest,RestNewSynonyms),
        append([Synonym],RestNewSynonyms,NewSynonyms)  % use the original one
    ).


