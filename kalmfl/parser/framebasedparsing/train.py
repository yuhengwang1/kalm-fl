import os, subprocess
from kalmfl.parser.processor import Processor
import argparse

arg_parser = argparse.ArgumentParser()
arg_parser.add_argument('--mode', default='train', help='train or test')
arg_parser.add_argument('--ont', default='user', help='ontology')
arg_parser.add_argument('--isomorph', default=False, help='do isomorphic check or not')
arg_parser.add_argument('--dep_num', default=1, help='# of dependency parses')

args = arg_parser.parse_args()

processor = Processor(args)

processor.load_batch_data()

processor.stanza_parse()

if args.mode == 'test' or args.ont != 'metaqa':

    sentence_level_rejected_sentences = processor.get_sentence_level_rejected_sentences()

    for idx, (sent_id, sent_text) in enumerate(sentence_level_rejected_sentences.items(), 1):
        print(str(idx) + ': ' + str(sent_id) + '. ' + sent_text)

processor.paraparse()

processor.serialize()

f = open('config/xsb.properties', 'r')
lines = f.read().split('\n')
xsb_path_str = lines[4].split('=')[1]
f.close()
os.chdir('kalmfl/parser/framebasedparsing/' + args.mode + '/run')
subprocess.call(xsb_path_str + " -e \"[" + args.mode + "_" + args.ont + "], halt.\"", shell=True)
os.chdir('../../../')