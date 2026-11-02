#!/usr/bin/env python3
"""Rewrite fish_model_embedding_only.onnx with concrete dims so Diamond
   can parse it. Only touches shapes — no graph/weight changes."""
import onnx, os
from onnx.shape_inference import infer_shapes

SRC = "resources/fish_model_embedding_only.onnx"
DST = "resources/fish_model_embedding_only.onnx"  # overwrite in place

m = onnx.load(SRC)

# 1. Force input: [1, 5, 224, 224]
inp = m.graph.input[0]
del inp.type.tensor_type.shape.dim[:]
for v in [1, 5, 224, 224]:
    d = inp.type.tensor_type.shape.dim.add()
    d.dim_value = v
inp.type.tensor_type.elem_type = onnx.TensorProto.FLOAT

# 2. Force all graph outputs: [1, 576]
for out in m.graph.output:
    del out.type.tensor_type.shape.dim[:]
    for v in [1, 576]:
        d = out.type.tensor_type.shape.dim.add()
        d.dim_value = v
    out.type.tensor_type.elem_type = onnx.TensorProto.FLOAT

# 3. Purge any leftover dim_param in value_info (shape inference will
#    repopulate these concretely from the fixed input)
keep_vi = []
for vi in m.graph.value_info:
    # Drop — infer_shapes will recompute from the concrete input
    pass
m.graph.ClearField("value_info")

# 4. Run shape inference — fills value_info with concrete [1, 576] dims
m = infer_shapes(m, strict_mode=False)

# 5. Strip any remaining symbolic dims that inference left behind
for vi in m.graph.value_info:
    for d in vi.type.tensor_type.shape.dim:
        if d.HasField("dim_param"):
            d.dim_value = 1

onnx.save(m, DST)

# 6. Verify
m2 = onnx.load(DST, load_external_data=False)
def dims_str(tt):
    return [(d.dim_value if d.HasField("dim_value") else
             d.dim_param or "?") for d in tt.shape.dim]

print(f"size            = {os.path.getsize(DST)/1e6:.2f} MB")
print(f"input dims      = {dims_str(m2.graph.input[0].type.tensor_type)}")
print(f"output dims     = {dims_str(m2.graph.output[0].type.tensor_type)}")

n_sym = sum(1 for vi in m2.graph.value_info
            for d in vi.type.tensor_type.shape.dim
            if d.HasField("dim_param"))
print(f"symbolic dims   = {n_sym} (must be 0)")

# 7. Smoke-test onnxruntime
import onnxruntime as ort
import numpy as np
sess = ort.InferenceSession(DST, providers=["CPUExecutionProvider"])
out = sess.run(None, {sess.get_inputs()[0].name:
                      np.random.rand(1, 5, 224, 224).astype(np.float32)})[0]
print(f"ort output      = {out.shape}")

assert n_sym == 0
assert out.shape == (1, 576)
print("READY")
