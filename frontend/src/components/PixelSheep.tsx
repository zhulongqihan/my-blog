import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import './PixelSheep.css';

// ─── Pixel Art Config ────────────────────────────
const PX = 3;

const PALETTE: Record<string, string> = {
  O: '#3E2723', // dark brown — outline & legs
  W: '#FAFAFA', // white — wool
  C: '#E0E0E0', // light grey — wool curl detail
  G: '#78909C', // blue-grey — face
  E: '#263238', // very dark — eyes
  N: '#EC407A', // pink — nose
  P: '#F48FB1', // pink — inner ear
};

type SheepState = 'idle' | 'walk' | 'sleep' | 'play' | 'graze';

// ─── Sprite Frames (17 wide × 14 tall) ──────────
const FRAMES: Record<SheepState, string[][]> = {
  idle: [
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGEGGGGGEGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWCWWWWWWCWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '...OO.......OO...',
    ],
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGEGGGGGEGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWWCWWWWCWWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '...OO.......OO...',
    ],
  ],
  walk: [
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGEGGGGGEGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWCWWWWWWCWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '..OO..........OO.',
      '..OO..........OO.',
    ],
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGEGGGGGEGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWCWWWWWWCWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '....OO.....OO....',
      '....OO.....OO....',
    ],
  ],
  sleep: [
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGOGGGGGOGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWCWWWWWWCWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '...OO.......OO...',
    ],
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGOGGGGGOGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWWCWWWWCWWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWCWWWWWCWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '...OO.......OO...',
    ],
  ],
  play: [
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGEEGGGGGEEGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWCWWWWWWCWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '..............OW.',
    ],
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGEEGGGGGEEGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWCWWWWWWCWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '.............OW..',
    ],
  ],
  graze: [
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGOGGGGGEGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGNGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWCWWWWWWCWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '...OO.......OO...',
    ],
    [
      '....O.......O....',
      '...OPO.....OPO...',
      '...OGGGGGGGGGO...',
      '..OGGOGGGGGEGGO..',
      '..OGGGGGNGGGGGO..',
      '..OGGGGGGGGGGGO..',
      '.OWWWWWWWWWWWWWO.',
      'OWWWWWWWWWWWWWWWO',
      'OWWWWCWWWWCWWWWWO',
      'OWWWWWWWWWWWWWWWO',
      '.OWWWWWWWWWWWWWO.',
      '..OWWWWWWWWWWO...',
      '...OO.......OO...',
      '...OO.......OO...',
    ],
  ],
};

// ─── Clover Pixel Art (7×5) ─────────────────────
const CLOVER_FRAME = [
  '..BBB..',
  '.BCCCB.',
  'BCCECCB',
  '.BCCCB.',
  '..BBB..',
];
const CLOVER_PALETTE: Record<string, string> = {
  B: '#2E7D32',
  C: '#81C784',
  E: '#1B5E20',
};

function cloverToBoxShadow(): string {
  const shadows: string[] = [];
  for (let y = 0; y < CLOVER_FRAME.length; y++) {
    for (let x = 0; x < CLOVER_FRAME[y].length; x++) {
      const ch = CLOVER_FRAME[y][x];
      if (ch !== '.' && CLOVER_PALETTE[ch]) {
        shadows.push(`${x * PX}px ${y * PX}px 0 ${CLOVER_PALETTE[ch]}`);
      }
    }
  }
  return shadows.join(',');
}

const CLOVER_SHADOW = cloverToBoxShadow();

// ─── Easter Egg Messages ─────────────────────────
const CLICK_MESSAGES: Record<number, string> = {
  1: '咩~',
  3: '别薅羊毛！',
  5: '痒痒的...',
  7: '咩咩咩~ 🐑',
  10: '十连薅！✨',
  15: '真的要秃了 💢',
  20: '🏆 解锁成就：牧羊人',
  30: '🌟 黄金小羊变身！',
};

// ─── State Machine Config ────────────────────────
const STATE_DURATIONS: Record<SheepState, [number, number]> = {
  idle: [5000, 10000],
  walk: [3000, 6000],
  sleep: [8000, 15000],
  play: [2000, 4000],
  graze: [3000, 5000],
};

