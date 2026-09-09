const visionListen=window.__TAURI__?.event?.listen;
if(visionListen){
  visionListen('faery://vision-result',e=>{
    const a=e.payload||{},summary=String(a.summary||'').trim();
    if(!summary)return;
    const signal=document.querySelector('#signalState');
    if(signal)signal.textContent=`FAE EYES // ${String(a.scope||'VISION').toUpperCase()} // ${a.model||'LOCAL'}`;
    const lead=document.querySelector('.fae[data-id="paimon"]')||document.querySelector('.fae.lead')||document.querySelector('.fae');
    if(!lead)return;
    const speech=lead.querySelector('.speech');
    if(speech){speech.textContent=`👁 ${summary.slice(0,150)}`;speech.classList.remove('pop');void speech.offsetWidth;speech.classList.add('pop')}
    lead.classList.add('lead','pounce');
    setTimeout(()=>lead.classList.remove('pounce'),1800);
    if(a.scope==='desktop'){
      const sylph=document.querySelector('.fae[data-id="sylph"]');
      if(sylph){const s=sylph.querySelector('.speech');if(s){s.textContent='I CAN SEE THE WHOLE FLOOR.';s.classList.remove('pop');void s.offsetWidth;s.classList.add('pop')}}
    }
  });
}
