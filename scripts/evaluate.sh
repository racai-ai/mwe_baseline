#!/bin/sh

source /data/vasile/IMAGES/venv-3.11/bin/activate

python ./sharedtask-data/1.2/bin/evaluate.py --gold ../RO/test.cupt --pred RO_test_pred.cupt
