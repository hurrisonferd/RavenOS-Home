const invoke=window.__TAURI__.core.invoke;
const listen=window.__TAURI__.event.listen;
const fae=[
{id:'kyu',name:'KYU',color:'#ff4e9d',sigil:'♡',row:0,lines:['HI!','LET’S GO!','ON IT!','hehe.','BONK','sus...']},
{id:'paimon',name:'PAIMON',color:'#5ef27e',sigil:'✣',row:1,lines:['hmm...','I SEE IT.','EXACTLY.','BIG BRAIN','sus.','ALL GOOD.']},
{id:'luma',name:'LUMA',color:'#ffc85c',sigil:'✿',row:2,lines:['GOOD MORNING','YOU GOT THIS','COMFY','IT’S OKAY','BEAUTIFUL','HOME. ♡']},
{id:'nyx',name:'NYX',color:'#5e8cff',sigil:'☾',row:3,lines:['...','WATCHING.','UNDERSTOOD.','REST.','NOTED.','LATER.']},
{id:'sylph',name:'SYLPH',color:'#55dcff',sigil:'🦋',row:4,lines:['LET’S EXPLORE!','SO COOL!','IDEA!','ZOOM!','CURIOUS...','NEW PATH!']},
{id:'qira',name:'QIRA',color:'#f04cff',sigil:'◇',row:5,lines:['YES.','NO.','SAY IT.','BOUNDARIES.','EXCUSE ME?','REAL TALK.']}
];
const body=document.querySelector('#bodyImg'),speech=document.querySelector('#speech'),sticker=document.querySelector('#sticker'),sigil=document.querySelector('#sigil'),faeRow=document.querySelector('#faeRow'),contextLine=document.querySelector('#contextLine');
let state={fae:0,haunt:'FERAL',clickThrough:true,edge:'right'};
try{state={...state,...JSON.parse(localStorage.getItem('faeryware.desktop.state')||'{}')}}catch{}
const save=()=>localStorage.setItem('faeryware.desktop.state',JSON.stringify(state));
function active(){return fae[state.fae%fae.length]}
function render(){const f=active();document.documentElement.style.setProperty('--accent',f.color);body.src=window.FAERY_ASSETS[f.id];body.alt=f.name;sigil.textContent=f.sigil}
function react(line,index){const f=active(),i=Number.isInteger(index)?index:Math.floor(Math.random()*6);speech.textContent=line||f.lines[i];sticker.style.backgroundImage=`url(${window.FAERY_ASSETS.stickerAtlas})`;sticker.style.backgroundPosition=`${i*20}% ${f.row*20}%`;sticker.classList.remove('pop');void sticker.offsetWidth;sticker.classList.add('pop')}
fae.forEach((f,i)=>{const b=document.createElement('button');b.textContent=f.name;b.onclick=()=>{state.fae=i;save();render();react()};faeRow.appendChild(b)});
document.querySelectorAll('[data-haunt]').forEach(b=>b.onclick=async()=>{state.haunt=b.dataset.haunt;save();await invoke('sync_colony',{haunt:state.haunt,activeFae:state.fae});react(state.haunt==='FERAL'?'GOBLIN ARMY.':state.haunt)});
document.querySelector('#habitat').onclick=async()=>{await invoke('show_habitat');react('THE HOUSE IS LOOSE.',2)};
document.querySelector('#office').onclick=()=>invoke('open_office_board');
document.querySelector('#phone').onclick=async()=>{try{await invoke('launch_phone_portal');state.fae=4;save();render();react('PHONE PORTAL!',2)}catch(e){react(String(e),4)}};
document.querySelector('#autostart').onclick=async()=>{const on=await invoke('autostart_status');await invoke('set_autostart',{enabled:!on});await refreshAutostart();react(!on?'I LIVE HERE NOW.':'manual summon.',2)};
document.querySelector('#pass').onclick=async()=>{state.clickThrough=!state.clickThrough;save();await invoke('set_colony_click_through',{enabled:state.clickThrough});react(state.clickThrough?'ghost mode.':'touchable overlay.',3)};
document.querySelector('#perch').onclick=async()=>{state.edge=state.edge==='right'?'left':'right';save();await invoke('perch',{label:'main',edge:state.edge,y:100+Math.floor(Math.random()*320)});react('perched.',3)};
document.querySelector('#hideVisuals').onclick=async()=>{await invoke('hide_habitat');react('visuals banished.',5)};
async function refreshAutostart(){try{const on=await invoke('autostart_status');document.querySelector('#autostart').textContent=`AUTOSTART: ${on?'ON':'OFF'}`}catch{}}
const route={browser:[4,'NEW PATH!'],phone:[4,'PHONE PORTAL!'],code:[1,'I SEE IT.'],document:[5,'SAY IT.'],spreadsheet:[1,'BIG BRAIN'],presentation:[4,'IDEA!'],meeting:[0,'ON IT!'],communication:[5,'BOUNDARIES.'],media:[3,'WATCHING.'],home:[2,'HOME. ♡'],game:[0,'LET’S GO!']};
listen('faery://window-event',e=>{const p=e.payload||{};if(!p.window||p.window.kind==='self')return;contextLine.textContent=`${p.event_name} // ${p.window.kind.toUpperCase()} // ${p.window.process}`;if(p.event_name==='FOREGROUND'){const r=route[p.window.kind];if(r){state.fae=r[0];save();render();react(r[1],1)}}});
listen('faery://haunt',e=>{state.haunt=String(e.payload||'HAUNTED').toUpperCase();save();react(state.haunt==='FERAL'?'FERAL HOUSE.':state.haunt)});
body.onclick=()=>react();
body.ondblclick=()=>{state.fae=(state.fae+1)%fae.length;save();render();react()};
render();react();refreshAutostart();
