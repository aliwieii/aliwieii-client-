import React, { useState, useRef, useEffect } from 'react';
import {
  NexoraModule,
  clampHudPosition,
  snapToGrid,
  autoArrangeHudModules,
} from '../core/nexoraEngine';
import {
  Grid,
  Lock,
  Unlock,
  RotateCcw,
  LayoutGrid,
  Smartphone,
  Monitor,
  Sliders,
  Crosshair,
  Swords,
} from 'lucide-react';

interface HudEditorCanvasProps {
  modules: NexoraModule[];
  onUpdateModules: (updated: NexoraModule[]) => void;
  lang: 'ar' | 'en';
  liveFps: number;
  activeProfileName: string;
  reducedMotion: boolean;
}

export const HudEditorCanvas: React.FC<HudEditorCanvasProps> = ({
  modules,
  onUpdateModules,
  lang,
  liveFps,
  activeProfileName,
  reducedMotion,
}) => {
  const isRtl = lang === 'ar';
  const hudModules = modules.filter((m) => m.isHud);

  const [selectedId, setSelectedId] = useState<string>('fps');
  const [showGrid, setShowGrid] = useState<boolean>(true);
  const [aspectMode, setAspectMode] = useState<'16:9' | '20:9'>('20:9'); // 20:9 matches Moto G Stylus 5G (2024)
  const [draggingId, setDraggingId] = useState<string | null>(null);
  const dragOffset = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const canvasRef = useRef<HTMLDivElement | null>(null);

  // Interactive test state inside the HUD Editor (CPS, Combo, Keystrokes)
  const [leftClicks, setLeftClicks] = useState<number[]>([]);
  const [rightClicks, setRightClicks] = useState<number[]>([]);
  const [comboCount, setComboCount] = useState<number>(3);
  const [reachDist, setReachDist] = useState<string>('2.84');
  const [pressedKeys, setPressedKeys] = useState<Record<string, boolean>>({});

  const canvasWidth = aspectMode === '20:9' ? 800 : 768;
  const canvasHeight = aspectMode === '20:9' ? 360 : 432;

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (['INPUT', 'TEXTAREA'].includes((e.target as HTMLElement)?.tagName)) return;
      setPressedKeys((prev) => ({ ...prev, [e.code]: true }));
    };
    const onKeyUp = (e: KeyboardEvent) => {
      setPressedKeys((prev) => ({ ...prev, [e.code]: false }));
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
    };
  }, []);

  const selectedModule =
    hudModules.find((m) => m.id === selectedId) || hudModules[0];

  const handlePointerDown = (
    e: React.PointerEvent<HTMLDivElement>,
    mod: NexoraModule
  ) => {
    e.stopPropagation();
    setSelectedId(mod.id);
    if (mod.locked) return;

    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;
    const scaleRatioX = canvasWidth / rect.width;
    const scaleRatioY = canvasHeight / rect.height;

    const localX = (e.clientX - rect.left) * scaleRatioX;
    const localY = (e.clientY - rect.top) * scaleRatioY;

    dragOffset.current = {
      x: localX - mod.x,
      y: localY - mod.y,
    };
    setDraggingId(mod.id);
    (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
  };

  const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
    if (!draggingId) return;
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const scaleRatioX = canvasWidth / rect.width;
    const scaleRatioY = canvasHeight / rect.height;

    const localX = (e.clientX - rect.left) * scaleRatioX;
    const localY = (e.clientY - rect.top) * scaleRatioY;

    const updated = modules.map((m) => {
      if (m.id !== draggingId || m.locked) return m;
      let rawX = localX - dragOffset.current.x;
      let rawY = localY - dragOffset.current.y;
      if (showGrid) {
        rawX = snapToGrid(rawX, 8);
        rawY = snapToGrid(rawY, 8);
      }
      const clamped = clampHudPosition(
        rawX,
        rawY,
        m.width,
        m.height,
        m.scale,
        canvasWidth,
        canvasHeight
      );
      return { ...m, x: clamped.x, y: clamped.y };
    });

    onUpdateModules(updated);
  };

  const handlePointerUp = () => {
    if (draggingId) {
      setDraggingId(null);
    }
  };

  const updateSelectedProp = (patch: Partial<NexoraModule>) => {
    if (!selectedModule) return;
    const next = modules.map((m) => {
      if (m.id !== selectedModule.id) return m;
      const merged = { ...m, ...patch };
      const clamped = clampHudPosition(
        merged.x,
        merged.y,
        merged.width,
        merged.height,
        merged.scale,
        canvasWidth,
        canvasHeight
      );
      return { ...merged, x: clamped.x, y: clamped.y };
    });
    onUpdateModules(next);
  };

  const handleAutoArrange = () => {
    onUpdateModules(autoArrangeHudModules(modules, canvasWidth, canvasHeight));
  };

  const handleResetAllHud = () => {
    const next = modules.map((m) => {
      if (!m.isHud) return m;
      return {
        ...m,
        x: m.defaultX,
        y: m.defaultY,
        scale: 1.0,
        opacity: 0.88,
        fontSize: 13,
        textAlign: 'center' as const,
        accentColor: '#00A8FF',
        locked: false,
      };
    });
    onUpdateModules(next);
  };

  const triggerSimulatedHit = (button: 'left' | 'right') => {
    const now = Date.now();
    if (button === 'left') {
      setLeftClicks((prev) => [...prev.filter((t) => now - t <= 1000), now]);
      setComboCount((c) => c + 1);
      setReachDist((2.45 + ( (now % 55) / 100 )).toFixed(2));
    } else {
      setRightClicks((prev) => [...prev.filter((t) => now - t <= 1000), now]);
    }
  };

  const now = Date.now();
  const activeLmbCps = leftClicks.filter((t) => now - t <= 1000).length;
  const activeRmbCps = rightClicks.filter((t) => now - t <= 1000).length;

  const renderWidgetInner = (mod: NexoraModule) => {
    if (mod.id === 'fps') {
      return `${liveFps} FPS`;
    }
    if (mod.id === 'cps') {
      return `${activeLmbCps} | ${activeRmbCps} CPS`;
    }
    if (mod.id === 'combo') {
      return `${comboCount} Combo`;
    }
    if (mod.id === 'reach') {
      return `${reachDist} blocks`;
    }
    if (mod.id === 'keystrokes') {
      const w = pressedKeys['KeyW'] || pressedKeys['ArrowUp'];
      const a = pressedKeys['KeyA'] || pressedKeys['ArrowLeft'];
      const s = pressedKeys['KeyS'] || pressedKeys['ArrowDown'];
      const d = pressedKeys['KeyD'] || pressedKeys['ArrowRight'];
      const sp = pressedKeys['Space'];
      return (
        <div className="flex flex-col items-center gap-1 w-full p-1 select-none" dir="ltr">
          <div className="grid grid-cols-3 gap-1 w-full">
            <div />
            <div
              className={`h-6 flex items-center justify-center rounded text-[11px] font-mono font-semibold border ${
                w
                  ? 'bg-[#00A8FF] text-[#0B0E14] border-[#00A8FF]'
                  : 'bg-[#0B0E14]/80 text-[#E6EDF3] border-white/15'
              }`}
            >
              W
            </div>
            <div />
          </div>
          <div className="grid grid-cols-3 gap-1 w-full">
            <div
              className={`h-6 flex items-center justify-center rounded text-[11px] font-mono font-semibold border ${
                a
                  ? 'bg-[#00A8FF] text-[#0B0E14] border-[#00A8FF]'
                  : 'bg-[#0B0E14]/80 text-[#E6EDF3] border-white/15'
              }`}
            >
              A
            </div>
            <div
              className={`h-6 flex items-center justify-center rounded text-[11px] font-mono font-semibold border ${
                s
                  ? 'bg-[#00A8FF] text-[#0B0E14] border-[#00A8FF]'
                  : 'bg-[#0B0E14]/80 text-[#E6EDF3] border-white/15'
              }`}
            >
              S
            </div>
            <div
              className={`h-6 flex items-center justify-center rounded text-[11px] font-mono font-semibold border ${
                d
                  ? 'bg-[#00A8FF] text-[#0B0E14] border-[#00A8FF]'
                  : 'bg-[#0B0E14]/80 text-[#E6EDF3] border-white/15'
              }`}
            >
              D
            </div>
          </div>
          <div
            className={`w-full h-5 flex items-center justify-center rounded text-[10px] font-mono border ${
              sp
                ? 'bg-[#00A8FF] text-[#0B0E14] border-[#00A8FF]'
                : 'bg-[#0B0E14]/80 text-[#8B949E] border-white/15'
            }`}
          >
            SPACE
          </div>
        </div>
      );
    }
    return mod.previewText || mod.nameEn;
  };

  return (
    <div className="space-y-5">
      {/* Top Action Bar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-[#111621] border border-[#21262D] rounded-lg p-3.5">
        <div className="flex items-center gap-3">
          <span className="text-sm font-semibold text-[#E6EDF3]">
            {isRtl
              ? `محرر الواجهة المباشر — الملف النشط: ${activeProfileName}`
              : `Interactive HUD Studio — Active Profile: ${activeProfileName}`}
          </span>
          <span className="text-xs text-[#8B949E]" aria-hidden="true">
            ·
          </span>
          <span className="text-xs font-mono text-[#00A8FF]" dir="ltr">
            {hudModules.filter((m) => m.enabled).length} / {hudModules.length} HUD Active
          </span>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {/* Aspect Ratio Switcher */}
          <div className="inline-flex items-center bg-[#0B0E14] border border-[#21262D] rounded-md p-0.5">
            <button
              type="button"
              onClick={() => setAspectMode('20:9')}
              className={`inline-flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium rounded whitespace-nowrap shrink-0 transition-colors ${
                aspectMode === '20:9'
                  ? 'bg-[#00A8FF] text-[#0B0E14] font-semibold'
                  : 'text-[#8B949E] hover:text-[#E6EDF3]'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5" />
              <span>{isRtl ? 'شاشة هاتف 20:9 (Moto G)' : '20:9 Phone (Moto G)'}</span>
            </button>
            <button
              type="button"
              onClick={() => setAspectMode('16:9')}
              className={`inline-flex items-center gap-1.5 px-2.5 py-1.5 text-xs font-medium rounded whitespace-nowrap shrink-0 transition-colors ${
                aspectMode === '16:9'
                  ? 'bg-[#00A8FF] text-[#0B0E14] font-semibold'
                  : 'text-[#8B949E] hover:text-[#E6EDF3]'
              }`}
            >
              <Monitor className="w-3.5 h-3.5" />
              <span>{isRtl ? 'قياسي 16:9' : '16:9 Standard'}</span>
            </button>
          </div>

          <button
            type="button"
            onClick={() => setShowGrid((g) => !g)}
            className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md border whitespace-nowrap shrink-0 transition-colors ${
              showGrid
                ? 'bg-[#00A8FF]/15 text-[#00A8FF] border-[#00A8FF]/50'
                : 'bg-[#161B22] text-[#8B949E] border-[#21262D] hover:text-[#E6EDF3]'
            }`}
          >
            <Grid className="w-3.5 h-3.5" />
            <span>{showGrid ? (isRtl ? 'إخفاء الشبكة' : 'Hide Grid') : isRtl ? 'إظهار الشبكة' : 'Show Grid'}</span>
          </button>

          <button
            type="button"
            onClick={handleAutoArrange}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-[#161B22] text-[#E6EDF3] border border-[#21262D] hover:border-[#00A8FF]/60 whitespace-nowrap shrink-0 transition-colors"
          >
            <LayoutGrid className="w-3.5 h-3.5 text-[#00A8FF]" />
            <span>{isRtl ? 'ترتيب تلقائي' : 'Auto Arrange'}</span>
          </button>

          <button
            type="button"
            onClick={handleResetAllHud}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-md bg-[#161B22] text-[#F85149] border border-[#21262D] hover:border-[#F85149]/60 whitespace-nowrap shrink-0 transition-colors"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>{isRtl ? 'استعادة الترتيب الافتراضي' : 'Reset Default Layout'}</span>
          </button>
        </div>
      </div>

      {/* Main Editor Split: Viewport Canvas + Element Property Inspector */}
      <div className="grid grid-cols-1 xl:grid-cols-12 gap-5 items-start">
        {/* Interactive Canvas (8 cols) */}
        <div className="xl:col-span-8 space-y-3">
          <div
            ref={canvasRef}
            onPointerMove={handlePointerMove}
            onPointerUp={handlePointerUp}
            onPointerLeave={handlePointerUp}
            data-testid="hud-editor-canvas"
            className="relative w-full overflow-hidden rounded-lg border border-[#21262D] bg-[#0D131F] select-none touch-none"
            style={{
              aspectRatio: aspectMode === '20:9' ? '20 / 9' : '16 / 9',
              backgroundImage: showGrid
                ? 'linear-gradient(to right, rgba(255,255,255,0.04) 1px, transparent 1px), linear-gradient(to bottom, rgba(255,255,255,0.04) 1px, transparent 1px)'
                : undefined,
              backgroundSize: showGrid ? '24px 24px' : undefined,
            }}
          >
            {/* Simulated Minecraft 1.8.9 Sky/Arena Horizon Band */}
            <div
              className="absolute inset-0 pointer-events-none"
              style={{
                background:
                  'radial-gradient(circle at 50% 35%, rgba(0, 168, 255, 0.09), rgba(11, 14, 20, 0.92) 78%)',
              }}
            />

            {/* Center Alignment Crosshair Guides */}
            {showGrid && (
              <>
                <div className="absolute inset-y-0 left-1/2 w-px bg-[#00A8FF]/25 pointer-events-none" />
                <div className="absolute inset-x-0 top-1/2 h-px bg-[#00A8FF]/25 pointer-events-none" />
              </>
            )}

            {/* Center Custom Crosshair Reticle */}
            <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 pointer-events-none flex items-center justify-center">
              <Crosshair className="w-5 h-5 text-[#00A8FF]/80" />
            </div>

            {/* Active Draggable HUD Elements */}
            {hudModules
              .filter((m) => m.enabled)
              .map((mod) => {
                const isSelected = selectedModule?.id === mod.id;
                const leftPct = (mod.x / canvasWidth) * 100;
                const topPct = (mod.y / canvasHeight) * 100;
                const widthPct = ((mod.width * mod.scale) / canvasWidth) * 100;
                const heightPct = ((mod.height * mod.scale) / canvasHeight) * 100;

                return (
                  <div
                    key={mod.id}
                    role="button"
                    tabIndex={0}
                    aria-label={`HUD Widget ${mod.nameEn}`}
                    onPointerDown={(e) => handlePointerDown(e, mod)}
                    onClick={() => setSelectedId(mod.id)}
                    style={{
                      left: `${leftPct}%`,
                      top: `${topPct}%`,
                      width: `${widthPct}%`,
                      minHeight: `${heightPct}%`,
                      backgroundColor: mod.showBackground
                        ? `rgba(17, 22, 33, ${mod.opacity})`
                        : 'transparent',
                      borderColor: isSelected
                        ? mod.locked
                          ? '#F85149'
                          : '#00A8FF'
                        : 'rgba(255,255,255,0.14)',
                      fontSize: `${mod.fontSize}px`,
                      textAlign: mod.textAlign,
                      transition:
                        reducedMotion || draggingId === mod.id
                          ? 'none'
                          : 'border-color 120ms ease',
                    }}
                    className={`absolute rounded border px-2 py-1 flex flex-col justify-center ${
                      mod.locked ? 'cursor-not-allowed' : 'cursor-grab active:cursor-grabbing'
                    }`}
                  >
                    <div
                      className="font-mono font-semibold tabular-nums leading-tight truncate"
                      style={{ color: mod.accentColor }}
                      dir="ltr"
                    >
                      {renderWidgetInner(mod)}
                    </div>

                    {isSelected && (
                      <div
                        className="absolute -top-5 left-0 flex items-center gap-1 px-1.5 py-0.5 rounded bg-[#0B0E14] border border-[#21262D] text-[10px] font-mono text-[#8B949E] whitespace-nowrap pointer-events-none"
                        dir="ltr"
                      >
                        <span>{mod.nameEn}</span>
                        <span>·</span>
                        <span>
                          {mod.x},{mod.y}
                        </span>
                        {mod.locked && <Lock className="w-2.5 h-2.5 text-[#F85149]" />}
                      </div>
                    )}
                  </div>
                );
              })}

            {/* Bottom Simulated Minecraft 1.8.9 9-Slot Hotbar */}
            <div
              className="absolute bottom-2 left-1/2 -translate-x-1/2 flex items-center gap-1 px-2 py-1 rounded bg-[#0B0E14]/90 border border-[#21262D] pointer-events-none"
              dir="ltr"
            >
              {['1:Sword', '2:Blocks', '3:GApple', '4:Pearl', '5:Bow', '6:Pot', '7:TNT', '8:Tool', '9:Compass'].map(
                (slot, idx) => (
                  <div
                    key={slot}
                    className={`w-10 h-7 rounded flex items-center justify-center text-[9px] font-mono ${
                      idx === 0
                        ? 'bg-[#00A8FF]/20 border border-[#00A8FF] text-[#E6EDF3]'
                        : 'bg-[#161B22] border border-[#21262D] text-[#8B949E]'
                    }`}
                  >
                    {slot.split(':')[1]}
                  </div>
                )
              )}
            </div>
          </div>

          {/* Interactive Combat & Input Test Bar below Canvas */}
          <div className="flex flex-wrap items-center justify-between gap-3 bg-[#111621] border border-[#21262D] rounded-lg px-4 py-2.5">
            <div className="flex items-center gap-2 text-xs text-[#8B949E]">
              <Swords className="w-4 h-4 text-[#00A8FF] shrink-0" />
              <span>
                {isRtl
                  ? 'اختبر استجابة عناصر HUD مباشرة (اضغط الأزرار أو مفاتيح WASD):'
                  : 'Test live HUD telemetry (click buttons or press WASD keys):'}
              </span>
            </div>
            <div className="flex items-center gap-2" dir="ltr">
              <button
                type="button"
                onClick={() => triggerSimulatedHit('left')}
                className="px-3 py-1.5 text-xs font-mono font-semibold rounded bg-[#00A8FF] text-[#0B0E14] hover:bg-[#33B9FF] transition-colors whitespace-nowrap shrink-0"
              >
                LMB Attack (+CPS / Combo)
              </button>
              <button
                type="button"
                onClick={() => triggerSimulatedHit('right')}
                className="px-3 py-1.5 text-xs font-mono font-semibold rounded bg-[#161B22] text-[#E6EDF3] border border-[#21262D] hover:border-[#A371F7] transition-colors whitespace-nowrap shrink-0"
              >
                RMB Place Block (+CPS)
              </button>
            </div>
          </div>
        </div>

        {/* Element Inspector & HUD Toggle List (4 cols) */}
        <div className="xl:col-span-4 space-y-4">
          {selectedModule && (
            <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-4">
              <div className="flex items-center justify-between border-b border-[#21262D] pb-3">
                <div>
                  <h3 className="text-sm font-semibold text-[#E6EDF3]">
                    {isRtl ? selectedModule.nameAr : selectedModule.nameEn}
                  </h3>
                  <p className="text-xs font-mono text-[#8B949E] mt-0.5" dir="ltr">
                    X: {selectedModule.x}px · Y: {selectedModule.y}px · Scale: {selectedModule.scale.toFixed(2)}x
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() =>
                      updateSelectedProp({ locked: !selectedModule.locked })
                    }
                    aria-label={
                      selectedModule.locked ? 'Unlock HUD Element' : 'Lock HUD Element'
                    }
                    className={`inline-flex items-center gap-1 px-2.5 py-1.5 rounded text-xs font-medium border transition-colors ${
                      selectedModule.locked
                        ? 'bg-[#F85149]/15 text-[#F85149] border-[#F85149]/50'
                        : 'bg-[#161B22] text-[#8B949E] border-[#21262D] hover:text-[#E6EDF3]'
                    }`}
                  >
                    {selectedModule.locked ? (
                      <>
                        <Lock className="w-3.5 h-3.5" />
                        <span>{isRtl ? 'مقفل' : 'Locked'}</span>
                      </>
                    ) : (
                      <>
                        <Unlock className="w-3.5 h-3.5" />
                        <span>{isRtl ? 'قفل' : 'Lock'}</span>
                      </>
                    )}
                  </button>

                  <button
                    type="button"
                    onClick={() =>
                      updateSelectedProp({ enabled: !selectedModule.enabled })
                    }
                    className={`px-3 py-1.5 rounded text-xs font-semibold transition-colors ${
                      selectedModule.enabled
                        ? 'bg-[#238636] text-white'
                        : 'bg-[#21262D] text-[#8B949E]'
                    }`}
                  >
                    {selectedModule.enabled
                      ? isRtl
                        ? 'مفعل'
                        : 'Enabled'
                      : isRtl
                      ? 'معطل'
                      : 'Disabled'}
                  </button>
                </div>
              </div>

              {/* Scale Slider */}
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs">
                  <label htmlFor="hud-scale-slider" className="text-[#8B949E]">
                    {isRtl ? 'حجم العنصر (Scale)' : 'Element Scale'}
                  </label>
                  <span className="font-mono text-[#E6EDF3]" dir="ltr">
                    {Math.round(selectedModule.scale * 100)}%
                  </span>
                </div>
                <input
                  id="hud-scale-slider"
                  type="range"
                  min="0.6"
                  max="1.8"
                  step="0.05"
                  value={selectedModule.scale}
                  onChange={(e) =>
                    updateSelectedProp({ scale: parseFloat(e.target.value) })
                  }
                  className="w-full accent-[#00A8FF]"
                />
              </div>

              {/* Opacity Slider */}
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs">
                  <label htmlFor="hud-opacity-slider" className="text-[#8B949E]">
                    {isRtl ? 'شفافية الخلفية (Opacity)' : 'Background Opacity'}
                  </label>
                  <span className="font-mono text-[#E6EDF3]" dir="ltr">
                    {Math.round(selectedModule.opacity * 100)}%
                  </span>
                </div>
                <input
                  id="hud-opacity-slider"
                  type="range"
                  min="0.15"
                  max="1.0"
                  step="0.05"
                  value={selectedModule.opacity}
                  onChange={(e) =>
                    updateSelectedProp({ opacity: parseFloat(e.target.value) })
                  }
                  className="w-full accent-[#00A8FF]"
                />
              </div>

              {/* Font Size Slider */}
              <div className="space-y-1.5">
                <div className="flex justify-between text-xs">
                  <label htmlFor="hud-font-slider" className="text-[#8B949E]">
                    {isRtl ? 'حجم الخط (Font Size)' : 'Font Size'}
                  </label>
                  <span className="font-mono text-[#E6EDF3]" dir="ltr">
                    {selectedModule.fontSize}px
                  </span>
                </div>
                <input
                  id="hud-font-slider"
                  type="range"
                  min="10"
                  max="20"
                  step="1"
                  value={selectedModule.fontSize}
                  onChange={(e) =>
                    updateSelectedProp({ fontSize: parseInt(e.target.value, 10) })
                  }
                  className="w-full accent-[#00A8FF]"
                />
              </div>

              {/* Text Alignment & Accent Color */}
              <div className="grid grid-cols-2 gap-3 pt-1">
                <div className="space-y-1.5">
                  <span className="block text-xs text-[#8B949E]">
                    {isRtl ? 'محاذاة النص' : 'Text Alignment'}
                  </span>
                  <div className="grid grid-cols-3 gap-1 bg-[#0B0E14] p-1 rounded border border-[#21262D]">
                    {(['left', 'center', 'right'] as const).map((align) => (
                      <button
                        key={align}
                        type="button"
                        onClick={() => updateSelectedProp({ textAlign: align })}
                        className={`py-1 text-[11px] font-mono rounded capitalize ${
                          selectedModule.textAlign === align
                            ? 'bg-[#00A8FF] text-[#0B0E14] font-semibold'
                            : 'text-[#8B949E] hover:text-[#E6EDF3]'
                        }`}
                      >
                        {align}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-1.5">
                  <span className="block text-xs text-[#8B949E]">
                    {isRtl ? 'اللون الأساسي' : 'Accent Color'}
                  </span>
                  <div className="flex items-center gap-1.5 pt-1">
                    {['#00A8FF', '#A371F7', '#3FB950', '#E6EDF3', '#F85149'].map(
                      (hex) => (
                        <button
                          key={hex}
                          type="button"
                          aria-label={`Color ${hex}`}
                          onClick={() => updateSelectedProp({ accentColor: hex })}
                          style={{ backgroundColor: hex }}
                          className={`w-6 h-6 rounded-md border ${
                            selectedModule.accentColor === hex
                              ? 'border-white scale-110'
                              : 'border-transparent opacity-80 hover:opacity-100'
                          }`}
                        />
                      )
                    )}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Quick Toggle List of All HUD Widgets */}
          <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-2.5">
            <div className="flex items-center justify-between">
              <h4 className="text-xs font-semibold text-[#8B949E] flex items-center gap-1.5">
                <Sliders className="w-3.5 h-3.5 text-[#00A8FF]" />
                <span>
                  {isRtl ? 'عناصر الشاشة القابلة للتخصيص' : 'Customizable HUD Widgets'}
                </span>
              </h4>
            </div>
            <div className="max-h-56 overflow-y-auto space-y-1.5 pr-1">
              {hudModules.map((hm) => (
                <div
                  key={hm.id}
                  onClick={() => setSelectedId(hm.id)}
                  className={`flex items-center justify-between px-3 py-2 rounded-md border cursor-pointer transition-colors ${
                    selectedModule?.id === hm.id
                      ? 'bg-[#161B22] border-[#00A8FF]/60'
                      : 'bg-[#0B0E14]/60 border-[#21262D] hover:border-[#8B949E]/40'
                  }`}
                >
                  <div className="truncate">
                    <span className="text-xs font-medium text-[#E6EDF3] block truncate">
                      {isRtl ? hm.nameAr : hm.nameEn}
                    </span>
                    <span className="text-[11px] font-mono text-[#8B949E]" dir="ltr">
                      ({hm.x}, {hm.y}) · {hm.scale.toFixed(1)}x
                    </span>
                  </div>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      const next = modules.map((m) =>
                        m.id === hm.id ? { ...m, enabled: !m.enabled } : m
                      );
                      onUpdateModules(next);
                    }}
                    className={`px-2.5 py-1 rounded text-[11px] font-mono font-semibold shrink-0 ${
                      hm.enabled
                        ? 'bg-[#238636] text-white'
                        : 'bg-[#21262D] text-[#8B949E]'
                    }`}
                  >
                    {hm.enabled ? 'ON' : 'OFF'}
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
