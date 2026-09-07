import {GameEngine} from './engine.mjs';
const engine = new GameEngine();
engine.resize(432,768); engine.pause();
const $ = id => document.getElementById(id), canvas=$('game'), ctx=canvas.getContext('2d');
const frog=new Image(); frog.src='./assets/player.png';
let ready=false, best=0, last=0, started=false;
try { const value=Number(localStorage.getItem('taptoflip-best')); best=Number.isFinite(value)?Math.max(0,value):0; } catch {}
$('best').textContent=`Best: ${best}`;
$('start').disabled=true;
frog.onload=()=>{ready=true;$('start').disabled=false;};
frog.onerror=()=>{$('message').textContent='The frog could not load. Please reload the game.';};
function show(title,message,label){$('title').textContent=title;$('message').textContent=message;$('start').textContent=label;$('best').textContent=`Best: ${best}`;$('overlay').hidden=false;}
function play(){
 if(!ready)return;
 if(!started || engine.state==='GAME_OVER'){engine.reset();started=true;engine.jump();}else engine.resume();
 $('overlay').hidden=true; last=performance.now(); canvas.focus();
}
function pause(){if(engine.state!=='RUNNING')return;engine.pause();show('Paused','Your run is waiting.','Continue');}
$('start').addEventListener('click',play);
$('pause').addEventListener('click',pause);
canvas.addEventListener('pointerdown',e=>{e.preventDefault();if(engine.state==='RUNNING')engine.jump();});
document.addEventListener('keydown',e=>{
 if(e.code==='Space' && e.target.tagName!=='BUTTON'){e.preventDefault();if(e.repeat)return;if(engine.state==='RUNNING')engine.jump();else play();}
 if(e.code==='KeyP'&&!e.repeat){if(engine.state==='RUNNING')pause();else if(started && engine.state==='PAUSED')play();}
});
document.addEventListener('visibilitychange',()=>{if(document.hidden)pause();});
window.addEventListener('blur',pause);
function render(now){
 const dt=last?(now-last)/1000:0;last=now;
 const before=engine.state;engine.update(dt);
 if(before==='RUNNING' && engine.state==='GAME_OVER'){
  best=Math.max(best,engine.score);try{localStorage.setItem('taptoflip-best',String(best));}catch{}
  show('One more try?',`Score: ${engine.score}`,'Play again');
 }
 ctx.fillStyle='#bfeaff';ctx.fillRect(0,0,432,768);
 ctx.fillStyle='#76c5da';ctx.fillRect(0,744,432,24);
 const colors={GRASS:'#348a54',BIRD:'#347aab',BEE:'#bf8610',BAT:'#655084',FIREFLY:'#41734e'};
 for(const o of engine.platforms){ctx.fillStyle=colors[o.type];ctx.fillRect(o.x,o.y,o.width,o.height);ctx.fillStyle='#ffffff44';ctx.fillRect(o.x,o.y,o.width,5);}
 const p=engine.player;
 if(ready){ctx.save();ctx.translate(p.x+p.size/2,p.y+p.size/2);ctx.rotate(Math.max(-.35,Math.min(.6,p.velocityY/1500)));ctx.drawImage(frog,-p.size/2,-p.size/2,p.size,p.size);ctx.restore();}
 $('score').textContent=engine.score;
 requestAnimationFrame(render);
}
requestAnimationFrame(render);
