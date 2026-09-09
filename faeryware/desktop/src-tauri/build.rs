use std::{env, fs, path::PathBuf};

fn put_u16(v: &mut Vec<u8>, n: u16) { v.extend_from_slice(&n.to_le_bytes()); }
fn put_u32(v: &mut Vec<u8>, n: u32) { v.extend_from_slice(&n.to_le_bytes()); }
fn put_i32(v: &mut Vec<u8>, n: i32) { v.extend_from_slice(&n.to_le_bytes()); }

fn ensure_windows_icon() {
    let dir = PathBuf::from(env::var("CARGO_MANIFEST_DIR").expect("manifest dir")).join("icons");
    let path = dir.join("icon.ico");
    if path.exists() { return; }
    fs::create_dir_all(&dir).expect("create icons dir");
    const W: u32 = 32; const H: u32 = 32;
    let mask_bytes = ((W + 31) / 32 * 4) * H;
    let image_bytes = 40 + W * H * 4 + mask_bytes;
    let mut ico = Vec::with_capacity((22 + image_bytes) as usize);
    put_u16(&mut ico, 0); put_u16(&mut ico, 1); put_u16(&mut ico, 1);
    ico.push(W as u8); ico.push(H as u8); ico.push(0); ico.push(0);
    put_u16(&mut ico, 1); put_u16(&mut ico, 32); put_u32(&mut ico, image_bytes); put_u32(&mut ico, 22);
    put_u32(&mut ico, 40); put_i32(&mut ico, W as i32); put_i32(&mut ico, (H * 2) as i32);
    put_u16(&mut ico, 1); put_u16(&mut ico, 32); put_u32(&mut ico, 0); put_u32(&mut ico, W * H * 4);
    put_i32(&mut ico, 0); put_i32(&mut ico, 0); put_u32(&mut ico, 0); put_u32(&mut ico, 0);
    for y in (0..H).rev() { for x in 0..W {
        let dx=x as i32-16; let dy=y as i32-16; let r2=dx*dx+dy*dy;
        let (b,g,r,a)=if r2<145 {(120,22,(255_i32-r2).clamp(90,255) as u8,255)} else if r2<190 {(210,80,255,235)} else {(0,0,0,0)};
        ico.extend_from_slice(&[b,g,r,a]);
    }}
    ico.resize((22 + image_bytes) as usize, 0);
    fs::write(path, ico).expect("write generated icon.ico");
}

fn main() { ensure_windows_icon(); tauri_build::build(); }
