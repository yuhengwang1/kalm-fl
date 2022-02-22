:- ensure_loaded('../../train/lvps/lvps_kalm.pl').
:- ensure_loaded('../../train/lvps/parsing_rules_kalm.pl').
:- ensure_loaded('../../utils/utils.pl').
:- ensure_loaded('../extract/extract_candidate_parses.pl').
:- ensure_loaded('../extract/serialize_candidate_parses.pl').
:- ensure_loaded('../data/test_dgs_framenet.pl').
:- ensure_loaded('../data/test_sentences_framenet.pl').

?- parse_all(framenet).
