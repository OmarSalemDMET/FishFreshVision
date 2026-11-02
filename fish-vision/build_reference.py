#!/usr/bin/env python3
"""Extract 200 reference embeddings/class from MFED, write EDN for Clojure."""

import cv2
import numpy as np
import onnxruntime as ort
from pathlib import Path
from collections import defaultdict

MFED = Path("data/mfed_zero_shot")
ONNX = Path("resources/fish_model_embedding.onnx")
OUT  = Path("../clojure-core/resources/knn_reference.edn")
OUT.parent.mkdir(parents=True, exist_ok=True)

FOLDERS = {"Fresh": 0, "Highly_Fresh": 1, "Not_Fresh": 2}
EXTS = {".jpg", ".jpeg", ".png"}
PER_CLASS = 200

def preprocess(path):
    bgr = cv2.imread(str(path))
    bgr = cv2.resize(bgr, (224, 224), interpolation=cv2.INTER_AREA)
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    lab  = cv2.cvtColor(bgr, cv2.COLOR_BGR2LAB)
    hsv  = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    log_g = np.log1p(gray.astype(np.float32))
    log_b = np.log1p(cv2.GaussianBlur(gray, (0,0), 30).astype(np.float32))
    ret = np.clip((log_g - log_b + 3) * 60, 0, 255).astype(np.uint8)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8,8))
    L_eq = clahe.apply(lab[:,:,0])
    lab_b = lab[:,:,2]; sat = hsv[:,:,1]
    sob = np.clip(np.sqrt(cv2.Sobel(gray, cv2.CV_64F, 1, 0, ksize=3)**2 +
                          cv2.Sobel(gray, cv2.CV_64F, 0, 1, ksize=3)**2),
                  0, 255).astype(np.uint8)
    x = np.stack([ret, L_eq, lab_b, sat, sob], -1).astype(np.float32) / 255.0
    return x.transpose(2, 0, 1)[None]

sess = ort.InferenceSession(str(ONNX), providers=["CPUExecutionProvider"])
in_name = sess.get_inputs()[0].name
EMB_IDX = 1   # confirmed: outputs are [logits, embedding]

refs = []
for name, idx in FOLDERS.items():
    imgs = [p for p in (MFED / name).rglob("*")
            if p.is_file() and p.suffix.lower() in EXTS][:PER_CLASS]
    for p in imgs:
        emb = sess.run(None, {in_name: preprocess(p)})[EMB_IDX][0]
        # L2-normalize so cosine == dot product at inference
        emb = emb / (np.linalg.norm(emb) + 1e-9)
        refs.append({"label": idx, "embedding": emb.astype(np.float32).tolist()})
    print(f"{name}: {PER_CLASS}")

# Write EDN — human-readable, Clojure-native
with OUT.open("w") as f:
    f.write("[\n")
    for r in refs:
        emb_str = " ".join(f"{v:.5f}" for v in r["embedding"])
        f.write(f"  {{:label {r['label']} :embedding [{emb_str}]}}\n")
    f.write("]\n")

print(f"wrote {len(refs)} refs -> {OUT}")
print(f"size: {OUT.stat().st_size / 1e6:.2f} MB")
