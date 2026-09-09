use base64::{engine::general_purpose::STANDARD as B64, Engine as _};
use serde::Serialize;
use std::{thread, time::{Duration, Instant}};
use tauri::{AppHandle, Emitter, Manager};

#[cfg(windows)]
use windows_sys::Win32::{
    Foundation::{HWND, RECT},
    Graphics::Gdi::{
        BitBlt, CreateCompatibleBitmap, CreateCompatibleDC, DeleteDC, DeleteObject, GetDC,
        GetDIBits, ReleaseDC, SelectObject, BITMAPINFO, BITMAPINFOHEADER, BI_RGB, CAPTUREBLT,
        DIB_RGB_COLORS, SRCCOPY,
    },
    UI::WindowsAndMessaging::{
        GetForegroundWindow, GetSystemMetrics, GetWindowRect, IsWindow, SM_CXVIRTUALSCREEN,
        SM_CYVIRTUALSCREEN, SM_XVIRTUALSCREEN, SM_YVIRTUALSCREEN,
    },
};

#[derive(Debug, Clone, Serialize)]
pub struct VisionStatus {
    pub supported: bool,
    pub capture: String,
    pub ollama_reachable: bool,
    pub endpoint: String,
    pub models: Vec<String>,
    pub privacy: String,
}

#[derive(Debug, Clone, Serialize)]
pub struct VisionFrame {
    pub scope: String,
    pub window_id: String,
    pub width: u32,
    pub height: u32,
    pub jpeg_base64: String,
}

#[derive(Debug, Clone, Serialize)]
pub struct VisionAnalysis {
    pub scope: String,
    pub window_id: String,
    pub width: u32,
    pub height: u32,
    pub model: String,
    pub summary: String,
    pub elapsed_ms: u128,
    pub transport: String,
}

#[cfg(windows)]
fn hwnd_from_id(id: &str) -> Result<HWND, String> {
    let raw = usize::from_str_radix(id.trim_start_matches("0x"), 16)
        .map_err(|_| "invalid vision target window id".to_string())?;
    if raw == 0 { return Err("invalid vision target window id".into()); }
    Ok(raw as HWND)
}

#[cfg(windows)]
fn capture_rect(scope: &str, window_id: Option<&str>) -> Result<(i32, i32, i32, i32, String), String> {
    unsafe {
        match scope {
            "desktop" => {
                let x = GetSystemMetrics(SM_XVIRTUALSCREEN);
                let y = GetSystemMetrics(SM_YVIRTUALSCREEN);
                let w = GetSystemMetrics(SM_CXVIRTUALSCREEN).max(1);
                let h = GetSystemMetrics(SM_CYVIRTUALSCREEN).max(1);
                Ok((x, y, w, h, String::new()))
            }
            "window" | "foreground" => {
                let hwnd = if let Some(id) = window_id { hwnd_from_id(id)? } else { GetForegroundWindow() };
                if hwnd.is_null() || IsWindow(hwnd) == 0 { return Err("vision target window disappeared".into()); }
                let mut r = RECT { left: 0, top: 0, right: 0, bottom: 0 };
                if GetWindowRect(hwnd, &mut r) == 0 { return Err("could not read vision target bounds".into()); }
                let w = (r.right - r.left).max(1);
                let h = (r.bottom - r.top).max(1);
                Ok((r.left, r.top, w, h, format!("{:x}", hwnd as usize)))
            }
            _ => Err("vision scope must be desktop or window".into()),
        }
    }
}

