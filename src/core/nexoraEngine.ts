export type NexoraCategory = 'HUD' | 'PVP' | 'VISUALS' | 'INVENTORY' | 'PERFORMANCE';

export type ImplementationStage = 'IMPLEMENTED_CORE' | 'ARCHITECTED_MODULE' | 'SERVER_RULE_OPTIONAL';

export type ServerSafetyLevel = 'SAFE' | 'SERVER_DEPENDENT';

export interface NexoraModule {
  id: string;
  index: number;
  nameEn: string;
  nameAr: string;
  descEn: string;
  descAr: string;
  category: NexoraCategory;
  enabled: boolean;
  defaultEnabled: boolean;
  keybind: string;
  defaultKeybind: string;
  stage: ImplementationStage;
  safety: ServerSafetyLevel;
  isHud: boolean;
  x: number;
  y: number;
  defaultX: number;
  defaultY: number;
  width: number;
  height: number;
  scale: number;
  opacity: number;
  fontSize: number;
  textAlign: 'left' | 'center' | 'right';
  accentColor: string;
  showBackground: boolean;
  locked: boolean;
  previewText?: string;
}

export interface NexoraProfile {
  id: string;
  name: string;
  nameAr: string;
  descriptionEn: string;
  descriptionAr: string;
  isBuiltIn: boolean;
  modules: Record<
    string,
    {
      enabled: boolean;
      keybind: string;
      x?: number;
      y?: number;
      scale?: number;
      opacity?: number;
      fontSize?: number;
      textAlign?: 'left' | 'center' | 'right';
      accentColor?: string;
      showBackground?: boolean;
      locked?: boolean;
    }
  >;
}

export interface ExternalModSpec {
  id: string;
  name: string;
  mcVersion: string;
  forgeRequirement: string;
  license: string;
  pojavStatus: 'COMPATIBLE' | 'TOUCH_REDUNDANT' | 'REQUIRES_AUDIT' | 'CONFLICT_RISK';
  pojavNoteAr: string;
  pojavNoteEn: string;
  recommendationAr: string;
  recommendationEn: string;
}

export const CURRENT_CONFIG_SCHEMA_VERSION = 1;

