"""Converts every Sigma rule in a folder to a Lucene query for OpenSearch with pySigma and prints JSON: {rule name: {queries | error}}.

A Sigma correlation rule is tried on the whole file; when the backend cannot convert it the reason is reported, not skipped.
"""
import json
import sys
from pathlib import Path

import yaml

from sigma.backends.opensearch import OpensearchLuceneBackend
from sigma.collection import SigmaCollection
from sigma.rule import SigmaRule

backend = OpensearchLuceneBackend()
result = {}
for path in sorted(Path(sys.argv[1]).glob("*.yml")):
    documents = list(yaml.safe_load_all(path.read_text()))
    for document in documents:
        if "correlation" in document:
            try:
                result[document["name"]] = {"queries": [str(q) for q in backend.convert(SigmaCollection.from_yaml(path.read_text()))]}
            except NotImplementedError as e:
                result[document["name"]] = {"error": str(e)}
        else:
            # a rule on its own: inside a collection with a correlation it is marked as not to be output
            result[document["name"]] = {"queries": [str(q) for q in backend.convert_rule(SigmaRule.from_dict(document))]}
print(json.dumps(result, indent=1, sort_keys=True))
