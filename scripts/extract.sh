#!/bin/sh

MODEL=llama3.3
#MODEL=mistral
#MODEL=cogito:70b
#MODEL=glm4
#MODEL=gemma3:27b
#MODEL=qwen2.5:32b
#MODEL=llama4:scout
#MODEL=mistral-nemo
#MODEL=mistral-large
#MODEL=tinyllama:latest
TEMP=0.01
TEMPLATEUSER=template_user

for corpus in RO ; do

    for TEMPLATESYSTEM in template_system ; do

    /data/programs/jdk-23.0.2/bin/java -cp "../../../mwe.jar:../../../mwe_lib/*" mwe.SimpleMWE \
        --model=$MODEL \
        --csv_in=../$corpus.csv \
        --csv_out="${corpus}_${MODEL}_${TEMP}_${TEMPLATESYSTEM}_${TEMPLATEUSER}.csv" \
        --num_threads=1 \
        "--temperature=${TEMP}" \
        "--template_sys=../../../templates/${TEMPLATESYSTEM}.txt" \
        "--template=../../../templates/${TEMPLATEUSER}.txt" \
        2>&1 | tee "${corpus}_${MODEL}_${TEMP}_${TEMPLATESYSTEM}_${TEMPLATEUSER}.log"

    done

done
