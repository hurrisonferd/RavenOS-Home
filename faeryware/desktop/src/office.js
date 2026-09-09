const invoke=window.__TAURI__.core.invoke;
const listen=window.__TAURI__.event.listen;
const fae=[
{id:'kyu',name:'KYU',color:'#ff4e9d'},{id:'paimon',name:'PAIMON',color:'#5ef27e'},{id:'luma',name:'LUMA',color:'#ffc85c'},{id:'nyx',name:'NYX',color:'#5e8cff'},{id:'sylph',name:'SYLPH',color:'#55dcff'},{id:'qira',name:'QIRA',color:'#f04cff'}
];
const kindColor={browser:'#55dcff',phone:'#55dcff',code:'#5ef27e',document:'#f04cff',spreadsheet:'#5ef27e',presentation:'#55dcff',meeting:'#ff4e9d',communication:'#f04cff',media:'#5e8cff',home:'#ffc85c',game:'#ff4e9d',other:'#aaa'};
const lane=document.querySelector('#lane'),processEl=document.querySelector('#process'),lead=document.querySelector('#lead'),returnClass=document.querySelector('#returnClass'),authority=document.querySelector('#authority'),floor=document.querySelector('#floor'),historyEl=document.querySelector('#history'),windowsEl=document.querySelector('#windows'),portalStatus=document.querySelector('#portalStatus');
const defaultState={ts:null,process:'waiting',kind:'quiet',lane:'QUIET FLOOR',lead:'KYU',leadIndex:0,returnClass:'CONTRIBUTION',effectAuthority:'NONE',proof:'NO MATERIAL CONTEXT YET'};
let titleVision=false;
function readState(){try{return {...defaultState,...JSON.parse(localStorage.getItem('faeryware.office.state')||'{}')}}catch{return defaultState}}
function readHistory(){try{return JSON.parse(localStorage.getItem('faeryware.office.history')||'[]')}catch{return []}}
function desktopState(){try{return JSON.parse(localStorage.getItem('faeryware.desktop.state')||'{}')}catch{return {}}}
function renderCore(){
 const s=readState();lane.textContent=s.lane;processEl.textContent=s.process;lead.textContent=s.lead;returnClass.textContent=s.returnClass;authority.textContent=s.effectAuthority;
 floor.innerHTML='';fae.forEach((f,i)=>{const card=document.createElement('div');card.className='fae'+(i===s.leadIndex?' lead':'');card.style.setProperty('--fae',f.color);const img=document.createElement('img');img.src=window.FAERY_ASSETS[f.id];img.alt=f.name;const name=document.createElement('strong');name.textContent=f.name;const status=document.createElement('span');status.textContent=i===s.leadIndex?'ACTIVE CONTRIBUTOR':'AVAILABLE';card.append(img,name,status);floor.appendChild(card)});
 historyEl.innerHTML='';const hist=readHistory();if(!hist.length){historyEl.textContent='No observed work context yet.';return}hist.slice().reverse().forEach(e=>{const row=document.createElement('div');row.className='event';const t=e.ts?new Date(e.ts).toLocaleTimeString():'';row.textContent=`${e.lead} · ${e.lane} · ${e.process} · ${t}`;historyEl.appendChild(row)});
}
async function act(id,action){try{await invoke('window_action',{id,action});setTimeout(refreshWindows,180)}catch(e){console.warn(e)}}
function makeAction(label,id,action){const b=document.createElement('button');b.textContent=label;b.onclick=()=>act(id,action);return b}
async function refreshWindows(){
 let items=[];try{items=await invoke('desktop_windows',{includeTitles:titleVision})}catch(e){console.warn(e)}windowsEl.innerHTML='';
 if(!items.length){const x=document.createElement('div');x.className='empty';x.textContent='No visible external windows detected.';windowsEl.appendChild(x);return}
 items.slice(0,24).forEach(w=>{const row=document.createElement('div');row.className='windowRow';row.style.setProperty('--c',kindColor[w.kind]||kindColor.other);const top=document.createElement('div');top.className='windowTop';const name=document.createElement('div');name.className='windowName';name.textContent=w.process;const kind=document.createElement('div');kind.className='windowKind';kind.textContent=w.kind.toUpperCase();top.append(name,kind);const title=document.createElement('div');title.className='windowTitle';title.textContent=titleVision?(w.title||'untitled window'):'title vision off';const meta=document.createElement('div');meta.className='windowMeta';meta.textContent=`${w.width}×${w.height} @ ${w.x},${w.y}${w.minimized?' // MINIMIZED':''}`;const actions=document.createElement('div');actions.className='windowActions';actions.append(makeAction('FOCUS',w.id,'focus'),makeAction('MIN',w.id,'minimize'),makeAction('RESTORE',w.id,'restore'),makeAction('MAX',w.id,'maximize'),makeAction('⇤ LEFT',w.id,'snap-left'),makeAction('RIGHT ⇥',w.id,'snap-right'));row.append(top,title,meta,actions);windowsEl.appendChild(row)});
}
async function refreshPortal(){try{const p=await invoke('phone_portal_status');portalStatus.textContent=p.running?'PHONE PORTAL // LIVE':p.scrcpy_available?`PHONE PORTAL // READY // ${p.adb_devices} DEVICE${p.adb_devices===1?'':'S'}`:'PHONE PORTAL // SCRCPY NOT FOUND'}catch{portalStatus.textContent='PHONE PORTAL // UNKNOWN'}}
async function refreshAutostart(){try{const on=await invoke('autostart_status');document.querySelector('#autostart').textContent=`AUTOSTART: ${on?'ON':'OFF'}`}catch{}}
document.querySelectorAll('[data-haunt]').forEach(b=>b.onclick=async()=>{const h=b.dataset.haunt,s=readState();await invoke('sync_colony',{haunt:h,activeFae:s.leadIndex||0});localStorage.setItem('faeryware.desktop.haunt.request',JSON.stringify({haunt:h,ts:Date.now()}))});
document.querySelector('#sync').onclick=async()=>{const s=readState(),h=desktopState().haunt||'FERAL';await invoke('sync_colony',{haunt:h,activeFae:s.leadIndex||0})};
document.querySelector('#summon').onclick=()=>invoke('show_habitat');
document.querySelector('#phone').onclick=async()=>{try{await invoke('launch_phone_portal');portalStatus.textContent='PHONE PORTAL // SUMMONING';setTimeout(refreshPortal,1800)}catch(e){portalStatus.textContent=String(e)}};
document.querySelector('#titles').onclick=()=>{titleVision=!titleVision;document.querySelector('#titles').textContent=`TITLE VISION: ${titleVision?'ON':'OFF'}`;refreshWindows()};
document.querySelector('#autostart').onclick=async()=>{const on=await invoke('autostart_status');await invoke('set_autostart',{enabled:!on});refreshAutostart()};
document.querySelector('#hide').onclick=()=>invoke('hide_office_board');
listen('faery://window-event',e=>{const p=e.payload||{};if(p.window&&p.event_name==='FOREGROUND'){const idx={browser:4,phone:4,code:1,document:5,spreadsheet:1,presentation:4,meeting:0,communication:5,media:3,home:2,game:0}[p.window.kind]??1;const rec={ts:Date.now(),process:p.window.process,kind:p.window.kind,lane={browser:'DISCOVERY',phone:'PHONE PORTAL',code:'BUILD',document:'WRITE',spreadsheet:'ANALYZE',presentation:'PRESENT',meeting:'COORDINATE',communication:'COMMUNICATE',media:'LISTEN',home:'HOME',game:'PLAY'}[p.window.kind]||'OBSERVE',lead:fae[idx].name,leadIndex:idx,returnClass:'CONTRIBUTION',effectAuthority:'NONE',proof:'WINEVENT_PROCESS_AND_BOUNDS'};localStorage.setItem('faeryware.office.state',JSON.stringify(rec))}renderCore();refreshWindows()});
window.addEventListener('storage',e=>{if(e.key==='faeryware.office.state'||e.key==='faeryware.office.history')renderCore()});
renderCore();refreshWindows();refreshPortal();refreshAutostart();setInterval(refreshWindows,5000);setInterval(refreshPortal,10000);setInterval(renderCore,2200);
