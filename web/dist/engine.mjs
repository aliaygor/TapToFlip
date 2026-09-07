// Browser port of GameEngine.kt. World units and gameplay constants are retained.
export class GameEngine {
  constructor({random = Math.random, gravity = 1750, jumpVelocity = -690, speed = 245} = {}) {
    Object.assign(this, {random, gravity, jumpVelocity, speed, width: 0, height: 0});
    this.player = {x: 0, y: 0, velocityY: 0, size: 72};
    this.platforms = [];
    this.state = 'PAUSED';
  }
  resize(width, height) {
    if (!(width > 0 && height > 0 && Number.isFinite(width) && Number.isFinite(height))) return;
    const first = !this.width;
    Object.assign(this, {width, height});
    Object.assign(this.player, {x: width * .2, size: clamp(width * .17, 62, 88)});
    if (first) this.reset();
  }
  reset() {
    if (!this.width) return;
    Object.assign(this, {state: 'RUNNING', score: 0, elapsed: 0, age: 0, difficulty: 1, nextId: 1});
    Object.assign(this.player, {y: this.height * .48, velocityY: 0});
    this.platforms = [{id: this.nextId++, x: this.width * .78, y: this.height * .69,
      width: this.width * .34, height: this.platformHeight(), type: 'GRASS'}];
    while (this.rightEdge() < this.width * 1.75) this.spawn();
  }
  jump() { if (this.state === 'RUNNING') this.player.velocityY = this.jumpVelocity; }
  pause() { if (this.state === 'RUNNING') this.state = 'PAUSED'; }
  resume() { if (this.state === 'PAUSED') this.state = 'RUNNING'; }
  update(delta) {
    if (this.state !== 'RUNNING' || !this.width || !Number.isFinite(delta)) return;
    const dt = clamp(delta, 0, .033), p = this.player;
    this.age += dt;
    this.difficulty = 1 + Math.log(1 + this.score / 70) * .65;
    p.velocityY += this.gravity * dt;
    p.y += p.velocityY * dt;
    for (const o of this.platforms) o.x -= this.speed * this.difficulty * dt;
    this.elapsed += dt * 10;
    this.score = Math.floor(this.elapsed);
    if (p.y + p.size * .15 <= 0 || p.y + p.size * .85 >= this.height ||
      this.platforms.some(o => this.collides(o))) { this.state = 'GAME_OVER'; return; }
    this.platforms = this.platforms.filter(o => o.x + o.width >= -24);
    while (this.rightEdge() < this.width * 1.55) this.spawn();
  }
  collides(o) {
    const p = this.player;
    return p.x + p.size * .2 < o.x + o.width && p.x + p.size * .8 > o.x &&
      p.y + p.size * .16 < o.y + o.height && p.y + p.size * .84 > o.y;
  }
  platformHeight() { return clamp(this.height * .052, 34, 54); }
  rightEdge() { return Math.max(0, ...this.platforms.map(o => o.x + o.width)); }
  range(a, b) { return a + this.random() * Math.max(b - a, 0); }
  type() {
    const types = ['GRASS'];
    for (const [score, type] of [[300,'BIRD'],[600,'BEE'],[900,'BAT'],[1200,'FIREFLY']])
      if (this.score >= score) types.push(type);
    return types.length === 1 || this.random() < .48 ? 'GRASS' : types[1 + Math.floor(this.random() * (types.length - 1))];
  }
  spawn() {
    const previous = this.platforms.reduce((a,b) => !a || b.x+b.width > a.x+a.width ? b : a, null);
    const type = this.type(), w = this.width, h = this.height;
    const spec = {GRASS:[.22,96,.39,24],BIRD:[.14,72,.23,18],BEE:[.12,64,.19,16],BAT:[.15,76,.25,18],FIREFLY:[.11,58,.17,14]}[type];
    const min = Math.max(w * spec[0], spec[1]);
    const width = this.range(min, Math.max(w * spec[2], min + spec[3]));
    const crowd = clamp(this.score / 1000,0,1), expert = clamp((this.score-1000)/1500,0,1);
    const gap = this.range(w*(.27-crowd*.07-expert*.04), w*(.48-crowd*.13-expert*.08));
    const heights = {BIRD:[.62,38,58],BEE:[.52,34,50],BAT:[.66,42,62],FIREFLY:[.48,32,46]};
    const hs = heights[type];
    const height = hs ? clamp(this.player.size * hs[0],hs[1],hs[2]) : this.platformHeight();
    const minY = h*.025, maxY = h-height-h*.025, prevY = previous?.y ?? h*.5, change = h*.115;
    const lanes = [.02,.16,.31,.47,.63,.79,.98];
    const chooseY = () => minY+(maxY-minY)*lanes[Math.floor(this.random()*lanes.length)];
    let y = chooseY();
    for (let i=0; i<4 && Math.abs(y-prevY)<change; i++) y=chooseY();
    if (Math.abs(y-prevY)<change) y=prevY<h*.5 ? Math.min(prevY+change,maxY) : Math.max(prevY-change,minY);
    this.platforms.push({id:this.nextId++,x:(previous ? previous.x+previous.width : w)+gap,y,width,height,type});
  }
}
function clamp(n,min,max) { return Math.min(max,Math.max(min,n)); }
