// Generates a simple original 120 BPM dance beat into public/beat.wav
import fs from "node:fs";
const sr = 44100, bpm = 120, secs = 12, n = sr * secs;
const beat = 60 / bpm, buf = new Float32Array(n);
let seed = 1; const rnd = () => ((seed = (seed * 16807) % 2147483647) / 2147483647) * 2 - 1;
const bassNotes = [55, 55, 65.4, 49];
for (let i = 0; i < n; i++) {
  const t = i / sr, b = t / beat, bi = Math.floor(b), bt = (b - bi) * beat;
  let s = 0;
  // kick on every beat
  s += Math.sin(2 * Math.PI * (50 + 120 * Math.exp(-bt * 30)) * bt) * Math.exp(-bt * 8) * 0.9;
  // clap on 2 and 4
  if (bi % 2 === 1) s += rnd() * Math.exp(-bt * 25) * 0.35;
  // offbeat hi-hat
  const ht = ((b + 0.5) % 1) * beat;
  s += rnd() * Math.exp(-ht * 60) * 0.18;
  // bass on offbeats
  const bar = Math.floor(bi / 4) % 4, f = bassNotes[bar];
  if ((b % 1) >= 0.5) { const bt2 = ((b % 1) - 0.5) * beat; s += Math.sign(Math.sin(2 * Math.PI * f * t)) * 0.18 * Math.exp(-bt2 * 6); }
  // chord stab
  const st = ((b + 0.25) % 2) * beat;
  if (st < 0.15) [f * 4, f * 5, f * 6].forEach(fr => s += Math.sin(2 * Math.PI * fr * t) * 0.05 * Math.exp(-st * 20));
  const fade = Math.min(1, t / 0.05, (secs - t) / 1);
  buf[i] = Math.max(-1, Math.min(1, s * 0.7 * fade));
}
const out = Buffer.alloc(44 + n * 2);
out.write("RIFF", 0); out.writeUInt32LE(36 + n * 2, 4); out.write("WAVEfmt ", 8);
out.writeUInt32LE(16, 16); out.writeUInt16LE(1, 20); out.writeUInt16LE(1, 22);
out.writeUInt32LE(sr, 24); out.writeUInt32LE(sr * 2, 28); out.writeUInt16LE(2, 32); out.writeUInt16LE(16, 34);
out.write("data", 36); out.writeUInt32LE(n * 2, 40);
for (let i = 0; i < n; i++) out.writeInt16LE(Math.round(buf[i] * 32767), 44 + i * 2);
fs.writeFileSync("public/beat.wav", out);
