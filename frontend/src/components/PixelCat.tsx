import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import './PixelCat.css';

// ─── Pixel Art Config ────────────────────────────
const PX = 3;

const PALETTE: Record<string, string> = {
  B: '#4E342E', // dark brown — outline
  b: '#6D4C41', // medium brown — tail
  C: '#F5E6D3', // cream — body
  E: '#42A5F5', // blue — eyes
  N: '#EC407A', // pink — nose
  A: '#F48FB1', // pink — inner ear
  W: '#FFFDE7', // off-white — belly
};

type CatState = 'idle' | 'walk' | 'sleep' | 'play' | 'lick';

// ─── Sprite Frames (17 wide × 14 tall) ──────────
const FRAMES: Record<CatState, string[][]> = {
  idle: [
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCECCCCCECCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '..............bBb',
    ],
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCECCCCCECCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '.............bBb.',
    ],
  ],
  walk: [
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCECCCCCECCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '..B...........B..',
      '..B...........B..',
    ],
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCECCCCCECCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '....B.......B....',
      '....B.......B....',
    ],
  ],
  sleep: [
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCbBCCCbBCCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '..............bBb',
    ],
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCbBCCCbBCCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCWWWWWCCCB..',
      '..BCCCWWWWWCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '..............bBb',
    ],
  ],
  play: [
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCEECCCCCEECB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '..............Bb.',
    ],
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCEECCCCCEECB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '.............Bb..',
    ],
  ],
  lick: [
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCbBCCCCECCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '..............bBb',
    ],
    [
      '....B.......B....',
      '...BAB.....BAB...',
      '...BCBBBBBBBCB...',
      '..BCCCCCCCCCCCB..',
      '..BCCbBCCCCECCB..',
      '..BCCCCCNCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCCCCCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCWWWCCCCB..',
      '..BCCCCCCCCCCCB..',
      '...BCCCCCCCCCB...',
      '...BB.......BB...',
      '..............bBb',
    ],
  ],
};

// ─── Fish Pixel Art (7×5) ────────────────────────
const FISH_FRAME = [
  '..BBB..',
  '.BCCCB.',
  'BCCECCB',
  '.BCCCB.',
  '..BBB..',
];
const FISH_PALETTE: Record<string, string> = {
  B: '#8D6E63',
  C: '#FFD54F',
  E: '#4E342E',
};

function fishToBoxShadow(): string {
  const shadows: string[] = [];
  for (let y = 0; y < FISH_FRAME.length; y++) {
    for (let x = 0; x < FISH_FRAME[y].length; x++) {
      const ch = FISH_FRAME[y][x];
      if (ch !== '.' && FISH_PALETTE[ch]) {
        shadows.push(`${x * PX}px ${y * PX}px 0 ${FISH_PALETTE[ch]}`);
      }
    }
  }
  return shadows.join(',');
}

const FISH_SHADOW = fishToBoxShadow();

// ─── Easter Egg Messages ─────────────────────────
const CLICK_MESSAGES: Record<number, string> = {
  1: '喵~',
  3: '又戳我！',
  5: '好痒...',
  7: '朕准了 🐱',
  10: '十连抽猫猫！✨',
  15: '要把我戳穿吗 💢',
  20: '🏆 解锁成就：猫奴',
  30: '🌟 超级赛亚猫变身！',
};

// ─── State Machine Config ────────────────────────
const STATE_DURATIONS: Record<CatState, [number, number]> = {
  idle: [5000, 10000],
  walk: [3000, 6000],
  sleep: [8000, 15000],
  play: [2000, 4000],
  lick: [3000, 5000],
};

