:- ensure_loaded('../../train/lvps/lvps_metaqa.pl').
:- ensure_loaded('../../train/lvps/parsing_rules_metaqa.pl').
:- ensure_loaded('../../utils/utils.pl').
:- ensure_loaded('../extract/extract_candidate_parses.pl').
:- ensure_loaded('../extract/serialize_candidate_parses.pl').
:- ensure_loaded('../data/test_dgs_metaqa.pl').
:- ensure_loaded('../data/test_sentences_metaqa.pl').

?- parse_all(metaqa).