function randomBetween(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// ─── Mood-aware transitions ──────────────────────
function getTransitions(mood: number): Record<SheepState, SheepState[]> {
  if (mood > 70) {
    return {
      idle: ['walk', 'play', 'graze', 'idle'],
      walk: ['idle', 'play', 'graze'],
      sleep: ['idle', 'idle', 'play'],
      play: ['idle', 'walk', 'graze'],
      graze: ['idle', 'walk', 'play'],
    };
  }
  if (mood < 30) {
    return {
      idle: ['sleep', 'idle', 'idle', 'graze'],
      walk: ['idle', 'idle', 'sleep'],
      sleep: ['sleep', 'idle', 'graze'],
      play: ['idle', 'idle'],
      graze: ['idle', 'sleep'],
    };
  }
  return {
    idle: ['walk', 'graze', 'idle', 'idle'],
    walk: ['idle', 'idle', 'graze'],
    sleep: ['idle', 'idle', 'graze'],
    play: ['idle', 'graze'],
    graze: ['idle', 'idle', 'walk'],
  };
}

// ─── Helpers ─────────────────────────────────────
function frameToBoxShadow(frame: string[], dir: 1 | -1): string {
  const shadows: string[] = [];
  const numRows = frame.length;
  const numCols = frame[0].length;
  for (let y = 0; y < numRows; y++) {
    const row = frame[y];
    for (let x = 0; x < row.length; x++) {
      const ch = row[x];
      if (ch !== '.' && PALETTE[ch]) {
        const px = dir === 1 ? x * PX : (numCols - 1 - x) * PX;
        const py = (y - numRows + 1) * PX;
        shadows.push(`${px}px ${py}px 0 ${PALETTE[ch]}`);
      }
    }
  }
  return shadows.join(',');
}

function getMoodEmoji(mood: number): string {
  if (mood > 80) return '🐑';
  if (mood > 60) return '😊';
  if (mood > 40) return '😐';
  if (mood > 20) return '🥺';
  return '😢';
}

function getMoodColor(mood: number): string {
  if (mood > 60) return '#66BB6A';
  if (mood > 30) return '#FFA726';
  return '#EF5350';
}

// ─── Clover Type ─────────────────────────────────
interface Clover {
  id: number;
  x: number;
  duration: number;
  collected: boolean;
}

// ─── Wool Particle Type ──────────────────────────
interface WoolParticle {
  id: number;
  x: number;
  y: number;
  size: number;
  opacity: number;
}

// ─── Component ───────────────────────────────────
const PixelSheep: React.FC = () => {
  // ── Position ──
  const [posX, setPosX] = useState(() => {
    const saved = localStorage.getItem('pixelsheep-posX');
    return saved ? parseInt(saved, 10) : Math.floor(window.innerWidth / 2);
  });
  const [posY, setPosY] = useState(12);

  // ── State machine ──
  const [sheepState, setSheepState] = useState<SheepState>('idle');
  const [frameIdx, setFrameIdx] = useState(0);
  const [direction, setDirection] = useState<1 | -1>(1);

  // ── Click counter ──
  const [clickCount, setClickCount] = useState(() => {
    const saved = localStorage.getItem('pixelsheep-clicks');
    return saved ? parseInt(saved, 10) : 0;
  });

  // ── Mood (0-100) ──
  const [mood, setMood] = useState(() => {
    const saved = localStorage.getItem('pixelsheep-mood');
    return saved ? Math.min(100, Math.max(0, parseInt(saved, 10))) : 70;
  });

  // ── Grass collection ──
  const [grassCount, setGrassCount] = useState(() => {
    const saved = localStorage.getItem('pixelsheep-grass');
    return saved ? parseInt(saved, 10) : 0;
  });
  const [clovers, setClovers] = useState<Clover[]>([]);

  // ── Wool particles ──
  const [woolParticles, setWoolParticles] = useState<WoolParticle[]>([]);

  // ── UI ──
  const [message, setMessage] = useState<string | null>(null);
  const [isHidden, setIsHidden] = useState(() => {
    return localStorage.getItem('pixelsheep-hidden') === 'true';
  });
  const [isSuperSaiyan, setIsSuperSaiyan] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [isDropping, setIsDropping] = useState(false);
  const [panel, setPanel] = useState<{ x: number; y: number } | null>(null);

  // ── Refs ──
  const stateTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const messageTimerRef = useRef<ReturnType<typeof setTimeout>>(undefined);
  const lastActivityRef = useRef(Date.now());
  const posXRef = useRef(posX);
  const dragRef = useRef({ startX: 0, startY: 0, origX: 0, active: false, timer: 0 as number });
  const cloverIdRef = useRef(0);
  const woolIdRef = useRef(0);
  const moodRef = useRef(mood);

  useEffect(() => { posXRef.current = posX; }, [posX]);
  useEffect(() => { moodRef.current = mood; }, [mood]);

  // ── Sprite metrics ──
  const currentFrames = FRAMES[sheepState];
  const spriteWidth = currentFrames[0][0].length * PX;
  const spriteHeight = currentFrames[0].length * PX;

  const currentShadow = useMemo(
    () => frameToBoxShadow(currentFrames[frameIdx % currentFrames.length], direction),
    [currentFrames, frameIdx, direction]
  );

  // ─────────────────────────────────────────────────
  // MOOD SYSTEM: decay over time
  // ─────────────────────────────────────────────────
  useEffect(() => {
    const interval = setInterval(() => {
      setMood(prev => Math.max(0, prev - 1));
    }, 12_000);
    return () => clearInterval(interval);
  }, []);

  // ─────────────────────────────────────────────────
  // WOOL PARTICLE SYSTEM (walk state)
  // ─────────────────────────────────────────────────
  useEffect(() => {
    if (sheepState !== 'walk' || isHidden || isDragging) return;
    const interval = setInterval(() => {
      const id = ++woolIdRef.current;
      const offsetX = direction === 1 ? -randomBetween(2, 8) : spriteWidth + randomBetween(2, 8);
      setWoolParticles(prev => [
        ...prev,
        {
          id,
          x: posXRef.current + offsetX,
          y: window.innerHeight - 20 - randomBetween(5, spriteHeight - 10),
          size: randomBetween(2, 5),
          opacity: 0.6 + Math.random() * 0.4,
        },
      ]);
      setTimeout(() => {
        setWoolParticles(prev => prev.filter(p => p.id !== id));
      }, 1200);
    }, 350);
    return () => clearInterval(interval);
  }, [sheepState, isHidden, isDragging, direction, spriteWidth, spriteHeight]);

  // ─────────────────────────────────────────────────
  // CLOVER SPAWN SYSTEM
  // ─────────────────────────────────────────────────
  useEffect(() => {
    if (isHidden) return;
    const spawn = () => {
      const id = ++cloverIdRef.current;
      const x = randomBetween(30, window.innerWidth - 60);
      const duration = randomBetween(14, 22);
      setClovers(prev => [...prev, { id, x, duration, collected: false }]);
      setTimeout(() => {
        setClovers(prev => prev.filter(c => c.id !== id));
      }, duration * 1000 + 500);
    };
    const scheduleNext = () => {
      const delay = randomBetween(25_000, 55_000);
      return setTimeout(() => {
        spawn();
        timerRef = scheduleNext();
      }, delay);
    };
    let timerRef = setTimeout(() => {
      spawn();
      timerRef = scheduleNext();
    }, randomBetween(8_000, 15_000));
    return () => clearTimeout(timerRef);
  }, [isHidden]);

  // ── Collect clover ──
  const collectClover = useCallback((cloverId: number) => {
    setClovers(prev => prev.map(c => c.id === cloverId ? { ...c, collected: true } : c));
    setGrassCount(prev => prev + 1);
    setMood(prev => Math.min(100, prev + 8));
    showMessage('🌿 鲜草 +1！', 2000);
    setTimeout(() => {
      setClovers(prev => prev.filter(c => c.id !== cloverId));
    }, 500);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ─────────────────────────────────────────────────
  // SHOW MESSAGE
  // ─────────────────────────────────────────────────
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const showMessage = useCallback((msg: string, duration = 3000) => {
    setMessage(msg);
    clearTimeout(messageTimerRef.current);
    messageTimerRef.current = setTimeout(() => setMessage(null), duration);
  }, []);

  // ─────────────────────────────────────────────────
  // STATE TRANSITION (mood-aware)
  // ─────────────────────────────────────────────────
  const transitionState = useCallback(() => {
    setSheepState(prev => {
      const transitions = getTransitions(moodRef.current);
      const options = transitions[prev];
      const next = options[Math.floor(Math.random() * options.length)];
      if (next === 'walk') {
        setDirection(Math.random() > 0.5 ? 1 : -1);
      }
      return next;
    });
    setFrameIdx(0);
  }, []);

  // ── Frame Animation ──
  useEffect(() => {
    const fps = sheepState === 'walk' ? 200 : sheepState === 'sleep' ? 1000 : sheepState === 'play' ? 250 : 500;
    const interval = setInterval(() => {
      setFrameIdx(prev => (prev + 1) % currentFrames.length);
    }, fps);
    return () => clearInterval(interval);
  }, [sheepState, currentFrames.length]);

  // ── Walk Movement ──
  useEffect(() => {
    if (sheepState !== 'walk' || isDragging) return;
    const speed = 2;
    const interval = setInterval(() => {
      setPosX(prev => {
        const maxX = window.innerWidth - spriteWidth - 10;
        const next = prev + speed * direction;
        if (next <= 10) { setDirection(1); return 10; }
        if (next >= maxX) { setDirection(-1); return maxX; }
        return next;
      });
    }, 50);
    return () => clearInterval(interval);
  }, [sheepState, direction, spriteWidth, isDragging]);

  // ── Auto Transitions ──
  useEffect(() => {
    if (isDragging) return;
    const [minDur, maxDur] = STATE_DURATIONS[sheepState];
    stateTimerRef.current = setTimeout(transitionState, randomBetween(minDur, maxDur));
    return () => clearTimeout(stateTimerRef.current);
  }, [sheepState, transitionState, isDragging]);

  // ── Idle Timeout → Sleep ──
  useEffect(() => {
    const checkIdle = setInterval(() => {
      const elapsed = Date.now() - lastActivityRef.current;
      if (elapsed > 5 * 60 * 1000 && sheepState === 'idle') showMessage('有水喝吗...💧');
      if (elapsed > 10 * 60 * 1000 && sheepState !== 'sleep') {
        setSheepState('sleep');
        setFrameIdx(0);
        showMessage('💤 zzZ...');
      }
    }, 30_000);
    return () => clearInterval(checkIdle);
  }, [sheepState, showMessage]);

  // ── Mouse Proximity → Play ──
  useEffect(() => {
    let throttle = 0;
    const handler = (e: MouseEvent) => {
      const now = Date.now();
      if (now - throttle < 200) return;
      throttle = now;
      lastActivityRef.current = now;
      if (isDragging) return;
      const cx = posXRef.current + spriteWidth / 2;
      const cy = window.innerHeight - 12 - spriteHeight / 2;
      const dist = Math.hypot(e.clientX - cx, e.clientY - cy);
      if (dist < 80 && sheepState === 'idle') {
        setSheepState('play');
        setFrameIdx(0);
        setDirection(e.clientX > cx ? 1 : -1);
        clearTimeout(stateTimerRef.current);
        stateTimerRef.current = setTimeout(transitionState, randomBetween(2000, 4000));
      }
    };
    window.addEventListener('mousemove', handler, { passive: true });
    return () => window.removeEventListener('mousemove', handler);
  }, [sheepState, spriteWidth, spriteHeight, transitionState, isDragging]);

  // ─────────────────────────────────────────────────
  // DRAG SYSTEM
  // ─────────────────────────────────────────────────
  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    if (e.button === 2) return;
    const ref = dragRef.current;
    ref.startX = e.clientX;
    ref.startY = e.clientY;
    ref.origX = posXRef.current;
    ref.active = false;
    ref.timer = window.setTimeout(() => {
      ref.active = true;
      setIsDragging(true);
      setSheepState('play');
      setFrameIdx(0);
      showMessage('放我下来啦！😤', 5000);
      (e.target as HTMLElement).setPointerCapture?.(e.pointerId);
    }, 300);
  }, [showMessage]);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    const ref = dragRef.current;
    if (!ref.active) {
      if (Math.abs(e.clientX - ref.startX) > 5 || Math.abs(e.clientY - ref.startY) > 5) {
        clearTimeout(ref.timer);
      }
      return;
    }
    const newX = Math.max(0, Math.min(window.innerWidth - spriteWidth, e.clientX - spriteWidth / 2));
    const newBottom = Math.max(0, window.innerHeight - e.clientY - spriteHeight / 2);
    setPosX(newX);
    setPosY(newBottom);
  }, [spriteWidth, spriteHeight]);

  const handlePointerUp = useCallback(() => {
    const ref = dragRef.current;
    clearTimeout(ref.timer);
    if (!ref.active) return;
    ref.active = false;
    setIsDragging(false);
    setIsDropping(true);
    setPosY(12);
    setMood(prev => Math.min(100, prev + 5));
    setTimeout(() => {
      setIsDropping(false);
      setSheepState('idle');
      setFrameIdx(0);
    }, 500);
  }, []);

  // ─────────────────────────────────────────────────
  // RIGHT-CLICK PANEL
  // ─────────────────────────────────────────────────
  const handleContextMenu = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setPanel({ x: e.clientX, y: e.clientY });
  }, []);

  const closePanel = useCallback(() => setPanel(null), []);

  const feedSheep = useCallback(() => {
    if (grassCount <= 0) {
      showMessage('没有鲜草了...🥺', 2000);
      return;
    }
    setGrassCount(prev => prev - 1);
    setMood(prev => Math.min(100, prev + 15));
    setSheepState('graze');
    setFrameIdx(0);
    showMessage('好吃！😋', 3000);
    setPanel(null);
  }, [grassCount, showMessage]);

  const petSheep = useCallback(() => {
    setMood(prev => Math.min(100, prev + 5));
    setSheepState('play');
    setFrameIdx(0);
    showMessage('蹭蹭~ 🥰', 3000);
    setPanel(null);
  }, [showMessage]);

  // ─────────────────────────────────────────────────
  // PERSIST
  // ─────────────────────────────────────────────────
  useEffect(() => { localStorage.setItem('pixelsheep-clicks', String(clickCount)); }, [clickCount]);
  useEffect(() => { localStorage.setItem('pixelsheep-posX', String(posX)); }, [posX]);
  useEffect(() => { localStorage.setItem('pixelsheep-hidden', String(isHidden)); }, [isHidden]);
  useEffect(() => { localStorage.setItem('pixelsheep-mood', String(mood)); }, [mood]);
  useEffect(() => { localStorage.setItem('pixelsheep-grass', String(grassCount)); }, [grassCount]);

  // Time-of-day greeting
  useEffect(() => {
    const hour = new Date().getHours();
    const timer = setTimeout(() => {
      if (hour >= 0 && hour < 6) showMessage('这么晚还不睡？🌙');
      else if (hour >= 6 && hour < 9) showMessage('早上好~ ☀️');
      else if (hour >= 22) showMessage('该休息啦~ 🌙');
    }, 2000);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ─────────────────────────────────────────────────
  // CLICK HANDLER
  // ─────────────────────────────────────────────────
  const handleClick = useCallback((e: React.MouseEvent) => {
    if (isDragging) return;
    if (dragRef.current.active) return;
    e.stopPropagation();
    lastActivityRef.current = Date.now();
    const newCount = clickCount + 1;
    setClickCount(newCount);
    setMood(prev => Math.min(100, prev + 3));

    if (CLICK_MESSAGES[newCount]) {
      showMessage(CLICK_MESSAGES[newCount], 4000);
      if (newCount === 30) {
        setIsSuperSaiyan(true);
        setTimeout(() => setIsSuperSaiyan(false), 10_000);
      }
    } else if (newCount > 30 && newCount % 10 === 0) {
      showMessage(`${newCount} 次了还薅！🐾`, 2000);
    }

    if (sheepState === 'sleep') {
      setSheepState('idle');
      setFrameIdx(0);
      if (!CLICK_MESSAGES[newCount]) showMessage('咩？你把我吵醒了…');
    } else if (sheepState !== 'play') {
      setSheepState('play');
      setFrameIdx(0);
      clearTimeout(stateTimerRef.current);
      stateTimerRef.current = setTimeout(transitionState, 2000);
    }
  }, [clickCount, sheepState, showMessage, transitionState, isDragging]);

  // ─────────────────────────────────────────────────
  // RENDER
  // ─────────────────────────────────────────────────

  if (isHidden) {
    return (
      <button
        className="pixel-sheep__toggle pixel-sheep__toggle--show"
        onClick={() => setIsHidden(false)}
        title="显示小羊"
      >
        🐑
      </button>
    );
  }

  const sheepClasses = [
    'pixel-sheep',
    isSuperSaiyan && 'pixel-sheep--golden',
    isDragging && 'pixel-sheep--dragging',
    isDropping && 'pixel-sheep--dropping',
  ].filter(Boolean).join(' ');

  return (
    <>
      {/* ── Wool Particles ── */}
      {woolParticles.map(p => (
        <div
          key={p.id}
          className="pixel-sheep__wool-particle"
          style={{
            left: p.x,
            top: p.y,
            width: p.size,
            height: p.size,
            opacity: p.opacity,
          }}
        />
      ))}

      {/* ── Falling Clovers ── */}
      {clovers.map(clover => (
        <div
          key={clover.id}
          className={`pixel-sheep__clover${clover.collected ? ' pixel-sheep__clover--collected' : ''}`}
          style={{
            left: clover.x,
            animationDuration: `${clover.duration}s`,
            width: CLOVER_FRAME[0].length * PX + 16,
            height: CLOVER_FRAME.length * PX + 16,
          }}
          onClick={(e) => { e.stopPropagation(); if (!clover.collected) collectClover(clover.id); }}
        >
          <div
            className="pixel-sheep__clover-sprite"
            style={{
              width: PX,
              height: PX,
              boxShadow: CLOVER_SHADOW,
              marginLeft: 8,
              marginTop: 8,
            }}
          />
        </div>
      ))}

      {/* ── Sheep ── */}
      <div
        className={sheepClasses}
        style={{
          left: posX,
          bottom: posY,
          width: spriteWidth,
          height: spriteHeight,
        }}
        onClick={handleClick}
        onContextMenu={handleContextMenu}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
      >
        {/* Mood indicator */}
        <div className="pixel-sheep__mood">{getMoodEmoji(mood)}</div>

        {/* Speech Bubble */}
        {message && <div className="pixel-sheep__bubble">{message}</div>}

        {/* Sprite */}
        <div
          className={`pixel-sheep__sprite pixel-sheep__sprite--${sheepState}`}
          style={{
            position: 'absolute',
            bottom: 0,
            left: 0,
            width: PX,
            height: PX,
            boxShadow: currentShadow,
          }}
        />

        {/* Hide button */}
        <button
          className="pixel-sheep__hide"
          onClick={(e) => { e.stopPropagation(); setIsHidden(true); setMessage(null); }}
          title="隐藏小羊"
        >
          ×
        </button>
      </div>

      {/* ── Right-click Panel ── */}
      {panel && (
        <>
          <div className="pixel-sheep__panel-overlay" onClick={closePanel} onContextMenu={e => { e.preventDefault(); closePanel(); }} />
          <div
            className="pixel-sheep__panel"
            style={{
              left: Math.min(panel.x, window.innerWidth - 210),
              top: Math.min(panel.y, window.innerHeight - 250),
            }}
          >
            <div className="pixel-sheep__panel-title">🐑 小羊状态</div>
            <div className="pixel-sheep__panel-row">
              <span className="pixel-sheep__panel-label">心情</span>
              <span className="pixel-sheep__panel-value">
                {getMoodEmoji(mood)}
                <span className="pixel-sheep__panel-mood-bar">
                  <span className="pixel-sheep__panel-mood-fill" style={{ width: `${mood}%`, background: getMoodColor(mood) }} />
                </span>
              </span>
            </div>
            <div className="pixel-sheep__panel-row">
              <span className="pixel-sheep__panel-label">鲜草</span>
              <span className="pixel-sheep__panel-value">🌿 {grassCount}</span>
            </div>
            <div className="pixel-sheep__panel-row">
              <span className="pixel-sheep__panel-label">薅毛</span>
              <span className="pixel-sheep__panel-value">✋ {clickCount}</span>
            </div>
            <div className="pixel-sheep__panel-row">
              <span className="pixel-sheep__panel-label">状态</span>
              <span className="pixel-sheep__panel-value">{sheepState}</span>
            </div>
            <div className="pixel-sheep__panel-actions">
              <button className="pixel-sheep__panel-btn" onClick={feedSheep}>🌿 喂草</button>
              <button className="pixel-sheep__panel-btn" onClick={petSheep}>✋ 摸摸</button>
            </div>
          </div>
        </>
      )}
    </>
  );
};

export default PixelSheep;
