mod vision;

use serde::Serialize;
use std::{
    process::{Command, Stdio},
    sync::{mpsc, OnceLock},
    thread,
};
use tauri::{
    menu::{Menu, MenuItem},
    tray::TrayIconBuilder,
    AppHandle, Emitter, Manager, WebviewUrl, WebviewWindowBuilder,
};

#[cfg(windows)]
use windows_sys::Win32::{
    Foundation::{CloseHandle, HWND, LPARAM, RECT},
    System::Threading::{
        OpenProcess, QueryFullProcessImageNameW, PROCESS_QUERY_LIMITED_INFORMATION,
    },
    UI::{
        Accessibility::{SetWinEventHook, UnhookWinEvent},
        WindowsAndMessaging::{
            DispatchMessageW, EnumWindows, GetForegroundWindow, GetMessageW, GetSystemMetrics,
            GetWindowRect, GetWindowTextLengthW, GetWindowTextW, GetWindowThreadProcessId,
            IsIconic, IsWindow, IsWindowVisible, SetForegroundWindow, SetWindowPos, ShowWindow,
            TranslateMessage, MSG, SM_CXSCREEN, SM_CYSCREEN, SW_MAXIMIZE, SW_MINIMIZE, SW_RESTORE,
            SWP_NOZORDER, SWP_SHOWWINDOW, EVENT_OBJECT_DESTROY, EVENT_OBJECT_HIDE,
            EVENT_OBJECT_LOCATIONCHANGE, EVENT_OBJECT_SHOW, EVENT_SYSTEM_FOREGROUND,
            EVENT_SYSTEM_MINIMIZEEND, EVENT_SYSTEM_MINIMIZESTART, WINEVENT_OUTOFCONTEXT,
            WINEVENT_SKIPOWNPROCESS,
        },
    },
};

#[derive(Debug, Clone, Serialize)]
struct WindowInfo {
    id: String,
    process: String,
    kind: String,
    title: String,
    x: i32,
    y: i32,
    width: i32,
    height: i32,
    minimized: bool,
    visible: bool,
}

#[derive(Debug, Clone, Serialize)]
struct WinEventPayload {
    event: u32,
    event_name: String,
    window: WindowInfo,
}

#[derive(Debug, Clone, Serialize)]
struct PhonePortalStatus {
    scrcpy_available: bool,
    running: bool,
    adb_devices: usize,
}

fn classify_process(process: &str) -> String {
    let p = process.to_ascii_lowercase();
    if p.contains("faeryware") {
        "self"
    } else if p.contains("scrcpy") {
        "phone"
    } else if ["chrome", "msedge", "firefox", "brave", "opera"].iter().any(|x| p.contains(x)) {
        "browser"
    } else if ["code.exe", "devenv", "idea64", "pycharm", "windowsterminal", "wt.exe", "powershell", "cmd.exe"].iter().any(|x| p.contains(x)) {
        "code"
    } else if ["winword", "notepad", "obsidian", "notepad++", "typora"].iter().any(|x| p.contains(x)) {
        "document"
    } else if p.contains("excel") {
        "spreadsheet"
    } else if ["powerpnt", "keynote"].iter().any(|x| p.contains(x)) {
        "presentation"
    } else if ["teams", "zoom", "webex"].iter().any(|x| p.contains(x)) {
        "meeting"
    } else if ["discord", "slack", "telegram", "whatsapp", "outlook"].iter().any(|x| p.contains(x)) {
        "communication"
    } else if ["spotify", "vlc", "musicbee", "foobar", "tidal"].iter().any(|x| p.contains(x)) {
        "media"
    } else if p.contains("explorer.exe") {
        "home"
    } else if ["steam", "epicgameslauncher", "battle.net"].iter().any(|x| p.contains(x)) {
        "game"
    } else {
        "other"
    }.to_string()
}

