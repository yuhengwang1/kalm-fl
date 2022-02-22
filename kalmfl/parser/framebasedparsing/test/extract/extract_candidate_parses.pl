:- import append/3, member/2, reverse/2, length/2 from basics.

parse_all(Ontology) :-
    (
        Ontology = metaqa
        ->
        open('../../../../../resources/results/candidate_parses/candidates_metaqa.txt',write,Stream) % empty the file
        ;
        Ontology = kalm
        ->
        open('../../../../../resources/results/candidate_parses/candidates.txt',write,Stream) % empty the file
        ;
        Ontology = kalm2
        ->
        open('../../../../../resources/results/candidate_parses/candidates2.txt',write,Stream) % empty the file;
        ;
        open('../../../../../resources/results/candidate_parses/candidates_framenet.txt',write,Stream) % empty the file;
    ),
    close(Stream),
    open('../../../../../resources/error_msg/message.txt',write,Stream2),
    close(Stream2),
    StartingSentenceIndex = 1,
    parse_sentence(StartingSentenceIndex,Ontology).

parse_sentence(SentenceID,Ontology) :-
    test_sentence(SentenceID,Sentence),
    StartingParseIndex = 1,
    findall(word(index(SentenceID,WordID),Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag),
            word(index(SentenceID,WordID,StartingParseIndex),Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag),
            DGSFacts),
    (
        DGSFacts = [word(_,_,_,_,_,_,_,_,_,_,rejected)|_]
        ->
        FinalCandidateParses = [],
        get_error_msg(DGSFacts,ErrMsg),
        serialize_cadidate_parses(SentenceID,Sentence,[],Ontology),
        serialize_error_messages(SentenceID,Sentence,ErrMsg)
        ;
        extract_candidate_parses(SentenceID,StartingParseIndex,Ontology,SubsumedCandidateParses),
        get_subsumed_candidate_parse_indices(SubsumedCandidateParses,StartingParseIndex,SubsumedIndex), % CandidateParses2 is a list without subsumptions
        remove_subsumed_cadidate_parses(SubsumedCandidateParses,StartingParseIndex,SubsumedIndex,CandidateParses),
        reverse(CandidateParses,ReversedCandidateParses), % reverse the list
        get_subsumed_candidate_parse_indices(ReversedCandidateParses,StartingParseIndex,ReversedSubsumedIndex), % CandidateParses2 is a list without subsumptions
        remove_subsumed_cadidate_parses(ReversedCandidateParses,StartingParseIndex,ReversedSubsumedIndex,FinalCandidateParses),
        serialize_cadidate_parses(SentenceID,Sentence,FinalCandidateParses,Ontology) % serialize the candidate parses, DGSFacts is an input variable, writing action is in the procedure
    ),
    NewSentenceID is SentenceID + 1,
    parse_sentence(NewSentenceID,Ontology).


get_error_msg([],[]).
get_error_msg([word(_,Word,_,_,_,_,_,_,_,_,_)|Rest],ErrMsg) :-
    findall(Template,lvp(Word,Template,_,_,_),Templates),
    get_error_msg(Rest,RestErrMsg),
    append(Templates,RestErrMsg,ErrMsg).


extract_candidate_parses(SentenceID,ParseID,Ontology,NewCandidateParses) :-
    findall(word(index(SentenceID,WordID),Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag),
            word(index(SentenceID,WordID,ParseID),Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag),
            DGSFacts), % findall(template, facts, list_of_grounded_template)
    (
        DGSFacts = []
        ->
        NewCandidateParses = []
        ;
        NewParseID is ParseID + 1,
        extract_candidate_parses(SentenceID,NewParseID,Ontology,Rest),
        get_index_list_from_dgs(DGSFacts,DGSIndices), % generate a index list of 1/1, 1/2 ....
        (
            Ontology = metaqa
            ->
            extract_candidate_parses_word_by_word(DGSFacts,DGSIndices,CandidateParses1),  % uncomment this when analyzing metaqa
            get_mask_type_metaqa(CandidateParses1,MaskType),  % uncomment this when analyzing metaqa
            rewrite_parse_metaqa(DGSFacts,CandidateParses1,MaskType,CandidateParses)  % uncomment this when analyzing metaqa
            ;
            extract_candidate_parses_word_by_word(DGSFacts,DGSIndices,CandidateParses)  % return a list of parsed frame_tuple(FrameName,pair(FEName,FE,FEIndex,FEPredicateName)) (candidate parses)
        ),
        append(CandidateParses,Rest,NewCandidateParses)
    ).


