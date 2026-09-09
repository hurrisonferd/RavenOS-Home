const invoke=window.__TAURI__.core.invoke;
const listen=window.__TAURI__.event.listen;

const FAE=[
  {id:'kyu',name:'KYU',accent:'#ff4e9d',glyph:'♡',role:'OPS',lines:['HI!','LET’S GO!','ON IT!','hehe.','BONK','sus...']},
  {id:'paimon',name:'PAIMON',accent:'#5ef27e',glyph:'✣',role:'META',lines:['hmm...','I SEE IT.','EXACTLY.','BIG BRAIN','sus.','ALL GOOD.']},
  {id:'luma',name:'LUMA',accent:'#ffc85c',glyph:'✿',role:'HOME',lines:['GOOD MORNING','YOU GOT THIS','COMFY','IT’S OKAY','BEAUTIFUL','HOME. ♡']},
  {id:'nyx',name:'NYX',accent:'#5e8cff',glyph:'☾',role:'WATCH',lines:['...','WATCHING.','UNDERSTOOD.','REST.','NOTED.','LATER.']},
  {id:'sylph',name:'SYLPH',accent:'#55dcff',glyph:'🦋',role:'SIGNAL',lines:['LET’S EXPLORE!','SO COOL!','IDEA!','ZOOM!','CURIOUS...','NEW PATH!']},
  {id:'qira',name:'QIRA',accent:'#f04cff',glyph:'◇',role:'BOUNDARY',lines:['YES.','NO.','SAY IT.','BOUNDARIES.','EXCUSE ME?','REAL TALK.']}
];
const KIND_TO_FAE={browser:4,phone:4,code:1,spreadsheet:1,document:5,presentation:4,meeting:0,communication:5,media:3,home:2,game:0};
const KIND_LINE={browser:'NEW PATH!',phone:'PHONE PORTAL!',code:'I SEE IT.',spreadsheet:'BIG BRAIN',document:'SAY IT.',presentation:'IDEA!',meeting:'ON IT!',communication:'BOUNDARIES.',media:'WATCHING.',home:'HOME. ♡',game:'LET’S GO!'};

const habitat=document.querySelector('#habitat');
const layer=document.querySelector('#faeLayer');
const moteLayer=document.querySelector('#motes');
const territoryLayer=document.querySelector('#territories');
const signalState=document.querySelector('#signalState');

let state={haunt:'FERAL',windows:[],active:null,phone:null,lastEvent:0};
try{const saved=JSON.parse(localStorage.getItem('faeryware.desktop.state')||'{}');if(saved.haunt)state.haunt=saved.haunt}catch{}
if(!['CALM','HAUNTED','FERAL'].includes(state.haunt))state.haunt='FERAL';

const residents=FAE.map((f,i)=>{
  const el=document.createElement('section');
  el.className='fae';el.dataset.id=f.id;el.style.setProperty('--accent',f.accent);el.style.setProperty('--x','40px');el.style.setProperty('--y','40px');el.style.setProperty('--scale',i===0?'1.04':'0.92');
  el.innerHTML=`<div class="stamp"></div><div class="speech"></div><img class="body" draggable="false" alt="${f.name} resident"><div class="name">${f.glyph} ${f.name}</div>`;
  el.querySelector('.body').src=window.FAERY_ASSETS[f.id];layer.appendChild(el);
  return {f,el,speech:el.querySelector('.speech'),stamp:el.querySelector('.stamp'),poseTimer:null};
});

for(let i=0;i<18;i++){
  const f=FAE[i%FAE.length],m=document.createElement('i');m.className='mote';m.style.setProperty('--accent',f.accent);m.style.setProperty('--glyph',`"${f.glyph}"`);m.style.setProperty('--dur',`${5.5+(i%7)*.8}s`);m.style.setProperty('--mx',`${(i%2?1:-1)*(18+(i*11)%70)}px`);m.style.setProperty('--my',`${-18-((i*17)%90)}px`);m.style.left=`${4+((i*37)%91)}%`;m.style.top=`${8+((i*53)%78)}%`;moteLayer.appendChild(m);
}

