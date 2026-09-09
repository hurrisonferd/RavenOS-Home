const RESPONSES = {
  "GOD SIGHT": ["GOD MAP online.", "24 Ghost Ports addressable.", "Public shell: private owner truth intentionally hidden."],
  "GOD REPLAY": ["God Replay source surface ready.", "Causal ancestry requires explicit recorded parents.", "Effect replay is disabled."],
  "GOD MEMORY": ["Haunt Ledger surface ready.", "Historical residue is separate from current truth.", "Perfect memory is not claimed."],
  "WHAT IS HOLDING": ["Public shell cannot inspect private hold receipts.", "Private RavenOS keeps runner, host, owner, and runtime holds typed separately."],
  "WHO IS HERE": ["KYU • PAIMON • LUMA • SYLPH • QIRA • NYX", "Presence shown here is UI projection, not effect authority."],
  "WHERE ARE THE GHOSTS": ["Raven Desk • Map Room • Boundary Room • Game Room", "Learning Room • Workshop • Ghost Archive • Fae Bullpen • Evidence"],
  "WHAT IS HAUNTING": ["Current public projection: God Map + FairyOS + haunted rooms.", "This page contains no private runtime state."],
};

const terminal = document.getElementById('terminal');
function renderCommand(name){
  const lines = RESPONSES[name] || ["Known public command surface; live private result unavailable in this shell."];
  terminal.innerHTML = `<div><span class="prompt">RAVEN&gt;</span> ${name}</div>` + lines.map(x => `<div>${x}</div>`).join('');
  try { localStorage.setItem('ravenos:lastCommand', name); } catch (_) {}
}
document.querySelectorAll('.command').forEach(btn => btn.addEventListener('click', () => renderCommand(btn.dataset.command)));

let deferredPrompt = null;
const installTop = document.getElementById('installPwa');
const installBottom = document.getElementById('installPwaBottom');
function showInstall(){ installTop?.classList.remove('hidden'); }
window.addEventListener('beforeinstallprompt', e => { e.preventDefault(); deferredPrompt = e; showInstall(); });
async function installPwa(){
  if (deferredPrompt){
    deferredPrompt.prompt();
    await deferredPrompt.userChoice;
    deferredPrompt = null;
    installTop?.classList.add('hidden');
    return;
  }
  terminal.innerHTML = `<div><span class="prompt">RAVEN&gt;</span> PHONE MODE</div><div>Android Chrome: menu ⋮ → Add to Home screen / Install app.</div><div>iPhone Safari: Share → Add to Home Screen.</div>`;
}
installTop?.addEventListener('click', installPwa);
installBottom?.addEventListener('click', installPwa);

async function wireLatestApk(){
  const links = [document.getElementById('apkLink'), document.getElementById('apkLinkBottom')];
  try {
    const r = await fetch('https://api.github.com/repos/hurrisonferd/RavenOS-Home/releases/latest', {headers:{'Accept':'application/vnd.github+json'}});
    if (!r.ok) throw new Error('no release');
    const release = await r.json();
    const asset = (release.assets || []).find(x => /\.apk$/i.test(x.name));
    if (!asset) throw new Error('no apk');
    links.forEach(a => { a.href = asset.browser_download_url; a.textContent = 'Download APK'; a.classList.remove('disabled'); a.removeAttribute('aria-disabled'); });
  } catch (_) {
    links.forEach(a => { a.textContent = 'APK pending build'; });
  }
}
wireLatestApk();

if ('serviceWorker' in navigator) window.addEventListener('load', () => navigator.serviceWorker.register('./sw.js').catch(() => {}));

try {
  const previous = localStorage.getItem('ravenos:lastCommand');
  if (previous && RESPONSES[previous]) renderCommand(previous);
} catch (_) {}