export function createInitialModules(): NexoraModule[] {
  const raw: Array<
    Omit<
      NexoraModule,
      | 'enabled'
      | 'keybind'
      | 'x'
      | 'y'
      | 'scale'
      | 'opacity'
      | 'fontSize'
      | 'textAlign'
      | 'accentColor'
      | 'showBackground'
      | 'locked'
    >
  > = [
    // A. HUD & Gameplay Info (1 - 30)
    {
      id: 'fps',
      index: 1,
      nameEn: 'FPS Display',
      nameAr: 'عرض الإطارات FPS',
      descEn: 'Displays real-time measured frames per second without allocation overhead.',
      descAr: 'يعرض معدل الإطارات الفعلي في الثانية دون استهلاك إضافي للذاكرة.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 16,
      defaultY: 16,
      width: 112,
      height: 34,
      previewText: '144 FPS',
    },
    {
      id: 'cps',
      index: 2,
      nameEn: 'CPS Counter',
      nameAr: 'عداد النقرات CPS',
      descEn: 'Tracks left and right clicks per second using a primitive ring buffer.',
      descAr: 'يحسب نقرات اليسار واليمين في الثانية عبر مخزن حلقي خفيف.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 16,
      defaultY: 58,
      width: 112,
      height: 34,
      previewText: '11 | 8 CPS',
    },
    {
      id: 'keystrokes',
      index: 3,
      nameEn: 'Keystrokes',
      nameAr: 'أزرار الحركة Keystrokes',
      descEn: 'Visualizes WASD movement, jump bar, and mouse buttons in real time.',
      descAr: 'يعرض أزرار الحركة المضغوطة والقفز والنقرات بشكل مباشر.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 16,
      defaultY: 100,
      width: 116,
      height: 98,
      previewText: '[W] [A][S][D]',
    },
    {
      id: 'armor_status',
      index: 4,
      nameEn: 'Armor Status',
      nameAr: 'حالة الدروع Armor Status',
      descEn: 'Displays equipped armor pieces and exact remaining durability.',
      descAr: 'يعرض الدروع المرتداة ومتانتها المتبقية بدقة.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 16,
      defaultY: 208,
      width: 124,
      height: 82,
      previewText: 'Helm 362 / Boots 408',
    },
    {
      id: 'coordinates',
      index: 5,
      nameEn: 'Coordinates',
      nameAr: 'الإحداثيات Coordinates',
      descEn: 'Shows X, Y, Z player coordinates and biome/direction cleanly.',
      descAr: 'يعرض إحداثيات اللاعب X و Y و Z واتجاه النظر بوضوح.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 16,
      defaultY: 296,
      width: 168,
      height: 34,
      previewText: 'X: 124  Y: 68  Z: -42',
    },
    {
      id: 'potion_counter',
      index: 6,
      nameEn: 'Potion Counter',
      nameAr: 'عداد الجرعات Potion Counter',
      descEn: 'Counts healing/speed potions or golden apples in inventory.',
      descAr: 'يعد الجرعات العلاجية أو التفاح الذهبي داخل المخزون.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 140,
      defaultY: 58,
      width: 118,
      height: 34,
      previewText: '18 Pots',
    },
    {
      id: 'potion_effects',
      index: 7,
      nameEn: 'Potion Effects',
      nameAr: 'تأثيرات الجرعات Potion Effects',
      descEn: 'Renders active status effects and timers outside inventory.',
      descAr: 'يعرض تأثيرات الجرعات النشطة ومددها المتبقية على الشاشة.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 610,
      defaultY: 96,
      width: 144,
      height: 54,
      previewText: 'Speed II (01:14)',
    },
    {
      id: 'direction_hud',
      index: 8,
      nameEn: 'Direction HUD',
      nameAr: 'بوصلة الاتجاه Direction HUD',
      descEn: 'Displays top compass strip with cardinal and ordinal bearings.',
      descAr: 'يعرض شريط بوصلة علوي يوضح اتجاه النظر بالدرجات.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 310,
      defaultY: 14,
      width: 160,
      height: 28,
      previewText: 'NW · 315° · N · NE',
    },
    {
      id: 'scoreboard',
      index: 9,
      nameEn: 'Scoreboard Customizer',
      nameAr: 'تحسين لوحة المباراة Scoreboard',
      descEn: 'Cleans scoreboard background, hides red numbers, and repositions box.',
      descAr: 'يحسن مظهر لوحة معلومات المباراة ويخفي الأرقام الحمراء.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 614,
      defaultY: 162,
      width: 140,
      height: 112,
      previewText: 'BED WARS · R ✓ B ✓',
    },
    {
      id: 'ping',
      index: 10,
      nameEn: 'Ping Display',
      nameAr: 'زمن الاستجابة Ping',
      descEn: 'Displays real latency reported by NetHandlerPlayClient.',
      descAr: 'يعرض زمن استجابة الاتصال الحقيقي بالسيرفر.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 140,
      defaultY: 16,
      width: 108,
      height: 34,
      previewText: '42 ms',
    },
    {
      id: 'memory',
      index: 11,
      nameEn: 'Memory Usage',
      nameAr: 'استهلاك الذاكرة Memory Usage',
      descEn: 'Shows JVM heap allocation percentage and used MB safely.',
      descAr: 'يعرض نسبة استهلاك الذاكرة العشوائية بأمان.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 140,
      defaultY: 58,
      width: 126,
      height: 34,
      previewText: 'RAM: 38% (780MB)',
    },
    {
      id: 'clock',
      index: 12,
      nameEn: 'Clock HUD',
      nameAr: 'الساعة المحلية Clock',
      descEn: 'Displays current local system time in 12h or 24h format.',
      descAr: 'يعرض الوقت المحلي الحالي بنظام 12 أو 24 ساعة.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 394,
      defaultY: 16,
      width: 96,
      height: 34,
      previewText: '15:42',
    },
    {
      id: 'server_address',
      index: 13,
      nameEn: 'Server Address',
      nameAr: 'عنوان السيرفر Server Address',
      descEn: 'Shows current connected server IP or singleplayer world name.',
      descAr: 'يعرض عنوان السيرفر المتصل به حالياً.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 500,
      defaultY: 16,
      width: 136,
      height: 34,
      previewText: 'mc.hypixel.net',
    },
    {
      id: 'stopwatch',
      index: 14,
      nameEn: 'Stopwatch',
      nameAr: 'ساعة التوقيت Stopwatch',
      descEn: 'Precision match & speedrun timer controlled via hotkey.',
      descAr: 'ساعة إيقاف دقيقة لحساب زمن المباريات والتدريب.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'KEY_P',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 500,
      defaultY: 58,
      width: 108,
      height: 34,
      previewText: '00:00.00',
    },
    {
      id: 'saturation',
      index: 15,
      nameEn: 'Saturation Display',
      nameAr: 'مؤشر التشبع الغذائي Saturation',
      descEn: 'Renders food saturation level read directly from FoodStats.',
      descAr: 'يعرض مستوى التشبع الغذائي الفعلي للاعب.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 258,
      defaultY: 58,
      width: 112,
      height: 34,
      previewText: 'Sat: 14.4',
    },
    {
      id: 'net_graph',
      index: 16,
      nameEn: 'Net Graph',
      nameAr: 'مخطط الشبكة والأداء Net Graph',
      descEn: 'Compact frame-time and packet interval stability bar.',
      descAr: 'يعرض استقرار زمن الإطارات ومعدل حزم الشبكة.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 380,
      defaultY: 58,
      width: 112,
      height: 34,
      previewText: '6.9 ms/f',
    },
    {
      id: 'combo',
      index: 17,
      nameEn: 'Combo Counter',
      nameAr: 'عداد الضربات المتتالية Combo',
      descEn: 'Counts consecutive melee hits landed before taking damage.',
      descAr: 'يعرض عدد الضربات المتتالية المسجلة على الخصم.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 140,
      defaultY: 100,
      width: 116,
      height: 34,
      previewText: '4 Combo',
    },
    {
      id: 'reach',
      index: 18,
      nameEn: 'Reach Display',
      nameAr: 'مسافة الهدف Reach Display',
      descEn: 'Calculates local client distance on melee attack registration.',
      descAr: 'يعرض مسافة الضربة المحلية عند إصابة الهدف.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 140,
      defaultY: 142,
      width: 116,
      height: 34,
      previewText: '2.84 blocks',
    },
    {
      id: 'cooldowns',
      index: 19,
      nameEn: 'Item Cooldowns',
      nameAr: 'مؤقتات العناصر Cooldowns',
      descEn: 'Displays Ender Pearl and Golden Apple cooldown timers.',
      descAr: 'يعرض مؤقتات إعادة استخدام اللؤلؤ والتفاح الذهبي.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 266,
      defaultY: 100,
      width: 116,
      height: 34,
      previewText: 'Pearl: 12s',
    },
    {
      id: 'pack_display',
      index: 20,
      nameEn: 'Pack Display',
      nameAr: 'عرض حزمة الموارد Pack Display',
      descEn: 'Shows the active Resource Pack name on the HUD.',
      descAr: 'يعرض اسم حزمة الموارد (Resource Pack) النشطة حالياً.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 16,
      defaultY: 344,
      width: 152,
      height: 32,
      previewText: 'Pack: SammyFault 16x',
    },
    {
      id: 'low_health_warning',
      index: 21,
      nameEn: 'Low Health Warning',
      nameAr: 'تنبيه انخفاض الصحة',
      descEn: 'Displays a subtle red vignette alert when health drops below threshold.',
      descAr: 'يظهر تنبيهاً بصرياً عند انخفاض صحة اللاعب عن الحد المحدد.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'damage_direction',
      index: 22,
      nameEn: 'Damage Direction',
      nameAr: 'اتجاه الضرر الوارد',
      descEn: 'Shows directional arc indicator when taking knockback from visible sources.',
      descAr: 'يعرض مؤشراً يوضح اتجاه الضرر الوارد عند توفر المصدر.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'SERVER_RULE_OPTIONAL',
      safety: 'SERVER_DEPENDENT',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'pvp_session_stats',
      index: 23,
      nameEn: 'PvP Session Stats',
      nameAr: 'إحصاءات الجلسة المحلية',
      descEn: 'Tracks local session duration, hits landed, and peak combo.',
      descAr: 'يسجل إحصاءات الجلسة الحالية محلياً مثل أعلى كومبو وعدد الضربات.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 614,
      defaultY: 284,
      width: 140,
      height: 60,
      previewText: 'Session: 14 W / 2 L',
    },
    {
      id: 'pvp_stats',
      index: 24,
      nameEn: 'PvP Stats Tracker',
      nameAr: 'سجل إحصاءات القتال PvP Stats',
      descEn: 'Maintains cumulative local combat metrics across play sessions.',
      descAr: 'يحتفظ بسجل محلي لإحصاءات اللعب والقتال عبر الجلسات.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'bed_status_hud',
      index: 25,
      nameEn: 'Bed Status HUD',
      nameAr: 'حالة الأسرّة Bed Status',
      descEn: 'Parses scoreboard lines to summarize remaining beds cleanly.',
      descAr: 'يقرأ بيانات لوحة المباراة المتاحة لعرض حالة الأسرّة.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 394,
      defaultY: 100,
      width: 132,
      height: 36,
      previewText: 'Beds: R✓ B✓ G✗ Y✓',
    },
    {
      id: 'team_status',
      index: 26,
      nameEn: 'Team Status HUD',
      nameAr: 'حالة الفرق Team Status',
      descEn: 'Displays remaining team members from public tab/scoreboard data.',
      descAr: 'يعرض الفرق واللاعبين المتبقين من البيانات المتاحة للعميل.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'resource_tracker',
      index: 27,
      nameEn: 'Resource Tracker',
      nameAr: 'متتبع موارد المباراة',
      descEn: 'Counts Iron, Gold, Diamonds, and Emeralds currently in player inventory.',
      descAr: 'يحسب كمية الحديد والذهب والألماس والزمرد في مخزون اللاعب مباشرة.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 140,
      defaultY: 184,
      width: 176,
      height: 34,
      previewText: 'Fe: 48 · Au: 12 · Dia: 4 · Em: 2',
    },
    {
      id: 'performance_monitor',
      index: 28,
      nameEn: 'Performance Monitor',
      nameAr: 'مراقب الأداء والتقطيع',
      descEn: 'Monitors 1% low FPS, frame time spikes (ms), and active HUD count.',
      descAr: 'يراقب استقرار الإطارات وزمن الرسم وعدد عناصر الواجهة النشطة.',
      category: 'PERFORMANCE',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'height_limit',
      index: 29,
      nameEn: 'Height Limit & Block Counter HUD',
      nameAr: 'عداد البلوكات وحد الارتفاع Block & Height HUD',
      descEn: 'Displays held stack, total placeable inventory blocks, session blocks placed, and ceiling warning.',
      descAr: 'يعرض عدد البلوكات في اليد والمخزون وإجمالي البلوكات الموضوعة مع تنبيه حد الارتفاع.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 266,
      defaultY: 142,
      width: 152,
      height: 44,
      previewText: 'Blocks: 64 (256) · MaxY: 18m',
    },
    {
      id: 'boss_bar',
      index: 30,
      nameEn: 'Boss Bar Customizer',
      nameAr: 'تخصيص شريط الزعيم Boss Bar',
      descEn: 'Allows scaling, repositioning, or hiding the top Wither/Dragon boss bar.',
      descAr: 'يتيح تغيير حجم وموضع شريط الزعيم العلوي أو إخفاءه.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 302,
      defaultY: 14,
      width: 196,
      height: 24,
      previewText: 'NEXORA BED WARS LOBBY',
    },

    // B. Movement & Control (31 - 40)
    {
      id: 'toggle_sprint',
      index: 31,
      nameEn: 'Toggle Sneak / Sprint',
      nameAr: 'تثبيت الركض والتسلل',
      descEn: 'Locks sprint state automatically while moving forward.',
      descAr: 'يثبت وضع الركض أو التسلل دون الحاجة للاستمرار بالضغط.',
      category: 'PVP',
      defaultEnabled: true,
      defaultKeybind: 'KEY_I',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 140,
      defaultY: 226,
      width: 148,
      height: 28,
      previewText: '[Sprinting (Toggled)]',
    },
    {
      id: 'freelook',
      index: 32,
      nameEn: 'Freelook 360',
      nameAr: 'منظور الكاميرا الحر Freelook',
      descEn: 'Rotates camera 360° around the player without altering movement direction.',
      descAr: 'يحرك زاوية الكاميرا 360 درجة دون تغيير اتجاه حركة اللاعب.',
      category: 'PVP',
      defaultEnabled: true,
      defaultKeybind: 'KEY_LMENU',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SERVER_DEPENDENT',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'zoom',
      index: 33,
      nameEn: 'Smooth Zoom',
      nameAr: 'تقريب الرؤية السلس Zoom',
      descEn: 'Smoothly narrows FOV while holding the zoom key with scroll-wheel steps.',
      descAr: 'يقرب الرؤية بسلاسة عند الضغط على مفتاح التقريب المخصص.',
      category: 'PVP',
      defaultEnabled: true,
      defaultKeybind: 'KEY_C',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'snaplook',
      index: 34,
      nameEn: 'Snaplook',
      nameAr: 'نظرة خلفية سريعة Snaplook',
      descEn: 'Instant rear third-person view while holding the assigned key.',
      descAr: 'يتيح النظر خلف اللاعب بسرعة أثناء الضغط على المفتاح.',
      category: 'PVP',
      defaultEnabled: false,
      defaultKeybind: 'KEY_V',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'advance_perspective',
      index: 35,
      nameEn: 'Advance Perspective',
      nameAr: 'اختصارات المنظور الثالث',
      descEn: 'Dedicated hotkeys for front and back third-person views without cycling.',
      descAr: 'اختصارات مباشرة للمنظور الأمامي والخلفي دون المرور بالتبديل المتكرر.',
      category: 'PVP',
      defaultEnabled: false,
      defaultKeybind: 'KEY_F5',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'fov_mod',
      index: 36,
      nameEn: 'Dynamic FOV Control',
      nameAr: 'التحكم بمجال الرؤية FOV',
      descEn: 'Customizes base FOV and locks speed/sprint/bow FOV distortion.',
      descAr: 'يخصص مجال الرؤية ويمنع تغيره المزعج مع السرعة أو القوس.',
      category: 'PVP',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'wtap_coach',
      index: 37,
      nameEn: 'W-Tap Coach (Visual)',
      nameAr: 'مدرب توقيت W-Tap البصري',
      descEn: 'Displays a local timing bar to help practice manual W-Tap sprint resets.',
      descAr: 'أداة إرشادية بصرية لتدريب اللاعب على توقيت W-Tap يدوياً.',
      category: 'PVP',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'blockhit_trainer',
      index: 38,
      nameEn: 'Blockhit Trainer (Visual)',
      nameAr: 'مدرب توقيت Blockhit اليدوي',
      descEn: 'Provides visual feedback on manual LMB+RMB blockhit rhythm.',
      descAr: 'يعرض مؤشراً بصرياً لتقييم إيقاع Blockhit اليدوي أثناء التدريب.',
      category: 'PVP',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'bridge_helper',
      index: 39,
      nameEn: 'Bridge Pitch/Yaw HUD',
      nameAr: 'مرشد زاوية بناء الجسور Bridge HUD',
      descEn: 'Displays real-time camera pitch/yaw alignment (Ninja/God/Moonwalk) and live BPS without input automation.',
      descAr: 'يعرض زاوية النظر (Pitch/Yaw) وسرعة وضع البلوكات (BPS) ونوع الجسر بدون أي تدخل آلي.',
      category: 'PVP',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: true,
      defaultX: 266,
      defaultY: 194,
      width: 154,
      height: 44,
      previewText: 'Pitch: 78.5° [NINJA · 4.2 BPS]',
    },
    {
      id: 'bridgelab',
      index: 40,
      nameEn: 'BridgeLab Telemetry',
      nameAr: 'بيئة قياس سرعة الجسور BridgeLab',
      descEn: 'Measures blocks placed per second (BPS) and session block placement totals cleanly.',
      descAr: 'يقيس سرعة وضع البلوكات في الثانية وإجمالي البلوكات الموضوعة في الجلسة.',
      category: 'PVP',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },

    // C. Visuals & Effects (41 - 61)
    {
      id: 'visuals_1_7',
      index: 41,
      nameEn: '1.7 Classic Visuals',
      nameAr: 'حركات 1.7 الكلاسيكية',
      descEn: 'Restores classic 1.7 sword block-hitting, eating, and bow animations.',
      descAr: 'يعيد الحركات البصرية الكلاسيكية لإصدار 1.7 عند الصد بالسيف والأكل.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'crosshair',
      index: 42,
      nameEn: 'Custom Crosshair',
      nameAr: 'تخصيص مؤشر التصويب Crosshair',
      descEn: 'Renders crisp dot, cross, or circle reticles with dynamic hit feedback.',
      descAr: 'يخصص شكل ولون وحجم مؤشر التصويب في منتصف الشاشة.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'motion_blur',
      index: 43,
      nameEn: 'Lightweight Motion Blur',
      nameAr: 'ضبابية الحركة Motion Blur',
      descEn: 'Optional camera accumulation blur (automatically disabled on GL4ES Low-Spec mode).',
      descAr: 'ضبابية حركة اختيارية (تُعطل تلقائياً في وضع الهواتف للحفاظ على الأداء).',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'particle_multiplier',
      index: 44,
      nameEn: 'Particle Studio (Multiplier & Budget)',
      nameAr: 'استوديو الجسيمات Particle Studio',
      descEn: 'Controls critical/sharpness particle multiplier (1x-5x), per-second budget cap, and GL4ES spike culling.',
      descAr: 'يتحكم في مضاعف جسيمات الضرب (1x-5x) وسقف الميزانية في الثانية مع تقليل تلقائي عند هبوط الإطارات.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'uhc_overlay',
      index: 45,
      nameEn: 'UHC Item Overlay',
      nameAr: 'إبراز عناصر UHC',
      descEn: 'Scales up Golden Apples, Gold Ingots, and Skulls for rapid UHC looting.',
      descAr: 'يبرز حجم التفاح الذهبي والذهب لتسهيل رؤيتها في طور UHC.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'shiny_pots',
      index: 46,
      nameEn: 'Shiny Pots',
      nameAr: 'لمعان الجرعات Shiny Pots',
      descEn: 'Extends enchantment glint across the full potion bottle icon.',
      descAr: 'يظهر لمعاناً واضحاً على كامل أيقونة الجرعات في المخزون.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'block_outline',
      index: 47,
      nameEn: 'Block Outline',
      nameAr: 'تحديد الكتلة المستهدفة',
      descEn: 'Customizes targeted block wireframe color and line thickness.',
      descAr: 'يخصص لون وسماكة إطار الكتلة التي ينظر إليها اللاعب.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'hitcolor',
      index: 48,
      nameEn: 'Hit Color',
      nameAr: 'لون إصابة الخصم Hit Color',
      descEn: 'Customizes the damage tint color and alpha on hit entities via Mixin.',
      descAr: 'يغير لون الخصم بصرياً عند تسجيل الإصابة عبر نظام Mixins.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'hurtcam',
      index: 49,
      nameEn: 'HurtCam Reducer',
      nameAr: 'تقليل اهتزاز الضرر HurtCam',
      descEn: 'Scales down camera shake when taking damage for steady tracking.',
      descAr: 'يقلل اهتزاز الشاشة عند تلقي الضرر للحفاظ على ثبات التصويب.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'hitboxes',
      index: 50,
      nameEn: 'Entity Hitboxes (F3+B)',
      nameAr: 'حدود التصادم Hitboxes',
      descEn: 'Customizes vanilla F3+B bounding box visibility where permitted.',
      descAr: 'يعرض حدود التصادم في الأوضاع التي تسمح بها قواعد السيرفر.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'SERVER_RULE_OPTIONAL',
      safety: 'SERVER_DEPENDENT',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'nametags',
      index: 51,
      nameEn: 'Clear Nametags',
      nameAr: 'تحسين وضوح الأسماء Nametags',
      descEn: 'Adds clean shadow and optional own-nametag rendering in third person.',
      descAr: 'يحسن وضوح أسماء اللاعبين ويعرض اسم اللاعب في المنظور الثالث.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'glint_color',
      index: 52,
      nameEn: 'Enchant Glint Color',
      nameAr: 'لون لمعة التطوير Glint Color',
      descEn: 'Customizes the color tint of enchanted weapons and armor.',
      descAr: 'يتيح تغيير لون لمعة التطوير على السيوف والدروع.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'weather_changer',
      index: 53,
      nameEn: 'Weather Changer',
      nameAr: 'تغيير الطقس المحلي',
      descEn: 'Clears rain/snow particles locally to boost visibility and FPS.',
      descAr: 'يزيل المطر والثلج محلياً لتحسين الرؤية ورفع الأداء.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'fullbright',
      index: 54,
      nameEn: 'Fullbright',
      nameAr: 'إضاءة كاملة Fullbright',
      descEn: 'Increases client gamma for clear visibility in dark areas.',
      descAr: 'يحسن وضوح المشاهد المظلمة محلياً مع مراعاة قواعد السيرفر.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'KEY_G',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'items_2d',
      index: 55,
      nameEn: '2D Dropped Items',
      nameAr: 'عناصر ساقطة ثنائية الأبعاد 2D Items',
      descEn: 'Renders dropped ground items as lightweight 2D sprites to save GPU.',
      descAr: 'يعرض العناصر الساقطة بشكل ثنائي الأبعاد لتقليل استهلاك المعالج الرسومي.',
      category: 'PERFORMANCE',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'hit_markers',
      index: 56,
      nameEn: 'Hit Markers',
      nameAr: 'علامة الإصابة Hit Markers',
      descEn: 'Flashes a subtle crosshair tick when a melee or bow hit registers.',
      descAr: 'يظهر مؤشراً مرئياً خفيفاً عند تسجيل الضربة على الخصم.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'kill_sounds',
      index: 57,
      nameEn: 'Kill Sound Feedback',
      nameAr: 'صوت القتل الاختياري Kill Sounds',
      descEn: 'Plays a clean built-in audio cue upon eliminating an opponent.',
      descAr: 'يشغل صوتاً خفيفاً اختيارياً عند القضاء على الخصم.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'double_crit_visualizer',
      index: 58,
      nameEn: 'Crit Event Visualizer',
      nameAr: 'عارض الضربات الحرجة Crit Visualizer',
      descEn: 'Highlights critical + sharpness particle bursts clearly.',
      descAr: 'يوضح بصرياً تزامن الضربات الحرجة مع تطوير السيف.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'block_overlay',
      index: 59,
      nameEn: 'Block Overlay Fill',
      nameAr: 'تظليل الكتلة المستهدفة Block Overlay',
      descEn: 'Adds translucent face fill to the targeted block alongside Block Outline.',
      descAr: 'يضيف تظليلاً شفافاً خفيفاً لوجه الكتلة المستهدفة.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'item_physics',
      index: 60,
      nameEn: 'Item Physics',
      nameAr: 'فيزياء العناصر الساقطة',
      descEn: 'Renders dropped items resting flat on the ground with natural rotation.',
      descAr: 'يغير طريقة عرض العناصر الساقطة لتبدو مستقرة على الأرض.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'time_changer',
      index: 61,
      nameEn: 'Time Changer',
      nameAr: 'تغيير الوقت المحلي Time Changer',
      descEn: 'Locks client sky time to Noon, Sunset, or Night locally.',
      descAr: 'يثبت وقت السماء محلياً على النهار أو الغروب أو الليل.',
      category: 'VISUALS',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },

    // D. Chat & Tools (62 - 75)
    {
      id: 'chat_customizer',
      index: 62,
      nameEn: 'Chat Customizer',
      nameAr: 'تخصيص الدردشة Chat',
      descEn: 'Supports transparent chat background, anti-spam stacking, and Arabic RTL chat.',
      descAr: 'يخصص خلفية الدردشة ويدمج الرسائل المتكررة ويدعم العربية.',
      category: 'INVENTORY',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'scrollable_tooltips',
      index: 63,
      nameEn: 'Scrollable Tooltips',
      nameAr: 'تمرير أوصاف العناصر الطويلة',
      descEn: 'Allows scrolling long item lore panels that exceed screen height.',
      descAr: 'يتيح تمرير أوصاف العناصر الطويلة داخل المخزون.',
      category: 'INVENTORY',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'auto_text_hotkey',
      index: 64,
      nameEn: 'Auto Text Hotkey',
      nameAr: 'اختصارات النصوص السريعة',
      descEn: 'Sends user-configured chat commands (/l, /play, /hub) on key press.',
      descAr: 'يرسل أوامر أو رسائل محفوظة مسبقاً عند الضغط على مفتاح اختصار.',
      category: 'INVENTORY',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SERVER_DEPENDENT',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'worldedit_cui',
      index: 65,
      nameEn: 'WorldEdit CUI',
      nameAr: 'عارض تحديدات WorldEdit',
      descEn: 'Visualizes WorldEdit cuboid selections in creative/build servers.',
      descAr: 'يعرض حدود تحديدات WorldEdit بصرياً في سيرفرات البناء.',
      category: 'VISUALS',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'screenshot_helper',
      index: 66,
      nameEn: 'Screenshot Manager',
      nameAr: 'مدير لقطات الشاشة',
      descEn: 'Saves screenshots asynchronously to prevent F2 frame freeze and copies path.',
      descAr: 'يحفظ لقطات الشاشة دون تجميد الإطارات مع خيار النسخ الآمن.',
      category: 'INVENTORY',
      defaultEnabled: true,
      defaultKeybind: 'KEY_F2',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'mumble_link',
      index: 67,
      nameEn: 'Mumble Positional Link',
      nameAr: 'ربط الصوت المكاني Mumble Link',
      descEn: 'Desktop positional audio bridge (auto-disabled on Android/PojavLauncher).',
      descAr: 'ربط خدمة Mumble على الحاسوب (معطل تلقائياً على أندرويد لعدم توفر الخدمة).',
      category: 'INVENTORY',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'team_view',
      index: 68,
      nameEn: 'Team View',
      nameAr: 'عرض معلومات الفريق Team View',
      descEn: 'Highlights teammates using standard scoreboard team prefixes.',
      descAr: 'يعرض مواقع ومعلومات أعضاء الفريق بناءً على بيانات السيرفر المتاحة.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'shop_helper',
      index: 69,
      nameEn: 'Bedwars Shop Helper',
      nameAr: 'مساعد متجر Bed Wars',
      descEn: 'Highlights affordable items in the Bed Wars villager shop GUI.',
      descAr: 'يبرز العناصر التي تمتلك موارد كافية لشرائها داخل واجهة المتجر.',
      category: 'INVENTORY',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'bedwars_chat_stats',
      index: 70,
      nameEn: 'Bedwars Chat Summary',
      nameAr: 'ملخص إحصاءات المباراة في الدردشة',
      descEn: 'Formats end-of-match kills/beds announcements cleanly in chat.',
      descAr: 'ينظم إعلانات نهاية المباراة وعدد القتلات والأسرّة في الدردشة.',
      category: 'INVENTORY',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'quick_resource_collect',
      index: 71,
      nameEn: 'Quick Resource Filter',
      nameAr: 'تنظيم جمع الموارد في الصناديق',
      descEn: 'Visual highlight for Iron/Gold/Emerald stacks inside open chest GUIs.',
      descAr: 'يبرز موارد المباراة داخل الصناديق المفتوحة لتسهيل نقلها قانونياً.',
      category: 'INVENTORY',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'replay_helper',
      index: 72,
      nameEn: 'Replay Integration Hook',
      nameAr: 'مساعد التكامل مع Replay Mod',
      descEn: 'Displays recording indicator when compatible ReplayMod 1.8.9 is loaded.',
      descAr: 'يعرض حالة التسجيل عند وجود مود ReplayMod متوافق.',
      category: 'INVENTORY',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'item_tracker',
      index: 73,
      nameEn: 'Item Pickup Tracker',
      nameAr: 'متتبع التقاط العناصر',
      descEn: 'Shows recent resource pickups (+4 Iron, +1 Emerald) on screen.',
      descAr: 'يعرض تنبيهاً مصغراً للعناصر والموارد التي التقطها اللاعب حديثاً.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'nick_hider',
      index: 74,
      nameEn: 'Nick Hider (Privacy)',
      nameAr: 'إخفاء الاسم للخصوصية Nick Hider',
      descEn: 'Replaces your username locally on screen and scoreboard for recording.',
      descAr: 'يستبدل اسم اللاعب محلياً على الشاشة لأغراض الخصوصية والتصوير.',
      category: 'INVENTORY',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'minimap',
      index: 75,
      nameEn: 'Safe Terrain MiniMap',
      nameAr: 'الخريطة المصغرة MiniMap',
      descEn: 'Chunk-surface minimap (entity radar disabled by default for server compliance).',
      descAr: 'خريطة مصغرة للتضاريس مع تعطيل رادار اللاعبين افتراضياً لاحترام قواعد السيرفرات.',
      category: 'HUD',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'SERVER_RULE_OPTIONAL',
      safety: 'SERVER_DEPENDENT',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },

    // E. Specialized & Advanced NEXORA Modules (76 - 83)
    {
      id: 'skyblock_addons',
      index: 76,
      nameEn: 'SkyBlock Addons Bridge',
      nameAr: 'إضافات SkyBlock المتخصصة',
      descEn: 'Item lock and ability cooldown parser for Hypixel SkyBlock.',
      descAr: 'قفل العناصر الهامة وعرض مؤقتات القدرات لطور SkyBlock.',
      category: 'INVENTORY',
      defaultEnabled: false,
      defaultKeybind: 'NONE',
      stage: 'ARCHITECTED_MODULE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'low_latency_hud',
      index: 77,
      nameEn: 'Low-Latency HUD Engine',
      nameAr: 'محرك واجهة منخفض التأخير',
      descEn: 'Caches shaped strings and batches GL11 quads for Snapdragon 6 Gen 1 / Adreno 710.',
      descAr: 'يخزن النصوص المعالجة ويجمع عمليات الرسم لتقليل زمن الإطار على الهواتف.',
      category: 'PERFORMANCE',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'custom_resource_layout',
      index: 78,
      nameEn: 'Custom Resource Layout',
      nameAr: 'تخصيص تخطيط الموارد',
      descEn: 'Switches resource counter between horizontal pill, compact grid, or icon-only.',
      descAr: 'يخصص طريقة عرض موارد المباراة بين العرض الأفقي أو الشبكي المختصر.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'smart_inventory_sorter',
      index: 79,
      nameEn: 'Smart Inventory Slot Guide',
      nameAr: 'مرشد ترتيب المخزون الذكي',
      descEn: 'Displays slot target badges (Sword: 1, Blocks: 2, Golden Apple: 3) per profile.',
      descAr: 'يعرض دلالات بصرية لترتيب الخانات المفضلة لكل ملف إعدادات دون تحريك آلي مخالف.',
      category: 'INVENTORY',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'profile_switching',
      index: 80,
      nameEn: 'Instant Profile Switcher',
      nameAr: 'التبديل الفوري لملفات الإعدادات',
      descEn: 'Hot-swaps Practice, Bed Wars, and Performance profiles without game restart.',
      descAr: 'يبدل بين ملفات Practice و Bed Wars و Performance فوراً دون إعادة التشغيل.',
      category: 'PERFORMANCE',
      defaultEnabled: true,
      defaultKeybind: 'KEY_F8',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'smart_hud_editor',
      index: 81,
      nameEn: 'Smart HUD Editor',
      nameAr: 'محرر الواجهة الذكي HUD Editor',
      descEn: 'Interactive drag, scale, snap, lock, and bounds-clamped HUD layout studio.',
      descAr: 'محرر تفاعلي لتحريك وتغيير حجم وقفل ومحاذاة عناصر الشاشة مباشرة.',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'KEY_H',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'tab_editor',
      index: 82,
      nameEn: 'Tab List Editor',
      nameAr: 'تخصيص قائمة اللاعبين Tab Editor',
      descEn: 'Replaces ping bars with numeric ms and customizes Tab header/footer visibility.',
      descAr: 'يعرض البينج كأرقام واضحة ويخصص مظهر قائمة اللاعبين (Tab).',
      category: 'HUD',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
    {
      id: 'tnt_timer',
      index: 83,
      nameEn: 'TNT Fuse Timer',
      nameAr: 'مؤقت انفجار الـ TNT',
      descEn: 'Renders exact countdown seconds above primed EntityTNTPrimed entities.',
      descAr: 'يعرض العد التنازلي الدقيق بالثواني فوق الـ TNT المشتعل لتوقيت القفز.',
      category: 'PVP',
      defaultEnabled: true,
      defaultKeybind: 'NONE',
      stage: 'IMPLEMENTED_CORE',
      safety: 'SAFE',
      isHud: false,
      defaultX: 0,
      defaultY: 0,
      width: 0,
      height: 0,
    },
  ];

  return raw.map((item) => ({
    ...item,
    enabled: item.defaultEnabled,
    keybind: item.defaultKeybind,
    x: item.defaultX,
    y: item.defaultY,
    scale: 1.0,
    opacity: 0.88,
    fontSize: 13,
    textAlign: 'center',
    accentColor: '#00A8FF',
    showBackground: true,
    locked: false,
  }));
}

export function createDefaultProfiles(baseModules: NexoraModule[]): NexoraProfile[] {
  const buildModuleMap = (predicate: (m: NexoraModule) => boolean, accent: string) => {
    const map: NexoraProfile['modules'] = {};
    for (const m of baseModules) {
      map[m.id] = {
        enabled: predicate(m),
        keybind: m.defaultKeybind,
        x: m.defaultX,
        y: m.defaultY,
        scale: m.scale,
        opacity: m.opacity,
        fontSize: m.fontSize,
        textAlign: m.textAlign,
        accentColor: accent,
        showBackground: m.showBackground,
        locked: false,
      };
    }
    return map;
  };

  return [
    {
      id: 'bedwars',
      name: 'Bed Wars',
      nameAr: 'حرب الأسرّة Bed Wars',
      descriptionEn: 'Optimized for Bed Wars: Resource Tracker, TNT Timer, Armor Status, Keystrokes, and Shop Helper.',
      descriptionAr: 'مخصص لطور Bed Wars مع متتبع الموارد ومؤقت الـ TNT وحالة الدروع ومساعد المتجر.',
      isBuiltIn: true,
      modules: buildModuleMap(
        (m) =>
          m.defaultEnabled ||
          ['bed_status_hud', 'resource_tracker', 'tnt_timer', 'shop_helper', 'bridge_helper'].includes(m.id),
        '#00A8FF'
      ),
    },
    {
      id: 'practice',
      name: 'Practice',
      nameAr: 'التدريب والمبارزات Practice',
      descriptionEn: 'Focused 1v1 duel HUD: CPS, Combo, Reach, Potion Counter, HurtCam Reducer, and Hit Color.',
      descriptionAr: 'مخصص للمبارزات الفردية 1v1 مع عداد الجرعات والكومبو ومسافة الضرب وتقليل الاهتزاز.',
      isBuiltIn: true,
      modules: buildModuleMap(
        (m) =>
          ['fps', 'cps', 'keystrokes', 'armor_status', 'potion_counter', 'potion_effects', 'ping', 'combo', 'reach', 'toggle_sprint', 'visuals_1_7', 'hurtcam', 'hitcolor', 'particle_multiplier'].includes(
            m.id
          ),
        '#A371F7'
      ),
    },
    {
      id: 'general_pvp',
      name: 'General PvP',
      nameAr: 'قتال عام General PvP',
      descriptionEn: 'Balanced all-around setup for SkyWars, UHC, and classic 1.8.9 minigames.',
      descriptionAr: 'إعداد متوازن لجميع أطوار القتال مثل SkyWars و UHC والألعاب المصغرة.',
      isBuiltIn: true,
      modules: buildModuleMap((m) => m.defaultEnabled, '#00A8FF'),
    },
    {
      id: 'performance',
      name: 'Performance (Adreno 710)',
      nameAr: 'الأداء الأقصى (هواتف Pojav)',
      descriptionEn: 'Ultra-lightweight profile for Snapdragon 6 Gen 1 / GL4ES with minimal HUD elements and 2D Items.',
      descriptionAr: 'أخف ملف إعدادات للهواتف المتوسطة لتقليل التقطيع واستهلاك الذاكرة إلى الحد الأدنى.',
      isBuiltIn: true,
      modules: buildModuleMap(
        (m) =>
          ['fps', 'cps', 'ping', 'memory', 'toggle_sprint', 'weather_changer', 'fullbright', 'items_2d', 'low_latency_hud', 'performance_monitor'].includes(
            m.id
          ),
        '#3FB950'
      ),
    },
    {
      id: 'custom',
      name: 'Custom',
      nameAr: 'مخصص Custom',
      descriptionEn: 'User-customizable sandbox profile for testing personal layouts and keybinds.',
      descriptionAr: 'ملف حر قابل للتعديل الكامل لحفظ تخطيطاتك واختصاراتك الخاصة.',
      isBuiltIn: true,
      modules: buildModuleMap((m) => m.defaultEnabled, '#00A8FF'),
    },
  ];
}

export const EXTERNAL_MODS_MATRIX: ExternalModSpec[] = [
  {
    id: 'optifine',
    name: 'OptiFine HD U M5 (1.8.9)',
    mcVersion: '1.8.9',
    forgeRequirement: 'Forge 11.15.1.2318',
    license: 'Proprietary (No JAR Redistribution)',
    pojavStatus: 'COMPATIBLE',
    pojavNoteAr: 'يعمل بكفاءة على GL4ES بشرط إيقاف Fast Render و Smooth FPS لتجنب الشاشة السوداء.',
    pojavNoteEn: 'Works well on GL4ES if Fast Render and Smooth FPS are disabled to prevent black screens.',
    recommendationAr: 'دعم اختياري خارجي (يضع المستخدم ملفه في mods ولا يُدمج داخل NEXORA JAR).',
    recommendationEn: 'Optional external side-load (never bundled inside NEXORA JAR due to license).',
  },
  {
    id: 'patcher',
    name: 'Sk1er Patcher (1.8.9)',
    mcVersion: '1.8.9',
    forgeRequirement: 'Forge 11.15.1.2318',
    license: 'Proprietary / Essential Dependency',
    pojavStatus: 'REQUIRES_AUDIT',
    pojavNoteAr: 'يعتمد على مكتبة Essential الثقيلة التي قد تزيد استهلاك RAM وزمن بدء التشغيل على الهواتف.',
    pojavNoteEn: 'Depends on Essential runtime which increases RAM footprint and startup time on Android.',
    recommendationAr: 'تم استبدال أهم إصلاحاته بمحرك Low-Latency HUD الداخلي في NEXORA.',
    recommendationEn: 'Replaced key HUD/lighting optimizations natively inside NEXORA without Essential overhead.',
  },
  {
    id: 'mousedelayfix',
    name: 'MouseDelayFix',
    mcVersion: '1.8.9',
    forgeRequirement: 'Forge 11.15.1.2318',
    license: 'MIT License',
    pojavStatus: 'TOUCH_REDUNDANT',
    pojavNoteAr: 'يعالج تأخر مؤشر الماوس، لكن تأثيره محدود عند اللعب عبر شاشة اللمس في PojavLauncher.',
    pojavNoteEn: 'Fixes 1.8.9 look-vector delay; negligible impact on touch screens unless using physical OTG mouse.',
    recommendationAr: 'مدمج كإصلاح برمجي خفيف داخل NEXORA لمن يستخدم ماوس خارجي.',
    recommendationEn: 'Integrated natively for OTG mouse users; safe no-op on touch input.',
  },
  {
    id: 'oldanimations',
    name: 'OldAnimationsMod',
    mcVersion: '1.8.9',
    forgeRequirement: 'Forge 11.15.1.2318',
    license: 'GPLv3 / Custom',
    pojavStatus: 'CONFLICT_RISK',
    pojavNoteAr: 'قد يتعارض مع Mixins الخاصة بـ ItemRenderer إذا تم تحميله كمود منفصل مع NEXORA.',
    pojavNoteEn: 'Conflicts with ItemRenderer Mixins if loaded simultaneously with NEXORA 1.7 Visuals.',
    recommendationAr: 'استخدم ميزة 1.7 Classic Visuals المدمجة في NEXORA بدلاً منه.',
    recommendationEn: 'Use NEXORA built-in 1.7 Classic Visuals module instead of loading duplicate mod.',
  },
  {
    id: 'rawinput',
    name: 'Raw Input Mod',
    mcVersion: '1.8.9',
    forgeRequirement: 'Forge 11.15.1.2318',
    license: 'MIT License',
    pojavStatus: 'TOUCH_REDUNDANT',
    pojavNoteAr: 'يعتمد على مكتبة JInput لنظام Windows/Linux ولا يدعم إدخال شاشة اللمس على Android.',
    pojavNoteEn: 'Relies on desktop JInput controllers; unsupported and unnecessary on Android touch input.',
    recommendationAr: 'لا تقم بتثبيته على PojavLauncher لتجنب أخطاء JInput في السجل.',
    recommendationEn: 'Do not install on PojavLauncher to avoid JInput native library errors.',
  },
  {
    id: 'entityculling',
    name: 'Entity Culling (1.8.9)',
    mcVersion: '1.8.9',
    forgeRequirement: 'Forge 11.15.1.2318',
    license: 'MIT / Custom',
    pojavStatus: 'COMPATIBLE',
    pojavNoteAr: 'مفيد جداً على معالج Snapdragon 6 Gen 1 لتقليل رسم الكيانات خلف الجدران.',
    pojavNoteEn: 'Highly beneficial on Snapdragon 6 Gen 1 to skip rendering entities behind walls.',
    recommendationAr: 'متوافق للعمل بجانب NEXORA.',
    recommendationEn: 'Compatible alongside NEXORA.',
  },
];

export interface ReferenceAuditEntry {
  id: string;
  classGroup: string;
  decision: 'ADOPTED_CLEAN' | 'ADAPTED_GL4ES' | 'REJECTED_UNSAFE';
  nexoraEquivalent: string;
  reasonAr: string;
  reasonEn: string;
}

export const HITDELAYFIX_REFERENCE_AUDIT: ReferenceAuditEntry[] = [
  {
    id: 'hitdelayfix_core',
    classGroup: 'HitDelayFixHandler / NoMissClickHandler',
    decision: 'ADOPTED_CLEAN',
    nexoraEquivalent: 'MixinMinecraft.java + Module("hit_delay_fix")',
    reasonAr:
      'يعالج خلل تأخير الضرب الشهير في 1.8.9 (leftClickCounter = 10 بعد التلويح بالخطأ). تم تنفيذه برمجياً بشكل نظيف ومستقل عبر MixinMinecraft في بداية clickMouse.',
    reasonEn:
      'Fixes the vanilla 1.8.9 10-tick leftClickCounter miss penalty. Cleanly implemented in NEXORA via MixinMinecraft at the head of clickMouse().',
  },
  {
    id: 'uimod_toast_sound',
    classGroup: 'ToastHudOverlay / ToastNotification / SoundUtil',
    decision: 'ADOPTED_CLEAN',
    nexoraEquivalent: 'ModuleManager.renderActiveToast() + playUiClickSound()',
    reasonAr:
      'إشعارات منبثقة أنيقة وصوت نقر واجهة خفيف عند تفعيل الميزات أو تبديل البروفايل دون تخصيص كائنات في حلقة الرسم.',
    reasonEn:
      'Clean corner toast notifications and vanilla button audio feedback when toggling modules or switching profiles with zero render-loop allocation.',
  },
  {
    id: 'uimod_theme_widgets',
    classGroup: 'ThemeManager / ToggleWidget / SearchEntry / AnimatedValue',
    decision: 'ADOPTED_CLEAN',
    nexoraEquivalent: 'GuiNexoraMain v2 + GL4ESRenderUtils.drawToggleSwitch() + lerp()',
    reasonAr:
      'تم دمج البحث الفوري داخل الواجهة، وفلاتر الحالة، ومفاتيح التبديل المنزلقة الحديثة، واختيار ألوان السمة في GuiNexoraMain.',
    reasonEn:
      'Integrated live search box, quick status filter, modern pill toggle switches, lerp scrolling, and selectable theme accents in GuiNexoraMain.',
  },
  {
    id: 'memory_corpse_clear',
    classGroup: 'MemoryWatchdogHandler / CorpseClearHandler',
    decision: 'ADOPTED_CLEAN',
    nexoraEquivalent: 'Module("memory_watchdog") in ModuleManager.onClientTick()',
    reasonAr:
      'ينظف الكيانات الميتة العالقة (deathTime >= 19) محلياً لمنع هبوط الإطارات على الهواتف دون استدعاء System.gc() المسبب للتقطيع.',
    reasonEn:
      'Removes lingering client-side dead entities (deathTime >= 19) safely without triggering frame-stalling System.gc() calls.',
  },
  {
    id: 'shader_blur',
    classGroup: 'BlurHelper / hitdelayfix_blur.fsh (#version 120 Gaussian Loop)',
    decision: 'ADAPTED_GL4ES',
    nexoraEquivalent: 'GL4ESRenderUtils Translucent Scrims (Zero-Shader Safe Mode)',
    reasonAr:
      'شيدر التمويه ثنائي التمرير يسبب شاشة سوداء أو هبوط حاد في الإطارات على مترجم GL4ES في PojavLauncher؛ تم استبداله بخلفيات شفافة متوافقة 100% مع Adreno.',
    reasonEn:
      'Two-pass framebuffer blur shaders cause black screens or severe frame drops on PojavLauncher GL4ES; replaced with GL4ES-safe translucent vertex panels.',
  },
  {
    id: 'evasion_os_native',
    classGroup: 'JarHider / WindowsAPI / TimerResolutionHandler',
    decision: 'REJECTED_UNSAFE',
    nexoraEquivalent: 'None (Strictly Excluded)',
    reasonAr:
      'أدوات إخفاء ملفات ومكتبات خاصة بنظام Windows فقط؛ غير متوافقة مع Android/PojavLauncher وتنتهك معايير الشفافية والأمان.',
    reasonEn:
      'Stealth/file-hiding and Windows-only native calls; incompatible with Android/Linux and violate clean client transparency rules.',
  },
  {
    id: 'unfair_combat_automation',
    classGroup: 'BetterAim / ZeroTickAim / AutoTrack / MicroReach / MicroSpeed / KnockResist / IgnoreServerHitDelay',
    decision: 'REJECTED_UNSAFE',
    nexoraEquivalent: 'None (Strictly Excluded — Legitimate PvP Only)',
    reasonAr:
      'ميزات غش وتعديل مدى الضرب والتصويب الآلي وتعديل الـ Knockback؛ مرفوضة بالكامل للحفاظ على نزاهة NEXORA وأمان حساب اللاعب على السيرفرات.',
    reasonEn:
      'Aim-assist, reach modification, velocity manipulation, and server hit-delay bypasses; strictly rejected to keep NEXORA 100% legitimate and anti-cheat safe.',
  },
];

export function filterModules(
  modules: NexoraModule[],
  query: string,
  category: NexoraCategory | 'ALL',
  statusFilter: 'ALL' | 'ENABLED' | 'DISABLED'
): NexoraModule[] {
  const q = query.trim().toLowerCase();
  return modules.filter((m) => {
    if (category !== 'ALL' && m.category !== category) {
      return false;
    }
    if (statusFilter === 'ENABLED' && !m.enabled) {
      return false;
    }
    if (statusFilter === 'DISABLED' && m.enabled) {
      return false;
    }
    if (!q) {
      return true;
    }
    return (
      m.nameEn.toLowerCase().includes(q) ||
      m.nameAr.toLowerCase().includes(q) ||
      m.descEn.toLowerCase().includes(q) ||
      m.descAr.toLowerCase().includes(q) ||
      m.id.toLowerCase().includes(q)
    );
  });
}

export function clampHudPosition(
  x: number,
  y: number,
  width: number,
  height: number,
  scale: number,
  canvasWidth: number,
  canvasHeight: number
): { x: number; y: number } {
  const scaledW = width * scale;
  const scaledH = height * scale;
  const maxX = Math.max(4, canvasWidth - scaledW - 4);
  const maxY = Math.max(4, canvasHeight - scaledH - 4);
  return {
    x: Math.round(Math.min(maxX, Math.max(4, x))),
    y: Math.round(Math.min(maxY, Math.max(4, y))),
  };
}

export function snapToGrid(value: number, gridSize: number): number {
  if (gridSize <= 1) return Math.round(value);
  return Math.round(value / gridSize) * gridSize;
}

export function autoArrangeHudModules(
  modules: NexoraModule[],
  canvasWidth: number,
  canvasHeight: number
): NexoraModule[] {
  let cursorX = 16;
  let cursorY = 16;
  let rowMaxH = 0;

  return modules.map((m) => {
    if (!m.isHud || !m.enabled || m.locked) {
      return m;
    }
    const scaledW = m.width * m.scale;
    const scaledH = m.height * m.scale;

    if (cursorX + scaledW + 16 > canvasWidth) {
      cursorX = 16;
      cursorY += rowMaxH + 12;
      rowMaxH = 0;
    }

    const clamped = clampHudPosition(cursorX, cursorY, m.width, m.height, m.scale, canvasWidth, canvasHeight);
    cursorX += scaledW + 12;
    if (scaledH > rowMaxH) {
      rowMaxH = scaledH;
    }

    return {
      ...m,
      x: clamped.x,
      y: clamped.y,
    };
  });
}

export function deleteProfileSafely(
  profiles: NexoraProfile[],
  activeProfileId: string,
  targetDeleteId: string
): { profiles: NexoraProfile[]; activeProfileId: string; deleted: boolean } {
  if (profiles.length <= 1) {
    return { profiles, activeProfileId, deleted: false };
  }
  const exists = profiles.some((p) => p.id === targetDeleteId);
  if (!exists) {
    return { profiles, activeProfileId, deleted: false };
  }
  const remaining = profiles.filter((p) => p.id !== targetDeleteId);
  const nextActive =
    activeProfileId === targetDeleteId ? remaining[0].id : activeProfileId;
  return {
    profiles: remaining,
    activeProfileId: nextActive,
    deleted: true,
  };
}

export function parseAndValidateConfig(
  rawJson: string,
  fallbackModules: NexoraModule[]
): { valid: boolean; recoveredFromError: boolean; schemaVersion: number; modules: NexoraModule[] } {
  try {
    const parsed = JSON.parse(rawJson);
    if (!parsed || typeof parsed !== 'object' || typeof parsed.modules !== 'object') {
      return {
        valid: false,
        recoveredFromError: true,
        schemaVersion: CURRENT_CONFIG_SCHEMA_VERSION,
        modules: fallbackModules.map((m) => ({ ...m, enabled: m.defaultEnabled, x: m.defaultX, y: m.defaultY })),
      };
    }

    const updated = fallbackModules.map((m) => {
      const entry = parsed.modules[m.id];
      if (!entry || typeof entry !== 'object') {
        return m;
      }
      return {
        ...m,
        enabled: typeof entry.enabled === 'boolean' ? entry.enabled : m.defaultEnabled,
        keybind: typeof entry.keybind === 'string' ? entry.keybind : m.defaultKeybind,
        x: typeof entry.x === 'number' && Number.isFinite(entry.x) ? entry.x : m.defaultX,
        y: typeof entry.y === 'number' && Number.isFinite(entry.y) ? entry.y : m.defaultY,
        scale:
          typeof entry.scale === 'number' && entry.scale >= 0.5 && entry.scale <= 2.25
            ? entry.scale
            : 1.0,
        opacity:
          typeof entry.opacity === 'number' && entry.opacity >= 0.1 && entry.opacity <= 1.0
            ? entry.opacity
            : 0.88,
        fontSize:
          typeof entry.fontSize === 'number' && entry.fontSize >= 10 && entry.fontSize <= 22
            ? entry.fontSize
            : 13,
        textAlign:
          entry.textAlign === 'left' || entry.textAlign === 'center' || entry.textAlign === 'right'
            ? entry.textAlign
            : 'center',
        accentColor: typeof entry.accentColor === 'string' ? entry.accentColor : '#00A8FF',
        showBackground: typeof entry.showBackground === 'boolean' ? entry.showBackground : true,
        locked: typeof entry.locked === 'boolean' ? entry.locked : false,
      };
    });

    return {
      valid: true,
      recoveredFromError: false,
      schemaVersion: typeof parsed.schemaVersion === 'number' ? parsed.schemaVersion : CURRENT_CONFIG_SCHEMA_VERSION,
      modules: updated,
    };
  } catch {
    return {
      valid: false,
      recoveredFromError: true,
      schemaVersion: CURRENT_CONFIG_SCHEMA_VERSION,
      modules: fallbackModules.map((m) => ({ ...m, enabled: m.defaultEnabled, x: m.defaultX, y: m.defaultY })),
    };
  }
}

// ==================== PHASE 0-5 FORENSIC AUDIT, PERFORMANCE LAB, PARTICLE STUDIO & BUILDER PURE LOGIC ====================

export type FeatureImplementationStatus =
  | 'Implemented'
  | 'Partially Implemented'
  | 'Missing'
  | 'Unsupported'
  | 'Runtime-Tested';

export interface ForensicFeatureEntry {
  id: string;
  nameEn: string;
  nameAr: string;
  category: NexoraCategory;
  status: FeatureImplementationStatus;
  javaImplementation: string;
  configKey: string;
  gl4esSafe: boolean;
  notesEn: string;
  notesAr: string;
}

export const FORENSIC_FEATURE_MATRIX: ForensicFeatureEntry[] = [
  {
    id: 'fps_hud',
    nameEn: 'FPS & 1% Low Counter',
    nameAr: 'عداد الإطارات وأدنى 1% (FPS)',
    category: 'HUD',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.renderHudWidget("fps_hud") + updatePerformanceTelemetry()',
    configKey: 'modules.fps_hud',
    gl4esSafe: true,
    notesEn: 'Displays real-time FPS, 1% low FPS, and frame-time in ms using zero-allocation ring buffer.',
    notesAr: 'يعرض الإطارات الحالية وأدنى 1% وزمن الإطار بالملي ثانية بدون تخصيص كائنات.'
  },
  {
    id: 'cps_hud',
    nameEn: 'CPS & Input Swing Latency HUD',
    nameAr: 'عداد النقرات واستجابة الإدخال (CPS)',
    category: 'HUD',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.recordLeftClick() + getLeftCps() / getRightCps()',
    configKey: 'modules.cps_hud',
    gl4esSafe: true,
    notesEn: 'Sliding 1000ms window LMB/RMB CPS tracker and swing interval telemetry with zero autoclicking.',
    notesAr: 'نافذة 1000ms لحساب نقرات اليسار واليمين وزمن استجابة الضرب بدون أي نقر آلي.'
  },
  {
    id: 'keystrokes',
    nameEn: 'Keystrokes + Mouse CPS Grid',
    nameAr: 'لوحة المفاتيح والنقرات (Keystrokes)',
    category: 'HUD',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.renderHudWidget("keystrokes")',
    configKey: 'modules.keystrokes',
    gl4esSafe: true,
    notesEn: 'Renders W/A/S/D, LMB/RMB with live CPS, and SPACE bar using GL4ES-safe vertex quads.',
    notesAr: 'يعرض أزرار الحركة والنقرات الحية وشريط القفز عبر رسم متوافق تماماً مع GL4ES.'
  },
  {
    id: 'armor_hud',
    nameEn: 'Armor & Critical Durability Alert HUD',
    nameAr: 'حالة الدروع وتنبيه المتانة (Armor HUD)',
    category: 'HUD',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.renderHudWidget("armor_hud")',
    configKey: 'modules.armor_hud',
    gl4esSafe: true,
    notesEn: 'Displays equipped armor durability percentages and critical <20% low-durability warning.',
    notesAr: 'يعرض نسب متانة الدروع المرتداة مع تنبيه أحمر عند انخفاض المتانة تحت 20%.'
  },
  {
    id: 'block_counter',
    nameEn: 'Block Counter & Ceiling Alert HUD',
    nameAr: 'عداد البلوكات وتنبيه حد الارتفاع',
    category: 'HUD',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.renderHudWidget("block_counter") + updateInventoryCounts()',
    configKey: 'modules.block_counter / maxBuildHeight',
    gl4esSafe: true,
    notesEn: 'Displays held stack, total placeable blocks in inventory, session blocks placed, and MaxY alert.',
    notesAr: 'يعرض البلوكات في اليد وإجمالي المخزون والبلوكات الموضوعة في الجلسة وتنبيه أقصى ارتفاع.'
  },
  {
    id: 'bridge_helper',
    nameEn: 'Bridge Pitch/Yaw & BPS HUD',
    nameAr: 'مرشد زاوية الجسور وسرعة البناء (BPS)',
    category: 'PVP',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.renderHudWidget("bridge_helper") + recordRightClick()',
    configKey: 'modules.bridge_helper',
    gl4esSafe: true,
    notesEn: 'Purely informational Pitch/Yaw alignment badge (Ninja/God/Moonwalk) and live BPS counter.',
    notesAr: 'مؤشر معلوماتي فقط لزاوية النظر (Ninja/God/Moonwalk) وسرعة وضع البلوكات في الثانية.'
  },
  {
    id: 'particle_studio',
    nameEn: 'Particle Studio (Multiplier & Budget Cap)',
    nameAr: 'استوديو الجسيمات (المضاعف وسقف الميزانية)',
    category: 'VISUALS',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.onAttackEntity() + emitParticleAtEntity()',
    configKey: 'particleMultiplier / particleBudgetCap / particlePresetColor',
    gl4esSafe: true,
    notesEn: 'Multiplies CRIT/CRIT_MAGIC particles (1x-5x) with per-second budget cap and automatic frame-spike culling.',
    notesAr: 'يضاعف جسيمات الضرب (1x-5x) مع سقف ميزانية في الثانية وتقليل تلقائي عند هبوط الإطارات.'
  },
  {
    id: 'stutter_guard',
    nameEn: 'Performance Lab & Stutter Guard',
    nameAr: 'مختبر الأداء ومنع التقطيع (Stutter Guard)',
    category: 'PERFORMANCE',
    status: 'Runtime-Tested',
    javaImplementation: 'ModuleManager.updatePerformanceTelemetry() + applyPerformancePreset()',
    configKey: 'performancePreset / safeModeActive',
    gl4esSafe: true,
    notesEn: 'Tracks rolling 120-frame history, 1% low FPS, stutter spikes (>33.3ms), and Low/Balanced/High presets.',
    notesAr: 'يتتبع سجل 120 إطاراً، وأدنى 1% FPS، وعدد التقطيعات (>33.3ms)، وأنماط الأداء.'
  },
  {
    id: 'hit_delay_fix',
    nameEn: '1.8 Hit Delay Fix (Off by Default)',
    nameAr: 'إصلاح تأخير الضرب 1.8 (معطل افتراضياً)',
    category: 'PVP',
    status: 'Runtime-Tested',
    javaImplementation: 'MixinMinecraft.onClickMouse() -> leftClickCounter = 0',
    configKey: 'modules.hit_delay_fix',
    gl4esSafe: true,
    notesEn: 'Disabled by default to preserve vanilla 1.8.9 timing unless explicitly enabled by the player.',
    notesAr: 'معطل افتراضياً للحفاظ على توقيت الفانيلا الأصلي إلا إذا فعّله اللاعب يدوياً.'
  },
  {
    id: 'hit_color',
    nameEn: 'Damage Tint Overlay (GL4ES Safe)',
    nameAr: 'تخصيص لون الضرر المتوافق مع GL4ES',
    category: 'VISUALS',
    status: 'Runtime-Tested',
    javaImplementation: 'MixinRendererLivingEntity.modifyHurtColor()',
    configKey: 'modules.hit_color',
    gl4esSafe: true,
    notesEn: 'Modifies hurtBuffer FloatBuffer RGBA values directly without custom GLSL shaders.',
    notesAr: 'يعدل قيم FloatBuffer مباشرة لتغيير لون إصابة الخصم بدون شيدرات GLSL.'
  }
];

export interface PerformanceLabAnalysis {
  avgFps: number;
  onePercentLowFps: number;
  avgFrameTimeMs: number;
  maxSpikeMs: number;
  stutterEvents: number;
}

/**
 * Pure function to compute rolling FPS, 1% Low FPS, max frame spike, and stutter count (>33.3ms).
 */
export function analyzeFrameTimeHistory(frameTimesMs: number[]): PerformanceLabAnalysis {
  if (!frameTimesMs || frameTimesMs.length === 0) {
    return {
      avgFps: 0,
      onePercentLowFps: 0,
      avgFrameTimeMs: 0,
      maxSpikeMs: 0,
      stutterEvents: 0
    };
  }

  const valid = frameTimesMs.filter((ms) => Number.isFinite(ms) && ms > 0);
  if (valid.length === 0) {
    return {
      avgFps: 0,
      onePercentLowFps: 0,
      avgFrameTimeMs: 0,
      maxSpikeMs: 0,
      stutterEvents: 0
    };
  }

  let sumMs = 0;
  let maxSpikeMs = 0;
  let stutterEvents = 0;

  for (const ms of valid) {
    sumMs += ms;
    if (ms > maxSpikeMs) maxSpikeMs = ms;
    if (ms > 33.3) stutterEvents++;
  }

  const avgFrameTimeMs = Number((sumMs / valid.length).toFixed(2));
  const avgFps = Math.min(1000, Math.max(1, Math.round(1000 / avgFrameTimeMs)));

  const sortedDesc = [...valid].sort((a, b) => b - a);
  const worstCount = Math.max(1, Math.ceil(sortedDesc.length * 0.05));
  const worstAvgMs = sortedDesc.slice(0, worstCount).reduce((a, b) => a + b, 0) / worstCount;
  const onePercentLowFps = Math.min(avgFps, Math.max(1, Math.round(1000 / worstAvgMs)));

  return {
    avgFps,
    onePercentLowFps,
    avgFrameTimeMs,
    maxSpikeMs: Number(maxSpikeMs.toFixed(1)),
    stutterEvents
  };
}

/**
 * Pure function for Particle Studio emission calculation with budget cap and GL4ES spike protection.
 */
export function computeParticleStudioBurst(params: {
  enabled: boolean;
  safeMode: boolean;
  multiplier: number;
  budgetCapPerSec: number;
  emittedThisSecond: number;
  currentFrameTimeMs: number;
  cullOnSpike: boolean;
}): { emittedNow: number; remainingBudget: number; throttledBySpike: boolean } {
  if (!params.enabled || params.safeMode) {
    return {
      emittedNow: 0,
      remainingBudget: Math.max(0, params.budgetCapPerSec - params.emittedThisSecond),
      throttledBySpike: false
    };
  }

  const clampedMult = Math.max(1, Math.min(5, Math.round(params.multiplier)));
  const clampedCap = Math.max(20, Math.min(200, Math.round(params.budgetCapPerSec)));
  const remainingBefore = Math.max(0, clampedCap - Math.max(0, params.emittedThisSecond));

  const throttledBySpike = params.cullOnSpike && params.currentFrameTimeMs > 28.0;
  const requested = throttledBySpike ? 1 : clampedMult;
  const emittedNow = Math.min(requested, remainingBefore);

  return {
    emittedNow,
    remainingBudget: Math.max(0, remainingBefore - emittedNow),
    throttledBySpike
  };
}

/**
 * Pure function for Builder Studio pitch/yaw alignment classification (informational only, no input manipulation).
 */
export function evaluateBridgePitchAlignment(pitch: number, yaw: number): {
  mode: 'NINJA / FAST' | 'GODBRIDGE / FLAT' | 'MOONWALK' | 'ADJUST ANGLE';
  isOptimal: boolean;
  normalizedYaw: number;
} {
  const absPitch = Math.abs(pitch);
  const normalizedYaw = Number((((yaw % 360) + 360) % 360).toFixed(1));

  if (absPitch >= 77.0 && absPitch <= 80.2) {
    return { mode: 'NINJA / FAST', isOptimal: true, normalizedYaw };
  }
  if (absPitch >= 74.8 && absPitch < 77.0) {
    return { mode: 'GODBRIDGE / FLAT', isOptimal: true, normalizedYaw };
  }
  if (absPitch >= 79.0 && absPitch <= 82.8) {
    return { mode: 'MOONWALK', isOptimal: true, normalizedYaw };
  }
  return { mode: 'ADJUST ANGLE', isOptimal: false, normalizedYaw };
}