% extract the index part of the DGSFacts
get_index_list_from_dgs([],[]).
get_index_list_from_dgs([word(Index,_,_,_,_,_,_,_,_,_)|Rest],[Index|RestIndex]) :-
    get_index_list_from_dgs(Rest,RestIndex).




% given the facts, check EVERY PREDICATE to see if any can trigger an lvp, if so, do frame extraction (without semantics) predicate
% by predicate (word by word) and return a list of frame_tuple(FrameName,FEList).
extract_candidate_parses_word_by_word(_,[],[]).
extract_candidate_parses_word_by_word(DGSFacts,[WordIndex|Rest],CandidateParses) :- % (DRS list, index list (sentence/word), frame list)
    findall(CandidateParse,extract_role_fillers_given_lu(DGSFacts,WordIndex,CandidateParse),CurrentCandidateParses), % find all frame names and their corresponding (roleOnt, roleOnt-filler)s of this predicate
    extract_candidate_parses_word_by_word(DGSFacts,Rest,RestCandidateParses), % keep doing
    append(CurrentCandidateParses,RestCandidateParses,CandidateParses). % aggregate all the results, return a list of frame_tuple(FrameName,pair(FEName,FE,FEIndex,FEPredicateName))


% given a predicate, get all lvps that can be triggered by the predicate, then extract roleOnt-fillers using this lvp
extract_role_fillers_given_lu(DGSFacts,WordIndex,candidate_parse(FrameName,WordIndex,RoleFillers)) :-
    get_predicate_by_index(DGSFacts,WordIndex,word(WordIndex,Lexeme,_,_,UPOS,_,_,_,_,_)), % get predicate from the DGSFacts by a specified index (1/4 or 3/2)
    lvp(Lexeme,_,FrameName,UPOS,SyntacticPatterns),  % it invokes all matching lvps (gives a recursion here)
    extract_role_fillers_by_lvp(DGSFacts,WordIndex,SyntacticPatterns,_,RoleFillers), % extract a list of (roleOnt,roleOnt-fillers) in a frame by all possible patterns related to the given fact
    length(RoleFillers,Len), % the number of role_role-filler list should greater than 1
    Len > 1.

% extract a list of (roleOnt,roleOnt-fillers) of a frame by all possible patterns related to the given fact
extract_role_fillers_by_lvp(_,_,[],[],[]).
extract_role_fillers_by_lvp(DGSFacts,LUIndex,[pair(RoleName,_,Path,_)|RestSyntacticPatterns],RoleFillerIDs,[pair(RoleName,RoleFiller,RoleFillerIndex,WordUPOS,NerType,Quantity)|RestRoleFillerPairs]) :-
    apply_pattern_to_target(Path,DGSFacts,LUIndex,RoleFillerIndex),
    get_rolefiller_by_index(DGSFacts,RoleFillerIndex,Path,NerType,[Left,Right],WordUPOS,RoleFiller),
    is_valid_pair(RoleName,RoleFiller),
    get_quantity_of_rolefiller(DGSFacts,RoleFillerIndex,Quantity),
    extract_role_fillers_by_lvp(DGSFacts,LUIndex,RestSyntacticPatterns,RestRoleFillerIDs,RestRoleFillerPairs),
    expand_interval([Left,Right],NewIDs),
    \+ member_check(NewIDs,RestRoleFillerIDs),
    append(NewIDs,RestRoleFillerIDs,RoleFillerIDs).

% if we cannot get the roleOnt-filler then try next pattern
extract_role_fillers_by_lvp(DGSFacts,LUIndex,[pair(_,_,_,optional)|RestSyntacticPatterns],RoleFillerIDs,RoleFillerPairs) :-
    extract_role_fillers_by_lvp(DGSFacts,LUIndex,RestSyntacticPatterns,RoleFillerIDs,RoleFillerPairs).


