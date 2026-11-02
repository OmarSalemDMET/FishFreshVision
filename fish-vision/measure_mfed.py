import cv2, numpy as np
from pathlib import Path
import pandas as pd

MFED = Path("data/mfed_zero_shot")
FOLDERS = {"Fresh": "Fresh", "Highly_Fresh": "Highly Fresh", "Not_Fresh": "Not Fresh"}

def features(path):
    bgr = cv2.imread(str(path))
    if bgr is None: return None
    bgr = cv2.resize(bgr, (224, 224), interpolation=cv2.INTER_AREA)
    gray = cv2.cvtColor(bgr, cv2.COLOR_BGR2GRAY)
    lab  = cv2.cvtColor(bgr, cv2.COLOR_BGR2LAB)
    hsv  = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV)

    edges = cv2.Canny(gray, 50, 150)
    edge_density = edges.mean() / 255.0
    yellowness   = lab[:, :, 2].mean()
    saturation   = hsv[:, :, 1].mean()

    # 64x64 center crop, 2-D DFT, low/total energy
    cy, cx, k = 112, 112, 32
    crop = gray[cy-k:cy+k, cx-k:cx+k].astype(np.float32)
    mag  = np.abs(np.fft.fftshift(np.fft.fft2(crop)))
    # low-freq block (1/8 of side)
    n = 8
    h, w = mag.shape
    low = (mag[:n, :n].sum() + mag[:n, -n:].sum() +
           mag[-n:, :n].sum() + mag[-n:, -n:].sum())
    total = mag.sum()
    fft_ratio = float(low / (total + 1e-9))

    return {
        "edge_density": float(edge_density),
        "yellowness":   float(yellowness),
        "saturation":   float(saturation),
        "fft_ratio":    float(fft_ratio),
    }

rows = []
for folder, label in FOLDERS.items():
    for p in (MFED / folder).rglob("*"):
        if p.suffix.lower() not in {".jpg", ".jpeg", ".png"}: continue
        f = features(p)
        if f is None: continue
        rows.append({"label": label, **f})

df = pd.DataFrame(rows)
print(f"total: {len(df)}")
print("\n=== Per-class means ===")
print(df.groupby("label").mean().round(3))
print("\n=== Per-class std ===")
print(df.groupby("label").std().round(3))

# Separation between adjacent classes (Fresh vs Not Fresh is the hardest pair)
print("\n=== Effect size (Cohen's d) per feature ===")
for feat in ["edge_density", "yellowness", "saturation", "fft_ratio"]:
    for a, b in [("Fresh", "Highly Fresh"), ("Highly Fresh", "Not Fresh"), ("Fresh", "Not Fresh")]:
        ga, gb = df[df.label == a][feat], df[df.label == b][feat]
        d = (ga.mean() - gb.mean()) / np.sqrt((ga.std()**2 + gb.std()**2) / 2 + 1e-9)
        print(f"  {feat:14s} {a:14s} vs {b:14s}  d = {d:+.2f}")

df.to_csv("mfed_features.csv", index=False)
print("\nwrote mfed_features.csv")
