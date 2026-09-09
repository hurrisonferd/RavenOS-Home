const invoke=window.__TAURI__.core.invoke;
const fae=[
{id:'kyu',name:'KYU',color:'#ff4e9d',sigil:'♡',row:0,lines:['HI!','LET\'S GO!','ON IT!','hehe.','BONK','sus...'],pace:1.25,perch:.14},
{id:'paimon',name:'PAIMON',color:'#5ef27e',sigil:'✣',row:1,lines:['hmm...','I SEE IT.','EXACTLY.','BIG BRAIN','sus.','ALL GOOD.'],pace:.8,perch:.2},
{id:'luma',name:'LUMA',color:'#ffc85c',sigil:'✿',row:2,lines:['GOOD MORNING','YOU GOT THIS','COMFY','IT\'S OKAY','BEAUTIFUL','HOME. ♡'],pace:.55,perch:.34},
{id:'nyx',name:'NYX',color:'#5e8cff',sigil:'☾',row:3,lines:['...','WATCHING.','UNDERSTOOD.','REST.','NOTED.','LATER.'],pace:.65,perch:.42},
{id:'sylph',name:'SYLPH',color:'#55dcff',sigil:'🦋',row:4,lines:['LET\'S EXPLORE!','SO COOL!','IDEA!','ZOOM!','CURIOUS...','NEW PATH!'],pace:1.45,perch:.09},
{id:'qira',name:'QIRA',color:'#f04cff',sigil:'◇',row:5,lines:['YES.','NO.','SAY IT.','BOUNDARIES.','EXCUSE ME?','REAL TALK.'],pace:.9,perch:.26}
];
const resident=document.querySelector('#resident'),bodyImg=document.querySelector('#bodyImg'),speech=document.querySelector('#speech'),sticker=document.querySelector('#sticker'),sigil=document.querySelector('#sigil'),house=document.querySelector('#house'),faeRow=document.querySelector('#faeRow');
let state=JSON.parse(localStorage.getItem('faeryware.desktop.state')||'{"fae":0,"haunt":"HAUNTED","clickThrough":false,"edge":"right","mochi":true}');
let dragStart=null,poseTimer=null;
const save=()=>localStorage.setItem('faeryware.desktop.state',JSON.stringify(state));
const active=()=>fae[state.fae%fae.length];
function setPose(p,ms=1100){resident.dataset.pose=p;clearTimeout(poseTimer);poseTimer=setTimeout(()=>resident.dataset.pose='idle',ms)}
function render(){const f=active();document.documentElement.style.setProperty('--accent',f.color);bodyImg.src=window.FAERY_ASSETS[f.id];bodyImg.alt=f.name+' desktop resident';sigil.textContent=f.sigil;document.body.classList.toggle('feral',state.haunt==='FERAL');document.body.classList.toggle('haunted',state.haunt==='HAUNTED');document.body.classList.toggle('clickthrough',!!state.clickThrough);document.body.classList.toggle('mochi',!!state.mochi);document.querySelector('#mochi').textContent=state.mochi?'MOCHI ON':'MOCHI OFF'}
function popSticker(i){const f=active();sticker.style.backgroundImage=`url(${window.FAERY_ASSETS.stickerAtlas})`;sticker.style.backgroundPosition=`${i*20}% ${f.row*20}%`;sticker.classList.remove('pop');void sticker.offsetWidth;sticker.classList.add('pop');setTimeout(()=>sticker.classList.remove('pop'),1800)}
function react(force,index){const f=active(),i=index??Math.floor(Math.random()*6);speech.textContent=force??f.lines[i];popSticker(i)}
function cycle(){state.fae=(state.fae+1)%fae.length;save();render();react()}
fae.forEach((f,i)=>{const b=document.createElement('button');b.textContent=f.name;b.onclick=()=>{state.fae=i;save();render();react()};faeRow.appendChild(b)});
bodyImg.onclick=()=>{setPose('pounce',600);react()};
bodyImg.ondblclick=e=>{e.preventDefault();cycle()};
bodyImg.onpointerdown=e=>{if(e.button===0)dragStart={x:e.clientX,y:e.clientY}};
bodyImg.onpointermove=async e=>{const r=bodyImg.getBoundingClientRect();document.documentElement.style.setProperty('--look-x',((e.clientX-r.left)/r.width-.5).toFixed(2));if(dragStart&&Math.hypot(e.clientX-dragStart.x,e.clientY-dragStart.y)>8){dragStart=null;try{await invoke('begin_drag')}catch{}}};
window.onpointerup=()=>dragStart=null;
resident.oncontextmenu=e=>{e.preventDefault();house.hidden=!house.hidden};
document.querySelectorAll('[data-haunt]').forEach(b=>b.onclick=()=>{state.haunt=b.dataset.haunt;save();render();react(state.haunt==='FERAL'?'hehe. feral.':state.haunt.toLowerCase())});
document.querySelector('#mochi').onclick=()=>{state.mochi=!state.mochi;save();render();react(state.mochi?'desktop pet mode.':'staying put.',state.mochi?3:2)};
document.querySelector('#pass').onclick=async()=>{state.clickThrough=!state.clickThrough;save();render();await invoke('set_click_through',{enabled:state.clickThrough})};
document.querySelector('#perch').onclick=async()=>{state.edge=state.edge==='right'?'left':'right';save();setPose('perch',1600);await invoke('perch',{edge:state.edge,y:120+Math.floor(Math.random()*420)})};
document.querySelector('#quit').onclick=()=>invoke('quit');
async function petTick(){if(!state.mochi||state.clickThrough)return;const f=active();if(state.haunt==='CALM'){if(Math.random()<.12){setPose('doze',1800);react(f.lines[3],3)}return}const feral=state.haunt==='FERAL',roll=Math.random();if(roll<f.perch*(feral?1.25:1)){state.edge=Math.random()<.5?'left':'right';save();setPose('perch',1800);try{await invoke('perch',{edge:state.edge,y:80+Math.floor(Math.random()*500)})}catch{}if(Math.random()<.45)react();return}const step=(feral?44:18)*f.pace;setPose(roll>.78?'pounce':'walk',900);try{await invoke('move_resident',{dx:Math.round((Math.random()-.5)*step),dy:Math.round((Math.random()-.5)*step*.55)})}catch{}if(Math.random()<(feral?.48:.22))react()}
render();react();setInterval(petTick,1450);setInterval(()=>{if(!state.clickThrough&&Math.random()<.55)react()},30000);