get_rolefiller_by_index(DGSFacts,RoleFillerIndex,Path,NerType,[Left,Right],WordUPOS,RoleFiller) :-
    get_predicate_by_index(DGSFacts,RoleFillerIndex,word(index(_,WordID),Word,_,_,WordUPOS,_,_,_,NerTag,_)),
    string:substring(NerTag,0,1,Prefix),
    string:substring(NerTag,2,-1,Suffix),
    (
        basics:memberchk(amod,Path)
        ->
        Left = WordID,
        Right = WordID,
        RoleFiller = Word,
        ( Prefix = o -> NerType = null ; NerType = Suffix )
        ;
        basics:memberchk(compound,Path)
        ->
        Left = WordID,
        Right = WordID,
        RoleFiller = Word,
        ( Prefix = o -> NerType = null ; NerType = Suffix )
        ;
        (
            Prefix \= o,
            Prefix \= s,
            Suffix \= money,
            Suffix \= date
            ->
            NerType = Suffix,
            reverse(DGSFacts,ReversedDGSFacts),
            get_ner_left_bound(ReversedDGSFacts,RoleFillerIndex,Left),
            get_ner_right_bound(DGSFacts,RoleFillerIndex,Right),
            get_ner(DGSFacts,[Left,Right],RoleFiller)
            ;
            Prefix = s
            ->
            NerType = Suffix,
            get_real_left_bound(DGSFacts,WordID,Left),
            get_real_right_bound(DGSFacts,WordID,Right),
            ( Left \= Right -> get_ner(DGSFacts,[Left,Right],RoleFiller) ; get_word_by_index(DGSFacts,RoleFillerIndex,RoleFiller) )
            ;
            % NerType = null,
            get_real_left_bound(DGSFacts,WordID,Left),
            get_real_right_bound(DGSFacts,WordID,Right),
            get_real_ner_type(DGSFacts,[Left,Right],RealNerType),
            NerType = RealNerType,
            ( Left \= Right -> get_ner(DGSFacts,[Left,Right],RoleFiller) ; get_word_by_index(DGSFacts,RoleFillerIndex,RoleFiller) )
        )
    ).


get_ner([word(index(_,Right),Word,_,_,_,_,_,_,_,_)|_],[_,Right],Word).
get_ner([word(index(_,WordID),Word,_,_,_,_,_,_,_,_)|Rest],[Left,Right],RoleFiller) :-
    (
        WordID >= Left,
        WordID < Right
        ->
        get_ner(Rest,[Left,Right],RestRoleFiller),
        (
            number(Word)
            ->
            number_chars(Word,WordChars),
            append(['_'],WordChars,WordChars2),
            append(WordChars2,['_'],WordChars3),
            atom_chars(Word2,WordChars3)
            ;
            atom_concat(Word,'_',Word2)
        ),
        (
            number(RestRoleFiller)
            ->
            number_chars(RestRoleFiller,RestRoleFillerChars),
            append(['_'],RestRoleFillerChars,RestRoleFillerChars2),
            atom_chars(RestRoleFiller2,RestRoleFillerChars2),
            atom_concat(Word2,RestRoleFiller2,RoleFiller)
            ;
            atom_concat(Word2,RestRoleFiller,RoleFiller)
        )
        ;
        get_ner(Rest,[Left,Right],RoleFiller)
    ).


get_quantity_of_rolefiller(DGSFacts,RoleFillerIndex,Quantity) :-
    get_predicate_by_index(DGSFacts,RoleFillerIndex,word(_,_,Edges,_,_,_,_,_,_,_)),
    ( member(edge(QuantIndex,nummod),Edges) -> get_word_by_index(DGSFacts,QuantIndex,Quantity) ; Quantity = null ).




get_subsumed_candidate_parse_indices([],_,[]).
get_subsumed_candidate_parse_indices([Parse|Rest],ParseIndex,SubsumedIndex) :-
    get_subsumed_candidate_parse_indices_helper(Parse,Rest,ParseIndex,PartialSubsumedIndex),
    NewParseIndex is ParseIndex + 1,
    get_subsumed_candidate_parse_indices(Rest,NewParseIndex,RestSubsumedIndex),
    append_index(PartialSubsumedIndex,RestSubsumedIndex,SubsumedIndex).


get_subsumed_candidate_parse_indices_helper(_,[],_,[]).
get_subsumed_candidate_parse_indices_helper(Parse1,[Parse2|Rest],Parse2Index,PartialSubsumedIndex) :-
    NewParse2Index is Parse2Index + 1,
    get_subsumed_candidate_parse_indices_helper(Parse1,Rest,NewParse2Index,RestPartialSubsumedIndex),
    ( candidate_parse_subsumed(Parse2,Parse1) -> append([NewParse2Index],RestPartialSubsumedIndex,PartialSubsumedIndex) ; PartialSubsumedIndex = RestPartialSubsumedIndex ).


remove_subsumed_cadidate_parses([],_,_,[]).
remove_subsumed_cadidate_parses([Parse|Rest],Index,SubsumedIndex,CandidateParses) :-
    NewIndex is Index + 1,
    (
        member(Index,SubsumedIndex)
        ->
        remove_subsumed_cadidate_parses(Rest,NewIndex,SubsumedIndex,RestCandidateParses),
        CandidateParses = RestCandidateParses
        ;
        remove_subsumed_cadidate_parses(Rest,NewIndex,SubsumedIndex,RestCandidateParses),
        append([Parse],RestCandidateParses,CandidateParses)
    ).


