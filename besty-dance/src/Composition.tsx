import { CalculateMetadataFunction, Composition, AbsoluteFill, Audio, staticFile, useCurrentFrame, interpolate, Easing } from "remotion";

type Props = {};
const FPS = 30;
const BEAT = 15; // 120 BPM
const DURATION = 12 * FPS;

const calculateMetadata: CalculateMetadataFunction<Props> = () => ({});

export const MyComposition = () => (
  <Composition
    id="BestyDance"
    component={MyComponent}
    durationInFrames={DURATION}
    fps={FPS}
    width={1080}
    height={1080}
    calculateMetadata={calculateMetadata}
  />
);

const MOVES = ["bounce", "sway", "wave", "spin", "jump", "bounce"] as const;
const MOVE_LEN = DURATION / MOVES.length; // 2s each

const Besty: React.FC<{ frame: number }> = ({ frame }) => {
  const beatPhase = (frame % BEAT) / BEAT; // 0..1 per beat
  const beatIdx = Math.floor(frame / BEAT);
  const move = MOVES[Math.min(MOVES.length - 1, Math.floor(frame / MOVE_LEN))];
  const local = frame % MOVE_LEN;
  const pulse = Math.sin(beatPhase * Math.PI); // 0→1→0 each beat
  const side = beatIdx % 2 === 0 ? 1 : -1;

  let y = -pulse * 30, x = 0, rot = 0, squash = 1 - pulse * 0.06 + 0.06 * (1 - pulse);
  let armL = -30 + pulse * 20, armR = 30 - pulse * 20, legL = 0, legR = 0;

  if (move === "sway") {
    x = Math.sin((frame / BEAT) * Math.PI) * 120;
    rot = Math.sin((frame / BEAT) * Math.PI) * 12;
    armL = -60 + Math.sin(frame / 4) * 30; armR = 60 - Math.sin(frame / 4) * 30;
    legL = side * 15; legR = side * 15;
  } else if (move === "wave") {
    armL = -150 + Math.sin(frame / 3) * 30; armR = 150 + Math.sin(frame / 3 + 1) * 30;
    rot = Math.sin(frame / 6) * 6;
    legL = -pulse * 20; legR = pulse * 20;
  } else if (move === "spin") {
    const p = interpolate(local % (BEAT * 4), [0, BEAT * 4], [0, 1], { easing: Easing.inOut(Easing.cubic) });
    rot = p * 360;
    armL = -100; armR = 100; y = -40 - pulse * 20;
  } else if (move === "jump") {
    const jp = (frame % (BEAT * 2)) / (BEAT * 2);
    y = -Math.sin(jp * Math.PI) * 220;
    squash = jp < 0.08 || jp > 0.92 ? 0.85 : 1.05;
    armL = -160; armR = 160; legL = -25 * Math.sin(jp * Math.PI); legR = 25 * Math.sin(jp * Math.PI);
  } else if (move === "bounce") {
    armL = side > 0 ? -140 : -40; armR = side > 0 ? 40 : 140;
    legL = side > 0 ? -20 : 0; legR = side > 0 ? 0 : 20;
  }

  const blink = frame % 70 < 4;
  const stroke = "#2b1b3d";

  return (
    <div style={{ position: "absolute", left: 540 - 200 + x, top: 330 + y, width: 400, height: 520, transform: `rotate(${rot}deg) scaleY(${squash}) scaleX(${2 - squash})`, transformOrigin: move === "spin" ? "50% 50%" : "50% 100%" }}>
      <svg width={400} height={520} viewBox="0 0 400 520" style={{ overflow: "visible" }}>
        {/* legs */}
        <g transform={`rotate(${legL} 160 380)`}><rect x={140} y={370} width={40} height={110} rx={20} fill="#ff8fb1" stroke={stroke} strokeWidth={8} /><ellipse cx={155} cy={490} rx={40} ry={20} fill="#ffd34e" stroke={stroke} strokeWidth={8} /></g>
        <g transform={`rotate(${legR} 240 380)`}><rect x={220} y={370} width={40} height={110} rx={20} fill="#ff8fb1" stroke={stroke} strokeWidth={8} /><ellipse cx={245} cy={490} rx={40} ry={20} fill="#ffd34e" stroke={stroke} strokeWidth={8} /></g>
        {/* arms */}
        <g transform={`rotate(${armL} 90 250)`}><rect x={-10} y={235} width={110} height={34} rx={17} fill="#ff8fb1" stroke={stroke} strokeWidth={8} /><circle cx={-12} cy={252} r={24} fill="#fff" stroke={stroke} strokeWidth={8} /></g>
        <g transform={`rotate(${armR} 310 250)`}><rect x={300} y={235} width={110} height={34} rx={17} fill="#ff8fb1" stroke={stroke} strokeWidth={8} /><circle cx={412} cy={252} r={24} fill="#fff" stroke={stroke} strokeWidth={8} /></g>
        {/* body */}
        <ellipse cx={200} cy={250} rx={140} ry={150} fill="#ff6f9f" stroke={stroke} strokeWidth={10} />
        <ellipse cx={200} cy={300} rx={85} ry={80} fill="#ffc2d6" />
        {/* hair tuft / bow */}
        <path d="M200 105 C 160 40, 130 90, 200 105 C 270 90, 240 40, 200 105 Z" fill="#7c5cff" stroke={stroke} strokeWidth={8} />
        <circle cx={200} cy={103} r={14} fill="#ffd34e" stroke={stroke} strokeWidth={6} />
        {/* sunglasses-free cute face */}
        {blink ? (
          <><path d="M135 215 q20 12 40 0" stroke={stroke} strokeWidth={9} fill="none" strokeLinecap="round" /><path d="M225 215 q20 12 40 0" stroke={stroke} strokeWidth={9} fill="none" strokeLinecap="round" /></>
        ) : (
          <><ellipse cx={155} cy={212} rx={20} ry={26} fill={stroke} /><ellipse cx={245} cy={212} rx={20} ry={26} fill={stroke} /><circle cx={162} cy={202} r={7} fill="#fff" /><circle cx={252} cy={202} r={7} fill="#fff" /></>
        )}
        <ellipse cx={120} cy={255} rx={22} ry={13} fill="#ff3d7f" opacity={0.6} />
        <ellipse cx={280} cy={255} rx={22} ry={13} fill="#ff3d7f" opacity={0.6} />
        <path d={`M170 260 Q200 ${290 + pulse * 15} 230 260`} stroke={stroke} strokeWidth={9} fill="#ff3d7f" strokeLinecap="round" />
      </svg>
    </div>
  );
};

