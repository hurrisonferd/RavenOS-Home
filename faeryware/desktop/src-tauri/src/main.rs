use tauri::{Manager, PhysicalPosition};

#[tauri::command]
fn set_click_through(app: tauri::AppHandle, enabled: bool) -> Result<(), String> {
    let w = app.get_webview_window("main").ok_or("main window missing")?;
    w.set_ignore_cursor_events(enabled).map_err(|e| e.to_string())
}

#[tauri::command]
fn begin_drag(app: tauri::AppHandle) -> Result<(), String> {
    let w = app.get_webview_window("main").ok_or("main window missing")?;
    w.start_dragging().map_err(|e| e.to_string())
}

#[tauri::command]
fn move_resident(app: tauri::AppHandle, dx: i32, dy: i32) -> Result<(), String> {
    let w = app.get_webview_window("main").ok_or("main window missing")?;
    let pos = w.outer_position().map_err(|e| e.to_string())?;
    let size = w.outer_size().map_err(|e| e.to_string())?;
    let monitor = w.current_monitor().map_err(|e| e.to_string())?;
    let (mw, mh) = monitor.map(|m| { let s=m.size(); (s.width as i32,s.height as i32) }).unwrap_or((1920,1080));
    let nx=(pos.x+dx).clamp(-50,(mw-size.width as i32+50).max(-50));
    let ny=(pos.y+dy).clamp(0,(mh-size.height as i32).max(0));
    w.set_position(PhysicalPosition::new(nx,ny)).map_err(|e| e.to_string())
}

#[tauri::command]
fn perch(app: tauri::AppHandle, edge: String, y: i32) -> Result<(), String> {
    let w=app.get_webview_window("main").ok_or("main window missing")?;
    let size=w.outer_size().map_err(|e| e.to_string())?;
    let monitor=w.current_monitor().map_err(|e| e.to_string())?;
    let (mw,mh)=monitor.map(|m|{let s=m.size();(s.width as i32,s.height as i32)}).unwrap_or((1920,1080));
    let x=if edge.eq_ignore_ascii_case("right"){mw-size.width as i32+62}else{-62};
    w.set_position(PhysicalPosition::new(x,y.clamp(0,(mh-size.height as i32).max(0)))).map_err(|e| e.to_string())
}

#[tauri::command]
fn quit(app: tauri::AppHandle){ app.exit(0); }

fn main(){
    tauri::Builder::default()
      .invoke_handler(tauri::generate_handler![set_click_through,begin_drag,move_resident,perch,quit])
      .run(tauri::generate_context!())
      .expect("error while running Faeryware Resident");
}
