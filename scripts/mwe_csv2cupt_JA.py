#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
CSV (many rows) + blind CoNLL-U Plus (cupt) -> annotated cupt with PARSEME:MWE

Usage:
  python annotate_many_jp_mwe.py input.csv input.blind.cupt output.cupt \
      --encoding utf-8 --csv-has-header
"""

import csv
import argparse
import unicodedata
from pathlib import Path

PARSEME_COL = "PARSEME:MWE"

def nfkc(s: str) -> str:
    if s is None:
        return ""
    s = unicodedata.normalize("NFKC", s)
    return s.replace("\ufeff", "").replace("\u200b", "").strip()

def read_csv_expressions(csv_path: Path, has_header=False, encoding='utf-8'):
    seq = []
    mapping = {}
    with csv_path.open("r", encoding=encoding, newline="") as f:
        reader = csv.reader(f)
        if has_header:
            next(reader, None)
        for row in reader:
            if not row:
                continue
            sent = nfkc(row[0] if len(row) > 0 else "")
            expr_field = nfkc(row[1] if len(row) > 1 else "NONE")
            if expr_field.upper() == "NONE" or expr_field == "":
                exprs = []
            else:
                exprs = [nfkc(x) for x in expr_field.split("|") if nfkc(x)]
            seq.append((sent, exprs))
            mapping.setdefault(sent, []).append(exprs)
    return seq, mapping

def find_all_substrings_nonoverlap(text, pattern):
    if not pattern:
        return
    start = 0
    L = len(pattern)
    while True:
        i = text.find(pattern, start)
        if i == -1:
            break
        yield (i, i + L)
        start = i + L

def compute_token_offsets(text, token_forms):
    offsets = []
    cursor = 0
    for form in token_forms:
        if form is None:
            offsets.append((None, None))
            continue
        i = text.find(form, cursor)
        if i < 0:
            i = cursor
        j = i + len(form)
        offsets.append((i, j))
        cursor = j
    return offsets

def _split_tags(cell: str):
    """Return list of tag items in a PARSEME cell, ignoring '*'."""
    if not cell or cell == "*":
        return []
    return [p for p in cell.split(";") if p]

def _ids_in_cell(cell: str):
    """Return set of IDs present in a PARSEME cell; handles 'k' and 'k:MWE'."""
    ids = set()
    for item in _split_tags(cell):
        if ":" in item:
            k, _ = item.split(":", 1)
            ids.add(k)
        else:
            ids.add(item)
    return ids

def _append_tag(cell: str, tag: str):
    """Append tag to cell if its ID not already present; preserve existing content."""
    current = cell if cell and cell != "" else "*"
    if current == "*":
        return tag
    # Avoid duplicate by ID
    cur_ids = _ids_in_cell(current)
    new_id = tag.split(":", 1)[0]
    if new_id in cur_ids:
        return current
    return current + ";" + tag

def assign_mwe_column_csv_order(text, token_offsets_numeric, expressions_csv_order):
    """
    Build PARSEME column for numeric tokens:
      - First token of each matched span gets "k:MWE"
      - Subsequent tokens of the same span get "k"
      - Multiple expressions follow CSV order: expr1->1, expr2->2, ...
    """
    n = len(token_offsets_numeric)
    col = ["*"] * n

    # de-duplicate expressions preserving order
    seen = set()
    exprs = []
    for e in expressions_csv_order:
        if e and e not in seen:
            exprs.append(e)
            seen.add(e)

    next_id = 1
    for expr in exprs:
        for (cstart, cend) in find_all_substrings_nonoverlap(text, expr):
            covered = []
            for i, (tstart, tend) in enumerate(token_offsets_numeric):
                if tstart is None:
                    continue
                if not (tend <= cstart or tstart >= cend):
                    covered.append(i)
            if covered:
                # First token in span: "k:MWE"; rest: "k"
                first_idx = covered[0]
                first_tag = f"{next_id}:MWE"
                col[first_idx] = _append_tag(col[first_idx], first_tag)
                for i in covered[1:]:
                    rest_tag = f"{next_id}"
                    col[i] = _append_tag(col[i], rest_tag)
        next_id += 1
    return col

def split_cupt_sentences(lines):
    sents = []
    cur = []
    for ln in lines:
        if ln.strip() == "":
            if cur:
                sents.append(cur)
                cur = []
        else:
            cur.append(ln)
    if cur:
        sents.append(cur)
    return sents

def get_global_columns(lines):
    for ln in lines:
        if ln.startswith("# global.columns"):
            parts = ln.split("=", 1)[1].strip().split()
            return parts
    return ["ID","FORM","LEMMA","UPOS","XPOS","FEATS","HEAD","DEPREL","DEPS","MISC"]

def ensure_parseme_column(columns):
    cols = list(columns)
    if PARSEME_COL not in cols:
        cols.append(PARSEME_COL)
    return cols

def extract_text_from_block(block):
    for ln in block:
        if ln.startswith("# text ="):
            return ln.split("=", 1)[1].strip()
    return None

def rewrite_block(block, columns, exprs, text_norm):
    # replace any per-sentence global.columns
    new_block = []
    for ln in block:
        if ln.startswith("# global.columns"):
            new_block.append(f"# global.columns = {' '.join(columns)}\n")
        else:
            new_block.append(ln)

    # collect rows
    rows = []
    for ln in new_block:
        if ln.startswith("#") or ln.strip() == "":
            rows.append(("meta", ln))
        else:
            parts = ln.rstrip("\n").split("\t")
            rows.append(("tok", parts))

    # numeric token forms
    forms_numeric = []
    numeric_row_indices = []
    for idx, (kind, obj) in enumerate(rows):
        if kind != "tok":
            continue
        parts = obj
        tid = parts[0] if parts else ""
        is_numeric = (tid and tid[0].isdigit() and "-" not in tid and "." not in tid)
        if is_numeric:
            form = parts[1] if len(parts) > 1 else ""
            forms_numeric.append(form)
            numeric_row_indices.append(idx)

    token_offsets_numeric = compute_token_offsets(text_norm, forms_numeric)
    mwe_numeric = assign_mwe_column_csv_order(text_norm, token_offsets_numeric, exprs)

    cols = ensure_parseme_column(columns)
    parseme_idx = cols.index(PARSEME_COL)
    it_mwe = iter(mwe_numeric)

    final_block = []
    for idx, (kind, obj) in enumerate(rows):
        if kind != "tok":
            final_block.append(obj)
            continue
        parts = obj
        if len(parts) < len(cols):
            parts = parts + ["_"] * (len(cols) - len(parts))
        else:
            parts = parts[:len(cols)]

        tid = parts[0] if parts else ""
        is_numeric = (tid and tid[0].isdigit() and "-" not in tid and "." not in tid)
        if is_numeric:
            parts[parseme_idx] = next(it_mwe, "*")
        else:
            parts[parseme_idx] = parts[parseme_idx] if parts[parseme_idx] not in ("",) else "*"

        final_block.append("\t".join(parts) + "\n")

    return final_block

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("input_csv", type=Path, help="CSV with 2 columns: sentence, expressions (pipe-delimited or NONE)")
    ap.add_argument("input_blind_cupt", type=Path, help="Blind cupt to annotate")
    ap.add_argument("output_cupt", type=Path, help="Output cupt with PARSEME:MWE filled")
    ap.add_argument("--encoding", default="utf-8", help="CSV encoding (default: utf-8)")
    ap.add_argument("--csv-has-header", action="store_true", help="CSV has a header row")
    args = ap.parse_args()

    seq, mapping = read_csv_expressions(args.input_csv, has_header=args.csv_has_header, encoding=args.encoding)

    with args.input_blind_cupt.open("r", encoding="utf-8") as f:
        all_lines = f.readlines()

    columns = ensure_parseme_column(get_global_columns(all_lines))
    blocks = split_cupt_sentences(all_lines)

    # fallback iterator by CSV order
    fallback_exprs_iter = iter([exprs for (_, exprs) in seq])

    output_lines = []
    # write a single global header at the top
    i = 0
    while i < len(all_lines) and all_lines[i].strip() == "":
        output_lines.append(all_lines[i])
        i += 1
    output_lines.append(f"# global.columns = {' '.join(columns)}\n")

    for block in blocks:
        raw_text = extract_text_from_block(block)
        text_norm = nfkc(raw_text) if raw_text else None

        if text_norm and text_norm in mapping and mapping[text_norm]:
            exprs = mapping[text_norm].pop(0)
        else:
            try:
                exprs = next(fallback_exprs_iter)
            except StopIteration:
                exprs = []

        exprs = [nfkc(e) for e in exprs]

        if text_norm is None:
            forms = []
            for ln in block:
                if ln.startswith("#") or ln.strip() == "":
                    continue
                parts = ln.rstrip("\n").split("\t")
                tid = parts[0] if parts else ""
                is_numeric = (tid and tid[0].isdigit() and "-" not in tid and "." not in tid)
                if is_numeric and len(parts) > 1:
                    forms.append(parts[1])
            text_norm = nfkc("".join(forms))

        rewritten = rewrite_block(block, columns, exprs, text_norm)
        output_lines.extend(rewritten)
        output_lines.append("\n")

    with args.output_cupt.open("w", encoding="utf-8", newline="\n") as out:
        for ln in output_lines:
            out.write(ln)

if __name__ == "__main__":
    main()
