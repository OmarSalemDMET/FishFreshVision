use anyhow::{Context, Result};
use opencv::{
    core::{self, Mat, Size},
    imgcodecs, imgproc,
    prelude::*,
};
use serde::Serialize;

pub const TARGET_SIZE: i32 = 224;
pub const NUM_CHANNELS: usize = 5;
pub const TENSOR_LEN: usize = NUM_CHANNELS * (TARGET_SIZE as usize) * (TARGET_SIZE as usize);

// ==========================================
// 1. TENSOR PREPROCESSING (For the ONNX Model)
// ==========================================

pub fn preprocess_image(image_path: &str) -> Result<Vec<f32>> {
    let img = imgcodecs::imread(image_path, imgcodecs::IMREAD_COLOR)
        .with_context(|| format!("Failed to load image: {}", image_path))?;
    if img.empty() {
        anyhow::bail!("Image is empty or could not be decoded: {}", image_path);
    }

    let mut resized = Mat::default();
    imgproc::resize(
        &img,
        &mut resized,
        Size::new(TARGET_SIZE, TARGET_SIZE),
        0.0,
        0.0,
        imgproc::INTER_AREA,
    )?;

    let mut gray = Mat::default();
    imgproc::cvt_color(&resized, &mut gray, imgproc::COLOR_BGR2GRAY, 0)?;

    let mut lab = Mat::default();
    imgproc::cvt_color(&resized, &mut lab, imgproc::COLOR_BGR2Lab, 0)?;

    let mut hsv = Mat::default();
    imgproc::cvt_color(&resized, &mut hsv, imgproc::COLOR_BGR2HSV, 0)?;

    let ch_retinex = compute_retinex(&gray)?;
    let ch_clahe = compute_clahe_l(&lab)?;
    let ch_lab_b = extract_channel(&lab, 2)?;
    let ch_sat = extract_channel(&hsv, 1)?;
    let ch_sobel = compute_sobel_magnitude(&gray)?;

    let channels = [ch_retinex, ch_clahe, ch_lab_b, ch_sat, ch_sobel];
    let mut tensor = vec![0.0f32; TENSOR_LEN];
    let plane_size = (TARGET_SIZE * TARGET_SIZE) as usize;

    for (c_idx, mat) in channels.iter().enumerate() {
        let data = mat.data_bytes()?;
        for i in 0..plane_size {
            tensor[c_idx * plane_size + i] = data[i] as f32 / 255.0;
        }
    }

    Ok(tensor)
}

fn compute_retinex(gray: &Mat) -> Result<Mat> {
    let mut blurred = Mat::default();
    imgproc::gaussian_blur(
        gray,
        &mut blurred,
        Size::new(0, 0),
        30.0,
        0.0,
        core::BORDER_DEFAULT,
    )?;

    let gray_data = gray.data_bytes()?;
    let blur_data = blurred.data_bytes()?;
    let n = gray_data.len();
    let mut out = vec![0u8; n];

    for i in 0..n {
        let g = (gray_data[i] as f32).ln_1p();
        let b = (blur_data[i] as f32).ln_1p();
        let val = ((g - b + 3.0) * 60.0).clamp(0.0, 255.0);
        out[i] = val as u8;
    }

    let mat = Mat::from_slice(&out)?;
    let reshaped = mat.reshape(1, TARGET_SIZE)?;
    Ok(reshaped.clone_pointee())
}

fn compute_clahe_l(lab: &Mat) -> Result<Mat> {
    let mut l_channel = Mat::default();
    core::extract_channel(lab, &mut l_channel, 0)?;
    let mut clahe = imgproc::create_clahe(2.0, Size::new(8, 8))?;
    let mut out = Mat::default();
    clahe.apply(&l_channel, &mut out)?;
    Ok(out)
}

fn extract_channel(mat: &Mat, index: i32) -> Result<Mat> {
    let mut out = Mat::default();
    core::extract_channel(mat, &mut out, index)?;
    Ok(out)
}

fn compute_sobel_magnitude(gray: &Mat) -> Result<Mat> {
    let mut grad_x = Mat::default();
    let mut grad_y = Mat::default();
    imgproc::sobel(
        gray,
        &mut grad_x,
        core::CV_64F,
        1,
        0,
        3,
        1.0,
        0.0,
        core::BORDER_DEFAULT,
    )?;
    imgproc::sobel(
        gray,
        &mut grad_y,
        core::CV_64F,
        0,
        1,
        3,
        1.0,
        0.0,
        core::BORDER_DEFAULT,
    )?;

    let mut magnitude = Mat::default();
    core::magnitude(&grad_x, &grad_y, &mut magnitude)?;
    let mut out = Mat::default();
    magnitude.convert_to(&mut out, core::CV_8U, 1.0, 0.0)?;
    Ok(out)
}

// ==========================================
// 2. IMAGE ANALYSIS TOOLS (For the Agent)
// ==========================================

#[derive(Serialize)]
pub struct AnalysisResult {
    pub edge_density: f32,
    pub color_yellowness: f32,
    pub color_saturation: f32,
    pub fft_low_freq_ratio: f32,
}

