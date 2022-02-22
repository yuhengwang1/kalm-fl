:- import member/2, append/3 from basics.

get_predicate_by_index(DGSFacts,TargetIndex,word(TargetIndex,Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag)) :-
    member(word(TargetIndex,Word,Edges,InDegree,UPOS,XPOS,RootIndex,AdjTag,NerTag,ValTag),DGSFacts).

get_word_by_index(DGSFacts,TargetIndex,Word) :-
    member(word(TargetIndex,Word,_,_,_,_,_,_,_,_),DGSFacts).



get_ner_left_bound([word(index(_,WordID),_,_,_,_,_,_,_,NerTag,_)|Rest],index(_,RoleFillerID),Left) :-
    (
        WordID =< RoleFillerID,
        string:substring(NerTag,0,1,Prefix),
        Prefix = b
        ->
        Left = WordID
        ;
        get_ner_left_bound(Rest,index(_,RoleFillerID),Left)
    ).

get_ner_right_bound([word(index(_,WordID),_,_,_,_,_,_,_,NerTag,_)|Rest],index(_,RoleFillerID),Right) :-
    (
        WordID >= RoleFillerID,
        string:substring(NerTag,0,1,Prefix),
        Prefix = e
        ->
        Right = WordID
        ;
        get_ner_right_bound(Rest,index(_,RoleFillerID),Right)
    ).

only_one_role_in_the_interval([index(_,WordID)|Rest],[Left,Right]) :-
    only_one_role_in_the_interval_helper([index(_,WordID)|Rest],[Left,Right],0,Count),
    Count = 1.

only_one_role_in_the_interval_helper([],_,CurrCount,CurrCount).
only_one_role_in_the_interval_helper([index(_,WordID)|Rest],[Left,Right],CurrCount,Count) :-
    (
        WordID >= Left,
        WordID =< Right
        ->
        NewCount is CurrCount + 1,
        only_one_role_in_the_interval_helper(Rest,[Left,Right],NewCount,Count)
        ;
        only_one_role_in_the_interval_helper(Rest,[Left,Right],CurrCount,Count)
    ).

get_interval_root([word(index(SentenceID,WordID),_,_,edge(index(_,InDegreeID),_),_,_,_,_,_,_)|Rest],[Left,Right],NewRoleFillerIndex) :-
    (
        WordID >= Left,
        WordID =< Right,
        InDegreeID < Left
        ->
        NewRoleFillerIndex = index(SentenceID,WordID)
        ;
        WordID >= Left,
        WordID =< Right,
        InDegreeID > Right
        ->
        NewRoleFillerIndex = index(SentenceID,WordID)
        ;
        get_interval_root(Rest,[Left,Right],NewRoleFillerIndex)
    ).



get_real_left_bound(DGSFacts,Left,RealLeft) :-
    (
        get_predicate_by_index(DGSFacts,index(_,Left),word(_,_,_,edge(index(_,RealLeft),flat),_,_,_,_,_,_))
        ->
        true
        ;
        get_predicate_by_index(DGSFacts,index(_,Left),word(_,_,Edges,_,_,_,_,_,_,_)),
        member(edge(index(_,NewLeft),compound),Edges),
        valid_compound(DGSFacts,NewLeft,Left)
        ->
        get_real_left_bound(DGSFacts,NewLeft,RealLeft)
        ;
        RealLeft = Left
    ).

get_real_right_bound(DGSFacts,Right,RealRight) :-
    (
        get_predicate_by_index(DGSFacts,index(_,Right),word(_,_,_,edge(index(_,NewRight),compound),_,_,_,_,_,_))
        ->
        get_real_right_bound(DGSFacts,NewRight,RealRight)
        ;
        get_predicate_by_index(DGSFacts,index(_,Right),word(_,_,Edges,_,_,_,_,_,_,_)),
        findall(FlatID,member(edge(index(_,FlatID),flat),Edges),FlatIDs),
        FlatIDs \= []
        ->
        max_list(FlatIDs,RealRight)
        ;
        RealRight = Right
    ).