#[cfg(windows)]
fn process_name(hwnd: HWND) -> String {
    unsafe {
        let mut pid = 0u32;
        GetWindowThreadProcessId(hwnd, &mut pid);
        if pid == 0 { return "unknown".into(); }
        let handle = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, 0, pid);
        if handle.is_null() { return format!("pid-{pid}"); }
        let mut buf = [0u16; 1024];
        let mut len = buf.len() as u32;
        let ok = QueryFullProcessImageNameW(handle, 0, buf.as_mut_ptr(), &mut len);
        let _ = CloseHandle(handle);
        if ok == 0 { return format!("pid-{pid}"); }
        let path = String::from_utf16_lossy(&buf[..len as usize]);
        path.rsplit(['\\', '/']).next().unwrap_or(&path).to_string()
    }
}

#[cfg(windows)]
fn window_title(hwnd: HWND) -> String {
    unsafe {
        let len = GetWindowTextLengthW(hwnd);
        if len <= 0 { return String::new(); }
        let mut buf = vec![0u16; len as usize + 1];
        let n = GetWindowTextW(hwnd, buf.as_mut_ptr(), buf.len() as i32);
        if n <= 0 { String::new() } else { String::from_utf16_lossy(&buf[..n as usize]) }
    }
}

#[cfg(windows)]
fn window_info_from_hwnd(hwnd: HWND, include_title: bool) -> Result<WindowInfo, String> {
    unsafe {
        if hwnd.is_null() || IsWindow(hwnd) == 0 { return Err("window unavailable".into()); }
        let mut rect = RECT { left: 0, top: 0, right: 0, bottom: 0 };
        if GetWindowRect(hwnd, &mut rect) == 0 { return Err("window bounds unavailable".into()); }
        let process = process_name(hwnd);
        Ok(WindowInfo {
            id: format!("{:x}", hwnd as usize),
            kind: classify_process(&process),
            process,
            title: if include_title { window_title(hwnd) } else { String::new() },
            x: rect.left,
            y: rect.top,
            width: (rect.right - rect.left).max(0),
            height: (rect.bottom - rect.top).max(0),
            minimized: IsIconic(hwnd) != 0,
            visible: IsWindowVisible(hwnd) != 0,
        })
    }
}

#[cfg(windows)]
struct EnumContext { include_titles: bool, windows: Vec<WindowInfo> }

#[cfg(windows)]
unsafe extern "system" fn enum_windows_proc(hwnd: HWND, lparam: LPARAM) -> i32 {
    let ctx = &mut *(lparam as *mut EnumContext);
    if IsWindowVisible(hwnd) == 0 { return 1; }
    if let Ok(info) = window_info_from_hwnd(hwnd, ctx.include_titles) {
        if info.width >= 90 && info.height >= 60 && info.kind != "self" && !info.process.starts_with("ApplicationFrameHost") {
            ctx.windows.push(info);
        }
    }
    1
}

#[cfg(windows)]
fn desktop_windows_internal(include_titles: bool) -> Vec<WindowInfo> {
    unsafe {
        let mut ctx = EnumContext { include_titles, windows: Vec::new() };
        let _ = EnumWindows(Some(enum_windows_proc), &mut ctx as *mut EnumContext as LPARAM);
        ctx.windows
    }
}

#[cfg(not(windows))]
fn desktop_windows_internal(_include_titles: bool) -> Vec<WindowInfo> { Vec::new() }

#[tauri::command]
fn desktop_windows(include_titles: bool) -> Vec<WindowInfo> { desktop_windows_internal(include_titles) }

#[cfg(windows)]
fn hwnd_from_id(id: &str) -> Result<HWND, String> {
    let raw = usize::from_str_radix(id.trim_start_matches("0x"), 16).map_err(|_| "invalid window id".to_string())?;
    if raw == 0 { return Err("invalid window id".into()); }
    Ok(raw as HWND)
}

