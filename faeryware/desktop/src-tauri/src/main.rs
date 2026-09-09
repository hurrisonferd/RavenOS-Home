use tauri::{Manager, PhysicalPosition, WebviewUrl, WebviewWindowBuilder};

fn resident_window(app: &tauri::AppHandle, label: Option<String>) -> Result<tauri::WebviewWindow, String> {
    let key = label.unwrap_or_else(|| "main".to_string());
    app.get_webview_window(&key).ok_or_else(|| format!("window missing: {key}"))
}

#[tauri::command]
fn set_click_through(app: tauri::AppHandle, label: Option<String>, enabled: bool) -> Result<(), String> {
    resident_window(&app, label)?.set_ignore_cursor_events(enabled).map_err(|e| e.to_string())
}

#[tauri::command]
fn set_colony_click_through(app: tauri::AppHandle, enabled: bool) -> Result<(), String> {
    for label in std::iter::once("main".to_string()).chain((1..=5).map(|i| format!("fae-companion-{i}"))) {
        if let Some(w) = app.get_webview_window(&label) {
            let _ = w.set_ignore_cursor_events(enabled);
        }
    }
    Ok(())
}

#[tauri::command]
fn begin_drag(app: tauri::AppHandle, label: Option<String>) -> Result<(), String> {
    resident_window(&app, label)?.start_dragging().map_err(|e| e.to_string())
}

#[tauri::command]
fn move_resident(app: tauri::AppHandle, label: Option<String>, dx: i32, dy: i32) -> Result<(), String> {
    let w = resident_window(&app, label)?;
    let pos = w.outer_position().map_err(|e| e.to_string())?;
    let size = w.outer_size().map_err(|e| e.to_string())?;
    let monitor = w.current_monitor().map_err(|e| e.to_string())?;
    let (mw, mh) = monitor.map(|m| { let s = m.size(); (s.width as i32, s.height as i32) }).unwrap_or((1920, 1080));
    let nx = (pos.x + dx).clamp(-70, (mw - size.width as i32 + 70).max(-70));
    let ny = (pos.y + dy).clamp(0, (mh - size.height as i32).max(0));
    w.set_position(PhysicalPosition::new(nx, ny)).map_err(|e| e.to_string())
}

#[tauri::command]
fn perch(app: tauri::AppHandle, label: Option<String>, edge: String, y: i32) -> Result<(), String> {
    let w = resident_window(&app, label)?;
    let size = w.outer_size().map_err(|e| e.to_string())?;
    let monitor = w.current_monitor().map_err(|e| e.to_string())?;
    let (mw, mh) = monitor.map(|m| { let s = m.size(); (s.width as i32, s.height as i32) }).unwrap_or((1920, 1080));
    let x = if edge.eq_ignore_ascii_case("right") { mw - size.width as i32 + 72 } else { -72 };
    w.set_position(PhysicalPosition::new(x, y.clamp(0, (mh - size.height as i32).max(0)))).map_err(|e| e.to_string())
}

fn station_for_fae(fae: usize, mw: i32, mh: i32) -> (i32, i32) {
    match fae % 6 {
        0 => ((mw - 270).max(0), (mh - 315).max(0)),
        1 => ((mw - 270).max(0), 45),
        2 => (8, (mh - 315).max(0)),
        3 => (((mw - 250) / 2).max(0), (mh - 300).max(0)),
        4 => (8, 42),
        _ => ((mw - 270).max(0), ((mh - 280) / 2).max(0)),
    }
}