get_real_ner_type(DGSFacts,[Left,Right],RealNerType) :-
    get_predicate_by_index(DGSFacts,index(_,Left),word(_,_,_,_,_,_,_,_,LeftNerTag,_)),
    get_predicate_by_index(DGSFacts,index(_,Right),word(_,_,_,_,_,_,_,_,RightNerTag,_)),
    string:substring(LeftNerTag,0,1,LeftPrefix),
    string:substring(LeftNerTag,2,-1,LeftSuffix),
    string:substring(RightNerTag,0,1,RightPrefix),
    string:substring(RightNerTag,2,-1,RightSuffix),
    (
        RightPrefix = o
        ->
        RealNerType = null
        ;
        RightSuffix = date
        ->
        RealNerType = null
        ;
        RightSuffix = money
        ->
        RealNerType = null
        ;
        RealNerType = RightSuffix
    ).
    % (
    %     LeftPrefix = o,
    %     RightPrefix = o
    %     ->
    %     RealNerType = null
    %     ;
    %     LeftPrefix = o
    %     ->
    %     RealNerType = RightSuffix
    %     ;
    %     RealNerType = LeftSuffix
    % ).


valid_compound(_,DestLeft,DestLeft).
valid_compound(DGSFacts,DestLeft,Left) :-
    NewLeft is Left - 1,
    (
        get_predicate_by_index(DGSFacts,index(_,NewLeft),word(_,_,_,_,verb,_,_,_,_,_))
        ->
        false
        ;
        get_predicate_by_index(DGSFacts,index(_,NewLeft),word(_,mask,_,_,_,_,_,_,_,_))
        ->
        false
        ;
        valid_compound(DGSFacts,DestLeft,NewLeft)
    ).


max_list([Max],Max).
max_list([Head|Rest],Max) :-
    max_list(Rest,MaxList),
    ( Head > MaxList -> Max = Head ; Max = MaxList ).



expand_interval([Left,Right],ExpandedIDs) :-
    (
        Left =< Right
        ->
        NewLeft is Left + 1,
        expand_interval([NewLeft,Right],RestExpandedIDs),
        append([Left],RestExpandedIDs,ExpandedIDs)
        ;
        ExpandedIDs = []
    ).


member_check([],List) :-
    basics:memberchk([],List).

member_check([Element|Rest],List) :-
    ( basics:memberchk(Element,List) -> true ; member_check(Rest,List) ).




is_valid_pair('Money','money') :- !.
is_valid_pair('Gender','gender') :- !.
is_valid_pair('Religion','religion') :- !.
is_valid_pair('Age','age') :- !.
is_valid_pair('Time','time') :- !.

filtered_role_name('Age').
filtered_role_name('Gender').
filtered_role_name('Religion').
filtered_role_name('Time').
filtered_role_name('Money').

filtered_valid_pair('Age',X) :- integer(X).
filtered_valid_pair('Time',X) :- integer(X).
filtered_valid_pair('Gender','male').
filtered_valid_pair('Gender','female').
filtered_valid_pair('Gender','girl').
filtered_valid_pair('Gender','boy').
filtered_valid_pair('Gender','man').
filtered_valid_pair('Gender','woman').
filtered_valid_pair('Gender','lady').
filtered_valid_pair('Gender','gentleman').
filtered_valid_pair('Gender','guy').
filtered_valid_pair('Religion','christian').
filtered_valid_pair('Religion','buddhist').
filtered_valid_pair('Religion','baptist').
filtered_valid_pair('Religion','muslim').
filtered_valid_pair('Religion','zealot').
filtered_valid_pair('Religion','mormon').
filtered_valid_pair('Religion','jew').
filtered_valid_pair('Money','dollar').

