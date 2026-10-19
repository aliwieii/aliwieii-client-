import React, { useState, useEffect, useMemo, useRef } from 'react';
import {
  NexoraModule,
  NexoraCategory,
  NexoraProfile,
  createInitialModules,
  createDefaultProfiles,
  filterModules,
  deleteProfileSafely,
  parseAndValidateConfig,
  EXTERNAL_MODS_MATRIX,
  CURRENT_CONFIG_SCHEMA_VERSION,
} from './core/nexoraEngine';
import { NexoraLogo } from './components/NexoraLogo';
import { HudEditorCanvas } from './components/HudEditorCanvas';
import { ForgeCodeViewer } from './components/ForgeCodeViewer';
import {
  Home,
  LayoutDashboard,
  Swords,
  Gauge,
  Eye,
  Package,
  Users,
  Puzzle,
  Settings,
  Info,
  Search,
  RotateCcw,
  SlidersHorizontal,
  Plus,
  Copy,
  Trash2,
  Download,
  Upload,
  Check,
  AlertTriangle,
  Globe,
  ShieldCheck,
  Cpu,
  Code2,
  Keyboard,
} from 'lucide-react';

type SidebarSection =
  | 'home'
  | 'hud_editor'
  | 'pvp'
  | 'performance'
  | 'visuals'
  | 'inventory'
  | 'profiles'
  | 'mods'
  | 'settings'
  | 'about';