#[tauri::command]
fn sync_colony(app: tauri::AppHandle, haunt: String, active_fae: usize) -> Result<usize, String> {
    let desired = match haunt.to_ascii_uppercase().as_str() {
        "FERAL" => 5,
        "HAUNTED" => 2,
        _ => 0,
    };
    let (mw, mh) = if let Some(main) = app.get_webview_window("main") {
        main.current_monitor().ok().flatten().map(|m| { let s = m.size(); (s.width as i32, s.height as i32) }).unwrap_or((1920, 1080))
    } else { (1920, 1080) };

    for slot in 1..=5 {
        let label = format!("fae-companion-{slot}");
        if slot <= desired {
            let fae = (active_fae + slot) % 6;
            if app.get_webview_window(&label).is_none() {
                let url = format!("index.html?companion=1&fae={fae}&label={label}");
                WebviewWindowBuilder::new(&app, label.clone(), WebviewUrl::App(url.into()))
                    .title("Faeryware Goblin")
                    .inner_size(250.0, 280.0)
                    .transparent(true)
                    .decorations(false)
                    .always_on_top(true)
                    .skip_taskbar(true)
                    .resizable(false)
                    .build()
                    .map_err(|e| e.to_string())?;
            }
            if let Some(w) = app.get_webview_window(&label) {
                let (x, y) = station_for_fae(fae, mw, mh);
                let _ = w.set_position(PhysicalPosition::new(x, y));
                let _ = w.show();
            }
        } else if let Some(w) = app.get_webview_window(&label) {
            let _ = w.close();
        }
    }
    Ok(desired + 1)
}

#[derive(serde::Serialize)]
struct ForegroundApp {
    process: String,
    kind: String,
    x: i32,
    y: i32,
    width: i32,
    height: i32,
}

fn classify_process(process: &str) -> String {
    let p = process.to_ascii_lowercase();
    if p.contains("faeryware") { "self" }
    else if ["chrome", "msedge", "firefox", "brave", "opera"].iter().any(|x| p.contains(x)) { "browser" }
    else if ["code.exe", "devenv", "idea64", "pycharm", "windowsterminal", "wt.exe", "powershell"].iter().any(|x| p.contains(x)) { "code" }
    else if ["winword", "notepad", "obsidian", "notepad++", "typora"].iter().any(|x| p.contains(x)) { "document" }
    else if p.contains("excel") { "spreadsheet" }
    else if ["powerpnt", "keynote"].iter().any(|x| p.contains(x)) { "presentation" }
    else if ["teams", "zoom", "webex"].iter().any(|x| p.contains(x)) { "meeting" }
    else if ["discord", "slack", "telegram", "whatsapp", "outlook"].iter().any(|x| p.contains(x)) { "communication" }
    else if ["spotify", "vlc", "musicbee", "foobar", "tidal"].iter().any(|x| p.contains(x)) { "media" }
    else if p.contains("explorer.exe") { "home" }
    else if ["steam", "epicgameslauncher", "battle.net"].iter().any(|x| p.contains(x)) { "game" }
    else { "other" }.to_string()
}

#[cfg(windows)]
fn foreground_details() -> Result<ForegroundApp, String> {
    use windows_sys::Win32::Foundation::{CloseHandle, RECT};
    use windows_sys::Win32::System::Threading::{OpenProcess, QueryFullProcessImageNameW, PROCESS_QUERY_LIMITED_INFORMATION};
    use windows_sys::Win32::UI::WindowsAndMessaging::{GetForegroundWindow, GetWindowRect, GetWindowThreadProcessId};
    unsafe {
        let hwnd = GetForegroundWindow();
        if hwnd.is_null() { return Err("no foreground window".into()); }
        let mut rect = RECT { left: 0, top: 0, right: 0, bottom: 0 };
        if GetWindowRect(hwnd, &mut rect) == 0 { return Err("foreground bounds unavailable".into()); }
        let mut pid = 0u32;
        GetWindowThreadProcessId(hwnd, &mut pid);
        if pid == 0 { return Err("foreground pid unavailable".into()); }
        let handle = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, 0, pid);
        if handle.is_null() { return Err("foreground process unavailable".into()); }
        let mut buf = [0u16; 1024];
        let mut len = buf.len() as u32;
        let ok = QueryFullProcessImageNameW(handle, 0, buf.as_mut_ptr(), &mut len);
        let _ = CloseHandle(handle);
        if ok == 0 { return Err("foreground process name unavailable".into()); }
        let path = String::from_utf16_lossy(&buf[..len as usize]);
        let process = path.rsplit(['\\', '/']).next().unwrap_or(&path).to_string();
        let kind = classify_process(&process);
        Ok(ForegroundApp {
            process,
            kind,
            x: rect.left,
            y: rect.top,
            width: (rect.right - rect.left).max(0),
            height: (rect.bottom - rect.top).max(0),
        })
    }
}

