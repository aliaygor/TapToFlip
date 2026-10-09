import {test} from 'node:test';
import assert from 'node:assert/strict';
import {GameEngine} from '../dist/engine.mjs';
function setup(options={}){const e=new GameEngine({random:()=>.5,...options});e.resize(432,768);return e;}
test('jump rises, pause freezes score and position, resume continues',()=>{
 const e=setup();const y=e.player.y;e.jump();e.update(.03);assert.ok(e.player.y<y);
 e.pause();const snapshot=JSON.stringify(e);e.update(1);assert.equal(JSON.stringify(e),snapshot);
 e.resume();e.update(.03);assert.equal(e.state,'RUNNING');
});
test('collision ends round and reset clears score',()=>{
 const e=setup({gravity:0,speed:0});const p=e.player;
 e.platforms=[{x:p.x,y:p.y,width:p.size,height:p.size}];e.update(.02);
 assert.equal(e.state,'GAME_OVER');e.reset();assert.equal(e.state,'RUNNING');assert.equal(e.score,0);
 assert.ok(e.platforms[0].x>p.x+p.size);
});
test('near misses survive and score advances without movement',()=>{
 const e=setup({gravity:0,speed:0});e.platforms=[];for(let i=0;i<40;i++)e.update(.03);
 assert.equal(e.state,'RUNNING');assert.ok(e.score>=11);
});
test('world edge kills and large frame delta is capped',()=>{
 const e=setup();e.update(20);assert.equal(e.age,.033);
 e.player.y=e.height;e.update(.001);assert.equal(e.state,'GAME_OVER');
});
test('generated obstacles stay within world and change vertical lanes',()=>{
 const e=setup();for(let i=0;i<100;i++){const prev=e.platforms.at(-1);e.spawn();const o=e.platforms.at(-1);
 assert.ok(o.y>=0 && o.y+o.height<=e.height);assert.ok(Math.abs(o.y-prev.y)>=e.height*.1);assert.ok(o.x>prev.x+prev.width);}
});