#[cfg(windows)]
fn capture_bgra(x: i32, y: i32, width: i32, height: i32) -> Result<Vec<u8>, String> {
    unsafe {
        let screen = GetDC(std::ptr::null_mut());
        if screen.is_null() { return Err("GetDC failed".into()); }
        let mem = CreateCompatibleDC(screen);
        if mem.is_null() { let _ = ReleaseDC(std::ptr::null_mut(), screen); return Err("CreateCompatibleDC failed".into()); }
        let bitmap = CreateCompatibleBitmap(screen, width, height);
        if bitmap.is_null() {
            let _ = DeleteDC(mem); let _ = ReleaseDC(std::ptr::null_mut(), screen);
            return Err("CreateCompatibleBitmap failed".into());
        }
        let old = SelectObject(mem, bitmap);
        let blt = BitBlt(mem, 0, 0, width, height, screen, x, y, SRCCOPY | CAPTUREBLT);
        if blt == 0 {
            let _ = SelectObject(mem, old); let _ = DeleteObject(bitmap); let _ = DeleteDC(mem); let _ = ReleaseDC(std::ptr::null_mut(), screen);
            return Err("BitBlt screen capture failed".into());
        }
        let mut info: BITMAPINFO = std::mem::zeroed();
        info.bmiHeader = BITMAPINFOHEADER {
            biSize: std::mem::size_of::<BITMAPINFOHEADER>() as u32,
            biWidth: width,
            biHeight: -height,
            biPlanes: 1,
            biBitCount: 32,
            biCompression: BI_RGB,
            ..std::mem::zeroed()
        };
        let mut pixels = vec![0u8; width as usize * height as usize * 4];
        let lines = GetDIBits(mem, bitmap, 0, height as u32, pixels.as_mut_ptr().cast(), &mut info, DIB_RGB_COLORS);
        let _ = SelectObject(mem, old); let _ = DeleteObject(bitmap); let _ = DeleteDC(mem); let _ = ReleaseDC(std::ptr::null_mut(), screen);
        if lines == 0 { return Err("GetDIBits failed".into()); }
        Ok(pixels)
    }
}

#[cfg(windows)]
fn encode_jpeg(scope: &str, window_id: Option<&str>, max_width: u32) -> Result<VisionFrame, String> {
    let (x, y, w, h, resolved_id) = capture_rect(scope, window_id)?;
    if w > 12000 || h > 12000 { return Err("capture surface too large".into()); }
    let bgra = capture_bgra(x, y, w, h)?;
    let mut rgb = Vec::with_capacity(w as usize * h as usize * 3);
    for p in bgra.chunks_exact(4) { rgb.extend_from_slice(&[p[2], p[1], p[0]]); }
    let mut image = image::RgbImage::from_raw(w as u32, h as u32, rgb).ok_or("could not construct vision frame")?;
    let cap = max_width.clamp(320, 1600);
    if image.width() > cap {
        let nh = ((image.height() as f64 * cap as f64 / image.width() as f64).round() as u32).max(1);
        image = image::imageops::resize(&image, cap, nh, image::imageops::FilterType::Triangle);
    }
    let mut jpeg = Vec::new();
    image::codecs::jpeg::JpegEncoder::new_with_quality(&mut jpeg, 76)
        .encode(&image, image.width(), image.height(), image::ExtendedColorType::Rgb8)
        .map_err(|e| format!("jpeg encode failed: {e}"))?;
    Ok(VisionFrame {
        scope: scope.to_string(),
        window_id: resolved_id,
        width: image.width(),
        height: image.height(),
        jpeg_base64: B64.encode(jpeg),
    })
}

#[cfg(not(windows))]
fn encode_jpeg(_scope: &str, _window_id: Option<&str>, _max_width: u32) -> Result<VisionFrame, String> {
    Err("Fae Eyes pixel capture currently targets Windows".into())
}

fn with_habitat_hidden<T>(app: &AppHandle, f: impl FnOnce() -> Result<T, String>) -> Result<T, String> {
    let habitat = app.get_webview_window("habitat");
    let was_visible = habitat.as_ref().and_then(|w| w.is_visible().ok()).unwrap_or(false);
    if was_visible {
        if let Some(w) = habitat.as_ref() { let _ = w.hide(); }
        thread::sleep(Duration::from_millis(75));
    }
    let out = f();
    if was_visible { if let Some(w) = habitat.as_ref() { let _ = w.show(); } }
    out
}