function persistHaunt(){let saved={};try{saved=JSON.parse(localStorage.getItem('faeryware.desktop.state')||'{}')}catch{}saved.haunt=state.haunt;localStorage.setItem('faeryware.desktop.state',JSON.stringify(saved))}
function setHaunt(level){state.haunt=['CALM','HAUNTED','FERAL'].includes(level)?level:'HAUNTED';habitat.dataset.haunt=state.haunt;persistHaunt();signalState.textContent=state.haunt==='FERAL'?'6 RESIDENTS // 18 ECHOES // WINDOW WATCH':state.haunt==='HAUNTED'?'6 RESIDENTS // 9 ECHOES':'1 LEAD RESIDENT // QUIET ECHOES';arrange(true)}
function clamp(v,min,max){return Math.max(min,Math.min(max,v))}
function frameFor(w){if(!w||w.minimized||w.width<80||w.height<60)return null;return {l:w.x,t:w.y,r:w.x+w.width,b:w.y+w.height,w:w.width,h:w.height}}
function desktop(){return {w:window.innerWidth,h:window.innerHeight}}
function preferredWindow(kind){if(kind==='phone')return state.windows.find(w=>w.kind==='phone'&&!w.minimized)||null;return state.windows.find(w=>w.kind===kind&&!w.minimized)||null}
function targetFor(i){
  const {w:sw,h:sh}=desktop(),active=frameFor(state.active),phone=frameFor(preferredWindow('phone')),own={w:190,h:220};let x=30,y=30,pose='perch';
  if(i===0){x=active?active.r-own.w*.72:sw-own.w-24;y=active?active.b-own.h*.70:sh-own.h-28;pose='bonk'}
  else if(i===1){x=active?active.r-own.w*.74:sw-own.w-30;y=active?active.t-35:36;pose='perch'}
  else if(i===2){x=24;y=sh-own.h-24;pose=state.active?.kind==='home'?'perch':'sleep'}
  else if(i===3){x=sw*.5-own.w*.5;y=sh-own.h+24;pose='sleep'}
  else if(i===4){const f=phone||frameFor(preferredWindow('browser'))||active;x=f?f.l-own.w*.28:24;y=f?f.t-42:24;pose='fly'}
  else{x=active?active.r-own.w*.42:sw-own.w+34;y=active?active.t+active.h*.42-own.h*.5:sh*.42;pose='perch'}
  x=clamp(x,-own.w*.35,sw-own.w*.65);y=clamp(y,-30,sh-own.h*.55);return {x,y,pose};
}
function setPose(r,pose,ms=1900){r.el.classList.remove('perch','pounce','sleep','fly','bonk');r.el.classList.add(pose);clearTimeout(r.poseTimer);r.poseTimer=setTimeout(()=>r.el.classList.remove(pose),ms)}
function arrange(force=false){
  residents.forEach((r,i)=>{const t=targetFor(i),feral=state.haunt==='FERAL',jitter=feral?7:state.haunt==='HAUNTED'?3:0,x=t.x+(Math.random()-.5)*jitter*2,y=t.y+(Math.random()-.5)*jitter*2;r.el.style.setProperty('--x',`${Math.round(x)}px`);r.el.style.setProperty('--y',`${Math.round(y)}px`);r.el.style.setProperty('--scale',i===0?(feral?'1.08':'1'):(feral?'.92':'.84'));r.el.classList.toggle('lead',i===(KIND_TO_FAE[state.active?.kind]??0));if(force||Math.random()<.34)setPose(r,t.pose)});
  if(state.haunt==='CALM'){const lead=KIND_TO_FAE[state.active?.kind]??0;residents.forEach((r,i)=>r.el.classList.toggle('lead',i===lead))}drawTerritories();
}
function reaction(i,line,index){const r=residents[i],f=r.f,col=Number.isInteger(index)?index:Math.floor(Math.random()*6);r.speech.textContent=line||f.lines[col];r.speech.classList.remove('pop');void r.speech.offsetWidth;r.speech.classList.add('pop');r.stamp.style.backgroundImage=`url(${window.FAERY_ASSETS.stickerAtlas})`;r.stamp.style.backgroundPosition=`${col*20}% ${i*20}%`;r.stamp.classList.remove('pop');void r.stamp.offsetWidth;r.stamp.classList.add('pop');setPose(r,i===0?'bonk':i===4?'fly':i===3?'sleep':'pounce',1500)}
function reactToWindow(info,eventName='FOREGROUND'){
  if(!info||info.kind==='self')return;state.active=info;state.lastEvent=Date.now();const i=KIND_TO_FAE[info.kind]??1,line=KIND_LINE[info.kind]||FAE[i].lines[1];if(eventName==='FOREGROUND'||eventName==='SHOW')reaction(i,line,1);if(info.kind==='phone'){state.phone=info;reaction(4,'PHONE PORTAL!',2);setTimeout(()=>reaction(0,'ON IT!',2),550)}arrange(true);
}
function drawTerritories(){territoryLayer.innerHTML='';const visible=state.windows.filter(w=>!w.minimized&&w.visible&&w.kind!=='self').slice(0,12);visible.forEach((w,i)=>{const d=document.createElement('div');d.className='territory';d.style.setProperty('--c',FAE[KIND_TO_FAE[w.kind]??(i%FAE.length)].accent);d.style.left=`${w.x}px`;d.style.top=`${w.y}px`;d.style.width=`${w.width}px`;d.style.height=`${w.height}px`;const s=document.createElement('span');s.textContent=`${w.kind.toUpperCase()} // ${w.process}`;d.appendChild(s);territoryLayer.appendChild(d)})}
async function refreshWindows(){try{state.windows=await invoke('desktop_windows',{includeTitles:false});state.phone=state.windows.find(w=>w.kind==='phone'&&!w.minimized)||null;drawTerritories();if(!state.active){const fg=await invoke('foreground_app');if(fg&&fg.kind!=='self')reactToWindow(fg,'FOREGROUND')}}catch{}}
async function refreshPhone(){try{const p=await invoke('phone_portal_status');if(p.running&&!state.phone){await refreshWindows();reaction(4,'PHONE FOUND.',2)}}catch{}}
listen('faery://window-event',e=>{const p=e.payload||{};if(p.window){const idx=state.windows.findIndex(w=>w.id===p.window.id);if(idx>=0)state.windows[idx]=p.window;else state.windows.push(p.window);if(p.event_name==='DESTROY'||p.event_name==='HIDE')state.windows=state.windows.filter(w=>w.id!==p.window.id);reactToWindow(p.window,p.event_name)}});
listen('faery://haunt',e=>setHaunt(String(e.payload||'HAUNTED').toUpperCase()));
listen('faery://presence',e=>{if(String(e.payload||'').toUpperCase()==='BANISH')habitat.style.display='none';if(String(e.payload||'').toUpperCase()==='SUMMON')habitat.style.display='block'});
window.addEventListener('storage',e=>{if(e.key==='faeryware.desktop.state'&&e.newValue){try{const n=JSON.parse(e.newValue);if(n.haunt)setHaunt(n.haunt)}catch{}}});
setHaunt(state.haunt);refreshWindows();refreshPhone();setInterval(refreshWindows,6500);setInterval(refreshPhone,12000);setInterval(()=>arrange(false),1300);setInterval(()=>{if(state.haunt==='FERAL'){reaction(Math.floor(Math.random()*FAE.length),null)}else if(state.haunt==='HAUNTED'&&Math.random()<.45){reaction(KIND_TO_FAE[state.active?.kind]??Math.floor(Math.random()*FAE.length),null)}},11000);