#[cfg(windows)]
#[tauri::command]
fn foreground_app() -> Result<ForegroundApp, String> { foreground_details() }

#[cfg(not(windows))]
#[tauri::command]
fn foreground_app() -> Result<ForegroundApp, String> {
    Ok(ForegroundApp { process: "unknown".into(), kind: "other".into(), x: 0, y: 0, width: 0, height: 0 })
}

#[cfg(windows)]
#[tauri::command]
fn arrange_colony_context(app: tauri::AppHandle) -> Result<usize, String> {
    let fg = foreground_details()?;
    if fg.kind == "self" { return Ok(0); }
    let labels: Vec<String> = std::iter::once("main".to_string()).chain((1..=5).map(|i| format!("fae-companion-{i}"))).collect();
    let mut moved = 0usize;
    for (i, label) in labels.iter().enumerate() {
        let Some(w) = app.get_webview_window(label) else { continue };
        let size = w.outer_size().map_err(|e| e.to_string())?;
        let ww = size.width as i32;
        let wh = size.height as i32;
        let pad = 14;
        let (mut x, mut y) = match i {
            0 => (fg.x + fg.width - ww / 2, fg.y + fg.height - wh / 2),
            1 => (fg.x - ww + pad, fg.y + pad),
            2 => (fg.x + fg.width - pad, fg.y + pad),
            3 => (fg.x + pad, fg.y + fg.height - wh + pad),
            4 => (fg.x + fg.width / 2 - ww / 2, fg.y - wh / 2),
            _ => (fg.x + fg.width / 2 - ww / 2, fg.y + fg.height - wh / 2),
        };
        if let Ok(Some(mon)) = w.current_monitor() {
            let s = mon.size();
            x = x.clamp(-80, (s.width as i32 - ww + 80).max(-80));
            y = y.clamp(0, (s.height as i32 - wh).max(0));
        }
        let _ = w.set_position(PhysicalPosition::new(x, y));
        moved += 1;
    }
    Ok(moved)
}

#[cfg(not(windows))]
#[tauri::command]
fn arrange_colony_context(_app: tauri::AppHandle) -> Result<usize, String> { Ok(0) }

#[tauri::command]
fn open_office_board(app: tauri::AppHandle) -> Result<(), String> {
    if let Some(w) = app.get_webview_window("office") {
        let _ = w.show();
        let _ = w.set_focus();
        return Ok(());
    }
    WebviewWindowBuilder::new(&app, "office", WebviewUrl::App("office.html".into()))
        .title("Faeryware // OfficeOS Floor")
        .inner_size(720.0, 760.0)
        .decorations(true)
        .always_on_top(false)
        .skip_taskbar(false)
        .resizable(true)
        .build()
        .map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
fn hide_office_board(app: tauri::AppHandle) -> Result<(), String> {
    if let Some(w) = app.get_webview_window("office") { w.hide().map_err(|e| e.to_string())?; }
    Ok(())
}

#[tauri::command]
fn quit(app: tauri::AppHandle) { app.exit(0); }

fn main() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![set_click_through, set_colony_click_through, begin_drag, move_resident, perch, sync_colony, foreground_app, arrange_colony_context, open_office_board, hide_office_board, quit])
        .run(tauri::generate_context!())
        .expect("error while running Faeryware Resident");
}