filtered_role_name_metaqa('Movie_name').
filtered_role_name_metaqa('Actor').
filtered_role_name_metaqa('Director').
filtered_role_name_metaqa('Writer').
filtered_role_name_metaqa('Language').
filtered_role_name_metaqa('Genre').
filtered_role_name_metaqa('Release_year').
filtered_role_name_metaqa('Entity_1').
filtered_role_name_metaqa('Entity_2').

filtered_valid_pair_metaqa('Movie_name','mask').
filtered_valid_pair_metaqa('Movie_name','movie').
filtered_valid_pair_metaqa('Movie_name','film').
filtered_valid_pair_metaqa('Movie_name','movie_mask').
filtered_valid_pair_metaqa('Movie_name','film_mask').
filtered_valid_pair_metaqa('Movie_name','which').
filtered_valid_pair_metaqa('Movie_name','what').
filtered_valid_pair_metaqa('Actor','mask').
filtered_valid_pair_metaqa('Actor','actor').
filtered_valid_pair_metaqa('Actor','actor_mask').
filtered_valid_pair_metaqa('Actor','person').
filtered_valid_pair_metaqa('Actor','who').
filtered_valid_pair_metaqa('Actor','which').
filtered_valid_pair_metaqa('Director','mask').
filtered_valid_pair_metaqa('Director','director').
filtered_valid_pair_metaqa('Director','movie_co_director').
filtered_valid_pair_metaqa('Director','film_co_director').
filtered_valid_pair_metaqa('Director','director_mask').
filtered_valid_pair_metaqa('Director','person').
filtered_valid_pair_metaqa('Director','who').
filtered_valid_pair_metaqa('Director','which').
filtered_valid_pair_metaqa('Writer','mask').
filtered_valid_pair_metaqa('Writer','writer').
filtered_valid_pair_metaqa('Writer','movie_co_writer').
filtered_valid_pair_metaqa('Writer','film_co_writer').
filtered_valid_pair_metaqa('Writer','screenwriter').
filtered_valid_pair_metaqa('Writer','scriptwriter').
filtered_valid_pair_metaqa('Writer','writer_mask').
filtered_valid_pair_metaqa('Writer','screenwriter_mask').
filtered_valid_pair_metaqa('Writer','scriptwriter_mask').
filtered_valid_pair_metaqa('Writer','person').
filtered_valid_pair_metaqa('Writer','who').
filtered_valid_pair_metaqa('Writer','which').
filtered_valid_pair_metaqa('Language','language').
filtered_valid_pair_metaqa('Language','what').
filtered_valid_pair_metaqa('Language','which').
filtered_valid_pair_metaqa('Genre','genre').
filtered_valid_pair_metaqa('Genre','type').
filtered_valid_pair_metaqa('Genre','what').
filtered_valid_pair_metaqa('Genre','which').
filtered_valid_pair_metaqa('Release_year','year').
filtered_valid_pair_metaqa('Release_year','release_year').
filtered_valid_pair_metaqa('Release_year','release_date').
filtered_valid_pair_metaqa('Release_year','when').
filtered_valid_pair_metaqa('Release_year','what').
filtered_valid_pair_metaqa('Release_year','which').
filtered_valid_pair_metaqa('Entity_1',X) :- X \= that.
filtered_valid_pair_metaqa('Entity_2',X) :- X \= that.

is_valid_pair(RoleName,RoleFiller) :-
    (
        filtered_role_name(RoleName)
        ->
        filtered_valid_pair(RoleName,RoleFiller)
        ;
        filtered_role_name_metaqa(RoleName)
        ->
        filtered_valid_pair_metaqa(RoleName,RoleFiller)
        ;
        true
    ).


