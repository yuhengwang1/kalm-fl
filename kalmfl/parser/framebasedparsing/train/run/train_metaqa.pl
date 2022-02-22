initialize :-
    open('../lvps/lvps_metaqa.pl',write,Stream),
    write(Stream,':- dynamic(fn_synonym/3).\n'),
    write(Stream,':- index(fn_synonym/3,trie).\n\n'),
    write(Stream,'fn_synonym(X, _, X).\n'),
    close(Stream),
    open('../lvps/parsing_rules_metaqa.pl',write,Stream2),
    write(Stream2,':- dynamic(logical_syntactic_pattern/1).\n\n'),
    close(Stream2),
    open('../lvps/learned_sentences_metaqa.pl',write,Stream3),
    write(Stream3,':- dynamic(learned_sentence/6).\n\n'),
    close(Stream3).

?- initialize.


:- ensure_loaded('../lvps/lvps_metaqa.pl').
:- ensure_loaded('../lvps/parsing_rules_metaqa.pl').
:- ensure_loaded('../lvps/learned_sentences_metaqa.pl').
:- ensure_loaded('../../utils/utils.pl').
:- ensure_loaded('../learn/lexicon/lemma_lexicon.pl').
:- ensure_loaded('../learn/learn_lvps_and_parsing_rules.pl').
:- ensure_loaded('../learn/serialize_lvps_and_parsing_rules.pl').
:- ensure_loaded('../data/train_dgs_metaqa.pl').
:- ensure_loaded('../data/train_metaqa.pl').

?- learn_all(metaqa).