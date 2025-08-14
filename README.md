# mwe_baseline
Baseline script for detecting MWEs with LLMs

# Usage

1. Download and extract a release package from the Releases page: https://github.com/racai-ai/mwe_baseline/releases

2. Assuming you have CUPT data, such as the PARSEME Shared Task data, first convert it to CSV:
```
java -cp "mwe.jar:mwe_lib/*" mwe.MWECUPT2CSV EN/test.cupt EN.csv
```
(Note: this example assumes you have English data extracted in the EN folder)

3. Run the extraction:
```
java -cp "mwe.jar:mwe_lib/*" mwe.SimpleMWE \
   --model=llama3.3 \
   --csv_in=EN.csv \
   --csv_out=EN_predictions.csv \
   --num_threads=1 \
   --temperature=0.01 \
   --template_sys=templates/template_system.txt \
   --template=templates/template_user.txt
```
(Note: make sure you create appropriate templates for the system and user prompts; examples are given in the templates folder of this repo)

4. Convert back to CUPT for the CUPT-based evaluation script (this step is required if you want to evaluate with the PARSEME evaluation script):
```
java -cp "mwe.jar:mwe_lib/*" mwe.CSV2MWECUPT EN/test.cupt EN_predictions.csv EN_predictions.cupt
```
(Note: the original CUPT file is needed for the tokenization; any MWE annotations in this file are ignored)

# Advanced usage

The MWE extraction program accepts the following parameters:
```
--model=<string> (optional, default=llama3.3) Model
--type=<string> (optional, default=ollama) Model type
--key=<string> (optional, default=null) API key (X.AI, ChatGPT, etc.)
--csv_in=<string> CSV
--csv_out=<string> CSV
--num_threads=<int> (optional, default=10) Number of threads
--endpoint=<string> (optional, default=http://127.0.0.1:11434/api/chat) Model endpoint(s). Separate multiple endpoints by ;
--template=<string> Template to use for user prompts
--template_sys=<string> System template to use
--temperature=<string> (optional, default=null) Temperature
--reasoning_effort=<string> (optional, default=null) Reasoning Effort
```

Setting num_threads higher than 1 makes sense if using an API that allows for parallel requests or if you provide multiple endpoints (like multiple ollama processes). Setting the parameter reasoning_effort is not supported by all models. Furthermore, the value of this parameter is dependent on the model.