filtered_valid_fillers_metaqa('movie','movie_mask').
filtered_valid_fillers_metaqa('movie','film_mask').
filtered_valid_fillers_metaqa('film','movie_mask').
filtered_valid_fillers_metaqa('film','film_mask').
filtered_valid_fillers_metaqa('what','movie_mask').
filtered_valid_fillers_metaqa('what','film_mask').
filtered_valid_fillers_metaqa('which','movie_mask').
filtered_valid_fillers_metaqa('which','film_mask').
filtered_valid_fillers_metaqa('who','actor_mask').
filtered_valid_fillers_metaqa('who','director_mask').
filtered_valid_fillers_metaqa('who','writer_mask').
filtered_valid_fillers_metaqa('who','screenwriter_mask').
filtered_valid_fillers_metaqa('who','scriptwriter_mask').

filtered_valid_fillers_metaqa_helper(Filler1,Filler2) :-
    filtered_valid_fillers_metaqa(Filler1,Filler2);
    filtered_valid_fillers_metaqa(Filler2,Filler1).

is_valid_parse_metaqa(FrameName,RoleFillers) :-
    (
        FrameName = 'Inequality'
        ->
        RoleFillers = [pair(_,Filler1,_,_,_,_),pair(_,Filler2,_,_,_,_)],
        filtered_valid_fillers_metaqa_helper(Filler1,Filler2)
        ;
        FrameName = 'Movie',
        RoleFillers = [pair(_,_,Index,_,_,_),pair(_,_,Index,_,_,_)]
        ->
        false
        ;
        FrameName = 'Coop',
        RoleFillers = [pair(_,_,Index,_,_,_),pair(_,_,Index,_,_,_)]
        ->
        false
        ;
        true
    ).

get_mask_type_metaqa([],'Null').
get_mask_type_metaqa([candidate_parse(FrameName,_,[pair(RN1,RF1,_,_,_,_),pair(RN2,RF2,_,_,_,_)])|Rest],MaskType) :-
    (
        basics:memberchk(FrameName,['Movie','Coop']),
        RF1 = mask
        ->
        MaskType = RN1
        ;
        basics:memberchk(FrameName,['Movie','Coop']),
        RF2 = mask
        ->
        MaskType = RN2
        ;
        get_mask_type_metaqa(Rest,MaskType)
    ).



rewrite_parse_metaqa(_,[],_,[]).
rewrite_parse_metaqa(DGSFacts,[candidate_parse(FrameName,Index,RoleFillers)|Rest],MaskType,CandidateParses) :-
    (
        MaskType \= 'Null'
        ->
        rewrite_mask_metaqa(RoleFillers,MaskType,RoleFillers1)
        ;
        RoleFillers1 = RoleFillers
    ),
    rewrite_wh_metaqa(DGSFacts,RoleFillers1,NewRoleFillers),
    (
        is_valid_parse_metaqa(FrameName,RoleFillers1)
        ->
        rewrite_parse_metaqa(DGSFacts,Rest,MaskType,RestCandidateParses),
        (
            is_valid_parse_metaqa(FrameName,NewRoleFillers)
            ->
            append([candidate_parse(FrameName,Index,NewRoleFillers)],RestCandidateParses,CandidateParses)
            ;
            rewrite_parse_metaqa(DGSFacts,Rest,MaskType,RestCandidateParses),
            CandidateParses = RestCandidateParses
        )
        ;
        rewrite_parse_metaqa(DGSFacts,Rest,MaskType,RestCandidateParses),
        CandidateParses = RestCandidateParses
    ).



