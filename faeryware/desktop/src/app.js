const invoke=window.__TAURI__.core.invoke;
const fae=[
{id:'kyu',name:'KYU',color:'#ff4e9d',sigil:'♡',emoji:'💗',lines:['HI!','LET\'S GO!','ON IT!','hehe.','BONK','sus...']},
{id:'paimon',name:'PAIMON',color:'#5ef27e',sigil:'✣',emoji:'🟢',lines:['hmm...','I SEE IT.','EXACTLY.','BIG BRAIN','sus.','ALL GOOD.']},
{id:'luma',name:'LUMA',color:'#ffc85c',sigil:'✿',emoji:'🟡',lines:['GOOD MORNING','YOU GOT THIS','COMFY','IT\'S OKAY','BEAUTIFUL','HOME. ♡']},
{id:'nyx',name:'NYX',color:'#5e8cff',sigil:'☾',emoji:'🌙',lines:['...','WATCHING.','UNDERSTOOD.','REST.','NOTED.','LATER.']},
{id:'sylph',name:'SYLPH',color:'#55dcff',sigil:'🦋',emoji:'🦋',lines:['LET\'S EXPLORE!','SO COOL!','IDEA!','ZOOM!','CURIOUS...','NEW PATH!']},
{id:'qira',name:'QIRA',color:'#f04cff',sigil:'◇',emoji:'🔒',lines:['YES.','NO.','SAY IT.','BOUNDARIES.','EXCUSE ME?','REAL TALK.']}
];
const resident=document.querySelector('#resident'),art=document.querySelector('#faeArt'),speech=document.querySelector('#speech'),sigil=document.querySelector('#sigil'),house=document.querySelector('#house'),faeRow=document.querySelector('#faeRow');
let state=JSON.parse(localStorage.getItem('faeryware.desktop.state')||'{"fae":0,"haunt":"HAUNTED","clickThrough":false,"edge":"right"}');
const save=()=>localStorage.setItem('faeryware.desktop.state',JSON.stringify(state));
const active=()=>fae[state.fae%fae.length];
function render(){const f=active();resident.dataset.fae=f.id;document.documentElement.style.setProperty('--accent',f.color);art.dataset.sigil=f.emoji;art.setAttribute('aria-label',f.name+' resident');sigil.textContent=f.sigil;document.body.classList.toggle('feral',state.haunt==='FERAL');document.body.classList.toggle('haunted',state.haunt==='HAUNTED');document.body.classList.toggle('clickthrough',!!state.clickThrough)}
function react(force){const f=active(),bucket=Math.floor(Date.now()/30000);speech.textContent=force??f.lines[(bucket+state.fae)%f.lines.length]}
function cycle(){state.fae=(state.fae+1)%fae.length;save();render();react()}
fae.forEach((f,i)=>{const b=document.createElement('button');b.textContent=f.name;b.onclick=()=>{state.fae=i;save();render();react()};faeRow.appendChild(b)});
art.ondblclick=cycle;resident.oncontextmenu=e=>{e.preventDefault();house.hidden=!house.hidden};
document.querySelectorAll('[data-haunt]').forEach(b=>b.onclick=()=>{state.haunt=b.dataset.haunt;save();render();react(state.haunt==='FERAL'?'hehe. feral.':state.haunt.toLowerCase())});
document.querySelector('#pass').onclick=async()=>{state.clickThrough=!state.clickThrough;save();render();await invoke('set_click_through',{enabled:state.clickThrough})};
document.querySelector('#perch').onclick=async()=>{state.edge=state.edge==='right'?'left':'right';save();await invoke('perch',{edge:state.edge,y:120+Math.floor(Math.random()*420)})};
document.querySelector('#quit').onclick=()=>invoke('quit');
async function wander(){if(state.haunt==='CALM')return;const scale=state.haunt==='FERAL'?34:12;try{await invoke('move_resident',{dx:Math.round((Math.random()-.5)*scale),dy:Math.round((Math.random()-.5)*scale)})}catch{}if(Math.random()<(state.haunt==='FERAL'?.42:.18))react()}
render();react();setInterval(wander,2200);setInterval(()=>react(),30000);