#[cfg(windows)]
#[tauri::command]
fn window_action(id: String, action: String) -> Result<(), String> {
    unsafe {
        let hwnd = hwnd_from_id(&id)?;
        if IsWindow(hwnd) == 0 { return Err("window disappeared".into()); }
        match action.as_str() {
            "focus" => {
                let _ = ShowWindow(hwnd, SW_RESTORE);
                if SetForegroundWindow(hwnd) == 0 { return Err("Windows refused foreground focus".into()); }
            }
            "minimize" => { let _ = ShowWindow(hwnd, SW_MINIMIZE); }
            "restore" => { let _ = ShowWindow(hwnd, SW_RESTORE); }
            "maximize" => { let _ = ShowWindow(hwnd, SW_MAXIMIZE); }
            "snap-left" | "snap-right" => {
                let sw = GetSystemMetrics(SM_CXSCREEN).max(640);
                let sh = GetSystemMetrics(SM_CYSCREEN).max(480);
                let x = if action == "snap-right" { sw / 2 } else { 0 };
                let _ = ShowWindow(hwnd, SW_RESTORE);
                if SetWindowPos(hwnd, std::ptr::null_mut(), x, 0, sw / 2, sh, SWP_NOZORDER | SWP_SHOWWINDOW) == 0 {
                    return Err("window snap failed".into());
                }
            }
            _ => return Err("unsupported reversible window action".into()),
        }
        Ok(())
    }
}

#[cfg(not(windows))]
#[tauri::command]
fn window_action(_id: String, _action: String) -> Result<(), String> { Err("window control is available on Windows".into()) }

#[cfg(windows)]
#[tauri::command]
fn foreground_app() -> Result<WindowInfo, String> {
    unsafe { window_info_from_hwnd(GetForegroundWindow(), false) }
}

#[cfg(not(windows))]
#[tauri::command]
fn foreground_app() -> Result<WindowInfo, String> { Err("foreground awareness is available on Windows".into()) }