rewrite_mask_metaqa([Filler1,Filler2],MaskType,[NewFiller1,NewFiller2]) :-
    Filler1 = pair(RN1,RF1,Idx1,UPOS1,Ner1,Q1),
    Filler2 = pair(RN2,RF2,Idx2,UPOS2,Ner2,Q2),
    (
        RF1 = mask,
        MaskType = 'Movie_name'
        ->
        NewFiller1 = pair(RN1,movie_mask,Idx1,UPOS1,Ner1,Q1),
        NewFiller2 = Filler2
        ;
        RF1 = mask,
        MaskType = 'Actor'
        ->
        NewFiller1 = pair(RN1,actor_mask,Idx1,UPOS1,Ner1,Q1),
        NewFiller2 = Filler2
        ;
        RF1 = mask,
        MaskType = 'Director'
        ->
        NewFiller1 = pair(RN1,director_mask,Idx1,UPOS1,Ner1,Q1),
        NewFiller2 = Filler2
        ;
        RF1 = mask,
        MaskType = 'Writer'
        ->
        NewFiller1 = pair(RN1,writer_mask,Idx1,UPOS1,Ner1,Q1),
        NewFiller2 = Filler2
        ;
        RF2 = mask,
        MaskType = 'Movie_name'
        ->
        NewFiller2 = pair(RN2,movie_mask,Idx2,UPOS2,Ner2,Q2),
        NewFiller1 = Filler1
        ;
        RF2 = mask,
        MaskType = 'Actor'
        ->
        NewFiller2 = pair(RN2,actor_mask,Idx2,UPOS2,Ner2,Q2),
        NewFiller1 = Filler1
        ;
        RF2 = mask,
        MaskType = 'Director'
        ->
        NewFiller2 = pair(RN2,director_mask,Idx2,UPOS2,Ner2,Q2),
        NewFiller1 = Filler1
        ;
        RF2 = mask,
        MaskType = 'Writer'
        ->
        NewFiller2 = pair(RN2,writer_mask,Idx2,UPOS2,Ner2,Q2),
        NewFiller1 = Filler1
        ;
        NewFiller1 = Filler1,
        NewFiller2 = Filler2
    ).


rewrite_wh_metaqa(DGSFacts,[pair(RN1,RF1,Idx1,UPOS1,Ner1,Q1),pair(RN2,RF2,Idx2,UPOS2,Ner2,Q2)],[Filler1,Filler2]) :-
    rewrite_wh_metaqa_helper(DGSFacts,pair(RN1,RF1,Idx1,UPOS1,Ner1,Q1),Filler1),
    rewrite_wh_metaqa_helper(DGSFacts,pair(RN2,RF2,Idx2,UPOS2,Ner2,Q2),Filler2).

rewrite_wh_metaqa_helper(DGSFacts,pair(RoleName,RF,Idx,P,N,Q),pair(RoleName,RoleFiller,RoleFillerIndex,UPOS,Ner,Quant)) :-
    get_predicate_by_index(DGSFacts,Idx,word(Idx,_,Edges,InDegree,_,_,_,_,_,_)),
    (
        basics:memberchk(edge(Index,det),Edges),
        get_predicate_by_index(DGSFacts,Index,word(RoleFillerIndex,Wh,_,_,_,_,_,_,_,_)),
        basics:memberchk(Wh,[which,what])
        ->
        get_predicate_by_index(DGSFacts,RoleFillerIndex,word(RoleFillerIndex,RoleFiller,_,_,UPOS,_,_,_,_,_)),
        Ner = null,
        Quant = null
        ;
        basics:memberchk(edge(Index,nsubj),Edges),
        get_predicate_by_index(DGSFacts,Index,word(RoleFillerIndex,who,_,_,_,_,_,_,_,_))
        ->
        get_predicate_by_index(DGSFacts,RoleFillerIndex,word(RoleFillerIndex,RoleFiller,_,_,UPOS,_,_,_,_,_)),
        Ner = null,
        Quant = null
        ;
        InDegree = edge(Index,nsubj),
        get_predicate_by_index(DGSFacts,Index,word(RoleFillerIndex,Wh,_,_,_,_,_,_,_,_)),
        basics:memberchk(Wh,[which,what,who])
        ->
        get_predicate_by_index(DGSFacts,RoleFillerIndex,word(RoleFillerIndex,RoleFiller,_,_,UPOS,_,_,_,_,_)),
        Ner = null,
        Quant = null
        ;
        RoleFillerIndex = Idx,
        RoleFiller = RF,
        UPOS = P,
        Ner = N,
        Quant = Q
    ).