const COLORS = ["#ff3d7f", "#7c5cff", "#2ee6c5", "#ffd34e"];

export const MyComponent: React.FC<Props> = () => {
  const frame = useCurrentFrame();
  const beatIdx = Math.floor(frame / BEAT);
  const beatPhase = (frame % BEAT) / BEAT;
  const flash = 1 - beatPhase;
  const titleIn = interpolate(frame, [0, 20], [0, 1], { extrapolateRight: "clamp", easing: Easing.out(Easing.back(2)) });
  const outro = interpolate(frame, [DURATION - 20, DURATION], [1, 0], { extrapolateLeft: "clamp" });

  return (
    <AbsoluteFill style={{ background: "radial-gradient(circle at 50% 40%, #3a1d6e, #120624)", overflow: "hidden", opacity: outro }}>
      <Audio src={staticFile("beat.wav")} />
      {/* rotating light beams */}
      {COLORS.map((c, i) => (
        <div key={i} style={{ position: "absolute", left: 540 - 60, top: -200, width: 120, height: 1600, background: `linear-gradient(${c}88, transparent 80%)`, transformOrigin: "50% 0%", transform: `rotate(${Math.sin(frame / 25 + i * 1.6) * 40 + (i - 1.5) * 18}deg)`, filter: "blur(18px)", mixBlendMode: "screen" }} />
      ))}
      {/* checker dance floor */}
      <div style={{ position: "absolute", left: -200, right: -200, bottom: 0, height: 300, display: "grid", gridTemplateColumns: "repeat(12, 1fr)", transform: "perspective(500px) rotateX(55deg)", transformOrigin: "50% 100%" }}>
        {Array.from({ length: 48 }).map((_, i) => {
          const on = (i + beatIdx) % 3 === 0;
          return <div key={i} style={{ background: on ? COLORS[(i + beatIdx) % 4] : "#1d0c3a", opacity: on ? 0.5 + flash * 0.5 : 1, border: "2px solid #0a0315" }} />;
        })}
      </div>
      {/* shadow */}
      <div style={{ position: "absolute", left: 540 - 150, top: 840, width: 300, height: 50, borderRadius: "50%", background: "rgba(0,0,0,0.45)", filter: "blur(6px)" }} />
      {/* sparkles */}
      {Array.from({ length: 14 }).map((_, i) => {
        const sx = (i * 173) % 1000 + 40, sy = (i * 97) % 500 + 60;
        const tw = Math.max(0, Math.sin(frame / 6 + i * 1.3));
        return <div key={i} style={{ position: "absolute", left: sx, top: sy, fontSize: 40, color: COLORS[i % 4], opacity: tw, transform: `scale(${0.5 + tw})` }}>✦</div>;
      })}
      <Besty frame={frame} />
      {/* title */}
      <div style={{ position: "absolute", top: 50, width: "100%", textAlign: "center", fontFamily: "Arial Black, Arial, sans-serif", fontWeight: 900, fontSize: 120, color: "#fff", letterSpacing: 4, textShadow: `0 0 ${20 + flash * 30}px ${COLORS[beatIdx % 4]}, 6px 6px 0 #2b1b3d`, transform: `scale(${titleIn * (1 + flash * 0.05)})` }}>
        BESTY DANCE!
      </div>
      <div style={{ position: "absolute", bottom: 40, width: "100%", textAlign: "center", fontFamily: "Arial, sans-serif", fontWeight: 800, fontSize: 56, color: "#ffd34e", textShadow: "4px 4px 0 #2b1b3d" }}>
        {["🎵 베스티와 함께 춤춰요! 🎵", "좌우로 흔들흔들~", "손을 흔들어요 👋", "빙글빙글 회전!", "점프 점프!", "최고의 베스티 💖"][Math.min(5, Math.floor(frame / MOVE_LEN))]}
      </div>
    </AbsoluteFill>
  );
};