#[tauri::command]
pub fn capture_vision_frame(app: AppHandle, scope: String, window_id: Option<String>, max_width: Option<u32>) -> Result<VisionFrame, String> {
    with_habitat_hidden(&app, || encode_jpeg(&scope.to_ascii_lowercase(), window_id.as_deref(), max_width.unwrap_or(1120)))
}

#[tauri::command]
pub async fn vision_status() -> VisionStatus {
    let endpoint = "http://127.0.0.1:11434".to_string();
    let client = reqwest::Client::builder().timeout(Duration::from_millis(900)).build();
    let mut models = Vec::new();
    let mut reachable = false;
    if let Ok(client) = client {
        if let Ok(resp) = client.get(format!("{endpoint}/api/tags")).send().await {
            reachable = resp.status().is_success();
            if let Ok(v) = resp.json::<serde_json::Value>().await {
                if let Some(items) = v.get("models").and_then(|x| x.as_array()) {
                    for item in items {
                        if let Some(name) = item.get("name").or_else(|| item.get("model")).and_then(|x| x.as_str()) { models.push(name.to_string()); }
                    }
                }
            }
        }
    }
    VisionStatus {
        supported: cfg!(windows),
        capture: "explicit desktop/window snapshot; no continuous recording".into(),
        ollama_reachable: reachable,
        endpoint,
        models,
        privacy: "pixels stay in memory; only 127.0.0.1 Ollama is contacted; :cloud models are blocked".into(),
    }
}

#[tauri::command]
pub async fn ollama_vision(app: AppHandle, scope: String, window_id: Option<String>, model: String, prompt: Option<String>) -> Result<VisionAnalysis, String> {
    let model = if model.trim().is_empty() { "gemma3".to_string() } else { model.trim().to_string() };
    if model.to_ascii_lowercase().contains(":cloud") { return Err("Fae Eyes blocks Ollama cloud models because screen pixels must stay local".into()); }
    let started = Instant::now();
    let frame = with_habitat_hidden(&app, || encode_jpeg(&scope.to_ascii_lowercase(), window_id.as_deref(), 1120))?;
    let instruction = prompt.unwrap_or_else(|| "You are Fae Eyes inside a playful desktop companion. Describe only what is visibly present in this screenshot. In at most 70 words: identify the current activity, important visible UI/window state, and one useful reversible next action. Do not infer hidden/private facts. Do not claim an action occurred. Be concrete.".into());
    let request = serde_json::json!({
        "model": model,
        "messages": [{"role":"user","content":instruction,"images":[frame.jpeg_base64]}],
        "stream": false,
        "options": {"temperature": 0.2}
    });
    let client = reqwest::Client::builder().timeout(Duration::from_secs(75)).build().map_err(|e| e.to_string())?;
    let response = client.post("http://127.0.0.1:11434/api/chat").json(&request).send().await
        .map_err(|e| format!("local Ollama vision request failed: {e}"))?;
    if !response.status().is_success() { return Err(format!("local Ollama returned HTTP {}", response.status())); }
    let value: serde_json::Value = response.json().await.map_err(|e| format!("invalid Ollama response: {e}"))?;
    let summary = value.pointer("/message/content").and_then(|v| v.as_str()).unwrap_or("").trim().chars().take(1200).collect::<String>();
    if summary.is_empty() { return Err("local vision model returned an empty observation".into()); }
    let analysis = VisionAnalysis {
        scope: frame.scope,
        window_id: frame.window_id,
        width: frame.width,
        height: frame.height,
        model,
        summary,
        elapsed_ms: started.elapsed().as_millis(),
        transport: "127.0.0.1:11434 only".into(),
    };
    let _ = app.emit("faery://vision-result", analysis.clone());
    Ok(analysis)
}
