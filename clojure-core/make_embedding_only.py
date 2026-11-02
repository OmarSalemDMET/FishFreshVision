#!/usr/bin/env python3
"""Extract a single-output embedding model from fish_model_embedding.onnx.

Uses manual graph reconstruction (onnx.helper) instead of onnx.utils.extract_model,
which chokes on some MobileNet exports during shape inference.

Walks backward from the embedding tensor, keeps only contributing nodes,
and rebuilds the graph with the embedding as the sole output.
"""

import onnx
from onnx import helper, TensorProto

SRC = "resources/fish_model_embedding.onnx"
DST = "resources/fish_model_embedding_only.onnx"

# ---- Load ----
print(f"loading {SRC}")
model = onnx.load(SRC)
graph = model.graph
print(f"  nodes:        {len(graph.node)}")
print(f"  initializers: {len(graph.initializer)}")
print(f"  outputs:      {[o.name for o in graph.output]}")

# ---- Find the embedding tensor (input of the FIRST Gemm/MatMul) ----
first_linear = None
for node in graph.node:
    if node.op_type in ("Gemm", "MatMul"):
        first_linear = node
        break

if first_linear is None:
    raise SystemExit("no Gemm/MatMul found in graph — wrong model?")

feature_tensor = first_linear.input[0]
print(f"\nembedding tensor = {feature_tensor}")
print(f"produced by op    = {first_linear.op_type}")
print(f"feeding op        = {first_linear.op_type} (first linear)")

# ---- Walk backward: keep only nodes contributing to feature_tensor ----
needed_node_ids = set()
needed_tensors  = {feature_tensor}

changed = True
while changed:
    changed = False
    for i, node in enumerate(graph.node):
        if i in needed_node_ids:
            continue
        if any(out in needed_tensors for out in node.output):
            needed_node_ids.add(i)
            for inp in node.input:
                if inp and inp not in needed_tensors:
                    needed_tensors.add(inp)
                    changed = True

print(f"\nkept {len(needed_node_ids)} / {len(graph.node)} nodes")
dropped = len(graph.node) - len(needed_node_ids)
print(f"dropped {dropped} nodes (classifier head)")

# ---- Rebuild graph ----
new_nodes = [n for i, n in enumerate(graph.node) if i in needed_node_ids]

# Keep only initializers referenced by surviving nodes
needed_inits = [init for init in graph.initializer
                if init.name in needed_tensors]
print(f"kept {len(needed_inits)} / {len(graph.initializer)} initializers")

# New output — dynamic batch, 576 features
new_output = helper.make_tensor_value_info(
    feature_tensor, TensorProto.FLOAT, ["batch_size", 576])

new_graph = helper.make_graph(
    nodes=new_nodes,
    name=graph.name + "_embedding_only",
    inputs=list(graph.input),
    outputs=[new_output],
    initializer=needed_inits,
)

new_model = helper.make_model(
    new_graph,
    opset_imports=list(model.opset_import),
    producer_name="extract_embedding",
    ir_version=model.ir_version,
)

# ---- Save ----
onnx.checker.check_model(new_model)
onnx.save(new_model, DST)

import os
print(f"\nwrote {DST}")
print(f"  size: {os.path.getsize(DST) / 1e6:.2f} MB")

# ---- Verify ----
import onnxruntime as ort
import numpy as np

sess = ort.InferenceSession(DST, providers=["CPUExecutionProvider"])
out  = sess.run(None,
                {sess.get_inputs()[0].name:
                 np.random.rand(1, 5, 224, 224).astype(np.float32)})
print(f"  input shape:  {sess.get_inputs()[0].shape}")
print(f"  output shape: {out[0].shape}")

assert out[0].shape == (1, 576), f"expected (1, 576), got {out[0].shape}"
print("\nOK — embedding-only model verified.")