append_index([],ExistingSubsumedIndex,ExistingSubsumedIndex).
append_index([Index|Rest],ExistingSubsumedIndex,SubsumedIndex) :-
    (
        member(Index,ExistingSubsumedIndex)
        ->
        append_index(Rest,ExistingSubsumedIndex,SubsumedIndex)
        ;
        append_index(Rest,ExistingSubsumedIndex,RestSubsumedIndex),
        append([Index],RestSubsumedIndex,SubsumedIndex)
    ).


candidate_parse_subsumed(candidate_parse(FrameName,_,[]),candidate_parse(FrameName,_,_)).
candidate_parse_subsumed(candidate_parse('Inequality',_,[pair(_,_,Index1,_,_,_),pair(_,_,Index2,_,_,_)]),candidate_parse('Inequality',_,[pair(_,_,Index2,_,_,_),pair(_,_,Index1,_,_,_)])).
candidate_parse_subsumed(candidate_parse('Inequality',_,[pair(_,_,Index1,_,_,_),pair(_,_,Index2,_,_,_)]),candidate_parse('Coop',_,[pair(_,_,Index1,_,_,_),pair(_,_,Index2,_,_,_)])).
candidate_parse_subsumed(candidate_parse('Inequality',_,[pair(_,_,Index1,_,_,_),pair(_,_,Index2,_,_,_)]),candidate_parse('Coop',_,[pair(_,_,Index2,_,_,_),pair(_,_,Index1,_,_,_)])).
candidate_parse_subsumed(candidate_parse('Movie',_,[pair('Movie_name',_,Index,_,_,_),pair('Director',director,_,_,_,_)]),candidate_parse('Movie',_,[pair('Movie_name',_,Index,_,_,_),pair('Director',who,_,_,_,_)])).
candidate_parse_subsumed(candidate_parse('Movie',_,[pair('Movie_name',_,Index,_,_,_),pair('Writer',screenwriter,_,_,_,_)]),candidate_parse('Movie',_,[pair('Movie_name',_,Index,_,_,_),pair('Writer',who,_,_,_,_)])).
candidate_parse_subsumed(candidate_parse(FrameName,_,[RoleFiller|Rest1]),candidate_parse(FrameName,_,RoleFillers2)) :-
    contains_rolefiller(RoleFiller,RoleFillers2),
    candidate_parse_subsumed(candidate_parse(FrameName,_,Rest1),candidate_parse(FrameName,_,RoleFillers2)).

contains_rolefiller(_,[]) :- false.
contains_rolefiller(pair(RoleName,Filler,_,_,_,_),[RoleFiller|Rest]) :-
    ( RoleFiller = pair(RoleName,Filler,_,_,_,_) -> true ; contains_rolefiller(pair(RoleName,Filler,_,_,_,_),Rest) ).


