import os, subprocess
from kalmfl.parser.processor import Processor
from flask import Flask, render_template, request, Response
import json, jpype

app = Flask(__name__)


@app.route('/',  methods=['GET'])
def main():
    return render_template("main.html")

@app.route('/switch-ontology', methods=['GET', 'POST'])
def switch_ontology():
    input_data_raw = request.get_data().decode("utf-8")
    processor.set_ontology(input_data_raw)
    return Response(status=200)

@app.route('/stanza-parse-input', methods=['GET', 'POST'])
def stanza_parse_input():
    input_data_raw = request.get_data().decode("utf-8")
    processor.load_input_box_data(input_data_raw)
    processor.stanza_parse()
    return Response(status=200)

@app.route('/stanza-parse-batch', methods=['GET', 'POST'])
def stanza_parse_batch():
    processor.load_batch_data()
    processor.stanza_parse()
    return Response(status=200)

@app.route('/get-sentence-level-rejected-sentences', methods=['GET', 'POST'])
def get_sentence_level_rejected_sentences():
    sentence_level_rejected_sentences = processor.get_sentence_level_rejected_sentences()
    output_data = json.dumps(sentence_level_rejected_sentences)
    return output_data

@app.route('/get-clause-level-rejected-sentences', methods=['GET', 'POST'])
def get_clause_level_rejected_sentences():
    clause_level_rejected_sentences = processor.get_sentence_level_rejected_sentences()
    output_data = json.dumps(clause_level_rejected_sentences)
    return output_data

@app.route('/paraparse-and-serialize', methods=['GET', 'POST'])
def paraparse_and_serialize():
    processor.paraparse()
    processor.serialize()
    ont = processor.get_ontology()
    os.chdir('kalmfl/parser/framebasedparsing/test/run')
    subprocess.call(xsb_path_str + " -e \"[test_" + ont + "], halt.\"", shell=True)
    os.chdir('../../../')
    return Response(status=200)

@app.route('/disambiguate', methods=['GET', 'POST'])
def disambiguate():
    if processor.get_ontology() == 'metaqa':
        output_data = {}
    else:
        ont = processor.get_ontology()
        if ont == 'kalm':
            source_file_name = 'candidates.txt'
            target_file_name = 'disambiguated.txt'
        elif ont == 'kalm2':
            source_file_name = 'candidates2.txt'
            target_file_name = 'disambiguated2.txt'
        elif ont == 'framenet':
            source_file_name = 'candidates_framenet.txt'
            target_file_name = 'disambiguated_framenet.txt'
        output_data = json.loads(disambiguation_class.disambiguate(source_file_name, target_file_name))
    return render_template('results.html', data=output_data)


if __name__ == '__main__':

    import argparse

    arg_parser = argparse.ArgumentParser()
    arg_parser.add_argument('--mode', default='test', help='train or test')
    arg_parser.add_argument('--ont', default='kalm', help='ontology')
    arg_parser.add_argument('--isomorph', default=False, help='do isomorphic check or not')
    arg_parser.add_argument('--dep_num', default=1, help='# of dependency parses')

    args = arg_parser.parse_args()

    processor = Processor(args)

    f = open('config/xsb.properties', 'r')
    lines = f.read().split('\n')
    xsb_path_str = lines[3].split('=')[1]
    f.close()

    jar_path = os.path.join(os.path.abspath("."), 'resources/disambiguation.jar')
    jvm_path = jpype.getDefaultJVMPath()
    jpype.startJVM(jvm_path, "-ea", "-Djava.class.path=%s" % jar_path, convertStrings=True)
    main_class = jpype.JClass("main.java.edu.stonybrook.cs.Main")
    main_class.main([])
    disambiguation_class = jpype.JClass("main.java.edu.stonybrook.cs.utils.Disambiguation")
    
    app.run()
    jpype.shutdownJVM()
