# add_embedding_output.py
import onnx
from onnx import TensorProto, helper

SRC = "resources/fish_model_fp32.onnx"   # fp32, not _static
DST = "resources/fish_model_embedding.onnx"

m = onnx.load(SRC)

# Find the final Gemm/MatMul — its input is the penultimate features
last_gemm = next(n for n in m.graph.node
                 if n.op_type in ("Gemm", "MatMul"))
feature_tensor = last_gemm.input[0]
print(f"Using tensor: {feature_tensor}")

# Add it as an additional graph output
m.graph.output.append(helper.make_tensor_value_info(
    feature_tensor, TensorProto.FLOAT, None))

onnx.save(m, DST)
print(f"wrote {DST}")