function randomBetween(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// ─── Mood-aware transitions ──────────────────────
function getTransitions(mood: number): Record<CatState, CatState[]> {
  if (mood > 70) {
    return {
      idle: ['walk', 'play', 'lick', 'idle'],
      walk: ['idle', 'play', 'lick'],
      sleep: ['idle', 'idle', 'play'],
      play: ['idle', 'walk', 'lick'],
      lick: ['idle', 'walk', 'play'],
    };
  }
  if (mood < 30) {
    return {
      idle: ['sleep', 'idle', 'idle', 'lick'],
      walk: ['idle', 'idle', 'sleep'],
      sleep: ['sleep', 'idle', 'lick'],
      play: ['idle', 'idle'],
      lick: ['idle', 'sleep'],
    };
  }
  return {
    idle: ['walk', 'lick', 'idle', 'idle'],
    walk: ['idle', 'idle', 'lick'],
    sleep: ['idle', 'idle', 'lick'],
    play: ['idle', 'lick'],
    lick: ['idle', 'idle', 'walk'],
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
  if (mood > 80) return '😸';
  if (mood > 60) return '😊';
  if (mood > 40) return '😐';
  if (mood > 20) return '😿';
  return '😢';
}

function getMoodColor(mood: number): string {
  if (mood > 60) return '#66BB6A';
  if (mood > 30) return '#FFA726';
  return '#EF5350';
}

// ─── Fish Type ───────────────────────────────────
interface Fish {
  id: number;
  x: number;
  duration: number;
  collected: boolean;
}

// ─── Component ───────────────────────────────────
const PixelCat: React.FC = () => {
  // ── Position ──
  const [posX, setPosX] = useState(() => {
    const saved = localStorage.getItem('pixelcat-posX');
    return saved ? parseInt(saved, 10) : Math.floor(window.innerWidth / 2);
  });
  const [posY, setPosY] = useState(12); // bottom offset (used during drag)

  // ── State machine ──
  const [catState, setCatState] = useState<CatState>('idle');
  const [frameIdx, setFrameIdx] = useState(0);
  const [direction, setDirection] = useState<1 | -1>(1);

  // ── Click counter ──
  const [clickCount, setClickCount] = useState(() => {
    const saved = localStorage.getItem('pixelcat-clicks');
    return saved ? parseInt(saved, 10) : 0;
  });

  // ── Mood (0-100) ──
  const [mood, setMood] = useState(() => {
    const saved = localStorage.getItem('pixelcat-mood');
    return saved ? Math.min(100, Math.max(0, parseInt(saved, 10))) : 70;
  });

  // ── Fish collection ──
  const [fishCount, setFishCount] = useState(() => {
    const saved = localStorage.getItem('pixelcat-fish');
    return saved ? parseInt(saved, 10) : 0;
  });
  const [fishes, setFishes] = useState<Fish[]>([]);

  // ── UI ──
  const [message, setMessage] = useState<string | null>(null);
  const [isHidden, setIsHidden] = useState(() => {
    return localStorage.getItem('pixelcat-hidden') === 'true';
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
  const fishIdRef = useRef(0);
  const moodRef = useRef(mood);

  useEffect(() => { posXRef.current = posX; }, [posX]);
  useEffect(() => { moodRef.current = mood; }, [mood]);

  // ── Sprite metrics ──
  const currentFrames = FRAMES[catState];
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
      setMood(prev => Math.max(0, prev - 1)); // -1 every 12s = ~5/min
    }, 12_000);
    return () => clearInterval(interval);
  }, []);

  // ─────────────────────────────────────────────────
  // FISH SPAWN SYSTEM
  // ─────────────────────────────────────────────────
  useEffect(() => {
    if (isHidden) return;
    const spawn = () => {
      const id = ++fishIdRef.current;
      const x = randomBetween(30, window.innerWidth - 60);
      const duration = randomBetween(14, 22);
      setFishes(prev => [...prev, { id, x, duration, collected: false }]);
      // Auto-remove after fall
      setTimeout(() => {
        setFishes(prev => prev.filter(f => f.id !== id));
      }, duration * 1000 + 500);
    };
    const scheduleNext = () => {
      const delay = randomBetween(25_000, 55_000);
      return setTimeout(() => {
        spawn();
        timerRef = scheduleNext();
      }, delay);
    };
    // First fish after short delay
    let timerRef = setTimeout(() => {
      spawn();
      timerRef = scheduleNext();
    }, randomBetween(8_000, 15_000));
    return () => clearTimeout(timerRef);
  }, [isHidden]);

  // ── Collect fish ──
  const collectFish = useCallback((fishId: number) => {
    setFishes(prev => prev.map(f => f.id === fishId ? { ...f, collected: true } : f));
    setFishCount(prev => prev + 1);
    setMood(prev => Math.min(100, prev + 8));
    showMessage('🐟 鱼干 +1！', 2000);
    setTimeout(() => {
      setFishes(prev => prev.filter(f => f.id !== fishId));
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
    setCatState(prev => {
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
    const fps = catState === 'walk' ? 200 : catState === 'sleep' ? 1000 : catState === 'play' ? 250 : 500;
    const interval = setInterval(() => {
      setFrameIdx(prev => (prev + 1) % currentFrames.length);
    }, fps);
    return () => clearInterval(interval);
  }, [catState, currentFrames.length]);

  // ── Walk Movement ──
  useEffect(() => {
    if (catState !== 'walk' || isDragging) return;
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
  }, [catState, direction, spriteWidth, isDragging]);

  // ── Auto Transitions ──
  useEffect(() => {
    if (isDragging) return;
    const [minDur, maxDur] = STATE_DURATIONS[catState];
    stateTimerRef.current = setTimeout(transitionState, randomBetween(minDur, maxDur));
    return () => clearTimeout(stateTimerRef.current);
  }, [catState, transitionState, isDragging]);

  // ── Idle Timeout → Sleep ──
  useEffect(() => {
    const checkIdle = setInterval(() => {
      const elapsed = Date.now() - lastActivityRef.current;
      if (elapsed > 5 * 60 * 1000 && catState === 'idle') showMessage('有水喝吗...💧');
      if (elapsed > 10 * 60 * 1000 && catState !== 'sleep') {
        setCatState('sleep');
        setFrameIdx(0);
        showMessage('💤 zzZ...');
      }
    }, 30_000);
    return () => clearInterval(checkIdle);
  }, [catState, showMessage]);

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
      if (dist < 80 && catState === 'idle') {
        setCatState('play');
        setFrameIdx(0);
        setDirection(e.clientX > cx ? 1 : -1);
        clearTimeout(stateTimerRef.current);
        stateTimerRef.current = setTimeout(transitionState, randomBetween(2000, 4000));
      }
    };
    window.addEventListener('mousemove', handler, { passive: true });
    return () => window.removeEventListener('mousemove', handler);
  }, [catState, spriteWidth, spriteHeight, transitionState, isDragging]);

  // ─────────────────────────────────────────────────
  // DRAG SYSTEM
  // ─────────────────────────────────────────────────
  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    if (e.button === 2) return; // ignore right-click
    const ref = dragRef.current;
    ref.startX = e.clientX;
    ref.startY = e.clientY;
    ref.origX = posXRef.current;
    ref.active = false;
    // Start drag after 300ms hold
    ref.timer = window.setTimeout(() => {
      ref.active = true;
      setIsDragging(true);
      setCatState('play');
      setFrameIdx(0);
      showMessage('放我下来啦！😹', 5000);
      (e.target as HTMLElement).setPointerCapture?.(e.pointerId);
    }, 300);
  }, [showMessage]);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    const ref = dragRef.current;
    if (!ref.active) {
      // If moved too much before timer, cancel drag intent
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
    setPosY(12); // snap back to bottom
    setMood(prev => Math.min(100, prev + 5));
    setTimeout(() => {
      setIsDropping(false);
      setCatState('idle');
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

  const feedCat = useCallback(() => {
    if (fishCount <= 0) {
      showMessage('没有鱼干了...🥺', 2000);
      return;
    }
    setFishCount(prev => prev - 1);
    setMood(prev => Math.min(100, prev + 15));
    setCatState('lick');
    setFrameIdx(0);
    showMessage('好吃！😋', 3000);
    setPanel(null);
  }, [fishCount, showMessage]);

  const petCat = useCallback(() => {
    setMood(prev => Math.min(100, prev + 5));
    setCatState('play');
    setFrameIdx(0);
    showMessage('咕噜咕噜~ 🥰', 3000);
    setPanel(null);
  }, [showMessage]);

  // ─────────────────────────────────────────────────
  // PERSIST
  // ─────────────────────────────────────────────────
  useEffect(() => { localStorage.setItem('pixelcat-clicks', String(clickCount)); }, [clickCount]);
  useEffect(() => { localStorage.setItem('pixelcat-posX', String(posX)); }, [posX]);
  useEffect(() => { localStorage.setItem('pixelcat-hidden', String(isHidden)); }, [isHidden]);
  useEffect(() => { localStorage.setItem('pixelcat-mood', String(mood)); }, [mood]);
  useEffect(() => { localStorage.setItem('pixelcat-fish', String(fishCount)); }, [fishCount]);

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
    // Don't count if just finished dragging
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
      showMessage(`${newCount} 次了还戳！🐾`, 2000);
    }

    if (catState === 'sleep') {
      setCatState('idle');
      setFrameIdx(0);
      if (!CLICK_MESSAGES[newCount]) showMessage('喵？你把我吵醒了…');
    } else if (catState !== 'play') {
      setCatState('play');
      setFrameIdx(0);
      clearTimeout(stateTimerRef.current);
      stateTimerRef.current = setTimeout(transitionState, 2000);
    }
  }, [clickCount, catState, showMessage, transitionState, isDragging]);

  // ─────────────────────────────────────────────────
  // RENDER
  // ─────────────────────────────────────────────────

  // Hidden state
  if (isHidden) {
    return (
      <button className="pixel-cat__toggle pixel-cat__toggle--show" onClick={() => setIsHidden(false)} title="显示猫猫">🐱</button>
    );
  }

  const catClasses = [
    'pixel-cat',
    isSuperSaiyan && 'pixel-cat--golden',
    isDragging && 'pixel-cat--dragging',
    isDropping && 'pixel-cat--dropping',
  ].filter(Boolean).join(' ');

  return (
    <>
      {/* ── Falling Fishes ── */}
      {fishes.map(fish => (
        <div
          key={fish.id}
          className={`pixel-cat__fish${fish.collected ? ' pixel-cat__fish--collected' : ''}`}
          style={{
            left: fish.x,
            animationDuration: `${fish.duration}s`,
            width: FISH_FRAME[0].length * PX + 16,
            height: FISH_FRAME.length * PX + 16,
          }}
          onClick={(e) => { e.stopPropagation(); if (!fish.collected) collectFish(fish.id); }}
        >
          <div
            className="pixel-cat__fish-sprite"
            style={{
              width: PX,
              height: PX,
              boxShadow: FISH_SHADOW,
              marginLeft: 8,
              marginTop: 8,
            }}
          />
        </div>
      ))}

      {/* ── Cat ── */}
      <div
        className={catClasses}
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
        <div className="pixel-cat__mood">{getMoodEmoji(mood)}</div>

        {/* Speech Bubble */}
        {message && <div className="pixel-cat__bubble">{message}</div>}

        {/* Sprite */}
        <div
          className={`pixel-cat__sprite pixel-cat__sprite--${catState}`}
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
          className="pixel-cat__hide"
          onClick={(e) => { e.stopPropagation(); setIsHidden(true); setMessage(null); }}
          title="隐藏猫猫"
        >
          ×
        </button>
      </div>

      {/* ── Right-click Panel ── */}
      {panel && (
        <>
          <div className="pixel-cat__panel-overlay" onClick={closePanel} onContextMenu={e => { e.preventDefault(); closePanel(); }} />
          <div
            className="pixel-cat__panel"
            style={{
              left: Math.min(panel.x, window.innerWidth - 210),
              top: Math.min(panel.y, window.innerHeight - 250),
            }}
          >
            <div className="pixel-cat__panel-title">🐱 猫猫状态</div>
            <div className="pixel-cat__panel-row">
              <span className="pixel-cat__panel-label">心情</span>
              <span className="pixel-cat__panel-value">
                {getMoodEmoji(mood)}
                <span className="pixel-cat__panel-mood-bar">
                  <span className="pixel-cat__panel-mood-fill" style={{ width: `${mood}%`, background: getMoodColor(mood) }} />
                </span>
              </span>
            </div>
            <div className="pixel-cat__panel-row">
              <span className="pixel-cat__panel-label">鱼干</span>
              <span className="pixel-cat__panel-value">🐟 {fishCount}</span>
            </div>
            <div className="pixel-cat__panel-row">
              <span className="pixel-cat__panel-label">摸头</span>
              <span className="pixel-cat__panel-value">✋ {clickCount}</span>
            </div>
            <div className="pixel-cat__panel-row">
              <span className="pixel-cat__panel-label">状态</span>
              <span className="pixel-cat__panel-value">{catState}</span>
            </div>
            <div className="pixel-cat__panel-actions">
              <button className="pixel-cat__panel-btn" onClick={feedCat}>🐟 喂食</button>
              <button className="pixel-cat__panel-btn" onClick={petCat}>✋ 摸摸</button>
            </div>
          </div>
        </>
      )}
    </>
  );
};

export default PixelCat;