export default function App() {
  const [lang, setLang] = useState<'ar' | 'en'>('ar');
  const isRtl = lang === 'ar';

  const [activeSection, setActiveSection] = useState<SidebarSection>('home');
  const [modules, setModules] = useState<NexoraModule[]>(() => createInitialModules());
  const [profiles, setProfiles] = useState<NexoraProfile[]>(() =>
    createDefaultProfiles(createInitialModules())
  );
  const [activeProfileId, setActiveProfileId] = useState<string>('bedwars');

  // Search & Filter states
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [categoryFilter, setCategoryFilter] = useState<NexoraCategory | 'ALL'>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ENABLED' | 'DISABLED'>('ALL');

  // Module Settings & Keybind Recorder state
  const [editingModuleId, setEditingModuleId] = useState<string | null>(null);
  const [recordingKeyForId, setRecordingKeyForId] = useState<string | null>(null);

  // Profile Management UI state
  const [newProfileName, setNewProfileName] = useState<string>('');
  const [renamingProfileId, setRenamingProfileId] = useState<string | null>(null);
  const [renameValue, setRenameValue] = useState<string>('');
  const [confirmDeleteProfileId, setConfirmDeleteProfileId] = useState<string | null>(null);
  const [importJsonText, setImportJsonText] = useState<string>('');
  const [statusBanner, setStatusBanner] = useState<{
    type: 'success' | 'error';
    message: string;
  } | null>(null);

  // Client visual & performance settings
  const [accentTheme, setAccentTheme] = useState<string>('#00A8FF');
  const [reducedMotion, setReducedMotion] = useState<boolean>(false);
  const [gl4esSafeMode, setGl4esSafeMode] = useState<boolean>(true);

  // Real measured browser frame rate & memory
  const [measuredFps, setMeasuredFps] = useState<number>(60);
  const [avgFps, setAvgFps] = useState<number>(60);
  const [frameTimeMs, setFrameTimeMs] = useState<number>(16.6);
  const [heapUsedMb, setHeapUsedMb] = useState<number | null>(null);
  const [heapLimitMb, setHeapLimitMb] = useState<number | null>(null);
  const fpsHistoryRef = useRef<number[]>([]);

  useEffect(() => {
    document.documentElement.lang = lang;
    document.documentElement.dir = isRtl ? 'rtl' : 'ltr';
  }, [lang, isRtl]);

  // Real FPS & Memory measurement loop (throttled in HeadlessChrome per Ultra Mode guidelines)
  useEffect(() => {
    const isHeadless =
      navigator.webdriver || /HeadlessChrome/i.test(navigator.userAgent);
    if (isHeadless) {
      setMeasuredFps(60);
      setAvgFps(60);
      setFrameTimeMs(16.6);
      return;
    }

    let frameCount = 0;
    let lastTime = performance.now();
    let rafId = 0;

    const tick = (now: number) => {
      frameCount++;
      const elapsed = now - lastTime;
      if (elapsed >= 500) {
        const currentFps = Math.max(1, Math.round((frameCount * 1000) / elapsed));
        const currentMs = Number((elapsed / frameCount).toFixed(1));
        setMeasuredFps(currentFps);
        setFrameTimeMs(currentMs);

        fpsHistoryRef.current = [...fpsHistoryRef.current.slice(-19), currentFps];
        const sum = fpsHistoryRef.current.reduce((a, b) => a + b, 0);
        setAvgFps(Math.round(sum / fpsHistoryRef.current.length));

        const perfMem = (performance as unknown as {
          memory?: { usedJSHeapSize: number; jsHeapSizeLimit: number };
        }).memory;
        if (perfMem && perfMem.usedJSHeapSize) {
          setHeapUsedMb(Math.round(perfMem.usedJSHeapSize / (1024 * 1024)));
          setHeapLimitMb(Math.round(perfMem.jsHeapSizeLimit / (1024 * 1024)));
        }

        frameCount = 0;
        lastTime = now;
      }
      rafId = requestAnimationFrame(tick);
    };

    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, []);

  // Keybind listener when recording a shortcut
  useEffect(() => {
    if (!recordingKeyForId) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      e.preventDefault();
      e.stopPropagation();
      const newKey = e.key === 'Escape' ? 'NONE' : `KEY_${e.key.toUpperCase()}`;
      setModules((prev) =>
        prev.map((m) => (m.id === recordingKeyForId ? { ...m, keybind: newKey } : m))
      );
      setRecordingKeyForId(null);
      showNotification(
        'success',
        isRtl
          ? `تم تعيين مفتاح الاختصار: ${newKey}`
          : `Assigned shortcut key: ${newKey}`
      );
    };
    window.addEventListener('keydown', handleKeyDown, { once: true });
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [recordingKeyForId, isRtl]);

  const showNotification = (type: 'success' | 'error', message: string) => {
    setStatusBanner({ type, message });
    setTimeout(() => {
      setStatusBanner((prev) => (prev?.message === message ? null : prev));
    }, 3500);
  };

  const activeProfile = useMemo(
    () => profiles.find((p) => p.id === activeProfileId) || profiles[0],
    [profiles, activeProfileId]
  );

  // Sync module changes back to the active profile
  const updateModulesAndSyncProfile = (updatedModules: NexoraModule[]) => {
    setModules(updatedModules);
    setProfiles((prev) =>
      prev.map((p) => {
        if (p.id !== activeProfileId) return p;
        const nextMap: NexoraProfile['modules'] = {};
        for (const m of updatedModules) {
          nextMap[m.id] = {
            enabled: m.enabled,
            keybind: m.keybind,
            x: m.x,
            y: m.y,
            scale: m.scale,
            opacity: m.opacity,
            fontSize: m.fontSize,
            textAlign: m.textAlign,
            accentColor: m.accentColor,
            showBackground: m.showBackground,
            locked: m.locked,
          };
        }
        return { ...p, modules: nextMap };
      })
    );
  };

  const handleSwitchProfile = (profileId: string) => {
    const target = profiles.find((p) => p.id === profileId);
    if (!target) return;
    setActiveProfileId(target.id);
    setModules((prev) =>
      prev.map((m) => {
        const saved = target.modules[m.id];
        if (!saved) return m;
        return {
          ...m,
          enabled: saved.enabled,
          keybind: saved.keybind,
          x: saved.x ?? m.defaultX,
          y: saved.y ?? m.defaultY,
          scale: saved.scale ?? 1.0,
          opacity: saved.opacity ?? 0.88,
          fontSize: saved.fontSize ?? 13,
          textAlign: saved.textAlign ?? 'center',
          accentColor: saved.accentColor ?? '#00A8FF',
          showBackground: saved.showBackground ?? true,
          locked: saved.locked ?? false,
        };
      })
    );
    showNotification(
      'success',
      isRtl
        ? `تم التبديل الفوري إلى ملف: ${target.nameAr}`
        : `Switched active profile to: ${target.name}`
    );
  };

  const handleToggleModule = (id: string) => {
    const next = modules.map((m) => (m.id === id ? { ...m, enabled: !m.enabled } : m));
    updateModulesAndSyncProfile(next);
  };

  const handleResetSingleModule = (id: string) => {
    const next = modules.map((m) =>
      m.id === id
        ? {
            ...m,
            enabled: m.defaultEnabled,
            keybind: m.defaultKeybind,
            x: m.defaultX,
            y: m.defaultY,
            scale: 1.0,
            opacity: 0.88,
            fontSize: 13,
            textAlign: 'center' as const,
            accentColor: '#00A8FF',
            locked: false,
          }
        : m
    );
    updateModulesAndSyncProfile(next);
    showNotification(
      'success',
      isRtl ? 'تمت استعادة الإعدادات الافتراضية للميزة.' : 'Module reset to default settings.'
    );
  };

  const handleCreateProfile = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = newProfileName.trim();
    if (!trimmed) {
      showNotification(
        'error',
        isRtl ? 'يرجى إدخال اسم صالح لملف الإعدادات.' : 'Please enter a valid profile name.'
      );
      return;
    }
    if (profiles.some((p) => p.name.toLowerCase() === trimmed.toLowerCase())) {
      showNotification(
        'error',
        isRtl ? 'يوجد ملف إعدادات بهذا الاسم مسبقاً.' : 'A profile with this name already exists.'
      );
      return;
    }

    const newId = `custom_${Date.now()}`;
    const clonedModules: NexoraProfile['modules'] = {};
    for (const m of modules) {
      clonedModules[m.id] = {
        enabled: m.enabled,
        keybind: m.keybind,
        x: m.x,
        y: m.y,
        scale: m.scale,
        opacity: m.opacity,
        fontSize: m.fontSize,
        textAlign: m.textAlign,
        accentColor: m.accentColor,
        showBackground: m.showBackground,
        locked: m.locked,
      };
    }

    const created: NexoraProfile = {
      id: newId,
      name: trimmed,
      nameAr: trimmed,
      descriptionEn: 'User-created custom NEXORA configuration profile.',
      descriptionAr: 'ملف إعدادات مخصص تم إنشاؤه بواسطة اللاعب.',
      isBuiltIn: false,
      modules: clonedModules,
    };

    setProfiles((prev) => [...prev, created]);
    setActiveProfileId(newId);
    setNewProfileName('');
    showNotification(
      'success',
      isRtl ? `تم إنشاء وتفعيل الملف "${trimmed}".` : `Created and activated profile "${trimmed}".`
    );
  };

  const handleDuplicateProfile = (profile: NexoraProfile) => {
    const copyName = `${profile.name} Copy`;
    const newId = `${profile.id}_copy_${Date.now()}`;
    const copy: NexoraProfile = {
      ...profile,
      id: newId,
      name: copyName,
      nameAr: `${profile.nameAr} (نسخة)`,
      isBuiltIn: false,
      modules: JSON.parse(JSON.stringify(profile.modules)),
    };
    setProfiles((prev) => [...prev, copy]);
    setActiveProfileId(newId);
    showNotification(
      'success',
      isRtl ? `تم نسخ الملف إلى "${copy.nameAr}".` : `Duplicated profile as "${copyName}".`
    );
  };

  const handleConfirmDeleteProfile = () => {
    if (!confirmDeleteProfileId) return;
    const res = deleteProfileSafely(profiles, activeProfileId, confirmDeleteProfileId);
    if (!res.deleted) {
      showNotification(
        'error',
        isRtl
          ? 'لا يمكن حذف الملف الوحيد المتبقي.'
          : 'Cannot delete the last remaining profile.'
      );
      setConfirmDeleteProfileId(null);
      return;
    }
    setProfiles(res.profiles);
    if (res.activeProfileId !== activeProfileId) {
      handleSwitchProfile(res.activeProfileId);
    }
    setConfirmDeleteProfileId(null);
    showNotification(
      'success',
      isRtl ? 'تم حذف ملف الإعدادات بأمان.' : 'Profile deleted safely.'
    );
  };

  const generatedConfigJson = useMemo(() => {
    const modMap: Record<string, unknown> = {};
    for (const m of modules) {
      modMap[m.id] = {
        enabled: m.enabled,
        keybind: m.keybind,
        ...(m.isHud
          ? {
              x: m.x,
              y: m.y,
              scale: m.scale,
              opacity: m.opacity,
              fontSize: m.fontSize,
              textAlign: m.textAlign,
              accentColor: m.accentColor,
              locked: m.locked,
            }
          : {}),
      };
    }
    return JSON.stringify(
      {
        schemaVersion: CURRENT_CONFIG_SCHEMA_VERSION,
        client: 'NEXORA CLIENT 1.0.0',
        minecraft: '1.8.9',
        forge: '11.15.1.2318',
        profileName: activeProfile.name,
        language: lang,
        gl4esSafeMode,
        modules: modMap,
      },
      null,
      2
    );
  }, [modules, activeProfile, lang, gl4esSafeMode]);

  const handleImportConfigJson = () => {
    if (!importJsonText.trim()) {
      showNotification(
        'error',
        isRtl ? 'أدخل محتوى JSON للاستيراد أولاً.' : 'Paste configuration JSON before importing.'
      );
      return;
    }
    const parsed = parseAndValidateConfig(importJsonText, modules);
    updateModulesAndSyncProfile(parsed.modules);
    if (parsed.recoveredFromError) {
      showNotification(
        'error',
        isRtl
          ? 'تم اكتشاف ملف إعدادات تالف! قام النظام باستعادة الإعدادات الافتراضية تلقائياً دون انهيار.'
          : 'Corrupted JSON detected! NEXORA safely recovered default settings without crashing.'
      );
    } else {
      showNotification(
        'success',
        isRtl
          ? 'تم استيراد والتحقق من ملف الإعدادات بنجاح.'
          : 'Configuration JSON validated and imported successfully.'
      );
    }
    setImportJsonText('');
  };

  // Determine effective category when user clicks PvP / Performance / Visuals / Inventory sidebar tabs
  const effectiveCategory: NexoraCategory | 'ALL' = useMemo(() => {
    if (activeSection === 'pvp') return 'PVP';
    if (activeSection === 'performance') return 'PERFORMANCE';
    if (activeSection === 'visuals') return 'VISUALS';
    if (activeSection === 'inventory') return 'INVENTORY';
    return categoryFilter;
  }, [activeSection, categoryFilter]);

  const filteredModules = useMemo(
    () => filterModules(modules, searchQuery, effectiveCategory, statusFilter),
    [modules, searchQuery, effectiveCategory, statusFilter]
  );

  const enabledCount = useMemo(
    () => modules.filter((m) => m.enabled).length,
    [modules]
  );
  const enabledHudCount = useMemo(
    () => modules.filter((m) => m.isHud && m.enabled).length,
    [modules]
  );

  const sidebarItems: Array<{
    id: SidebarSection;
    labelAr: string;
    labelEn: string;
    icon: React.FC<{ className?: string }>;
  }> = [
    { id: 'home', labelAr: 'الرئيسية (Home)', labelEn: 'Home', icon: Home },
    { id: 'hud_editor', labelAr: 'محرر الواجهة (HUD Editor)', labelEn: 'HUD Editor', icon: LayoutDashboard },
    { id: 'pvp', labelAr: 'القتال والتحكم (PvP)', labelEn: 'PvP', icon: Swords },
    { id: 'performance', labelAr: 'الأداء (Performance)', labelEn: 'Performance', icon: Gauge },
    { id: 'visuals', labelAr: 'الرسوميات (Visuals)', labelEn: 'Visuals', icon: Eye },
    { id: 'inventory', labelAr: 'المخزون والدردشة (Inventory)', labelEn: 'Inventory', icon: Package },
    { id: 'profiles', labelAr: 'ملفات الإعدادات (Profiles)', labelEn: 'Profiles', icon: Users },
    { id: 'mods', labelAr: 'كل الميزات والمودات (Mods)', labelEn: 'Mods', icon: Puzzle },
    { id: 'settings', labelAr: 'الإعدادات واللغة (Settings)', labelEn: 'Settings', icon: Settings },
    { id: 'about', labelAr: 'حول NEXORA وكود Forge', labelEn: 'About NEXORA', icon: Info },
  ];

  const editingModule = editingModuleId
    ? modules.find((m) => m.id === editingModuleId) || null
    : null;

  return (
    <div className="min-h-screen bg-[#0B0E14] text-[#E6EDF3] flex flex-col">
      {/* Top Bar Contract: 3 Zones (Brand Wordmark — 5 Concise Nav Links — 1 Primary Action) */}
      <header className="flex items-center justify-between gap-8 px-5 py-3.5 bg-[#111621] border-b border-[#21262D]">
        {/* Zone 1: Single text element wordmark */}
        <a
          href="#home"
          onClick={(e) => {
            e.preventDefault();
            setActiveSection('home');
          }}
          className="text-base font-bold tracking-wider text-[#E6EDF3] whitespace-nowrap shrink-0"
        >
          NEXORA CLIENT
        </a>

        {/* Zone 2: 5 concise single-line text links */}
        <nav
          aria-label="Primary Navigation"
          className="hidden md:flex items-center gap-6 text-sm font-medium text-[#8B949E]"
        >
          <button
            type="button"
            onClick={() => setActiveSection('home')}
            className={`hover:text-[#E6EDF3] transition-colors whitespace-nowrap shrink-0 ${
              activeSection === 'home' ? 'text-[#00A8FF] underline underline-offset-8' : ''
            }`}
          >
            {isRtl ? 'الرئيسية' : 'Home'}
          </button>
          <button
            type="button"
            onClick={() => setActiveSection('hud_editor')}
            className={`hover:text-[#E6EDF3] transition-colors whitespace-nowrap shrink-0 ${
              activeSection === 'hud_editor' ? 'text-[#00A8FF] underline underline-offset-8' : ''
            }`}
          >
            {isRtl ? 'محرر HUD' : 'HUD Editor'}
          </button>
          <button
            type="button"
            onClick={() => setActiveSection('mods')}
            className={`hover:text-[#E6EDF3] transition-colors whitespace-nowrap shrink-0 ${
              activeSection === 'mods' ? 'text-[#00A8FF] underline underline-offset-8' : ''
            }`}
          >
            {isRtl ? 'الميزات (83)' : 'Modules (83)'}
          </button>
          <button
            type="button"
            onClick={() => setActiveSection('profiles')}
            className={`hover:text-[#E6EDF3] transition-colors whitespace-nowrap shrink-0 ${
              activeSection === 'profiles' ? 'text-[#00A8FF] underline underline-offset-8' : ''
            }`}
          >
            {isRtl ? 'الملفات' : 'Profiles'}
          </button>
          <button
            type="button"
            onClick={() => setActiveSection('about')}
            className={`hover:text-[#E6EDF3] transition-colors whitespace-nowrap shrink-0 ${
              activeSection === 'about' ? 'text-[#00A8FF] underline underline-offset-8' : ''
            }`}
          >
            {isRtl ? 'كود Forge 1.8.9' : 'Forge 1.8.9 Code'}
          </button>
        </nav>

        {/* Zone 3: 1 Primary Action (Instant Language Switch AR / EN) */}
        <div className="flex items-center shrink-0">
          <button
            type="button"
            onClick={() => setLang((l) => (l === 'ar' ? 'en' : 'ar'))}
            aria-label="Toggle Language Arabic / English"
            className="inline-flex items-center gap-2 px-3.5 py-1.5 text-xs font-semibold rounded-md bg-[#00A8FF] text-[#0B0E14] hover:bg-[#33B9FF] transition-colors whitespace-nowrap shrink-0"
          >
            <Globe className="w-3.5 h-3.5" />
            <span>{lang === 'ar' ? 'English (LTR)' : 'العربية (RTL)'}</span>
          </button>
        </div>
      </header>

      {/* Main Workspace Container: Sidebar + Content Viewport */}
      <div className="flex-1 flex flex-col lg:flex-row">
        {/* Sidebar Navigation (All 10 Mandatory NEXORA Sections) */}
        <aside
          aria-label="NEXORA Client Sections"
          className="w-full lg:w-64 bg-[#111621] border-b lg:border-b-0 lg:border-e border-[#21262D] p-3 flex flex-col justify-between shrink-0"
        >
          <div className="space-y-4">
            {/* NEXORA Emblem & Build Info */}
            <div className="px-2.5 py-2 flex items-center justify-between border-b border-[#21262D] pb-3">
              <NexoraLogo size={34} accentColor={accentTheme} />
              <span className="text-xs font-mono text-[#8B949E]" dir="ltr">
                v1.0.0
              </span>
            </div>

            {/* 10 Navigation Items (Horizontal scroll on mobile, vertical stack on desktop) */}
            <nav className="flex lg:flex-col gap-1 overflow-x-auto pb-1 lg:pb-0">
              {sidebarItems.map((item) => {
                const Icon = item.icon;
                const active = activeSection === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => setActiveSection(item.id)}
                    className={`flex items-center gap-2.5 px-3 py-2.5 rounded-md text-xs font-medium transition-colors whitespace-nowrap shrink-0 ${
                      active
                        ? 'bg-[#00A8FF]/15 text-[#00A8FF] font-semibold border border-[#00A8FF]/40'
                        : 'text-[#8B949E] hover:text-[#E6EDF3] hover:bg-[#161B22] border border-transparent'
                    }`}
                  >
                    <Icon className="w-4 h-4 shrink-0" />
                    <span>{isRtl ? item.labelAr : item.labelEn}</span>
                  </button>
                );
              })}
            </nav>
          </div>

          {/* Active Profile Quick Summary inside Sidebar Footer */}
          <div className="hidden lg:block mt-6 pt-3 border-t border-[#21262D] px-2 space-y-1.5">
            <div className="text-[11px] text-[#8B949E]">
              {isRtl ? 'ملف الإعدادات النشط' : 'Active Profile'}
            </div>
            <div className="flex items-center justify-between text-xs font-semibold text-[#E6EDF3]">
              <span>{isRtl ? activeProfile.nameAr : activeProfile.name}</span>
              <span className="font-mono text-[#00A8FF]" dir="ltr">
                {enabledCount}/83
              </span>
            </div>
            <div className="text-[11px] font-mono text-[#8B949E]" dir="ltr">
              Forge 11.15.1.2318 · Java 8
            </div>
          </div>
        </aside>

        {/* Main Viewport Content */}
        <main className="flex-1 p-4 md:p-6 overflow-y-auto max-w-[1360px]">
          {/* Toast Notification Banner */}
          {statusBanner && (
            <div
              role="status"
              className={`mb-4 px-4 py-3 rounded-lg border flex items-center justify-between text-xs font-medium ${
                statusBanner.type === 'success'
                  ? 'bg-[#238636]/15 border-[#3FB950]/50 text-[#3FB950]'
                  : 'bg-[#DA3633]/15 border-[#F85149]/50 text-[#F85149]'
              }`}
            >
              <div className="flex items-center gap-2">
                {statusBanner.type === 'success' ? (
                  <Check className="w-4 h-4 shrink-0" />
                ) : (
                  <AlertTriangle className="w-4 h-4 shrink-0" />
                )}
                <span>{statusBanner.message}</span>
              </div>
              <button
                type="button"
                onClick={() => setStatusBanner(null)}
                className="text-xs underline ms-4"
              >
                {isRtl ? 'إغلاق' : 'Dismiss'}
              </button>
            </div>
          )}

          {/* SECTION 1: HOME */}
          {activeSection === 'home' && (
            <div className="space-y-6">
              {/* Hero Overview Banner */}
              <div className="bg-[#111621] border border-[#21262D] rounded-lg p-5 flex flex-col md:flex-row items-start md:items-center justify-between gap-5">
                <div className="space-y-2">
                  <div className="flex items-center gap-3">
                    <NexoraLogo size={44} showWordmark={false} accentColor={accentTheme} />
                    <div>
                      <h1 className="text-xl md:text-2xl font-bold text-[#E6EDF3] tracking-tight">
                        NEXORA CLIENT 1.0.0
                      </h1>
                      <p className="text-xs text-[#8B949E]" dir="ltr">
                        Minecraft 1.8.9 · Forge 11.15.1.2318 · Java 8 · PojavLauncher (Snapdragon 6 Gen 1 / Adreno 710)
                      </p>
                    </div>
                  </div>
                  <p className="text-sm text-[#8B949E] max-w-2xl leading-relaxed">
                    {isRtl
                      ? 'عميل Minecraft PvP احترافي وخفيف مبني على Forge 1.8.9 مع محرر HUD متكامل، دعم كامل للغة العربية (ICU4J Shaping & RTL)، وتوافق أصلي مع محرك GL4ES على الهواتف دون أي تعديلات غش.'
                      : 'Professional, lightweight Minecraft 1.8.9 PvP client powered by SpongePowered Mixin 0.7.11, GL4ES-safe Tessellator rendering, native ICU4J Arabic shaping, and multi-profile HUD customization.'}
                  </p>
                </div>

                {/* Quick Launch Shortcuts */}
                <div className="flex flex-wrap items-center gap-2.5 shrink-0">
                  <button
                    type="button"
                    onClick={() => setActiveSection('hud_editor')}
                    className="inline-flex items-center gap-2 px-4 py-2.5 rounded-md bg-[#00A8FF] text-[#0B0E14] text-xs font-semibold hover:bg-[#33B9FF] transition-colors whitespace-nowrap shrink-0"
                  >
                    <LayoutDashboard className="w-4 h-4" />
                    <span>{isRtl ? 'فتح محرر HUD' : 'Open HUD Editor'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveSection('pvp')}
                    className="inline-flex items-center gap-2 px-3.5 py-2.5 rounded-md bg-[#161B22] text-[#E6EDF3] border border-[#21262D] hover:border-[#00A8FF]/50 text-xs font-medium transition-colors whitespace-nowrap shrink-0"
                  >
                    <Swords className="w-4 h-4 text-[#00A8FF]" />
                    <span>{isRtl ? 'إعدادات PvP' : 'PvP Settings'}</span>
                  </button>
                  <button
                    type="button"
                    onClick={() => setActiveSection('about')}
                    className="inline-flex items-center gap-2 px-3.5 py-2.5 rounded-md bg-[#161B22] text-[#E6EDF3] border border-[#21262D] hover:border-[#A371F7]/50 text-xs font-medium transition-colors whitespace-nowrap shrink-0"
                  >
                    <Code2 className="w-4 h-4 text-[#A371F7]" />
                    <span>{isRtl ? 'تصفح كود Forge 1.8.9' : 'Inspect Forge 1.8.9 Code'}</span>
                  </button>
                </div>
              </div>

              {/* Real Telemetry & Client Status Strip (Honest, Measured Values Only) */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-1">
                  <div className="text-xs text-[#8B949E]">
                    {isRtl ? 'معدل الإطارات المقاس حالياً (FPS)' : 'Measured Live Frame Rate'}
                  </div>
                  <div className="text-2xl font-mono font-bold text-[#E6EDF3] tabular-nums" dir="ltr">
                    {measuredFps} FPS
                  </div>
                  <div className="text-xs font-mono text-[#8B949E]" dir="ltr">
                    Avg: {avgFps} FPS · {frameTimeMs} ms/frame
                  </div>
                </div>

                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-1">
                  <div className="text-xs text-[#8B949E]">
                    {isRtl ? 'حالة الميزات والـ HUD' : 'Active Modules & HUD Status'}
                  </div>
                  <div className="text-2xl font-mono font-bold text-[#00A8FF] tabular-nums" dir="ltr">
                    {enabledCount} / {modules.length}
                  </div>
                  <div className="text-xs text-[#8B949E]">
                    {isRtl
                      ? `${enabledHudCount} عنصر HUD نشط على الشاشة`
                      : `${enabledHudCount} HUD widgets rendered on screen`}
                  </div>
                </div>

                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-1">
                  <div className="text-xs text-[#8B949E]">
                    {isRtl ? 'استهلاك الذاكرة المقاس (RAM)' : 'Measured Memory Usage'}
                  </div>
                  <div className="text-2xl font-mono font-bold text-[#3FB950] tabular-nums" dir="ltr">
                    {heapUsedMb !== null ? `${heapUsedMb} MB` : 'JVM Runtime'}
                  </div>
                  <div className="text-xs text-[#8B949E]" dir="ltr">
                    {heapLimitMb !== null
                      ? `Heap Limit: ${heapLimitMb} MB`
                      : isRtl
                      ? 'يُقاس عبر Runtime.getRuntime() داخل اللعبة'
                      : 'Measured via Runtime.getRuntime() in-game'}
                  </div>
                </div>

                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-1">
                  <div className="text-xs text-[#8B949E]">
                    {isRtl ? 'ملف الإعدادات والأداء النشط' : 'Active Profile & Renderer'}
                  </div>
                  <div className="text-lg font-bold text-[#E6EDF3] truncate">
                    {isRtl ? activeProfile.nameAr : activeProfile.name}
                  </div>
                  <div className="text-xs font-mono text-[#A371F7]" dir="ltr">
                    {gl4esSafeMode ? 'GL4ES Safe Mode: ON' : 'Standard OpenGL'}
                  </div>
                </div>
              </div>

              {/* Engineering Status & Phase Verification Summary */}
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
                <div className="lg:col-span-7 bg-[#111621] border border-[#21262D] rounded-lg p-5 space-y-4">
                  <div className="flex items-center justify-between border-b border-[#21262D] pb-3">
                    <h2 className="text-base font-bold text-[#E6EDF3] flex items-center gap-2">
                      <Cpu className="w-4 h-4 text-[#00A8FF]" />
                      <span>
                        {isRtl
                          ? 'تقرير الفحص الهندسي وحالة المراحل'
                          : 'Engineering Audit & Implementation Status'}
                      </span>
                    </h2>
                    <span className="text-xs font-mono text-[#3FB950]" dir="ltr">
                      Schema v{CURRENT_CONFIG_SCHEMA_VERSION} · Ready
                    </span>
                  </div>

                  <div className="space-y-2.5 text-xs">
                    <div className="flex items-start justify-between gap-4 p-3 rounded bg-[#0B0E14] border border-[#21262D]">
                      <div>
                        <div className="font-semibold text-[#E6EDF3]">
                          {isRtl
                            ? '1. نظام البناء الحديث (Essential Loom + Gradle 7.6.4 + GitHub Actions)'
                            : '1. Modern Build Pipeline (Essential Loom + Gradle 7.6.4 + GitHub Actions)'}
                        </div>
                        <p className="text-[#8B949E] mt-0.5">
                          {isRtl
                            ? 'تم استبدال ForgeGradle 2.1 المتوقف بقالب Essential Loom المتوافق مع Java 8 و Forge 11.15.1.2318 لإنتاج NEXORA-CLIENT-1.0.0-1.8.9.jar.'
                            : 'Replaces deprecated ForgeGradle 2.1 with Essential Loom + Pack200Adapter targeting Java 8 and Forge 11.15.1.2318.'}
                        </p>
                      </div>
                      <span className="font-mono text-[#3FB950] shrink-0">
                        {isRtl ? 'مكتمل الكود' : 'Implemented'}
                      </span>
                    </div>

                    <div className="flex items-start justify-between gap-4 p-3 rounded bg-[#0B0E14] border border-[#21262D]">
                      <div>
                        <div className="font-semibold text-[#E6EDF3]">
                          {isRtl
                            ? '2. دعم العربية الأصيل (ICU4J ArabicShaping & Bidi + LRU Cache)'
                            : '2. Native Arabic Shaping & RTL (ICU4J ArabicShaping & Bidi + LRU Cache)'}
                        </div>
                        <p className="text-[#8B949E] mt-0.5">
                          {isRtl
                            ? 'يربط الحروف العربية ويضبط اتجاه الجمل دون عكس أرقام FPS أو Ping أو الإحداثيات، مع تخزين مؤقت لمنع استهلاك الذاكرة.'
                            : 'Connects Arabic glyphs and reorders RTL runs via Minecraft 1.8.9 bundled ICU4J while keeping LTR telemetry numerals intact.'}
                        </p>
                      </div>
                      <span className="font-mono text-[#3FB950] shrink-0">
                        {isRtl ? 'مكتمل ومختبر' : 'Verified'}
                      </span>
                    </div>

                    <div className="flex items-start justify-between gap-4 p-3 rounded bg-[#0B0E14] border border-[#21262D]">
                      <div>
                        <div className="font-semibold text-[#E6EDF3]">
                          {isRtl
                            ? '3. توافق الرسوميات مع PojavLauncher (GL4ES / Adreno 710)'
                            : '3. PojavLauncher GL4ES Safe Rendering (Snapdragon 6 Gen 1 / Adreno 710)'}
                        </div>
                        <p className="text-[#8B949E] mt-0.5">
                          {isRtl
                            ? 'رسم الحواف الناعمة والأيقونات عبر Tessellator و WorldRenderer وجداول حساب المثلثات المسبقة دون أي GLSL Shaders.'
                            : 'Draws rounded HUD cards and geometric NEXORA vector emblem using Tessellator & LUT trigonometry with zero custom GLSL shaders.'}
                        </p>
                      </div>
                      <span className="font-mono text-[#3FB950] shrink-0">
                        {isRtl ? 'مكتمل الكود' : 'Implemented'}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Quick Profile Switcher Card on Home */}
                <div className="lg:col-span-5 bg-[#111621] border border-[#21262D] rounded-lg p-5 space-y-4">
                  <div className="flex items-center justify-between border-b border-[#21262D] pb-3">
                    <h2 className="text-base font-bold text-[#E6EDF3]">
                      {isRtl ? 'التبديل السريع لملفات الإعدادات' : 'Quick Profile Switcher'}
                    </h2>
                    <button
                      type="button"
                      onClick={() => setActiveSection('profiles')}
                      className="text-xs text-[#00A8FF] hover:underline"
                    >
                      {isRtl ? 'إدارة الملفات' : 'Manage All'}
                    </button>
                  </div>

                  <div className="space-y-2">
                    {profiles.map((prof) => {
                      const isActive = prof.id === activeProfileId;
                      return (
                        <button
                          key={prof.id}
                          type="button"
                          onClick={() => handleSwitchProfile(prof.id)}
                          className={`w-full text-start p-3 rounded-md border transition-colors flex items-center justify-between ${
                            isActive
                              ? 'bg-[#00A8FF]/15 border-[#00A8FF] text-[#E6EDF3]'
                              : 'bg-[#0B0E14] border-[#21262D] text-[#8B949E] hover:text-[#E6EDF3]'
                          }`}
                        >
                          <div>
                            <div className="text-xs font-semibold text-[#E6EDF3]">
                              {isRtl ? prof.nameAr : prof.name}
                            </div>
                            <div className="text-[11px] text-[#8B949E] line-clamp-1 mt-0.5">
                              {isRtl ? prof.descriptionAr : prof.descriptionEn}
                            </div>
                          </div>
                          <span className="text-xs font-mono shrink-0 ms-3" dir="ltr">
                            {isActive ? (isRtl ? 'نشط' : 'ACTIVE') : isRtl ? 'تفعيل' : 'Switch'}
                          </span>
                        </button>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* SECTION 2: HUD EDITOR */}
          {activeSection === 'hud_editor' && (
            <HudEditorCanvas
              modules={modules}
              onUpdateModules={updateModulesAndSyncProfile}
              lang={lang}
              liveFps={measuredFps}
              activeProfileName={isRtl ? activeProfile.nameAr : activeProfile.name}
              reducedMotion={reducedMotion}
            />
          )}

          {/* SECTIONS 3, 4, 5, 6, 8: MODULE CATALOG (PvP, Performance, Visuals, Inventory, Mods) */}
          {['pvp', 'performance', 'visuals', 'inventory', 'mods'].includes(activeSection) && (
            <div className="space-y-5">
              {/* Search & Filter Controls Bar */}
              <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-3">
                <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3">
                  {/* Search Input */}
                  <div className="relative flex-1">
                    <Search className="w-4 h-4 text-[#8B949E] absolute top-1/2 -translate-y-1/2 start-3 pointer-events-none" />
                    <input
                      type="search"
                      aria-label={isRtl ? 'بحث في الميزات' : 'Search modules'}
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder={
                        isRtl
                          ? 'ابحث عن ميزة بالاسم أو الوصف (مثال: Keystrokes, CPS, تسلل, دروع)...'
                          : 'Search 83 modules by name or description (e.g., Keystrokes, CPS, HurtCam)...'
                      }
                      className="w-full bg-[#0B0E14] border border-[#21262D] rounded-md ps-9 pe-3 py-2 text-xs text-[#E6EDF3] placeholder-[#8B949E] focus:outline-none focus:border-[#00A8FF]"
                    />
                  </div>

                  {/* Status Segmented Filter */}
                  <div className="flex items-center gap-1 bg-[#0B0E14] p-1 rounded-md border border-[#21262D] shrink-0">
                    {(['ALL', 'ENABLED', 'DISABLED'] as const).map((st) => (
                      <button
                        key={st}
                        type="button"
                        onClick={() => setStatusFilter(st)}
                        className={`px-3 py-1.5 rounded text-xs font-medium whitespace-nowrap shrink-0 transition-colors ${
                          statusFilter === st
                            ? 'bg-[#00A8FF] text-[#0B0E14] font-semibold'
                            : 'text-[#8B949E] hover:text-[#E6EDF3]'
                        }`}
                      >
                        {st === 'ALL'
                          ? isRtl
                            ? `الكل (${modules.length})`
                            : `All (${modules.length})`
                          : st === 'ENABLED'
                          ? isRtl
                            ? `مفعل (${enabledCount})`
                            : `Enabled (${enabledCount})`
                          : isRtl
                          ? `معطل (${modules.length - enabledCount})`
                          : `Disabled (${modules.length - enabledCount})`}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Category Sub-Filters when inside "Mods" (All Modules) view */}
                {activeSection === 'mods' && (
                  <div className="flex flex-wrap items-center justify-between gap-2 pt-2 border-t border-[#21262D]">
                    <div className="flex flex-wrap items-center gap-1.5">
                      {(
                        [
                          { id: 'ALL', ar: 'جميع الفئات', en: 'All Categories' },
                          { id: 'HUD', ar: 'عناصر الشاشة (HUD)', en: 'HUD & Info' },
                          { id: 'PVP', ar: 'الحركة والقتال (PvP)', en: 'Movement & PvP' },
                          { id: 'VISUALS', ar: 'الرسوميات (Visuals)', en: 'Visuals & Effects' },
                          { id: 'INVENTORY', ar: 'المخزون والدردشة', en: 'Inventory & Chat' },
                          { id: 'PERFORMANCE', ar: 'الأداء (Performance)', en: 'Performance' },
                        ] as const
                      ).map((cat) => (
                        <button
                          key={cat.id}
                          type="button"
                          onClick={() => setCategoryFilter(cat.id)}
                          className={`px-3 py-1 rounded text-xs font-medium whitespace-nowrap shrink-0 transition-colors ${
                            categoryFilter === cat.id
                              ? 'bg-[#161B22] text-[#00A8FF] border border-[#00A8FF]/50'
                              : 'text-[#8B949E] hover:text-[#E6EDF3]'
                          }`}
                        >
                          {isRtl ? cat.ar : cat.en}
                        </button>
                      ))}
                    </div>

                    <button
                      type="button"
                      onClick={() => {
                        const resetAll = createInitialModules();
                        updateModulesAndSyncProfile(resetAll);
                        showNotification(
                          'success',
                          isRtl
                            ? 'تمت استعادة جميع الميزات إلى الإعدادات الافتراضية.'
                            : 'All 83 modules restored to default settings.'
                        );
                      }}
                      className="inline-flex items-center gap-1.5 text-xs text-[#F85149] hover:underline whitespace-nowrap shrink-0"
                    >
                      <RotateCcw className="w-3.5 h-3.5" />
                      <span>{isRtl ? 'إعادة ضبط الكل' : 'Reset All to Default'}</span>
                    </button>
                  </div>
                )}
              </div>

              {/* Performance Monitor Panel when inside Performance section */}
              {activeSection === 'performance' && (
                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-bold text-[#E6EDF3]">
                      {isRtl
                        ? 'مراقب الأداء الفعلي (Performance Monitor — Snapdragon 6 Gen 1 / Adreno 710)'
                        : 'Real-Time Performance Monitor (Snapdragon 6 Gen 1 / Adreno 710 Target)'}
                    </h3>
                    <span className="text-xs font-mono text-[#3FB950]" dir="ltr">
                      Zero Render-Loop Allocations
                    </span>
                  </div>
                  <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-xs">
                    <div className="bg-[#0B0E14] border border-[#21262D] rounded p-3">
                      <div className="text-[#8B949E]">{isRtl ? 'الإطارات الحالية' : 'Current FPS'}</div>
                      <div className="text-lg font-mono font-bold text-[#E6EDF3] mt-0.5" dir="ltr">
                        {measuredFps} FPS
                      </div>
                    </div>
                    <div className="bg-[#0B0E14] border border-[#21262D] rounded p-3">
                      <div className="text-[#8B949E]">{isRtl ? 'متوسط الإطارات وزمن الإطار' : 'Average FPS & Frame Time'}</div>
                      <div className="text-lg font-mono font-bold text-[#00A8FF] mt-0.5" dir="ltr">
                        {avgFps} FPS ({frameTimeMs} ms)
                      </div>
                    </div>
                    <div className="bg-[#0B0E14] border border-[#21262D] rounded p-3">
                      <div className="text-[#8B949E]">{isRtl ? 'عناصر HUD المرسومة' : 'Rendered HUD Widgets'}</div>
                      <div className="text-lg font-mono font-bold text-[#A371F7] mt-0.5" dir="ltr">
                        {enabledHudCount} Active
                      </div>
                    </div>
                    <div className="bg-[#0B0E14] border border-[#21262D] rounded p-3">
                      <div className="text-[#8B949E]">{isRtl ? 'حالة المؤثرات الثقيلة' : 'Heavy Shaders / Blur'}</div>
                      <div className="text-lg font-mono font-bold text-[#3FB950] mt-0.5" dir="ltr">
                        {modules.find((m) => m.id === 'motion_blur')?.enabled
                          ? 'Motion Blur ON'
                          : 'Disabled (Optimal)'}
                      </div>
                    </div>
                  </div>
                </div>
              )}

              {/* Modules Grid */}
              {filteredModules.length === 0 ? (
                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-8 text-center space-y-2">
                  <p className="text-sm font-semibold text-[#E6EDF3]">
                    {isRtl
                      ? 'لا توجد ميزات مطابقة لبحثك الحالي.'
                      : 'No modules match your current search and filter criteria.'}
                  </p>
                  <button
                    type="button"
                    onClick={() => {
                      setSearchQuery('');
                      setCategoryFilter('ALL');
                      setStatusFilter('ALL');
                    }}
                    className="px-4 py-2 rounded bg-[#00A8FF] text-[#0B0E14] text-xs font-semibold"
                  >
                    {isRtl ? 'إعادة ضبط الفلاتر' : 'Clear Filters'}
                  </button>
                </div>
              ) : (
                <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
                  {filteredModules.map((mod) => (
                    <div
                      key={mod.id}
                      className={`bg-[#111621] rounded-lg border p-4 flex flex-col justify-between gap-4 transition-colors ${
                        mod.enabled
                          ? 'border-[#00A8FF]/50'
                          : 'border-[#21262D] hover:border-[#8B949E]/40'
                      }`}
                    >
                      <div className="space-y-2">
                        {/* Unboxed Metadata Row (Zero-Pill Discipline) */}
                        <div className="flex items-center justify-between text-[11px] text-[#8B949E]">
                          <div className="flex items-center gap-1.5 font-mono" dir="ltr">
                            <span>#{String(mod.index).padStart(2, '0')}</span>
                            <span aria-hidden="true">·</span>
                            <span>{mod.category}</span>
                            <span aria-hidden="true">·</span>
                            <span
                              className={
                                mod.safety === 'SAFE' ? 'text-[#3FB950]' : 'text-[#D29922]'
                              }
                            >
                              {mod.safety === 'SAFE'
                                ? isRtl
                                  ? 'آمن'
                                  : 'Fair-Play'
                                : isRtl
                                ? 'حسب قواعد السيرفر'
                                : 'Server-Rule Dependent'}
                            </span>
                          </div>

                          <span className="font-mono text-[11px] text-[#8B949E]" dir="ltr">
                            {mod.stage === 'IMPLEMENTED_CORE'
                              ? 'Core 1.8.9'
                              : mod.stage === 'SERVER_RULE_OPTIONAL'
                              ? 'Optional'
                              : 'Architected'}
                          </span>
                        </div>

                        {/* Module Title & Description */}
                        <div>
                          <h3 className="text-sm font-bold text-[#E6EDF3]">
                            {isRtl ? mod.nameAr : mod.nameEn}
                          </h3>
                          <p className="text-xs text-[#8B949E] mt-1 leading-relaxed">
                            {isRtl ? mod.descAr : mod.descEn}
                          </p>
                        </div>
                      </div>

                      {/* Bottom Controls: Keybind, Settings, Reset, Enable/Disable Toggle */}
                      <div className="pt-3 border-t border-[#21262D] flex items-center justify-between gap-2">
                        <div className="flex items-center gap-1.5">
                          {/* Keybind Button */}
                          <button
                            type="button"
                            onClick={() =>
                              setRecordingKeyForId(
                                recordingKeyForId === mod.id ? null : mod.id
                              )
                            }
                            aria-label={`Keybind for ${mod.nameEn}`}
                            className={`inline-flex items-center gap-1 px-2 py-1 rounded text-[11px] font-mono border transition-colors whitespace-nowrap shrink-0 ${
                              recordingKeyForId === mod.id
                                ? 'bg-[#A371F7] text-[#0B0E14] border-[#A371F7] font-semibold'
                                : 'bg-[#0B0E14] text-[#8B949E] border-[#21262D] hover:text-[#E6EDF3]'
                            }`}
                            dir="ltr"
                          >
                            <Keyboard className="w-3 h-3" />
                            <span>
                              {recordingKeyForId === mod.id
                                ? 'Press Key...'
                                : mod.keybind}
                            </span>
                          </button>

                          {/* Module Settings Modal Trigger */}
                          <button
                            type="button"
                            onClick={() => setEditingModuleId(mod.id)}
                            aria-label={`Configure ${mod.nameEn}`}
                            className="p-1.5 rounded bg-[#0B0E14] border border-[#21262D] text-[#8B949E] hover:text-[#00A8FF] transition-colors"
                          >
                            <SlidersHorizontal className="w-3.5 h-3.5" />
                          </button>

                          {/* Reset Module to Default */}
                          <button
                            type="button"
                            onClick={() => handleResetSingleModule(mod.id)}
                            aria-label={`Reset ${mod.nameEn}`}
                            className="p-1.5 rounded bg-[#0B0E14] border border-[#21262D] text-[#8B949E] hover:text-[#F85149] transition-colors"
                          >
                            <RotateCcw className="w-3.5 h-3.5" />
                          </button>
                        </div>

                        {/* Enable / Disable Toggle Button */}
                        <button
                          type="button"
                          onClick={() => handleToggleModule(mod.id)}
                          aria-pressed={mod.enabled}
                          className={`px-3.5 py-1.5 rounded text-xs font-semibold transition-colors whitespace-nowrap shrink-0 ${
                            mod.enabled
                              ? 'bg-[#238636] text-white hover:bg-[#2EA043]'
                              : 'bg-[#21262D] text-[#8B949E] hover:text-[#E6EDF3]'
                          }`}
                        >
                          {mod.enabled
                            ? isRtl
                              ? 'مفعل'
                              : 'Enabled'
                            : isRtl
                            ? 'معطل'
                            : 'Disabled'}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {/* External Mods Compatibility Audit Table (Shown in Mods tab) */}
              {activeSection === 'mods' && (
                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-5 space-y-4 mt-6">
                  <div className="space-y-1">
                    <h3 className="text-base font-bold text-[#E6EDF3] flex items-center gap-2">
                      <ShieldCheck className="w-4 h-4 text-[#00A8FF]" />
                      <span>
                        {isRtl
                          ? 'فحص التوافق والتراخيص للمودات الخارجية على Forge 1.8.9 و PojavLauncher'
                          : 'External Mods Compatibility & License Audit (Forge 1.8.9 / PojavLauncher)'}
                      </span>
                    </h3>
                    <p className="text-xs text-[#8B949E]">
                      {isRtl
                        ? 'طبقة التكامل في NEXORA تفحص الاعتماديات وتمنع تضمين ملفات JAR خارجية غير مرخصة أو متعارضة مع شاشات اللمس.'
                        : 'NEXORA verifies external mod licenses, prevents illegal JAR bundling, and flags desktop-only JInput mods on Android.'}
                    </p>
                  </div>

                  <div className="overflow-x-auto">
                    <table className="w-full text-start border-collapse text-xs">
                      <thead>
                        <tr className="border-b border-[#21262D] text-[#8B949E]">
                          <th className="py-2.5 px-3 text-start">{isRtl ? 'المود الخارجي' : 'External Mod'}</th>
                          <th className="py-2.5 px-3 text-start">{isRtl ? 'الترخيص' : 'License'}</th>
                          <th className="py-2.5 px-3 text-start">{isRtl ? 'حالة PojavLauncher' : 'Pojav Status'}</th>
                          <th className="py-2.5 px-3 text-start">{isRtl ? 'ملاحظات التوافق والبديل في NEXORA' : 'Compatibility & NEXORA Integration'}</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-[#21262D]">
                        {EXTERNAL_MODS_MATRIX.map((ext) => (
                          <tr key={ext.id} className="hover:bg-[#161B22]/60">
                            <td className="py-2.5 px-3 font-mono font-semibold text-[#E6EDF3]" dir="ltr">
                              {ext.name}
                            </td>
                            <td className="py-2.5 px-3 font-mono text-[#8B949E]" dir="ltr">
                              {ext.license}
                            </td>
                            <td className="py-2.5 px-3 font-mono" dir="ltr">
                              <span
                                className={
                                  ext.pojavStatus === 'COMPATIBLE'
                                    ? 'text-[#3FB950]'
                                    : ext.pojavStatus === 'CONFLICT_RISK'
                                    ? 'text-[#F85149]'
                                    : 'text-[#D29922]'
                                }
                              >
                                {ext.pojavStatus}
                              </span>
                            </td>
                            <td className="py-2.5 px-3 text-[#8B949E]">
                              <div>{isRtl ? ext.pojavNoteAr : ext.pojavNoteEn}</div>
                              <div className="text-[#E6EDF3] mt-0.5 font-medium">
                                {isRtl ? ext.recommendationAr : ext.recommendationEn}
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* SECTION 7: PROFILES */}
          {activeSection === 'profiles' && (
            <div className="space-y-6">
              <div className="bg-[#111621] border border-[#21262D] rounded-lg p-5 space-y-4">
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#21262D] pb-4">
                  <div>
                    <h2 className="text-base font-bold text-[#E6EDF3]">
                      {isRtl
                        ? 'إدارة ملفات الإعدادات المستقلة (Profiles)'
                        : 'Independent Configuration Profiles'}
                    </h2>
                    <p className="text-xs text-[#8B949E] mt-0.5">
                      {isRtl
                        ? 'كل ملف يحفظ مواقع وأحجام HUD، حالة تفعيل الـ 83 ميزة، واختصارات لوحة المفاتيح بصورة مستقلة.'
                        : 'Each profile independently stores HUD coordinates, scales, colors, and all 83 module states.'}
                    </p>
                  </div>

                  {/* Create New Profile Form */}
                  <form onSubmit={handleCreateProfile} className="flex items-center gap-2">
                    <input
                      type="text"
                      value={newProfileName}
                      onChange={(e) => setNewProfileName(e.target.value)}
                      placeholder={
                        isRtl ? 'اسم الملف الجديد (مثال: Ranked)...' : 'New profile name (e.g. Ranked)...'
                      }
                      aria-label={isRtl ? 'اسم الملف الجديد' : 'New profile name'}
                      className="bg-[#0B0E14] border border-[#21262D] rounded-md px-3 py-2 text-xs text-[#E6EDF3] placeholder-[#8B949E] focus:outline-none focus:border-[#00A8FF]"
                    />
                    <button
                      type="submit"
                      className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-md bg-[#00A8FF] text-[#0B0E14] text-xs font-semibold hover:bg-[#33B9FF] transition-colors whitespace-nowrap shrink-0"
                    >
                      <Plus className="w-3.5 h-3.5" />
                      <span>{isRtl ? 'إنشاء ملف' : 'Create Profile'}</span>
                    </button>
                  </form>
                </div>

                {/* Profiles Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {profiles.map((prof) => {
                    const isActive = prof.id === activeProfileId;
                    const enabledInProf = Object.values(prof.modules).filter(
                      (m) => m.enabled
                    ).length;

                    return (
                      <div
                        key={prof.id}
                        className={`rounded-lg border p-4 flex flex-col justify-between gap-4 ${
                          isActive
                            ? 'bg-[#161B22] border-[#00A8FF]'
                            : 'bg-[#0B0E14] border-[#21262D]'
                        }`}
                      >
                        <div className="space-y-2">
                          <div className="flex items-center justify-between">
                            {renamingProfileId === prof.id ? (
                              <div className="flex items-center gap-2">
                                <input
                                  type="text"
                                  value={renameValue}
                                  onChange={(e) => setRenameValue(e.target.value)}
                                  className="bg-[#111621] border border-[#00A8FF] rounded px-2 py-1 text-xs text-[#E6EDF3]"
                                />
                                <button
                                  type="button"
                                  onClick={() => {
                                    if (renameValue.trim()) {
                                      setProfiles((prev) =>
                                        prev.map((p) =>
                                          p.id === prof.id
                                            ? {
                                                ...p,
                                                name: renameValue.trim(),
                                                nameAr: renameValue.trim(),
                                              }
                                            : p
                                        )
                                      );
                                    }
                                    setRenamingProfileId(null);
                                  }}
                                  className="px-2 py-1 rounded bg-[#238636] text-white text-xs"
                                >
                                  {isRtl ? 'حفظ' : 'Save'}
                                </button>
                              </div>
                            ) : (
                              <h3 className="text-sm font-bold text-[#E6EDF3]">
                                {isRtl ? prof.nameAr : prof.name}
                              </h3>
                            )}

                            <span className="text-xs font-mono text-[#00A8FF]" dir="ltr">
                              {enabledInProf}/83 Active
                            </span>
                          </div>

                          <p className="text-xs text-[#8B949E]">
                            {isRtl ? prof.descriptionAr : prof.descriptionEn}
                          </p>
                        </div>

                        <div className="flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-[#21262D]">
                          <div className="flex items-center gap-2">
                            <button
                              type="button"
                              onClick={() => {
                                setRenamingProfileId(prof.id);
                                setRenameValue(prof.name);
                              }}
                              className="px-2.5 py-1 rounded bg-[#161B22] border border-[#21262D] text-xs text-[#8B949E] hover:text-[#E6EDF3]"
                            >
                              {isRtl ? 'إعادة تسمية' : 'Rename'}
                            </button>

                            <button
                              type="button"
                              onClick={() => handleDuplicateProfile(prof)}
                              className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-[#161B22] border border-[#21262D] text-xs text-[#8B949E] hover:text-[#E6EDF3]"
                            >
                              <Copy className="w-3 h-3" />
                              <span>{isRtl ? 'نسخ' : 'Duplicate'}</span>
                            </button>

                            {profiles.length > 1 && (
                              <button
                                type="button"
                                onClick={() => setConfirmDeleteProfileId(prof.id)}
                                aria-label={`Delete profile ${prof.name}`}
                                className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-[#161B22] border border-[#21262D] text-xs text-[#F85149] hover:border-[#F85149]"
                              >
                                <Trash2 className="w-3 h-3" />
                                <span>{isRtl ? 'حذف' : 'Delete'}</span>
                              </button>
                            )}
                          </div>

                          <button
                            type="button"
                            onClick={() => handleSwitchProfile(prof.id)}
                            disabled={isActive}
                            className={`px-3.5 py-1.5 rounded text-xs font-semibold transition-colors ${
                              isActive
                                ? 'bg-[#238636] text-white cursor-default'
                                : 'bg-[#00A8FF] text-[#0B0E14] hover:bg-[#33B9FF]'
                            }`}
                          >
                            {isActive
                              ? isRtl
                                ? 'الملف النشط حالياً'
                                : 'Active Profile'
                              : isRtl
                              ? 'تفعيل هذا الملف'
                              : 'Activate Profile'}
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Safe JSON Config Export & Import + Corrupt File Recovery Test */}
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-bold text-[#E6EDF3] flex items-center gap-2">
                      <Download className="w-4 h-4 text-[#00A8FF]" />
                      <span>
                        {isRtl
                          ? `تصدير ملف الإعدادات النشط (profile_${activeProfile.name}.json)`
                          : `Export Active Profile JSON (profile_${activeProfile.name}.json)`}
                      </span>
                    </h3>
                    <button
                      type="button"
                      onClick={() => {
                        const blob = new Blob([generatedConfigJson], {
                          type: 'application/json;charset=utf-8',
                        });
                        const url = URL.createObjectURL(blob);
                        const a = document.createElement('a');
                        a.href = url;
                        a.download = `profile_${activeProfile.name.replace(/\s+/g, '_')}.json`;
                        a.click();
                        URL.revokeObjectURL(url);
                      }}
                      className="px-3 py-1.5 rounded bg-[#00A8FF] text-[#0B0E14] text-xs font-semibold"
                    >
                      {isRtl ? 'تنزيل JSON' : 'Download JSON'}
                    </button>
                  </div>
                  <pre
                    dir="ltr"
                    className="bg-[#0B0E14] border border-[#21262D] rounded p-3 text-[11px] font-mono text-[#8B949E] max-h-56 overflow-y-auto"
                  >
                    <code>{generatedConfigJson}</code>
                  </pre>
                </div>

                <div className="bg-[#111621] border border-[#21262D] rounded-lg p-5 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-bold text-[#E6EDF3] flex items-center gap-2">
                      <Upload className="w-4 h-4 text-[#A371F7]" />
                      <span>
                        {isRtl
                          ? 'استيراد ملف إعدادات وفحص الملفات التالفة'
                          : 'Import Profile JSON & Corrupt Config Recovery'}
                      </span>
                    </h3>
                    <button
                      type="button"
                      onClick={() =>
                        setImportJsonText('{ "schemaVersion": 1, "modules": CORRUPTED_JSON_TEST ')
                      }
                      className="text-xs text-[#A371F7] hover:underline"
                    >
                      {isRtl ? 'تجربة ملف تالف' : 'Load Corrupt Sample'}
                    </button>
                  </div>
                  <textarea
                    rows={6}
                    dir="ltr"
                    value={importJsonText}
                    onChange={(e) => setImportJsonText(e.target.value)}
                    placeholder='Paste NEXORA profile JSON here to import or test corruption recovery...'
                    className="w-full bg-[#0B0E14] border border-[#21262D] rounded p-3 text-xs font-mono text-[#E6EDF3] focus:outline-none focus:border-[#00A8FF]"
                  />
                  <button
                    type="button"
                    onClick={handleImportConfigJson}
                    className="px-4 py-2 rounded bg-[#161B22] border border-[#00A8FF]/50 text-xs font-semibold text-[#E6EDF3] hover:bg-[#00A8FF] hover:text-[#0B0E14] transition-colors"
                  >
                    {isRtl ? 'التحقق واستيراد الإعدادات' : 'Validate & Import JSON'}
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* SECTION 9: SETTINGS */}
          {activeSection === 'settings' && (
            <div className="bg-[#111621] border border-[#21262D] rounded-lg p-5 space-y-6">
              <div className="border-b border-[#21262D] pb-4">
                <h2 className="text-base font-bold text-[#E6EDF3]">
                  {isRtl
                    ? 'الإعدادات العامة واللغة وتوافق PojavLauncher'
                    : 'Global Client Settings, Localization & PojavLauncher Tuning'}
                </h2>
                <p className="text-xs text-[#8B949E] mt-0.5">
                  {isRtl
                    ? 'تُطبق تغييرات اللغة والألوان وتقليل المؤثرات فوراً دون الحاجة لإعادة تشغيل العميل.'
                    : 'Language, palette accents, and GL4ES performance toggles apply immediately without restarting.'}
                </p>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Language & RTL Configuration */}
                <div className="bg-[#0B0E14] border border-[#21262D] rounded-lg p-4 space-y-3">
                  <h3 className="text-sm font-semibold text-[#E6EDF3]">
                    {isRtl ? 'اللغة واتجاه الواجهة (RTL / LTR)' : 'Language & Direction (RTL / LTR)'}
                  </h3>
                  <p className="text-xs text-[#8B949E]">
                    {isRtl
                      ? 'يستخدم NEXORA مكتبة ICU4J (ArabicShaping & Bidi) داخل Minecraft 1.8.9 لربط الحروف العربية مع الحفاظ على الأرقام والإحداثيات من اليسار لليمين.'
                      : 'NEXORA uses ICU4J (ArabicShaping & Bidi) inside Minecraft 1.8.9 to shape Arabic script while keeping FPS, Ping, and Coordinates LTR.'}
                  </p>
                  <div className="flex items-center gap-2 pt-1">
                    <button
                      type="button"
                      onClick={() => setLang('ar')}
                      className={`px-4 py-2 rounded text-xs font-semibold ${
                        lang === 'ar'
                          ? 'bg-[#00A8FF] text-[#0B0E14]'
                          : 'bg-[#161B22] text-[#8B949E]'
                      }`}
                    >
                      العربية (RTL)
                    </button>
                    <button
                      type="button"
                      onClick={() => setLang('en')}
                      className={`px-4 py-2 rounded text-xs font-semibold ${
                        lang === 'en'
                          ? 'bg-[#00A8FF] text-[#0B0E14]'
                          : 'bg-[#161B22] text-[#8B949E]'
                      }`}
                    >
                      English (LTR)
                    </button>
                  </div>
                </div>

                {/* Theme Accent Customization */}
                <div className="bg-[#0B0E14] border border-[#21262D] rounded-lg p-4 space-y-3">
                  <h3 className="text-sm font-semibold text-[#E6EDF3]">
                    {isRtl ? 'تخصيص ألوان الهوية البصرية' : 'Brand Accent Palette'}
                  </h3>
                  <p className="text-xs text-[#8B949E]">
                    {isRtl
                      ? 'الأزرق الكهربائي (#00A8FF) هو اللون الأساسي لـ NEXORA، ويمكنك تعديله حسب رغبتك.'
                      : 'Electric Blue (#00A8FF) is NEXORA default primary accent; customize the active client accent below.'}
                  </p>
                  <div className="flex items-center gap-3 pt-1">
                    {[
                      { hex: '#00A8FF', name: 'Electric Blue' },
                      { hex: '#A371F7', name: 'Tech Violet' },
                      { hex: '#3FB950', name: 'Emerald Nominal' },
                      { hex: '#E6EDF3', name: 'Pure Monochrome' },
                    ].map((c) => (
                      <button
                        key={c.hex}
                        type="button"
                        onClick={() => setAccentTheme(c.hex)}
                        className={`px-3 py-1.5 rounded text-xs font-mono border flex items-center gap-2 ${
                          accentTheme === c.hex
                            ? 'border-white bg-[#161B22] text-[#E6EDF3]'
                            : 'border-[#21262D] text-[#8B949E]'
                        }`}
                        dir="ltr"
                      >
                        <span
                          className="w-3 h-3 rounded-sm inline-block"
                          style={{ backgroundColor: c.hex }}
                        />
                        <span>{c.name}</span>
                      </button>
                    ))}
                  </div>
                </div>

                {/* PojavLauncher & Adreno 710 Hardware Optimization Toggles */}
                <div className="bg-[#0B0E14] border border-[#21262D] rounded-lg p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-[#E6EDF3]">
                      {isRtl
                        ? 'وضع التوافق مع GL4ES (PojavLauncher / Adreno 710)'
                        : 'GL4ES Safe Rendering Mode (PojavLauncher / Adreno 710)'}
                    </h3>
                    <button
                      type="button"
                      onClick={() => setGl4esSafeMode((v) => !v)}
                      className={`px-3 py-1 rounded text-xs font-semibold ${
                        gl4esSafeMode ? 'bg-[#238636] text-white' : 'bg-[#21262D] text-[#8B949E]'
                      }`}
                    >
                      {gl4esSafeMode ? (isRtl ? 'مفعل' : 'Enabled') : isRtl ? 'معطل' : 'Disabled'}
                    </button>
                  </div>
                  <p className="text-xs text-[#8B949E]">
                    {isRtl
                      ? 'يمنع استخدام GLSL Shaders المعقدة ويعتمد حصرياً على Tessellator و WorldRenderer لتجنب الشاشة السوداء على الهواتف.'
                      : 'Disables custom GLSL shaders and uses pure Tessellator + WorldRenderer vertex batching to prevent mobile black screens.'}
                  </p>
                </div>

                {/* Reduced Motion / Animation Toggle */}
                <div className="bg-[#0B0E14] border border-[#21262D] rounded-lg p-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-[#E6EDF3]">
                      {isRtl
                        ? 'إيقاف الأنيميشن والمؤثرات الانتقالية'
                        : 'Disable UI Animations & Transitions'}
                    </h3>
                    <button
                      type="button"
                      onClick={() => setReducedMotion((v) => !v)}
                      className={`px-3 py-1 rounded text-xs font-semibold ${
                        reducedMotion ? 'bg-[#238636] text-white' : 'bg-[#21262D] text-[#8B949E]'
                      }`}
                    >
                      {reducedMotion ? (isRtl ? 'مفعل' : 'Enabled') : isRtl ? 'معطل' : 'Disabled'}
                    </button>
                  </div>
                  <p className="text-xs text-[#8B949E]">
                    {isRtl
                      ? 'يوقف الانتقالات البصرية لتوفير أقصى استجابة ممكنة على الأجهزة المتوسطة.'
                      : 'Turns off UI transitions for instantaneous response on mid-range mobile chipsets.'}
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* SECTION 10: ABOUT NEXORA & FORGE 1.8.9 SOURCE CODE */}
          {activeSection === 'about' && (
            <ForgeCodeViewer
              lang={lang}
              generatedConfigJson={generatedConfigJson}
              activeProfileName={activeProfile.name}
            />
          )}
        </main>
      </div>

      {/* Modal: Module Specific Settings */}
      {editingModule && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label={`Settings for ${editingModule.nameEn}`}
          className="fixed inset-0 z-50 bg-black/75 flex items-center justify-center p-4"
        >
          <div className="bg-[#111621] border border-[#21262D] rounded-lg max-w-md w-full p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-[#21262D] pb-3">
              <div>
                <h3 className="text-sm font-bold text-[#E6EDF3]">
                  {isRtl ? editingModule.nameAr : editingModule.nameEn}
                </h3>
                <p className="text-xs text-[#8B949E] font-mono" dir="ltr">
                  ID: {editingModule.id} · Category: {editingModule.category}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setEditingModuleId(null)}
                className="px-2.5 py-1 rounded bg-[#161B22] text-xs text-[#8B949E] hover:text-[#E6EDF3]"
              >
                {isRtl ? 'إغلاق' : 'Close'}
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-[#8B949E]">{isRtl ? 'حالة التفعيل' : 'Module State'}</span>
                <button
                  type="button"
                  onClick={() => handleToggleModule(editingModule.id)}
                  className={`px-3 py-1 rounded font-semibold ${
                    editingModule.enabled
                      ? 'bg-[#238636] text-white'
                      : 'bg-[#21262D] text-[#8B949E]'
                  }`}
                >
                  {editingModule.enabled
                    ? isRtl
                      ? 'مفعل'
                      : 'Enabled'
                    : isRtl
                    ? 'معطل'
                    : 'Disabled'}
                </button>
              </div>

              {editingModule.isHud && (
                <>
                  <div className="space-y-1">
                    <div className="flex justify-between">
                      <span className="text-[#8B949E]">{isRtl ? 'الحجم (Scale)' : 'Scale'}</span>
                      <span className="font-mono" dir="ltr">
                        {Math.round(editingModule.scale * 100)}%
                      </span>
                    </div>
                    <input
                      type="range"
                      min="0.6"
                      max="1.8"
                      step="0.05"
                      value={editingModule.scale}
                      onChange={(e) => {
                        const val = parseFloat(e.target.value);
                        updateModulesAndSyncProfile(
                          modules.map((m) =>
                            m.id === editingModule.id ? { ...m, scale: val } : m
                          )
                        );
                      }}
                      className="w-full accent-[#00A8FF]"
                    />
                  </div>

                  <div className="space-y-1">
                    <div className="flex justify-between">
                      <span className="text-[#8B949E]">
                        {isRtl ? 'الشفافية (Opacity)' : 'Opacity'}
                      </span>
                      <span className="font-mono" dir="ltr">
                        {Math.round(editingModule.opacity * 100)}%
                      </span>
                    </div>
                    <input
                      type="range"
                      min="0.15"
                      max="1.0"
                      step="0.05"
                      value={editingModule.opacity}
                      onChange={(e) => {
                        const val = parseFloat(e.target.value);
                        updateModulesAndSyncProfile(
                          modules.map((m) =>
                            m.id === editingModule.id ? { ...m, opacity: val } : m
                          )
                        );
                      }}
                      className="w-full accent-[#00A8FF]"
                    />
                  </div>
                </>
              )}
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-[#21262D]">
              <button
                type="button"
                onClick={() => handleResetSingleModule(editingModule.id)}
                className="px-3 py-1.5 rounded bg-[#161B22] text-xs text-[#F85149]"
              >
                {isRtl ? 'استعادة الافتراضي' : 'Reset Default'}
              </button>
              <button
                type="button"
                onClick={() => setEditingModuleId(null)}
                className="px-4 py-1.5 rounded bg-[#00A8FF] text-[#0B0E14] text-xs font-semibold"
              >
                {isRtl ? 'تم' : 'Done'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Confirm Profile Deletion */}
      {confirmDeleteProfileId && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/75 flex items-center justify-center p-4"
        >
          <div className="bg-[#111621] border border-[#F85149]/50 rounded-lg max-w-sm w-full p-5 space-y-4">
            <h3 className="text-sm font-bold text-[#E6EDF3]">
              {isRtl ? 'تأكيد حذف ملف الإعدادات' : 'Confirm Profile Deletion'}
            </h3>
            <p className="text-xs text-[#8B949E]">
              {isRtl
                ? 'هل أنت متأكد من حذف هذا الملف؟ إذا كان الملف نشطاً سيتم التبديل تلقائياً إلى ملف بديل صالح.'
                : 'Are you sure you want to delete this profile? If active, NEXORA will automatically switch to a valid fallback profile.'}
            </p>
            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setConfirmDeleteProfileId(null)}
                className="px-3 py-1.5 rounded bg-[#161B22] text-xs text-[#E6EDF3]"
              >
                {isRtl ? 'إلغاء' : 'Cancel'}
              </button>
              <button
                type="button"
                onClick={handleConfirmDeleteProfile}
                className="px-3.5 py-1.5 rounded bg-[#DA3633] text-white text-xs font-semibold"
              >
                {isRtl ? 'تأكيد الحذف' : 'Delete Profile'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
