#!/usr/bin/env python3
"""Test whether frozen FFE embeddings separate MFED classes.
Run from ~/Documents/work/rust/fish-vision/"""

import os
import sys
from pathlib import Path
from collections import defaultdict
import cv2
import numpy as np
import onnxruntime as ort
from sklearn.neighbors import KNeighborsClassifier
from sklearn.model_selection import cross_val_score

# ---- Sanity: CWD and paths ----
print(f"CWD: {os.getcwd()}")
MFED = Path("data/mfed_zero_shot")
ONNX = Path("resources/fish_model_embedding.onnx")
print(f"MFED exists:   {MFED.exists()}  ({MFED.resolve() if MFED.exists() else 'N/A'})")
print(f"ONNX exists:   {ONNX.exists()}")
if not MFED.exists() or not ONNX.exists():
    print("FATAL: run this script from ~/Documents/work/rust/fish-vision/")
    sys.exit(1)

# Show actual folder contents
print("\nMFED contents:")
for p in sorted(MFED.iterdir()):
    tag = "DIR " if p.is_dir() else "FILE"
    print(f"  [{tag}] {p.name}")

# ---- File discovery: case-insensitive, multiple extensions ----
EXTS = {".jpg", ".jpeg", ".png"}
FOLDERS = {"Fresh": 0, "Highly_Fresh": 1, "Not_Fresh": 2}

def find_images(folder_path, limit=None):
    out = []
    for p in folder_path.rglob("*"):
        if p.is_file() and p.suffix.lower() in EXTS:
            out.append(p)
            if limit and len(out) >= limit:
                break
    return out

print("\nImage counts per class:")
per_class_paths = {}
for name, idx in FOLDERS.items():
    folder = MFED / name
    if not folder.exists():
        print(f"  {name}: FOLDER MISSING")
        continue
    imgs = find_images(folder, limit=200)
    per_class_paths[idx] = imgs
    print(f"  {name}: {len(imgs)} sampled (folder has more?)")

if any(len(v) == 0 for v in per_class_paths.values()):
    print("\nFATAL: at least one class has 0 images. Check folder names above.")
    sys.exit(1)

# ---- Preprocess (matches Rust pipeline) ----
def preprocess(path):
    bgr = cv2.imread(str(path))
    if bgr is None:
        return None
    bgr = cv2.resize(bgr, (224, 224), interpolation=cv2.INTER_AREA)
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    lab  = cv2.cvtColor(bgr, cv2.COLOR_BGR2LAB)
    hsv  = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)
    log_g = np.log1p(gray.astype(np.float32))
    log_b = np.log1p(cv2.GaussianBlur(gray, (0, 0), 30).astype(np.float32))
    ret   = np.clip((log_g - log_b + 3) * 60, 0, 255).astype(np.uint8)
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    L_eq  = clahe.apply(lab[:, :, 0])
    lab_b = lab[:, :, 2]
    sat   = hsv[:, :, 1]
    sob   = np.clip(np.sqrt(
                cv2.Sobel(gray, cv2.CV_64F, 1, 0, ksize=3) ** 2 +
                cv2.Sobel(gray, cv2.CV_64F, 0, 1, ksize=3) ** 2),
             0, 255).astype(np.uint8)
    stacked = np.stack([ret, L_eq, lab_b, sat, sob], -1).astype(np.float32) / 255.0
    return stacked.transpose(2, 0, 1)[None]

# ---- Extract embeddings ----
sess = ort.InferenceSession(str(ONNX), providers=["CPUExecutionProvider"])
in_name = sess.get_inputs()[0].name
print(f"\nONNX outputs: {[o.shape for o in sess.get_outputs()]}")
# Expect [(1, 3), (1, 576)] — logits first, embedding second
EMB_IDX = 1   # 0=logits, 1=embedding

feats = defaultdict(list)
for idx, paths in per_class_paths.items():
    for p in paths:
        x = preprocess(p)
        if x is None:
            continue
        outs = sess.run(None, {in_name: x})
        emb = outs[EMB_IDX][0].astype(np.float32)   # (576,)
        feats[idx].append(emb)
    print(f"  class {idx}: {len(feats[idx])} embeddings extracted")

# ---- Separation ----
centroids = {i: np.mean(np.stack(v), axis=0) for i, v in feats.items()}

def dprime(i, j):
    ci, cj = centroids[i], centroids[j]
    fi, fj = np.stack(feats[i]), np.stack(feats[j])
    si = np.sqrt(np.mean(np.sum((fi - ci) ** 2, axis=1)))
    sj = np.sqrt(np.mean(np.sum((fj - cj) ** 2, axis=1)))
    return float(np.linalg.norm(ci - cj) / ((si + sj) / 2))

print("\n=== Pairwise separation (Cohen's d on embeddings) ===")
for a, b in [(0, 1), (1, 2), (0, 2)]:
    print(f"  class {a} vs {b}: d = {dprime(a, b):.2f}")

# ---- k-NN cross-validation ----
X = np.stack([e for v in feats.values() for e in v])
y = np.concatenate([[i] * len(v) for i, v in feats.items()])
print(f"\nX shape: {X.shape}, y shape: {y.shape}, y labels: {np.unique(y)}")

for k in [1, 3, 5, 9]:
    scores = cross_val_score(KNeighborsClassifier(n_neighbors=k), X, y, cv=5)
    print(f"  k={k}: CV accuracy = {scores.mean():.3f} ± {scores.std():.3f}")