get_target_index_of_given_edge(DGSFacts,word(_,_,Edges,_,_,XPOS,_,_,_,_),EdgeType,TargetIndex) :-
    (
        EdgeType = nmod_of
        ->
        (
            member(edge(TargetIndex,nmod),Edges),
            get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,NmodEdges,_,_,_,_,_,_,_)),
            member(edge(CaseIndex,case),NmodEdges),
            get_predicate_by_index(DGSFacts,CaseIndex,word(_,of,_,_,_,_,_,_,_,_))
            ->
            true
            ;
            member(edge(TargetIndex,nmod_poss),Edges),
            get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,PossEdges,_,_,_,_,_,_,_)),
            member(edge(_,case),PossEdges)
        )
        ;
        EdgeType = nmod_poss
        ->
        (
            member(edge(TargetIndex,nmod_poss),Edges)
            ->
            true
            ;
            member(edge(TargetIndex,nmod),Edges),
            get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,NmodEdges,_,_,_,_,_,_,_)),
            member(edge(CaseIndex,case),NmodEdges),
            get_predicate_by_index(DGSFacts,CaseIndex,word(_,of,_,_,_,_,_,_,_,_))
        )
        ;
        EdgeType = iobj,
        member(edge(TargetIndex,obl),Edges),
        get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,OblEdges,_,_,_,_,_,_,_)),
        member(edge(CaseIndex,case),OblEdges),
        get_predicate_by_index(DGSFacts,CaseIndex,word(_,to,_,_,_,_,_,_,_,_))
        ->
        true
        ;
        EdgeType = iobj,
        member(edge(TargetIndex,obl),Edges),
        get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,OblEdges,_,_,_,_,_,_,_)),
        member(edge(CaseIndex,case),OblEdges),
        get_predicate_by_index(DGSFacts,CaseIndex,word(_,for,_,_,_,_,_,_,_,_))
        ->
        true
        ;
        EdgeType = obl_to,
        member(edge(TargetIndex,iobj),Edges)
        ->
        true
        ;
        EdgeType = obl_for,
        member(edge(TargetIndex,iobj),Edges)
        ->
        true
        ;
        EdgeType = obl_by,
        member(edge(TargetIndex,obl_by),Edges)
        ->
        true
        ;
        EdgeType = amod,
        member(edge(TargetIndex,compound),Edges)
        ->
        true
        ;
        string:substring(EdgeType,0,4,Prefix),
        Prefix = poc_
        ->
        (
            string:substring(EdgeType,0,8,Pref),
            Pref = poc_null
            ->
            string:substring(EdgeType,9,-1,Suffix),
            member(edge(TargetIndex,poc),Edges),
            get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,_,_,UPOS,_,_,_,_,_)),
            UPOS = Suffix,
            get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,PocEdges,_,_,_,_,_,_,_)),
            \+ member(edge(CaseIndex,case),PocEdges)
            ;
            string:substring(EdgeType,4,-1,Suffix),
            member(edge(TargetIndex,poc),Edges),
            get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,PocEdges,_,_,_,_,_,_,_)),
            member(edge(CaseIndex,case),PocEdges),
            get_predicate_by_index(DGSFacts,CaseIndex,word(_,Suffix,_,_,_,_,_,_,_,_))
        )
        ;
        string:substring(EdgeType,0,4,Prefix),
        Prefix = obl_,
        string:substring(EdgeType,4,-1,Suffix)
        ->
        member(edge(TargetIndex,obl),Edges),
        get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,OblEdges,_,_,_,_,_,_,_)),
        member(edge(CaseIndex,case),OblEdges),
        get_predicate_by_index(DGSFacts,CaseIndex,word(_,Suffix,_,_,_,_,_,_,_,_))
        ;
        string:substring(EdgeType,0,5,Prefix),
        Prefix = nmod_,
        string:substring(EdgeType,5,-1,Suffix)
        ->
        member(edge(TargetIndex,nmod),Edges),
        get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,NmodEdges,_,_,_,_,_,_,_)),
        member(edge(CaseIndex,case),NmodEdges),
        get_predicate_by_index(DGSFacts,CaseIndex,word(_,Suffix,_,_,_,_,_,_,_,_))
        ;
        string:substring(EdgeType,0,4,Prefix),
        Prefix = lbo_,
        string:substring(EdgeType,4,-1,Suffix)
        ->
        (
            member(edge(TargetIndex,lbo),Edges),
            member(edge(CaseIndex,case),Edges),
            get_predicate_by_index(DGSFacts,CaseIndex,word(_,Suffix,_,_,_,_,_,_,_,_))
            ->
            true
            ;
            member(edge(TargetIndex,lbo),Edges),
            get_predicate_by_index(DGSFacts,TargetIndex,word(_,_,OblEdges,_,_,_,_,_,_,_)),
            member(edge(CaseIndex,obl),OblEdges),
            get_predicate_by_index(DGSFacts,CaseIndex,word(_,Suffix,_,_,adp,_,_,_,_,_))

        )
        ;
        string:substring(EdgeType,0,5,Prefix),
        Prefix = domn_,
        string:substring(EdgeType,5,-1,Suffix)
        ->
        member(edge(TargetIndex,domn),Edges),
        member(edge(CaseIndex,case),Edges),
        get_predicate_by_index(DGSFacts,CaseIndex,word(_,Suffix,_,_,_,_,_,_,_,_))
        ;
        EdgeType = obj
        ->
        contains(edge(Index,obj),Edges),
        (
            get_word_by_index(DGSFacts,Index,that)
            ->
            member(edge(TargetIndex,dep),Edges)
            ;
            contains(edge(TargetIndex,EdgeType),Edges)
        )
        ;
        contains(edge(TargetIndex,EdgeType),Edges)
    ).


contains(edge(Index1,EdgeType1),[edge(Index2,EdgeType2)|T]) :-
    Index1 = Index2,
    atom_length(EdgeType1,L),
    string:substring(EdgeType2,0,L,Prefix),
    Prefix = EdgeType1;
    contains(edge(Index1,EdgeType1),T).