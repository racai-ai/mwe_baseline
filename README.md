# mwe_baseline
Baseline script for detecting MWEs with LLMs

# Usage

Check at the end for details about the different parameters.
First download and extract a release package from the Releases page: https://github.com/racai-ai/mwe_baseline/releases

## Prerequisites

If you plan to use a local Ollama installation, make sure it is installed: https://ollama.com/download .
Furthermore, you should download the desired model and load it in memory (even though the first call will try to load the model into memory, it is possible to receive a timeout if your hardware is not fast enough). This can be done using a command similar to the following:
```
ollama run llama3.3 Hello --keepalive 1d
```
This command will load the llama3.3 model in memory and will generate text corresponding to the prompt "Hello". The model will be kept in memory for 1 day, unless another model is loaded or a specific command is issued to remove the model.

The application runs on most recent hardware, even without GPUs. However, a GPU is strongly advised for running local models through Ollama. Consult the Ollama documentation for specific requirements (if you use Ollama).

Example timings using a GTX1080 GPU with 8GB: 
- Rephrasing for FR, 159 sentences took 153 min,
- Detection for EN, 10 sentences took 4 min.


## 1. Task MWE Identification

1.1. Assuming you have CUPT data, such as the PARSEME Shared Task data, first convert it to CSV:
```
java -cp "mwe.jar:mwe_lib/*" mwe.MWECUPT2CSV EN/test.cupt EN.csv
```
(Note: this example assumes you have English data extracted in the EN folder)

1.2. Run the extraction:
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
(Note: make sure you create appropriate templates for the system and user prompts; examples are given in the templates folder of this repo; templates may require adjusting for each targeted language)

1.3. Convert back to CUPT for the CUPT-based evaluation script (this step is required if you want to evaluate with the PARSEME evaluation script):
```
java -cp "mwe.jar:mwe_lib/*" mwe.CSV2MWECUPT EN/test.cupt EN_predictions.csv EN_predictions.cupt
```
(Note: the original CUPT file is needed for the tokenization; any MWE annotations in this file are ignored)

For Japanese we use a separate conversion script [scripts/mwe_csv2cupt_JA.py](scripts/mwe_csv2cupt_JA.py) :
```
python annotate_many_jp_mwe.py input.csv input.blind.cupt output.cupt --encoding utf-8 --csv-has-header
```

## 2. Task MWE paraphrasing

2.1. Run paraphrasing
```
java -cp "mwe.jar:mwe_lib/*" mwe.SimpleMWEParaphrasing \
   --model=llama3.3 \
   --type=ollama \
   --endpoint=http://127.0.0.1:11434/api/chat \
   --json_in=FR.json \
   --csv_out=FR_pred.csv \
   --num_threads=1 \
   --temperature=0.01 \
   --template_sys=templates/template_paraphrasing_system.txt \
   --template=templates/template_paraphrasing_user.txt
```
(Note: make sure you create appropriate templates for the system and user prompts; examples are given in the templates folder of this repo; the templates may require adjustment for each targeted language)


2.2. Convert output to JSON
```
java -cp "mwe.jar:mwe_lib/*" mwe.CSV2MWEJSONParaphrase \
        "FR_pred.csv" "FR_pred.json"
```

(optional, THIS IS **NOT** NEEDED FOR THE SHARED TASK) Convert input JSON to CSV
```
java -cp "mwe.jar:mwe_lib/*" mwe.JSONMWEPara2CSV EN.json EN.csv
```


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
Type specifies the API type. Possible values include: 
- ollama: intended for interaction with Ollama servers. If running locally using default configuration, specify the endpoint as http://127.0.0.1:11434/api/chat
- grok: intended for Grok paid API
- openai: intended for models newer than "o1", where the "system" role was renamed as "developer"
- publicai: interface for Publicai Apertus.

For other paid APIs, you may try with the different available options. For example, together.ai works with type=grok. 
Not all APIs support all options. If an API does not support a specific option (temperature, reasoning_effort) just ommit that parameter. For example o1 does not support the temperature parameter.


