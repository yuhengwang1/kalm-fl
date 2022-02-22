% serialize every candidate parse ti the txt file

serialize_cadidate_parses(SentenceID,Sentence,CandidateParses,Ontology) :-
    (
        Ontology = metaqa
        ->
        open('../../../../../resources/results/candidate_parses/candidates_metaqa.txt',append,Stream)
        ;
        Ontology = kalm
        ->
        open('../../../../../resources/results/candidate_parses/candidates.txt',append,Stream)
        ;
        Ontology = kalm2
        ->
        open('../../../../../resources/results/candidate_parses/candidates2.txt',append,Stream)
        ;
        open('../../../../../resources/results/candidate_parses/candidates_framenet.txt',append,Stream)
    ),
    write(Stream,'============================'),
    write(Stream,SentenceID),
    write(Stream,'============================\n'),
    write(Stream,Sentence),
    write(Stream,'\n'),
    serialize_cadidate_parses_helper(Stream,CandidateParses),
    close(Stream).


% a helper iterate every candidate prase
serialize_cadidate_parses_helper(_,[]).
serialize_cadidate_parses_helper(Stream,[candidate_parse(FrameName,LUIndex,RoleFillers)|Rest]) :-
    fmt_write(Stream,"%S = ",FrameName), % first serialize the frame name
    write(Stream,LUIndex),
    write(Stream,'\n'),
    serialize_role_fillers(Stream,RoleFillers), % deal with every roleOnt in a cnadidate parse
    write(Stream,'\n'),
    serialize_cadidate_parses_helper(Stream,Rest).


% deal with every roleOnt in a cnadidate parse
serialize_role_fillers(_,[]).
serialize_role_fillers(Stream,[pair(RoleName,RoleFiller,RoleFillerIndex,WordPOS,NerType,Quantity)|Rest]) :-
    write(Stream,RoleName),
    write(Stream,' - '),
    write(Stream,RoleFiller),
    write(Stream,' - '),
    write(Stream,RoleFillerIndex),
    write(Stream,' - '),
    write(Stream,WordPOS),
    write(Stream,' - '),
    write(Stream,NerType),
    write(Stream,' - '),
    write(Stream,Quantity),
    write(Stream,'\n'),
    serialize_role_fillers(Stream,Rest).



serialize_error_messages(SentenceID,Sentence,Templates) :-
    open('../../../../../resources/error_msg/message.txt',append,Stream),
    write(Stream,'============================'),
    write(Stream,SentenceID),
    write(Stream,'============================\n'),
    write(Stream,Sentence),
    write(Stream,'\n'),
    serialize_templates(Stream,Templates),
    write(Stream,'\n'),
    close(Stream).


serialize_templates(_,[]).
serialize_templates(Stream,[Template|Rest]) :-
    write(Stream,Template),
    write(Stream,'\n'),
    serialize_templates(Stream,Rest).