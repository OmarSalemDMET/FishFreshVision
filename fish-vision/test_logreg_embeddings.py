#!/usr/bin/env python3
"""Logistic regression probe on the same 600 embeddings."""

import os, sys, pickle
from pathlib import Path
from collections import defaultdict
import cv2
import numpy as np
import onnxruntime as ort
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import make_pipeline
from sklearn.model_selection import cross_val_score, StratifiedKFold

MFED = Path("data/mfed_zero_shot")
ONNX = Path("resources/fish_model_embedding.onnx")
assert MFED.exists() and ONNX.exists(), "run from fish-vision/"

FOLDERS = {"Fresh": 0, "Highly_Fresh": 1, "Not_Fresh": 2}
EXTS = {".jpg", ".jpeg", ".png"}

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
EMB_IDX = 1

feats, paths_by_label = defaultdict(list), defaultdict(list)
for name, idx in FOLDERS.items():
    imgs = [p for p in (MFED / name).rglob("*")
            if p.is_file() and p.suffix.lower() in EXTS][:200]
    for p in imgs:
        emb = sess.run(None, {in_name: preprocess(p)})[EMB_IDX][0]
        feats[idx].append(emb.astype(np.float32))
        paths_by_label[idx].append(str(p))

X = np.stack([e for v in feats.values() for e in v])
y = np.concatenate([[i]*len(v) for i, v in feats.items()])
print(f"X {X.shape}  y {y.shape}")

cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
print("\n=== Logistic regression (standardized embeddings) ===")
for C in [0.001, 0.01, 0.1, 1.0, 10.0]:
    pipe = make_pipeline(
        StandardScaler(),
        LogisticRegression(max_iter=3000, C=C, solver="lbfgs"))
    scores = cross_val_score(pipe, X, y, cv=cv, n_jobs=-1)
    print(f"  C={C:>6}: {scores.mean():.3f} ± {scores.std():.3f}")

print("\n=== Ridge classifier (baseline, faster) ===")
from sklearn.linear_model import RidgeClassifier
pipe = make_pipeline(StandardScaler(), RidgeClassifier(alpha=1.0))
scores = cross_val_score(pipe, X, y, cv=cv, n_jobs=-1)
print(f"  ridge: {scores.mean():.3f} ± {scores.std():.3f}")

print("\n=== MLP (small, if you want a ceiling) ===")
from sklearn.neural_network import MLPClassifier
pipe = make_pipeline(
    StandardScaler(),
    MLPClassifier(hidden_layer_sizes=(128,), max_iter=500,
                  early_stopping=True, random_state=42))
scores = cross_val_score(pipe, X, y, cv=cv, n_jobs=-1)
print(f"  mlp-128: {scores.mean():.3f} ± {scores.std():.3f}")

# Fit final model on all data, save for deployment
pipe_final = make_pipeline(
    StandardScaler(),
    LogisticRegression(max_iter=3000, C=1.0, solver="lbfgs")).fit(X, y)
with open("logreg_embeddings.pkl", "wb") as f:
    pickle.dump(pipe_final, f)
print("\nWrote logreg_embeddings.pkl (for Clojure port or continued testing)")

# Export coefficients + scaler params as JSON for Clojure
import json
scaler = pipe_final.named_steps["standardscaler"]
lr     = pipe_final.named_steps["logisticregression"]
with open("logreg_embeddings.json", "w") as f:
    json.dump({
        "class_names": ["Fresh", "Highly Fresh", "Not Fresh"],
        "scaler_mean": scaler.mean_.tolist(),
        "scaler_scale": scaler.scale_.tolist(),
        "coef": lr.coef_.tolist(),
        "intercept": lr.intercept_.tolist(),
    }, f, indent=2)
print("Wrote logreg_embeddings.json (ready to port to core/inference.clj)")