pub fn analyze_image(image_path: &str) -> Result<AnalysisResult> {
    let img = imgcodecs::imread(image_path, imgcodecs::IMREAD_COLOR)
        .with_context(|| format!("Failed to load image: {}", image_path))?;
    if img.empty() {
        anyhow::bail!("Image is empty or could not be decoded: {}", image_path);
    }

    let mut resized = Mat::default();
    imgproc::resize(
        &img,
        &mut resized,
        Size::new(TARGET_SIZE, TARGET_SIZE),
        0.0,
        0.0,
        imgproc::INTER_AREA,
    )?;

    // 1. Edge Density (Canny)
    let mut gray = Mat::default();
    imgproc::cvt_color(&resized, &mut gray, imgproc::COLOR_BGR2GRAY, 0)?;
    let mut edges = Mat::default();
    imgproc::canny(&gray, &mut edges, 50.0, 150.0, 3, false)?;
    let edge_mean = core::mean(&edges, &core::no_array())?[0] as f32 / 255.0;

    // 2. Color Stats
    let mut lab = Mat::default();
    imgproc::cvt_color(&resized, &mut lab, imgproc::COLOR_BGR2Lab, 0)?;
    let mut hsv = Mat::default();
    imgproc::cvt_color(&resized, &mut hsv, imgproc::COLOR_BGR2HSV, 0)?;

    let lab_b_mean = core::mean(&lab, &core::no_array())?[2] as f32;
    let sat_mean = core::mean(&hsv, &core::no_array())?[1] as f32;

    // 3. Localized 2D FFT (crop centre 64x64 around the pupil)
    let cx = 112;
    let cy = 112;
    let crop_size = 64;
    let roi = core::Rect::new(cx - crop_size / 2, cy - crop_size / 2, crop_size, crop_size);
    let mut crop = Mat::default();
    Mat::roi(&gray, roi)?.copy_to(&mut crop)?;

    let fft_ratio = compute_fft_low_freq_ratio(&crop)?;

    Ok(AnalysisResult {
        edge_density: edge_mean,
        color_yellowness: lab_b_mean,
        color_saturation: sat_mean,
        fft_low_freq_ratio: fft_ratio,
    })
}

/// Computes a proper 2-D DFT low-frequency ratio.
///
/// The spectrum is left in "natural" OpenCV order (no fft-shift): DC is at (0,0)
/// and the low-frequency content is split across the four corners. For a real
/// image the spectrum is conjugate-symmetric, so all four corner blocks contain
/// equivalent low-frequency information. We sum a `k x k` block at each corner
/// and divide by the total spectral energy.
fn compute_fft_low_freq_ratio(gray_crop: &Mat) -> Result<f32> {
    // OpenCV's DFT works best on float32.
    let mut crop_f = Mat::default();
    gray_crop.convert_to(&mut crop_f, core::CV_32F, 1.0, 0.0)?;

    // Pad to an optimal DFT size (64 is already optimal, but be safe).
    let opt_rows = core::get_optimal_dft_size(crop_f.rows())?;
    let opt_cols = core::get_optimal_dft_size(crop_f.cols())?;
    let mut padded = Mat::default();
    core::copy_make_border(
        &crop_f,
        &mut padded,
        0,
        opt_rows - crop_f.rows(),
        0,
        opt_cols - crop_f.cols(),
        core::BORDER_CONSTANT,
        core::Scalar::all(0.0),
    )?;

    // Build a 2-channel complex Mat: [real = image, imag = zeros].
    let zero_plane = Mat::zeros(padded.rows(), padded.cols(), core::CV_32F)?.to_mat()?;
    let mut planes = core::Vector::<Mat>::new();
    planes.push(padded);
    planes.push(zero_plane);
    let mut complex_i = Mat::default();
    core::merge(&planes, &mut complex_i)?;

    // Forward 2-D DFT.
    let mut dft_out = Mat::default();
    core::dft(&complex_i, &mut dft_out, core::DFT_COMPLEX_OUTPUT, 0)?;

    // Split into real / imaginary and compute magnitude.
    let mut dft_planes = core::Vector::<Mat>::new();
    core::split(&dft_out, &mut dft_planes)?;
    let re = dft_planes.get(0)?;
    let im = dft_planes.get(1)?;
    let mut mag = Mat::default();
    core::magnitude(&re, &im, &mut mag)?;

    let total: f64 = core::sum_elems(&mag)?[0];

    // Low-frequency block size: 1/8 of each dimension.
    let k_rows = (opt_rows / 8).max(1);
    let k_cols = (opt_cols / 8).max(1);

    let low = sum_roi(&mag, 0, 0, k_rows, k_cols)?
        + sum_roi(&mag, 0, opt_cols - k_cols, k_rows, k_cols)?
        + sum_roi(&mag, opt_rows - k_rows, 0, k_rows, k_cols)?
        + sum_roi(&mag, opt_rows - k_rows, opt_cols - k_cols, k_rows, k_cols)?;

    // Ratio in [0, 1]. Guard against a zero spectrum.
    Ok((low / (total + 1e-9)) as f32)
}

fn sum_roi(m: &Mat, row_start: i32, col_start: i32, rows: i32, cols: i32) -> Result<f64> {
    let roi = core::Rect::new(col_start, row_start, cols, rows);
    let sub = Mat::roi(m, roi)?;
    Ok(core::sum_elems(&sub)?[0])
}