fn ensure_habitat(app: &AppHandle) -> Result<(), String> {
    if let Some(w) = app.get_webview_window("habitat") {
        w.show().map_err(|e| e.to_string())?;
        return Ok(());
    }
    let w = WebviewWindowBuilder::new(app, "habitat", WebviewUrl::App("habitat.html".into()))
        .title("Faeryware // Windows Habitat")
        .fullscreen(true)
        .transparent(true)
        .decorations(false)
        .always_on_top(true)
        .skip_taskbar(true)
        .resizable(false)
        .build()
        .map_err(|e| e.to_string())?;
    w.set_ignore_cursor_events(true).map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
fn show_habitat(app: AppHandle) -> Result<(), String> { ensure_habitat(&app) }

#[tauri::command]
fn hide_habitat(app: AppHandle) -> Result<(), String> {
    if let Some(w) = app.get_webview_window("habitat") { w.hide().map_err(|e| e.to_string())?; }
    Ok(())
}

#[tauri::command]
fn sync_colony(app: AppHandle, haunt: String, _active_fae: usize) -> Result<usize, String> {
    ensure_habitat(&app)?;
    app.emit("faery://haunt", haunt.clone()).map_err(|e| e.to_string())?;
    Ok(match haunt.to_ascii_uppercase().as_str() { "CALM" => 1, "HAUNTED" => 6, "FERAL" => 24, _ => 6 })
}

#[tauri::command]
fn set_colony_click_through(app: AppHandle, enabled: bool) -> Result<(), String> {
    if let Some(w) = app.get_webview_window("habitat") { w.set_ignore_cursor_events(enabled).map_err(|e| e.to_string())?; }
    Ok(())
}

fn resident_window(app: &AppHandle, label: Option<String>) -> Result<tauri::WebviewWindow, String> {
    let key = label.unwrap_or_else(|| "main".to_string());
    app.get_webview_window(&key).ok_or_else(|| format!("window missing: {key}"))
}

#[tauri::command]
fn begin_drag(app: AppHandle, label: Option<String>) -> Result<(), String> {
    resident_window(&app, label)?.start_dragging().map_err(|e| e.to_string())
}

#[tauri::command]
fn move_resident(app: AppHandle, label: Option<String>, dx: i32, dy: i32) -> Result<(), String> {
    let w = resident_window(&app, label)?;
    let pos = w.outer_position().map_err(|e| e.to_string())?;
    w.set_position(tauri::PhysicalPosition::new(pos.x + dx, pos.y + dy)).map_err(|e| e.to_string())
}

#[tauri::command]
fn perch(app: AppHandle, label: Option<String>, edge: String, y: i32) -> Result<(), String> {
    let w = resident_window(&app, label)?;
    let size = w.outer_size().map_err(|e| e.to_string())?;
    let monitor = w.current_monitor().map_err(|e| e.to_string())?;
    let (mw, mh) = monitor.map(|m| { let s = m.size(); (s.width as i32, s.height as i32) }).unwrap_or((1920, 1080));
    let x = if edge.eq_ignore_ascii_case("right") { mw - size.width as i32 + 72 } else { -72 };
    w.set_position(tauri::PhysicalPosition::new(x, y.clamp(0, (mh - size.height as i32).max(0)))).map_err(|e| e.to_string())
}

#[tauri::command]
fn arrange_colony_context(app: AppHandle) -> Result<usize, String> { ensure_habitat(&app)?; Ok(6) }

#[tauri::command]
fn open_office_board(app: AppHandle) -> Result<(), String> {
    if let Some(w) = app.get_webview_window("office") { let _ = w.show(); let _ = w.set_focus(); return Ok(()); }
    WebviewWindowBuilder::new(&app, "office", WebviewUrl::App("office.html".into()))
        .title("Faeryware // OfficeOS Floor")
        .inner_size(880.0, 820.0)
        .decorations(true)
        .always_on_top(false)
        .skip_taskbar(false)
        .resizable(true)
        .build().map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
fn hide_office_board(app: AppHandle) -> Result<(), String> {
    if let Some(w) = app.get_webview_window("office") { w.hide().map_err(|e| e.to_string())?; }
    Ok(())
}

#[cfg(windows)]
fn hidden_command(program: &str) -> Command {
    use std::os::windows::process::CommandExt;
    let mut cmd = Command::new(program);
    cmd.creation_flags(0x08000000);
    cmd.stdout(Stdio::piped()).stderr(Stdio::piped());
    cmd
}

#[cfg(windows)]
fn command_exists(name: &str) -> bool {
    hidden_command("where.exe").arg(name).status().map(|s| s.success()).unwrap_or(false)
}

#[cfg(windows)]
fn adb_device_count() -> usize {
    let Ok(out) = hidden_command("adb.exe").arg("devices").output() else { return 0; };
    String::from_utf8_lossy(&out.stdout).lines().skip(1).filter(|line| line.trim_end().ends_with("\tdevice")).count()
}

#[cfg(windows)]
#[tauri::command]
fn phone_portal_status() -> PhonePortalStatus {
    PhonePortalStatus {
        scrcpy_available: command_exists("scrcpy.exe"),
        running: desktop_windows_internal(false).iter().any(|w| w.kind == "phone"),
        adb_devices: adb_device_count(),
    }
}

#[cfg(not(windows))]
#[tauri::command]
fn phone_portal_status() -> PhonePortalStatus { PhonePortalStatus { scrcpy_available: false, running: false, adb_devices: 0 } }

#[cfg(windows)]
#[tauri::command]
fn launch_phone_portal() -> Result<(), String> {
    if !command_exists("scrcpy.exe") { return Err("scrcpy was not found on PATH".into()); }
    use std::os::windows::process::CommandExt;
    Command::new("scrcpy.exe")
        .args(["--window-title", "Faeryware Phone Portal", "--max-size", "1200", "--stay-awake"])
        .creation_flags(0x08000000)
        .spawn().map_err(|e| format!("could not launch scrcpy: {e}"))?;
    Ok(())
}

#[cfg(not(windows))]
#[tauri::command]
fn launch_phone_portal() -> Result<(), String> { Err("Phone Portal currently targets Windows + scrcpy".into()) }

#[cfg(windows)]
#[tauri::command]
fn autostart_status() -> bool {
    hidden_command("reg.exe").args(["QUERY", r"HKCU\Software\Microsoft\Windows\CurrentVersion\Run", "/v", "FaerywareResident"]).status().map(|s| s.success()).unwrap_or(false)
}

#[cfg(not(windows))]
#[tauri::command]
fn autostart_status() -> bool { false }

#[cfg(windows)]
#[tauri::command]
fn set_autostart(enabled: bool) -> Result<(), String> {
    let exe = std::env::current_exe().map_err(|e| e.to_string())?;
    let quoted = format!("\"{}\"", exe.display());
    let mut cmd = hidden_command("reg.exe");
    if enabled {
        cmd.args(["ADD", r"HKCU\Software\Microsoft\Windows\CurrentVersion\Run", "/v", "FaerywareResident", "/t", "REG_SZ", "/d"]).arg(quoted).arg("/f");
    } else {
        cmd.args(["DELETE", r"HKCU\Software\Microsoft\Windows\CurrentVersion\Run", "/v", "FaerywareResident", "/f"]);
    }
    let out = cmd.output().map_err(|e| e.to_string())?;
    if out.status.success() { Ok(()) } else { Err(String::from_utf8_lossy(&out.stderr).trim().to_string()) }
}

#[cfg(not(windows))]
#[tauri::command]
fn set_autostart(_enabled: bool) -> Result<(), String> { Err("autostart control currently targets Windows".into()) }

#[derive(Clone, Copy)]
struct RawWinEvent { event: u32, hwnd: isize }
static WIN_EVENT_TX: OnceLock<mpsc::Sender<RawWinEvent>> = OnceLock::new();

#[cfg(windows)]
unsafe extern "system" fn win_event_proc(
    _hook: windows_sys::Win32::UI::Accessibility::HWINEVENTHOOK,
    event: u32,
    hwnd: HWND,
    id_object: i32,
    _id_child: i32,
    _event_thread: u32,
    _event_time: u32,
) {
    if hwnd.is_null() { return; }
    if event >= 0x8000 && id_object != 0 { return; }
    if let Some(tx) = WIN_EVENT_TX.get() { let _ = tx.send(RawWinEvent { event, hwnd: hwnd as isize }); }
}

fn event_name(event: u32) -> String {
    #[cfg(windows)]
    { match event {
        EVENT_SYSTEM_FOREGROUND => "FOREGROUND",
        EVENT_SYSTEM_MINIMIZESTART => "MINIMIZE_START",
        EVENT_SYSTEM_MINIMIZEEND => "MINIMIZE_END",
        EVENT_OBJECT_LOCATIONCHANGE => "LOCATION",
        EVENT_OBJECT_SHOW => "SHOW",
        EVENT_OBJECT_HIDE => "HIDE",
        EVENT_OBJECT_DESTROY => "DESTROY",
        _ => "WINDOW",
    }.into() }
    #[cfg(not(windows))]
    { let _ = event; "WINDOW".into() }
}

fn start_win_event_bridge(app: AppHandle) {
    let (tx, rx) = mpsc::channel::<RawWinEvent>();
    if WIN_EVENT_TX.set(tx).is_err() { return; }
    let emitter = app.clone();
    thread::spawn(move || {
        while let Ok(raw) = rx.recv() {
            #[cfg(windows)] {
                let hwnd = raw.hwnd as HWND;
                if let Ok(window) = window_info_from_hwnd(hwnd, false) {
                    if window.kind != "self" {
                        let payload = WinEventPayload { event: raw.event, event_name: event_name(raw.event), window };
                        let _ = emitter.emit("faery://window-event", payload);
                    }
                }
            }
        }
    });
    #[cfg(windows)]
    thread::spawn(move || unsafe {
        let watched = [EVENT_SYSTEM_FOREGROUND, EVENT_SYSTEM_MINIMIZESTART, EVENT_SYSTEM_MINIMIZEEND, EVENT_OBJECT_LOCATIONCHANGE, EVENT_OBJECT_SHOW, EVENT_OBJECT_HIDE, EVENT_OBJECT_DESTROY];
        let mut hooks = Vec::new();
        for event in watched {
            let hook = SetWinEventHook(event, event, std::ptr::null_mut(), Some(win_event_proc), 0, 0, WINEVENT_OUTOFCONTEXT | WINEVENT_SKIPOWNPROCESS);
            if !hook.is_null() { hooks.push(hook); }
        }
        let mut msg: MSG = std::mem::zeroed();
        while GetMessageW(&mut msg, std::ptr::null_mut(), 0, 0) > 0 {
            let _ = TranslateMessage(&msg);
            DispatchMessageW(&msg);
        }
        for hook in hooks { let _ = UnhookWinEvent(hook); }
    });
}

fn show_control(app: &AppHandle) {
    if let Some(w) = app.get_webview_window("main") { let _ = w.show(); let _ = w.set_focus(); }
}

fn setup_tray(app: &tauri::App) -> tauri::Result<()> {
    let ghost = MenuItem::with_id(app, "ghost", "Ghost House", true, None::<&str>)?;
    let office = MenuItem::with_id(app, "office", "OfficeOS Floor", true, None::<&str>)?;
    let summon = MenuItem::with_id(app, "summon", "Summon Habitat", true, None::<&str>)?;
    let calm = MenuItem::with_id(app, "calm", "CALM // 1", true, None::<&str>)?;
    let haunted = MenuItem::with_id(app, "haunted", "HAUNTED // 6", true, None::<&str>)?;
    let feral = MenuItem::with_id(app, "feral", "FERAL // 6 + 18", true, None::<&str>)?;
    let phone = MenuItem::with_id(app, "phone", "Open Phone Portal", true, None::<&str>)?;
    let hide = MenuItem::with_id(app, "hide", "Banish Visuals", true, None::<&str>)?;
    let quit = MenuItem::with_id(app, "quit", "Quit Faeryware", true, None::<&str>)?;
    let menu = Menu::with_items(app, &[&ghost, &office, &summon, &calm, &haunted, &feral, &phone, &hide, &quit])?;
    let mut tray = TrayIconBuilder::with_id("faeryware-resident").menu(&menu).show_menu_on_left_click(false).tooltip("Faeryware // Digi Fae resident colony");
    if let Some(icon) = app.default_window_icon().cloned() { tray = tray.icon(icon); }
    tray.on_menu_event(|app, event| match event.id.as_ref() {
        "ghost" => show_control(app),
        "office" => { let _ = open_office_board(app.clone()); }
        "summon" => { let _ = ensure_habitat(app); }
        "calm" => { let _ = app.emit("faery://haunt", "CALM"); }
        "haunted" => { let _ = app.emit("faery://haunt", "HAUNTED"); }
        "feral" => { let _ = app.emit("faery://haunt", "FERAL"); }
        "phone" => { let _ = launch_phone_portal(); }
        "hide" => { let _ = hide_habitat(app.clone()); }
        "quit" => app.exit(0),
        _ => {}
    }).build(app)?;
    Ok(())
}

#[tauri::command]
fn quit(app: AppHandle) { app.exit(0); }

fn main() {
    tauri::Builder::default()
        .setup(|app| {
            setup_tray(app)?;
            ensure_habitat(app.handle()).map_err(std::io::Error::other)?;
            start_win_event_bridge(app.handle().clone());
            Ok(())
        })
        .invoke_handler(tauri::generate_handler![
            desktop_windows, window_action, foreground_app, show_habitat, hide_habitat, sync_colony,
            set_colony_click_through, begin_drag, move_resident, perch, arrange_colony_context,
            open_office_board, hide_office_board, phone_portal_status, launch_phone_portal,
            autostart_status, set_autostart, vision::capture_vision_frame, vision::vision_status,
            vision::ollama_vision, quit
        ])
        .run(tauri::generate_context!())
        .expect("error while running Faeryware Resident");
}
